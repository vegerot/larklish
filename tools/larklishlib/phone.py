"""The phone over adb: the app's files, its log, its debug hooks."""

import json
import pathlib
import re
import shlex
import subprocess
import sys
from typing import Any

PACKAGE = "com.vegerot.larklish"
LOG_TAGS = ["LarkListener:I", "MessageFetcher:I", "Translator:I", "FallbackTranslator:W", "*:S"]

Event = dict[str, Any]      # one line of events.jsonl (Recorder.kt): `event`, `at`, `key`, …
ChatCache = dict[str, str]  # chats.json (MessageFetcher.kt): title or `dm:<Sender>` → chat id


def run(args: list[str], **kw: Any) -> str:
    """Run a command and fail loudly with whatever it said."""
    out = subprocess.run(args, capture_output=True, text=True, **kw)
    if out.returncode != 0:
        sys.exit(f"{args[0]}: {(out.stderr or out.stdout).strip()}")
    return out.stdout


def adb(*args: str, allow_fail: bool = False) -> subprocess.CompletedProcess[str]:
    out = subprocess.run(["adb", *args], capture_output=True, text=True)
    if out.returncode != 0 and not allow_fail:
        sys.exit(f"adb {' '.join(args)}: {(out.stderr or out.stdout).strip()}")
    return out


def require_device() -> None:
    """`adb logcat` waits forever when no phone is attached, so check before anything blocks."""
    out = subprocess.run(["adb", "devices"], capture_output=True, text=True)
    if not [ln for ln in out.stdout.splitlines()[1:] if ln.strip().endswith("\tdevice")]:
        sys.exit("no phone on adb — check the cable, then `adb devices`")


def read_events(path: str | None = None) -> list[Event]:
    """`filesDir/events.jsonl` from the phone, or a saved copy."""
    if path:
        text = pathlib.Path(path).read_text(encoding="utf-8")
    else:
        require_device()
        text = adb("exec-out", "run-as", PACKAGE, "cat", "files/events.jsonl").stdout
    return [json.loads(line) for line in text.splitlines() if line.strip()]


def read_chat_cache(path: str | None = None) -> ChatCache:
    """`filesDir/chats.json` from the phone, or a saved copy. Empty when nothing has been resolved yet."""
    if path:
        raw = pathlib.Path(path).read_text(encoding="utf-8")
    else:
        # `adb exec-out` folds the device's stderr into stdout, so a missing file arrives as text.
        raw = adb("exec-out", "run-as", PACKAGE, "cat", "files/chats.json", allow_fail=True).stdout
    raw = raw.strip()
    return json.loads(raw) if raw.startswith("{") else {}


def debug_hook(what: str, title: str, text: str) -> None:
    """A MainActivity debug hook: user, refresh, fetch or thread."""
    # `adb shell` hands one string to the phone's shell, which splits it again: a title with a
    # space silently truncates unless every value is quoted for *that* shell. CLEAR_TOP|NEW_TASK
    # because MainActivity reads the hook in onCreate — a running instance must be recreated.
    adb("shell", " ".join([
        "am", "start", "-f", "0x10008000", "-n", f"{PACKAGE}/.MainActivity",
        "--es", "debug", shlex.quote(what),
        "--es", "title", shlex.quote(title),
        "--es", "text", shlex.quote(text),
    ]))


def print_log() -> None:
    """The app's logcat so far, time + level + tag + message."""
    for line in adb("logcat", "-d", "-s", *LOG_TAGS).stdout.splitlines():
        parts = line.split(None, 5)  # date time pid tid level tag: message
        print(" ".join(parts[1:2] + parts[4:]) if len(parts) > 5 else line)


def top() -> str:
    """The foreground activity."""
    dump = adb("shell", "dumpsys", "activity", "activities").stdout
    m = re.search(r"topResumedActivity=ActivityRecord\{\S+ u0 (\S+)", dump)
    return m.group(1) if m else "?"


def shot(path: str) -> None:
    with open(path, "wb") as f:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], check=True, stdout=f)
