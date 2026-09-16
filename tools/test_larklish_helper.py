"""Offline regression checks: python3 -m unittest discover -s tools -p 'test_*.py'."""

import argparse
import contextlib
import io
import json
import os
import pathlib
import subprocess
import sys
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch

import larklish_helper as h


def relay(at="2026-09-15T12:00:00Z", key="key", **extra):
    return dict(
        event="relayed",
        at=at,
        key=key,
        title="测试",
        text="Sender: 中文...",
        relayTitle="Test",
        relayText="Sender: Chinese...",
        truncated=True,
        **extra,
    )


def update(at="2026-09-15T12:00:04Z", key="key", **extra):
    return dict(
        event="updated",
        at=at,
        key=key,
        msgType="text",
        fullText="中文消息",
        relayText="Chinese message",
        **extra,
    )


def options(**extra):
    return argparse.Namespace(
        **dict(
            {
                "since": None,
                "until": None,
                "han": False,
                "exclude_test_group": False,
                "no_removed": False,
                "utc": True,
                "group_by": None,
                "json": False,
            },
            **extra,
        )
    )


class EventsTests(unittest.TestCase):
    def test_han_keeps_update_and_filters_english_cohort(self):
        english = dict(relay(key="english"), title="English", text="Sender: English...")
        report = h.soak_report(
            [relay(), english, update(), update(key="english")], options(han=True)
        )
        self.assertEqual(
            (report["summary"]["cut"], report["summary"]["updated"]), (1, 1)
        )
        self.assertEqual(report["summary"]["latencySeconds"]["median"], 4)

    def test_test_group_filter_keeps_other_outcomes(self):
        test = dict(relay(key="test"), title=h.TEST_TITLE)
        report = h.soak_report(
            [test, relay(), update(key="test"), update()],
            options(exclude_test_group=True),
        )
        self.assertEqual(report["summary"]["updated"], 1)
        self.assertEqual(report["raw"]["events"], 2)

    def test_unknown_truncation_and_missing_outcome(self):
        old = relay(key="old")
        old.pop("truncated")
        report = h.soak_report([old, relay(), update(key="old")], options())
        self.assertEqual(report["summary"]["unknown"], 1)
        self.assertEqual(report["summary"]["outcomes"], {"no recorded outcome": 1})
        self.assertEqual(report["summary"]["latencySeconds"]["count"], 0)

    def test_offset_boundaries_and_exclusive_end(self):
        report = h.soak_report(
            [relay(), update()],
            options(since="2026-09-15T05:00:00-07:00", until="2026-09-15T12:00:04Z"),
        )
        self.assertEqual(report["summary"]["relays"], 1)
        self.assertEqual(report["summary"]["updated"], 0)
        report = h.soak_report(
            [relay(), update()], options(since="2026-09-15T12:00:01Z")
        )
        self.assertEqual(report["summary"]["relays"], 0)
        self.assertEqual(report["raw"]["events"], 1)

    def test_reversed_window_rejected(self):
        with self.assertRaises(ValueError):
            h.soak_report([], options(since="2026-09-16", until="2026-09-15"))

    def test_same_key_repost_is_not_double_counted(self):
        newer = relay(at="2026-09-15T12:00:02Z")
        report = h.soak_report([relay(), newer, update()], options())
        self.assertEqual(report["summary"]["updated"], 1)
        self.assertEqual(report["summary"]["outcomes"]["no recorded outcome"], 1)
        self.assertEqual(report["summary"]["latencySeconds"]["median"], 2)

    def test_flow_id_keeps_late_outcome_with_older_original(self):
        older = relay(flowId="old")
        newer = relay(at="2026-09-15T12:00:02Z", flowId="new")
        late = update(at="2026-09-15T12:00:04Z", flowId="old")
        timing = {
            "event": "timing",
            "at": "2026-09-15T12:00:05Z",
            "key": "key",
            "flowId": "old",
            "outcome": "updated",
            "totalMs": 4000,
            "spans": [{"name": "original_to_relay", "ms": 100}],
        }
        paired = h.relays_with_outcomes([older, newer, late, timing])
        self.assertEqual(paired[0]["outcome"], "updated (text)")
        self.assertEqual(paired[1]["outcome"], "no recorded outcome")
        rows = h.timing_rows(
            [older, newer, late, timing], options(flow_id=None, limit=10)
        )
        self.assertEqual(rows[0]["spans"][0]["ms"], 100)
        self.assertNotIn("text", rows[0])

    def test_old_record_has_no_timing_rows(self):
        self.assertEqual(
            h.timing_rows([relay(), update()], options(flow_id=None, limit=10)), []
        )

    def test_raw_errors_differ_from_paired_outcomes(self):
        skipped = {
            "event": "skipped",
            "at": "2026-09-15T12:00:01Z",
            "key": "other",
            "reason": "error: Read timed out",
        }
        report = h.soak_report([relay(), skipped], options())
        self.assertEqual(report["raw"]["errors"], 1)
        self.assertEqual(report["raw"]["errorKinds"], {"read timeout": 1})
        self.assertEqual(report["summary"]["outcomes"], {"no recorded outcome": 1})

    def test_network_backend_and_day_groups(self):
        rows = [
            relay(network={"wifiConnected": False, "vpn": False}),
            update(backend="https://example.test"),
        ]
        network = h.soak_report(rows, options(group_by="network"))
        self.assertIn("no Wi-Fi; VPN off", network["groups"])
        self.assertNotIn("cellular", str(network))
        self.assertIn(
            "https://example.test",
            h.soak_report(rows, options(group_by="backend"))["groups"],
        )
        self.assertIn(
            "2026-09-15", h.soak_report(rows, options(group_by="day"))["groups"]
        )
        self.assertEqual(
            h.grade_group(relay(), "network", True), "unknown Wi-Fi; VPN unknown"
        )
        self.assertEqual(h.grade_group(relay(), "backend", True), "unknown")

    def test_latency_nearest_rank_and_empty(self):
        self.assertEqual(
            h.latency_stats([1, 2, 3, 4]),
            {"count": 4, "median": 2.5, "p90": 4, "max": 4},
        )
        self.assertIsNone(h.latency_stats([])["median"])

    def test_jsonl_unicode_line_separator_and_cli_json(self):
        with tempfile.TemporaryDirectory() as folder:
            path = pathlib.Path(folder) / "events.jsonl"
            path.write_text(
                json.dumps(dict(relay(), text="中文\u2028消息"), ensure_ascii=False)
                + "\n"
                + json.dumps(update())
                + "\n"
            )
            self.assertEqual(len(h.read_events(str(path))), 2)
            result = subprocess.run(
                [
                    sys.executable,
                    str(h.REPO / "tools/larklish-helper"),
                    "events",
                    "--file",
                    str(path),
                    "--han",
                    "grade",
                    "--json",
                ],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(json.loads(result.stdout)["summary"]["updated"], 1)


class ProbeTests(unittest.TestCase):
    marker = "[probe:12345678]"
    text = marker + " 中文消息"

    def rows(self, full=None):
        return [
            dict(relay(), text="Sender: " + self.text),
            dict(update(), fullText=full or self.text),
        ]

    def test_marker_ignores_unrelated_traffic(self):
        self.assertEqual(
            h.probe_observation([relay(), update()], self.marker, self.text, "updated")[
                "outcome"
            ],
            "no matching Relay observed",
        )
        report = h.probe_observation(self.rows(), self.marker, self.text, "updated")
        self.assertTrue(report["ok"])
        self.assertEqual(report["relayToUpdateSeconds"], 4)

    def test_wrong_full_text_fails_update(self):
        report = h.probe_observation(
            self.rows(full="unrelated message"), self.marker, self.text, "updated"
        )
        self.assertFalse(report["ok"])
        self.assertFalse(report["fullTextMatches"])

    def test_reply_marker_selects_the_thread_reply(self):
        root_marker = "[probe:root0001]"
        reply_marker = "[probe:reply001]"
        reply_text = reply_marker + " 回复：中文消息"
        rows = [
            dict(relay(key="root"), text="Sender: " + root_marker + " 中文消息"),
            dict(update(key="root"), fullText=root_marker + " 中文消息"),
            dict(relay(key="reply"), text="Sender: " + reply_text),
            dict(update(key="reply"), fullText=reply_text),
        ]

        report = h.probe_observation(rows, reply_marker, reply_text, "updated")

        self.assertTrue(report["ok"])
        self.assertEqual(report["key"], "reply")
        self.assertTrue(report["fullTextMatches"])

    def test_complete_preview_passes_relay_but_not_update(self):
        rows = [
            dict(self.rows()[0], truncated=False),
            {
                "event": "skipped",
                "key": "key",
                "at": "2026-09-15T12:00:01Z",
                "reason": "not-truncated",
            },
        ]
        self.assertTrue(
            h.probe_observation(rows, self.marker, self.text, "relayed")["ok"]
        )
        self.assertFalse(
            h.probe_observation(rows, self.marker, self.text, "updated")["ok"]
        )
        with (
            patch.object(h, "read_events", return_value=rows),
            patch.object(h.time, "sleep") as sleep,
        ):
            self.assertFalse(
                h.wait_for_probe(0, self.marker, self.text, "updated", 60)["ok"]
            )
            sleep.assert_not_called()

    def test_wait_stops_on_success_and_ignores_baseline(self):
        baseline = self.rows()
        with (
            patch.object(
                h, "read_events", side_effect=[baseline, baseline + self.rows()]
            ),
            patch.object(h.time, "sleep") as sleep,
        ):
            self.assertTrue(
                h.wait_for_probe(len(baseline), self.marker, self.text, "updated", 60)[
                    "ok"
                ]
            )
            sleep.assert_called_once()

    def test_auto_requires_known_cut_or_complete_outcome(self):
        self.assertTrue(
            h.probe_observation(self.rows(), self.marker, self.text, "auto")["ok"]
        )
        unknown = self.rows()
        unknown[0].pop("truncated")
        self.assertFalse(
            h.probe_observation(unknown, self.marker, self.text, "auto")["ok"]
        )
        complete = [
            dict(self.rows()[0], truncated=False),
            {
                "event": "skipped",
                "key": "key",
                "at": "2026-09-15T12:00:01Z",
                "reason": "not-truncated",
            },
        ]
        self.assertTrue(
            h.probe_observation(complete, self.marker, self.text, "auto")["ok"]
        )

    def test_thread_observes_reply_marker_and_prints_only_summary(self):
        args = argparse.Namespace(
            text="private synthetic text",
            debug=None,
            expect="auto",
            thread=True,
            idle=False,
            timeout=45,
        )
        output = io.StringIO()
        with (
            patch.object(h, "require_device"),
            patch.object(h, "read_events", return_value=[]),
            patch.object(h, "send_test_message", side_effect=["root", "reply"]) as send,
            patch.object(
                h,
                "wait_for_probe",
                return_value={"ok": True, "outcome": "updated (text)"},
            ) as wait,
            patch.object(h.time, "sleep"),
            contextlib.redirect_stdout(output),
        ):
            h.cmd_probe(args)
        first, second = send.call_args_list
        self.assertNotEqual(first.args[0].split()[0], second.args[0].split()[0])
        self.assertEqual(second.kwargs, {"reply_to": "root"})
        self.assertEqual(wait.call_args.args[1], second.args[0].split()[0])
        self.assertEqual(json.loads(output.getvalue())["messageId"], "reply")
        self.assertNotIn("private synthetic text", output.getvalue())

    def test_deadline_is_failure(self):
        with (
            patch.object(h, "read_events", return_value=[]),
            patch.object(h.time, "monotonic", side_effect=[0, 61]),
        ):
            self.assertFalse(
                h.wait_for_probe(0, self.marker, self.text, "updated", 60)["ok"]
            )

    def test_idle_restored_on_send_error_and_logs_not_cleared(self):
        args = argparse.Namespace(
            text="中文",
            debug=None,
            expect="updated",
            thread=False,
            idle=True,
            timeout=12,
        )
        with (
            patch.object(h, "require_device"),
            patch.object(h, "read_events", return_value=[]),
            patch.object(h, "adb") as adb,
            patch.object(
                h, "send_test_message", side_effect=RuntimeError("send failed")
            ),
        ):
            with self.assertRaises(RuntimeError):
                h.cmd_probe(args)
            self.assertEqual(
                adb.call_args.args, ("shell", "dumpsys", "deviceidle", "unforce")
            )
            self.assertFalse(
                any(call.args[0] == "logcat" for call in adb.call_args_list)
            )

    def test_failure_is_json_and_nonzero(self):
        args = argparse.Namespace(
            text="中文",
            debug=None,
            expect="updated",
            thread=False,
            idle=False,
            timeout=12,
        )
        out = io.StringIO()
        with (
            patch.object(h, "require_device"),
            patch.object(h, "read_events", return_value=[]),
            patch.object(h, "send_test_message", return_value="message-id"),
            patch.object(
                h,
                "wait_for_probe",
                return_value={"ok": False, "outcome": "no matching Relay observed"},
            ),
            patch.object(h, "phone_status", return_value={"lark": "not running"}),
            contextlib.redirect_stdout(out),
            self.assertRaises(SystemExit) as error,
        ):
            h.cmd_probe(args)
        self.assertEqual(error.exception.code, 1)
        self.assertEqual(json.loads(out.getvalue())["messageId"], "message-id")


class PhoneTests(unittest.TestCase):
    def test_connectivity_uses_current_networks_not_request_history(self):
        dump = """Active default network: 101
Current Networks:
  NetworkAgentInfo{network{100} handle{1} ni{CELLULAR CONNECTED} nc{[ Transports: CELLULAR Capabilities: INTERNET ]}}
  NetworkAgentInfo{network{101} handle{2} ni{WIFI CONNECTED} nc{[ Transports: WIFI Capabilities: INTERNET ]}}
  NetworkAgentInfo{network{102} handle{3} ni{VPN CONNECTED} nc{[ Transports: WIFI|VPN Capabilities: INTERNET ]}}
Status for known UIDs:
NetworkAgentInfo{network{999} nc{[ Transports: VPN Capabilities: INTERNET ]}}
"""
        self.assertEqual(
            h.connectivity_status(dump),
            {
                "default network": "101",
                "default transport": "WIFI",
                "active VPN networks": "102",
            },
        )
        self.assertEqual(h.connectivity_status("")["default transport"], "unknown")
        self.assertEqual(
            h.connectivity_status(
                "Active default network: none\nCurrent Networks:\n\nStatus for known UIDs:"
            )["default transport"],
            "none",
        )


class BackendTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.requests = []
        cls.mode = "found"

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def do_GET(self):
                if self.path == "/v1/ping":
                    self.send_response(200)
                    self.end_headers()
                    self.wfile.write(b"pong test")
                elif self.headers.get("Authorization") == "Bearer backend-secret":
                    self.send_response(200)
                    self.end_headers()
                    self.wfile.write(b"{}")
                else:
                    self.send_response(401)
                    self.end_headers()

            def do_POST(self):
                body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                cls.requests.append((self.path, dict(self.headers), body))
                if cls.mode == "unauthorized":
                    self.send_response(401)
                    self.end_headers()
                    self.wfile.write(b"user-secret backend-secret")
                    return
                self.send_response(200)
                self.end_headers()
                if cls.mode == "malformed":
                    self.wfile.write(b"not JSON user-secret")
                else:
                    self.wfile.write(
                        json.dumps(
                            {
                                "outcome": cls.mode,
                                "reason": "no-chat" if cls.mode == "skipped" else None,
                                "msgType": "text",
                                "fullText": "中文",
                                "english": None if cls.mode == "skipped" else "English",
                            }
                        ).encode()
                    )

        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()

    def setUp(self):
        type(self).mode = "found"
        self.requests.clear()
        self.env = dict(
            os.environ,
            LARKLISH_BACKEND=f"http://127.0.0.1:{self.server.server_port}",
            LARKLISH_BACKEND_TOKEN="backend-secret",
            LARKLISH_USER_TOKEN="user-secret",
        )
        self.event = dict(relay(at="2023-11-14T22:13:20.123Z"), title="Test")
        self.case = {
            "title": "Test",
            "text": "Sender: 中文...",
            "whenMs": 1700000000123,
        }

    def command(self, *args, env=None):
        return subprocess.run(
            [sys.executable, str(h.REPO / "tools/larklish-helper"), "backend", *args],
            env=env or self.env,
            capture_output=True,
            text=True,
            check=False,
        )

    def test_lookup_cli_preserves_timestamp_and_credentials_stay_out_of_output(self):
        with tempfile.TemporaryDirectory() as folder:
            path = pathlib.Path(folder) / "original.json"
            path.write_text(json.dumps(self.event) + "\n")
            result = self.command("lookup", "--file", str(path), "--repeat", "2")
        self.assertEqual(result.returncode, 0, result.stderr)
        report = json.loads(result.stdout)
        self.assertEqual(len(report["runs"]), 2)
        self.assertTrue(report["runs"][0]["translationAvailable"])
        for route, headers, body in self.requests:
            self.assertEqual(route, "/lookup")
            self.assertEqual(body, dict(self.case, userToken="user-secret"))
            self.assertEqual(headers["Authorization"], "Bearer backend-secret")
        self.assertNotIn("secret", result.stdout + result.stderr)

    def test_status_failure_exits_nonzero_and_json_is_parseable(self):
        result = self.command(
            "status", "--json", env=dict(self.env, LARKLISH_BACKEND_TOKEN="wrong")
        )
        self.assertEqual(result.returncode, 1)
        self.assertFalse(json.loads(result.stdout)["ok"])
        self.assertEqual(self.command("status", "--json").returncode, 0)

    def test_lookup_skip_http_failure_and_malformed_body(self):
        with patch.dict(os.environ, self.env):
            for mode in ("skipped", "unauthorized", "malformed"):
                type(self).mode = mode
                result = h.backend_lookup(self.event, 30)
                self.assertFalse(result["ok"])
                self.assertNotIn("secret", json.dumps(result))
                if mode == "unauthorized":
                    self.assertEqual(result["httpStatus"], 401)

    def test_phone_token_is_read_only_and_expired_token_stops(self):
        with (
            patch.dict(os.environ, {"LARKLISH_USER_TOKEN": ""}),
            patch.object(
                h,
                "read_phone_file",
                return_value=json.dumps(
                    {"accessToken": "old", "expiresAt": 1}
                ).encode(),
            ) as read,
        ):
            with self.assertRaises(SystemExit):
                h.phone_user_token()
            read.assert_called_once_with("files/user-token.json")
        with (
            patch.dict(os.environ, self.env),
            patch.object(h, "read_phone_file") as read,
        ):
            self.assertEqual(h.phone_user_token(), "user-secret")
            read.assert_not_called()

    def test_bad_repeat_is_rejected_before_reading_phone(self):
        result = self.command("lookup", "--repeat", "0")
        self.assertEqual(result.returncode, 2)
        self.assertIn("--repeat must be positive", result.stderr)


if __name__ == "__main__":
    unittest.main()
