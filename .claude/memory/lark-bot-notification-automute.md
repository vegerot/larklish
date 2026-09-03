---
name: lark-bot-notification-automute
description: Lark silently mutes a bot's notifications in a group after a burst of bot messages; probes then produce no Originals. How to see it and undo it.
metadata:
  type: project
---

On 2026-09-02, after ~6 bot messages in an hour to `Larklish 测试群`, Lark turned off
notifications from `Max Coplan's Feishu CLI` in that group "to reduce distractions". Every
later `tools/larklish-helper probe` produced no Original at all, while colleagues' messages
still relayed. No API reads this state; the chat is not "muted" (no bell-slash in the list).

**How to see it:** open the group; a banner reads "Notifications from '<bot>' are off in this
group … turn them back on in the group bot list". On the phone the same banner shows at the top
of the chat.

**How to undo it (Lark desktop):** open the group → the banner's "View Bot List" (or ⋯ → Bots)
→ **right-click** the bot → "Unmute Bot Notifications". Left-clicking the bell-slash only opens
the bot's details. A toast "Notifications on" confirms; the group's unread count jumps by the
silenced messages.

**Why it matters:** a silent probe looks like a listener or push failure. Check this banner
before rebooting phones. Related: [[larklish-test-phone]].
