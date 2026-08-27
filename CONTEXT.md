# Larklish

Larklish reads Lark's Android notifications, translates the Chinese preview to English
on-device, and posts an English notification in its place.

## Language

**Original**:
The notification that Lark posts.
_Avoid_: source notification, Lark notification, incoming notification

**Relay**:
The English notification that Larklish posts in place of an Original.
_Avoid_: replacement, translated notification, re-post, mirror

**Preview**:
The visible text of a notification: its title (the group name) and its text.
_Avoid_: content, body, message, snippet

**Sender**:
The person or bot named before the first `: ` in a Preview's text.
_Avoid_: author, user, from

**Tier**:
One of the three per-group notification settings in Lark: All, Mentions, or Blocked.
_Avoid_: level, mode, preference, rule

**Translator**:
The component that turns Chinese text into English text.
_Avoid_: engine, backend, provider

**Full text**:
The whole message body that the Lark Open API returns for one message.
_Avoid_: body, content, full message

**Update**:
A second `notify` on the same Relay key that replaces the Preview with the Full text.
_Avoid_: refresh, re-post, second post
