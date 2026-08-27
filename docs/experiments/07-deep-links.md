# 🧪 Experiment 07 — Deep links: how Lark's tap works, and what we can build ourselves

Date: 2026-08-27. Zero code: `logcat`, `dumpsys`, `am start`, `tools/phone`, Open Platform
docs. Question (Max): the Original has no reference to the message. How does Lark's own
tap reach the right place, and could a Relay open the exact message, thread, or mention?

## How Lark's own tap works

Tapping an Original starts, in order (`ActivityTaskManager` log):

1. `com.ss.android.lark.notification.export.transfer.NotificationTransferActivity (has extras)`
2. `com.ss.android.lark.main.app.MainActivity (has extras)`
3. the chat: `nexus.NexusBotActivity` (bot DM), `chatwindow.ChatWindowActivity` (person or
   group), or `threadwindow.ThreadWindowActivity` (topic-mode group)

The target lives in the intent **extras** of step 1. Android never prints extras
(`dumpsys activity intents`, `logcat`, root or not), and a `PendingIntent` is opaque to
the app that holds it. Lark logs nothing about the route. So the message id inside is
unreadable without hooking Lark's process. The Relay copies the `PendingIntent` whole,
which is why its tap already lands where Lark's would.

## AppLinks: the documented deep links, fired by us

Lark registers schemes `feishu:`, `x-lark:`, `http(s)` for hosts `applink.feishu.cn` and
`applink.larksuite.com`. Every form below, sent with
`am start -a android.intent.action.VIEW -d URL` (`tools/phone open URL`), opened Lark:

| AppLink | Opened |
| --- | --- |
| `https://applink.feishu.cn/client/chat/open?openChatId=oc_…` (group, topic mode) | `ThreadWindowActivity`, the right group ✅ |
| same via `applink.larksuite.com`, `lark://applink/…`, `feishu://applink/…` | same ✅ |
| `…/client/chat/open?openChatId=oc_…` (normal group 【高优】) | `ChatWindowActivity` ✅ |
| `…/client/chat/open?openId=ou_…` (a person) | `ChatWindowActivity`, the DM ✅ |
| `…/client/bot/open?appId=cli_…` | `NexusBotActivity`, the bot DM ✅ |
| `…/client/chat/open?openChatId=oc_…&messageId=om_…` (guess) | the chat at its unread position; `messageId` ignored ❌ |
| `…/client/message/link/open?token=<message_id>` (guess) | nothing useful; `token` is a client-minted share token, not a message id ❌ |

Docs: AppLink structure
(<https://open.feishu.cn/document/common-capabilities/applink-protocol/applink-introduction/applink-structure>),
open a chat (<https://open.feishu.cn/document/common-capabilities/applink-protocol/supported-protocol/open-a-chat-page>),
open a bot (<https://open.feishu.cn/document/common-capabilities/applink-protocol/supported-protocol/open-a-bot>).
The supported-protocol list has chat, bot, mini program, H5, calendar, approval, task,
workplace, web view. **No protocol opens a message, a thread, or a position in a chat.**
The client's "copy message link" (`client/message/link/open?token=…`) exists, but no
Open API mints that token (only a *group* share link API exists: `im/v1/chats/{id}/link`).

## What the API knows about threads and mentions

`GET /open-apis/im/v1/messages/{message_id}` (user token OK, `im:message:readonly`)
returns `message_id`, `root_id`, `parent_id`, `thread_id` (`omt_…`), `chat_id`, `mentions`,
`body`, `create_time`. The messages list accepts `container_id_type=thread` with a
`thread_id`. So Larklish can *know* the exact message, its thread, and whether Max is
mentioned — it just cannot deep-link to it.

## Answers

1. **Lark's tap** carries the target in private intent extras. Unreadable; copying the
   `PendingIntent` (Layer 3) is the only way to inherit it, and it works.
2. **Our own deep link** can open a chat, DM, or bot (documented AppLinks; verified on
   the phone). It cannot open a specific message or thread: no such protocol exists.
3. **Best available behaviour for a full-message Relay:** keep Lark's `PendingIntent`
   as the tap target (it opens the chat where Lark would, thread window included) and put
   the full text in the Relay body, so the tap matters less. If we ever need our own
   target (e.g. a Relay built without an Original), use `client/chat/open?openChatId=`.
