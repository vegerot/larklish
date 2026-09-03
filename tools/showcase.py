"""Showcase: post one message to the test group as the bot, then crop Lark's Original and
Larklish's Relay out of the shade and put them side by side in docs/showcase/.

    python3 tools/showcase.py <slug> "<message>" "<caption>"          # send, capture, compose
    python3 tools/showcase.py <slug> "<message>" "<caption>" --no-send  # the pair is in the shade already

Lark cuts a message at 45 characters only on the push path (Lark's process idle for hours, or
`adb shell dumpsys deviceidle force-idle`; `unforce` after). With Lark recently in the foreground
the Original arrives whole and the Backend is never asked (docs/progress.md 2026-09-03).
"""
import os
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

sys.path.insert(0, "tools")
from larklish_helper import adb, send_test_message, shot  # noqa: E402
from PIL import Image, ImageDraw, ImageFont  # noqa: E402

OUT = "docs/showcase"
ROW = "com.android.systemui:id/expandableNotificationRow"
SCREEN_BOTTOM = 2261  # the shade's scroller ends here (below it: Clear all)


def dump():
    raw = subprocess.run(["adb", "exec-out", "uiautomator", "dump", "/dev/tty"],
                         capture_output=True, text=True).stdout
    raw = raw[raw.find("<?xml"):]
    raw = raw[: raw.rfind(">") + 1]
    root = ET.fromstring(raw)
    parent = {c: p for p in root.iter() for c in p}
    return root, parent


def bounds(node):
    b = node.get("bounds")  # [l,t][r,b]
    left, t, r, bt = (int(x) for x in b.replace("][", ",").strip("[]").split(","))
    return left, t, r, bt


def tap(node):
    left, t, r, b = bounds(node)
    adb("shell", "input", "tap", str((left + r) // 2), str((t + b) // 2))
    time.sleep(1.5)


def rows_of(node, parent):
    """Ancestors that are notification rows, innermost first."""
    out = []
    while node is not None:
        if node.get("resource-id") == ROW:
            out.append(node)
        node = parent.get(node)
    return out


def find(root, **attrs):
    for n in root.iter("node"):
        if all(n.get(k) == v for k, v in attrs.items()):
            return n
    return None


def button(container, desc):
    for n in container.iter("node"):
        if n.get("class") == "android.widget.Button" and n.get("content-desc") == desc:
            return n
    return None


def scroll_up_by(px):
    adb("shell", "input", "swipe", "540", str(1200 + px // 2), "540", str(1200 - px // 2), "300")
    time.sleep(1.2)


def shade_open():
    root, parent = dump()
    if find(root, **{"resource-id": "com.android.systemui:id/notification_stack_scroller"}) is None:
        adb("shell", "cmd", "statusbar", "expand-notifications")
        time.sleep(2.5)
        root, parent = dump()
    return root, parent


def locate(app, title):
    """The row that shows `title`, and the group row around it (the same row when standalone).
    A collapsed group lists only its first lines, so open the app's group first; uiautomator
    lists only what is on screen, so scroll when it is still missing."""
    for attempt in range(5):
        root, parent = shade_open()
        node = find(root, text=title)
        if node is not None:
            rows = rows_of(node, parent)
            return rows[0], rows[-1]
        header = find(root, **{"resource-id": "android:id/app_name_text", "text": app})
        if header is not None and (b := button(rows_of(header, parent)[-1], "Expand")) is not None \
                and bounds(b)[1] < bounds(header)[3] + 100:
            tap(b)  # the collapsed group's chevron
        else:
            scroll_up_by(900)
    raise AssertionError(f"no {title!r} under {app} in the shade")


def header_button(group):
    """The chevron in the header of a group or a standalone notification."""
    for n in group.iter("node"):
        if n.get("class") == "android.widget.Button" and bounds(n)[1] < bounds(group)[1] + 200:
            return n
    return None


def capture(app, title, out_png):
    child, group = locate(app, title)
    hb = header_button(group)
    if hb is not None and hb.get("content-desc") == "Expand":
        tap(hb)  # a collapsed group's chevron, or a standalone notification's
        child, group = locate(app, title)
    if child is not group and (b := button(child, "Expand")) is not None:
        tap(b)  # the chat row's own chevron: the full text
        child, group = locate(app, title)
    gl, gt, gr, _ = bounds(group)
    _, _, _, cb = bounds(child)
    if cb > SCREEN_BOTTOM - 60:
        scroll_up_by(cb - SCREEN_BOTTOM + 200)
        child, group = locate(app, title)
        gl, gt, gr, _ = bounds(group)
        _, _, _, cb = bounds(child)
    full = out_png.replace(".png", "-full.png")
    shot(full)
    Image.open(full).crop((gl, gt, gr, cb)).save(out_png)
    print(f"{app}: {out_png} {gr - gl}x{cb - gt}")
    if (b := button(group, "Collapse")) is not None:
        tap(b)  # so the next group moves up


GAP, PAD, RADIUS = 48, 56, 56
BG = (246, 247, 249)
INK = (32, 33, 36)


def rounded(card):
    mask = Image.new("L", card.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, card.width - 1, card.height - 1), RADIUS, fill=255)
    out = Image.new("RGBA", card.size, BG + (255,))
    out.paste(card, (0, 0), mask)
    return out


def compose(slug, caption):
    lark = rounded(Image.open(f"{OUT}/{slug}-lark.png").convert("RGBA"))
    larklish = rounded(Image.open(f"{OUT}/{slug}-larklish.png").convert("RGBA"))
    font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 44)
    small = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 36)
    top = PAD + 70 + 40  # caption line, label line
    h = top + max(lark.height, larklish.height) + PAD
    w = PAD + lark.width + GAP + larklish.width + PAD
    page = Image.new("RGB", (w, h), BG)
    d = ImageDraw.Draw(page)
    d.text((PAD, PAD), caption, fill=INK, font=font)
    d.text((PAD, PAD + 70), "What Lark showed", fill=(95, 99, 104), font=small)
    d.text((PAD + lark.width + GAP, PAD + 70), "What Larklish showed", fill=(95, 99, 104), font=small)
    page.paste(lark, (PAD, top), lark)
    page.paste(larklish, (PAD + lark.width + GAP, top), larklish)
    page.save(f"{OUT}/{slug}.png")
    print(f"{OUT}/{slug}.png {w}x{h}")


def main():
    slug, text, caption = sys.argv[1], sys.argv[2], sys.argv[3]
    if "--no-send" not in sys.argv:
        adb("shell", "cmd", "statusbar", "collapse")
        print("sent", send_test_message(text))
        time.sleep(14)  # Relay in ~1 s, the Update through the Backend a few seconds later
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(1)
    adb("shell", "cmd", "statusbar", "expand-notifications")
    time.sleep(2.5)
    capture("Lark", "Larklish 测试群", f"{OUT}/{slug}-lark.png")
    capture("Larklish", "Larklish test group", f"{OUT}/{slug}-larklish.png")
    adb("shell", "cmd", "statusbar", "collapse")
    for f in (f"{OUT}/{slug}-lark-full.png", f"{OUT}/{slug}-larklish-full.png"):
        os.remove(f)  # the whole shade: colleagues' notifications are in it
    compose(slug, caption)


if __name__ == "__main__":
    main()
