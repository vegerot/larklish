# Larklish — Full Conversation Transcript

Source: <https://claude.ai/share/ccf308ed-d7e8-4475-a18b-b31701b9fa81>

> Transcription notes: captured from the rendered share page. UI chrome (the "Report"
> button, "Searched the web, edited a memory" status lines, duplicated response-preview
> lines, relative timestamps) has been dropped. Two long pasted attachments and one
> collapsed message were not expanded in the capture and are marked
> `[ATTACHMENT NOT CAPTURED]`.

---

## Max

I work at a Chinese company but only speak English. Our company chat tool (called Lark/Feishu; pretty much Slack) will translate messages while in the app, but not notifications. This makes the notifications pretty useless for me. I'm also an elite software engineer and I want to program a solution.

Come up with ideas to solve the problem. Some of my ideas 💡:

On Android, is it possible to make an app with permission to read notifications? If so, I could make an app that intercepts all Lark notifications, translates the content to English, and then sends me another notification with the translated content?

Does lark-cli (search GitHub) have an tool for showing notifications or unread messages? If so, I could build a service that polls Lark for notifications, translates them, and then send me a notifications.

What are other ways to solve it? Get creative

---

## Claude

Great problem to hack on 🛠️ Both your ideas are viable — here's the rundown plus more.

**Your idea 1: Android notification interceptor ✅ (best option)**

Yes — NotificationListenerService with the "Notification access" permission gives you every posted notification. The flow:

- Filter on Lark's package name, pull text from EXTRA_TITLE / EXTRA_TEXT / MessagingStyle extras
- Translate zh→en with ML Kit on-device translation (~30MB offline model, free, and crucially your work messages never leave the phone 🔒)
- Post a new notification, and copy the original's contentIntent + actions onto it so tapping still opens Lark (you can even preserve the inline-reply RemoteInput)
- Call cancelNotification() on the original so you don't get doubles

**Your idea 2: lark-cli ✅ (it exists, official even)**

There's an official CLI maintained by the larksuite team covering Messenger, Docs, Calendar and more with 200+ commands (npm install -g @larksuite/cli), with OAuth login so it acts under your user identity. No "notification stream" command, but you could poll recent messages per chat, diff, translate, and push. Two catches: it calls the Lark/Feishu Open Platform APIs, so your corporate tenant admin must allow the app authorization — and polling is clunkier than idea 1.

**More ideas 💡**

- **Zero-code test first:** Pixel's Live Translate auto-translates on-screen text and messages in some chat apps, on-device — worth 5 minutes to check if it handles Lark notifications before you build anything.
- **Tasker + AutoNotification:** no-app-dev version of idea 1 — intercept, HTTP-request a translation, re-post. Good for prototyping the UX in an evening.
- **Lark bot with event subscription:** register an Open Platform app subscribed to the message-receive event → webhook hits your server → translate → push to your phone via ntfy/Pushover. True push, cross-platform, but needs tenant approval.
- **Desktop D-Bus tap (your Linux boxes 🐧):** eavesdrop on org.freedesktop.Notifications, translate, re-emit — a fun dunst/mako-adjacent hack, though Lark desktop probably lives on your work Mac.
- **Browser extension:** run Lark web, MutationObserver watches for new messages, fires English Web Notifications. Desktop-only but dead simple.

One flag 🚩: piping work chat through cloud translation APIs (Google, DeepL, etc.) is exactly the kind of thing corporate DLP policies care about — on-device ML Kit sidesteps that entirely, which is another reason idea 1 is the winner. Want to sketch out the Kotlin service?

---

## Max

One issue with polling for new messages is that I don't get notifications from most channels (called Groups in Lark) I'm a part of. For several groups I get notifications for all messages, in most I get notifications for @mentions, and some I totally block (e.g. from some spammy bots that always @me)

---

## Claude

That's actually the strongest argument for the notification-listener approach 🎯

By the time a notification hits your Android status bar, Lark has already applied all your per-group rules — mutes, @mention-only, blocked bots. The listener sits downstream of that logic, so you inherit your settings for free and they stay in sync when you change them in-app.

Any upstream approach (polling via lark-cli, or a bot with message-event subscriptions) sees the raw firehose and forces you to reimplement the filtering yourself. I'm not aware of the Open Platform API cleanly exposing your per-chat notification preferences, so you'd be maintaining a shadow copy of your rules — an allowlist of chat IDs, @mention detection via the message's mentions array, a bot blocklist — that silently drifts every time you mute a new group in the app. Classic two-sources-of-truth problem 😬

One devil's-advocate angle: rebuilding the rules engine could be a feature if you ever want smarter filtering than Lark offers — keyword alerts, regex rules, or even a cheap LLM scoring "does Max actually care about this message?" But that's a v3 idea; it doesn't justify the maintenance cost for v1.

So the architecture picks itself: NotificationListenerService → ML Kit zh→en → re-post with the original's tap intent. Everything else is fighting the platform. Want to start with the listener skeleton?

---

## Max

First, I want to ask the Lark-cli Expert if there's other ways to get that to work. Draft a prompt to send to the expert asking it to dive deep into the lark-cli docs and implementation

---

## Claude

Here's a prompt for the expert — it front-loads your notification-preferences constraint so the deep-dive targets the actual blocker, not just "can I list messages" 🎯

> You are a lark-cli expert with access to the lark-cli repo (github.com/larksuite/cli) and the Lark/Feishu Open Platform docs. Do a deep-dive investigation, not surface-level README reading: inspect command schemas (`lark-cli schema ...`), the underlying Open Platform endpoints each command maps to, and the Go/Python source where needed. Use `lark-cli api` raw calls to probe endpoints the curated commands don't cover.
>
> Context: I want to build a service that polls (or subscribes to) my Lark account and re-sends translated English notifications. The blocker: my per-group notification preferences differ — a few groups notify on all messages, most on @mentions only, and some are fully muted. Naively polling all messages ignores those preferences. I need to know whether Lark's API surface lets me directly read — or faithfully reproduce — my notification state.
>
> Research questions:
> 1. Per-chat notification settings: Does any API/command expose my per-chat notification preference (mute status, "all messages" vs "@mentions only")? Check chat list/detail fields (im.v1.chats\*), user settings endpoints, and any schema fields mentioning mute/remind/notice/badge.
> 2. Unread/badge state: Can I query per-chat unread counts, and specifically "unread @mentions"? If unread + mention flags are queryable, polling that state would match my notification behavior without duplicating rules.
> 3. Mentions: How are @mentions represented in message payloads (mentions array, open_id matching)? How do I reliably detect "@me", including @all?
> 4. Push/eventing: Does lark-cli or the underlying SDK support event subscriptions or long-connection (WebSocket) mode for message-received events under my user identity? Which scopes and tenant-admin approvals are required? Does any event fire only for notification-worthy messages?
> 5. Auth/limits: For each relevant command, which token type (user_access_token vs tenant_access_token) and scopes are needed, what rate limits apply to a poll-every-30s design, and note any Feishu-vs-Lark regional differences.
>
> Deliverable: a report answering each question with exact commands/endpoints/fields (cite doc URLs or source files), a clear verdict on whether notification-preference-aware polling is feasible, and if yes, a recommended architecture with example lark-cli invocations. If not feasible, say so plainly and list the hard blockers. Do not pad the report with workarounds that require me to maintain my own copy of my notification rules unless you first show the direct approach is impossible.

That last line matters — without it, agents love to "helpfully" propose exactly the shadow-rules architecture you're trying to avoid 😄 If the expert comes back with "unread + @mention state is queryable," polling becomes competitive with the Android listener again; if not, you have your answer with receipts.

---

## Max

Here is its response

`[ATTACHMENT NOT CAPTURED — the Tika agent's lark-cli investigation report]`

Preview line shown on the share page:

> I'll resume the Tika agent and have it continue the same conversation — this time confronting Tika with our empirical findings and pressing on internal tooling: Tika follow-up dispatched 📨 — the agent is resuming the same conversation (ID 2810946079492) and will: 1. **Confront Tika with the two e**

---

## Claude

Solid report — and the retraction arc (Tika hallucinating get_unread_info, then folding when shown the 404) is a good reminder to always ground these consultations in probes 😄

One gap the report undersells though: the two-boolean model may not be able to represent your third tier at all. If direct @you mentions break through is_muted (which is the premise of the report's own tier mapping), then "spammy bot that always @s me, fully blocked" has no encoding — is_muted=true, is_mute_at_all=true still lets @you through. Your blocked tier probably lives in some other mechanism (bot blocking, chat box, or the internal Feed service Tika said is client-only RPC). That's the worst possible failure mode: the polling design would faithfully notify you for exactly the spammiest chats, via --is-at-me no less 🙃

So I'd sharpen the empirical test: when you run batch_query, include one chat from each tier plus one blocked-bot chat specifically, and check whether tiers 2 and 3 are even distinguishable. If they're not, polling is dead regardless of what scopes get approved.

Decision path as I see it:

- **2-min test:** lark-cli auth login --scope "im:chat.user_setting:read im:chat:read im:message.group_msg:get_as_user im:message.p2p_msg:get_as_user search:message" on the ByteDance tenant — ground truth on approval.
- **If granted:** the 4-chat batch_query test above. Only if tier 3 is distinguishable does the polling architecture survive.
- **Either way:** the Android NotificationListenerService build doesn't depend on any of this — it inherits the real rules engine including whatever mechanism blocks those bots.

Also worth pocketing from the report: Lark's built-in per-chat auto-translate covers in-app reading, so your listener app only needs to solve the notification preview — genuinely small scope. Nice when the expensive investigation confirms the weekend project was right all along 🏆 Want to run the login test and report back?

---

## Max

Summarize and explain what it said

---

## Claude

Here's the plain-English version 📋

**What you asked:** Can lark-cli (the official Lark command-line tool) poll for messages in a way that respects your per-group notification settings?

**The answer:** Technically yes, with big caveats — and it hinges on two unknowns.

**The good news 👍**

- There IS a real API that reads your per-chat mute settings (chat_user_setting/batch_query). The investigation found it by auditing the source code — notably, the "Tika" expert initially claimed it didn't exist and had to retract that.
- There's a great server-side search command (--is-at-me) that finds messages @mentioning you, including @all, so you wouldn't need to parse mentions yourself.
- Rate limits are a non-issue: polling every 30 seconds stays far under the caps.

**The bad news 👎**

- No unread counts, no badges, no push for users. Real-time message events only exist for bots, and a bot can't see your chats unless it's added to every group. So polling under your own identity is the only path.
- The API only knows two booleans — "muted" and "@all muted" — while you have three tiers (all messages / @mentions only / fully blocked). The mapping is a guess: nobody could confirm from docs whether your settings actually round-trip through those two flags. My added concern: since @you mentions break through mute, your blocked spammy bots tier may have no representation at all — meaning polling would notify you for exactly the chats you silenced 🙃
- Approval is the real wall. Reading your own messages via API needs user-token scopes, and on the ByteDance tenant that reportedly goes through security review and is "effectively blocked" for personal tools. The only way to know is a 2-minute lark-cli auth login attempt.

**Side discoveries 💎**

- One of Tika's claimed endpoints was a pure hallucination (404'd when probed) — it retracted after being shown evidence.
- The real notification engine (your true three-tier settings, unread counts) lives in an internal Lark service that's client-only and off-limits.
- Lark has built-in per-chat auto-translate for reading in-app — so the only genuine gap is notification previews.

**Bottom line 🎯:** Polling survives only if (1) the ByteDance tenant grants you the scopes and (2) the two-boolean API can actually distinguish your tiers. Your Android notification-listener idea needs neither — it sits downstream of Lark's real rules engine, so it inherits your settings perfectly. The report basically concluded your original plan was right.

---

## Max

Come up with some fun good names for this app

---

## Claude

Since Lark is a bird, the pun space is rich 🐦 My favorites:

- **Lyrebird** 🏆 — the actual bird famous for mimicking other birds' songs. Your app literally hears Lark's call and re-sings it in a language you understand. Thematically perfect.
- **Mockingbird** — repeats what it hears; also fits the interceptor pattern nicely.
- **Bird-in-the-Middle** — MITM pun for the security-minded. You're intercepting Lark's traffic and re-emitting it, after all 😄
- **Translark** — does exactly what it says on the tin.
- **Niaoyu (鸟语)** — "birdsong," which in Chinese is also slang for incomprehensible speech. The app that finally decodes the 鸟语. Deep-cut bilingual joke your colleagues would appreciate.
- **LarkFish** — Babel fish homage for the Hitchhiker's Guide fans; the fish that translates in your ear.
- **ReLark** — terse and daemon-y, if you want it to feel like a system utility.
- **Skylate** — Skylark + translate, for something a bit softer.

I'd go Lyrebird: it's a real word, sounds like a legit product, and the joke reveals itself the moment someone learns what a lyrebird does. Plus com.vegerot.lyrebird is a clean package name ✨

---

## Max

Is it pronounced like "liar bird"? 😬

---

## Claude

Yep — /ˈlaɪər-bird/, exactly like "liar bird" 😅 The name comes from its lyre-shaped tail feathers, but nobody hears the harp when you say it out loud.

Which is... not a great homophone for a translation app. "What did my manager say?" "Ask the liar bird" 💀 Machine translation already has a trust problem; no need to name the app after it.

So let me revise: **Mockingbird** takes the crown 🏆 — same repeats-what-it-hears concept, zero unfortunate homophones, and arguably an even better-known bird. Or if you want to fully dodge birds-that-sound-dishonest, **Translark** and **Niaoyu (鸟语)** are the runners-up.

(Counterpoint for the bold: leaning into it and calling the disclaimer screen "the lyrebird may embellish" is pretty funny. But that's a joke you have to explain, in two languages, forever.)

---

## Max

Come up with a prompt for me to give a marketing Agent to pick the name. Don't give it suggestions

---

## Claude

Here you go — deliberately spec-heavy but name-free, so the agent's ideas aren't anchored 🎯

> You are a product-naming and brand specialist. Your task: generate and select a name for a new app. Do the full naming exercise yourself — I am intentionally not sharing any candidate names, and I don't want you to ask for mine.
>
> ## The product
> An Android utility that intercepts notifications from Lark (the workplace chat app by ByteDance, known as Feishu in China), machine-translates the Chinese message previews into English on-device, and re-posts them as English notifications. It respects the user's existing per-group notification settings. It's a lightweight background service, not a chat client.
>
> ## Audience and context
> - Primary user: English-speaking employees at Chinese companies (initially the developer himself, potentially open-sourced for others in the same situation)
> - The audience is bilingual-workplace-savvy: names can play off English, Chinese, or both, but must be easy for English speakers to say and spell
> - Tone: clever indie developer tool, not corporate SaaS. Wit is welcome; cringe is not.
>
> ## Hard requirements
> 1. Pronounceable on first read by an English speaker, with no ambiguity about how to say it
> 2. No unfortunate homophones or near-homophones in English — say each candidate OUT LOUD and write down what it sounds like; a translation app especially cannot sound like anything implying dishonesty or inaccuracy
> 3. Check Chinese readings too: if the name is or contains Chinese, verify the characters/pinyin carry no negative or comical unintended meaning
> 4. Works as an Android package name (single lowercase word or two clean words) and as an app-store title
> 5. Not trademark-suicidal: avoid names confusable with existing major software products, and avoid implying official affiliation with Lark/ByteDance (a nod is fine; "Lark Official Translator" energy is not)
> 6. Nothing generic-descriptive like "Notification Translator" — it should be memorable
>
> ## Process (show your work)
> 1. Brainstorm at least 20 candidates across multiple strategies: e.g. metaphor/wordplay on what the app does, bird themes (Lark is a bird — but earn it, don't force it), bilingual puns, invented words, mundane-word-recontextualized
> 2. Cut to a shortlist of 5 using the hard requirements, documenting WHY each cut candidate failed (especially any pronunciation/homophone kills)
> 3. For the shortlist: give pronunciation, the story behind the name, how it looks as an app icon label and package name, and one risk
> 4. Pick ONE winner with a 2-3 sentence rationale, plus a runner-up
>
> Do not present a list of options and ask me to choose — commit to a recommendation.

The "say it out loud" step is doing the heavy lifting after our lyrebird incident 😄 And the "earn the bird theme" line keeps it from just regurgitating twenty bird species at you.

---

## Max

`[ATTACHMENT NOT CAPTURED — naming agent 1 output]`

Preview line shown on the share page:

> **1. Brainstorm (≥20 candidates)** **Metaphor / function wordplay** 1. Pinglish 2. Notifish 3. EchoEn 4. Shade 5. Whisperlish 6. Previewlish 7. Relaylish 8. BridgeNotif **Bird / Lark nods (earned, not forced)** 9. Songbird 10. Warbler 11. Nightingale 12. ChirpTranslate

`[ATTACHMENT NOT CAPTURED — naming agent 2 output]`

Preview line shown on the share page:

> **Winner: Larklish** **Runner-up: EchoLark** ### 1. Brainstorm (≥20 candidates) Strategies mixed: translation metaphors (echo/mirror/dub/shadow/relay as the English re-post of the Chinese preview), bird/Lark nods (earned via the product name + light playfulness), bilingual/portmanteau (Lark + Eng

is Mockingbird better?

---

## Claude

Honest answer: no — by the standard we set in the lyrebird incident, Mockingbird is wounded too 😅

Apply the say-it-out-loud test: "mock" means both ridicule and — fatally for your audience — fake. Every engineer reads "mock" as "fake implementation." And your app literally posts synthetic notifications, so "Mockingbird sends mock notifications" is either a delightful self-aware pun or the app admitting its notifications are fake, depending on the reader's mood. There's also a name collision the agents would've caught: Mockingbird is an existing Swift mocking framework (of course it is) plus a wireframing tool. It's less bad than liar-bird, but it's the same genus of problem: a translation app whose name whispers "not the real thing."

Grading the agents' picks while I'm at it:

- **Feather** — safest, cleanest... and generic to the point of vanishing. Both agents also missed dev-world collisions: Feather is a well-known columnar file format (Apache Arrow) and a hugely popular icon library. On a programmer's phone, "Feather" is already three other things.
- **Larklish** — the strongest pick, with one caveat the agent didn't flag: the -lish suffix family (Chinglish, Spanglish, Konglish) denotes broken hybrid English. Funny detail: agent 1 killed "Pinglish" for exactly this "pidgin" echo, then agent 2 crowned a name with the same suffix 😄 For your bilingual-savvy audience it reads as a knowing wink; whether that wink undermines a translation tool is a taste call.
- **EchoLark** — the clean compromise if the -lish wink bothers you: transparent metaphor, no dishonesty shadow, no collisions that matter.

My verdict: Larklish > EchoLark > Mockingbird > Feather. Larklish is distinctive, instantly communicates Lark→English, packages cleanly, and the Chinglish echo is a feature for the exact people who'd use this. But this is a personal tool — the tiebreaker is which name makes you grin when it pops up in your notification shade 🐦

---

## Max

`[ATTACHMENT NOT CAPTURED — naming agent 3 output, round 1]`

Preview line shown on the share page:

> I'd treat this as a **developer-tool name**, not a consumer translation brand. The interesting part of the product is not "translation" itself. It is that a little background bird catches a notification, changes its language, and puts it back where it came from. 🐦📨 I also did a current web sanity

`[MESSAGE COLLAPSED ON THE SHARE PAGE — naming agent 3 output, round 2, partially captured below]`

You're right. I over-indexed on the **indie developer aesthetic** and under-indexed on the fact that this is a **real consumer utility from the Play Store**. The name needs to pass a much harsher test:

> "I see this in Google Play. I understand that it's a legitimate app, I can pronounce it, and I trust it to translate my work messages accurately."

That changes my ranking substantially.

### Revised naming direction

I'd want the name to feel:

* **Approachable**, not like a GitHub project
* **Trustworthy**, because it's handling workplace messages
* **Clearly language-adjacent**, without being `Lark Translator`
* **Short enough to look good under an Android icon**
* Clever enough to be memorable, but not so clever that users have to decode it
* Independent of Lark/ByteDance branding

And I'd put **ChirpShift** down from #1. "Shift" is clever, but it doesn't tell a Play Store user much.

### My revised top 5

| Name | Why it works | Main concern |
| ------------------ | ------------------------------------------------- | ---------------------------------- |
| **ChirpTranslate** | Extremely clear: bird/Lark nod + translation | A little descriptive |
| **ChirpBridge** | Communicates Chinese ↔ English communication | "Bridge" is somewhat generic |
| **ChirpLingo** | Friendly, memorable, immediately language-related | Slightly playful |
| **ChirpSwitch** | Communicates switching notification language | Doesn't explicitly say translation |
| **ChirpShift** | Elegant description of transforming notifications | Too opaque for a consumer app |

But I'd actually take the naming exercise one step further and introduce a **new direction: names built around "English arriving in your notifications."**

For example:

* **EnglishPing**
* **PingLingo**
* **ClearPing**
* **PingBridge**
* **MessageWing**
* **LingoPing**
* **ChirpEnglish**
* **EnglishChirp**
* **ChirpTranslate**
* **ChirpText**

Of these, **ChirpTranslate** is much more commercially sensible than ChirpShift.

### 🏆 Revised winner: **ChirpTranslate**

**Pronunciation:** "CHURP translate"

**Why:** It gives you the Lark/bird connection without pretending to be Lark, while **Translate** immediately tells a Play Store visitor what the app does. "Chirp" makes an otherwise generic utility name distinctive, and importantly, there is no pronunciation puzzle or questionable Chinese/pinyin meaning.

**Icon label:** `Chirp` or `ChirpTranslate`
**Package:** `com.<developer>.chirptranslate`

The big advantage is that someone can see:

> **ChirpTranslate**
> *Translate your Lark notifications*

…and understand the product in about two seconds.

**Runner-up: ChirpBridge.** It's the better *brand* if you eventually want to translate notifications from multiple workplace apps, whereas ChirpTranslate is the better name for the product you are actually shipping today.

So I'd change my recommendation from **ChirpShift → ChirpTranslate**.

---

## Claude

That second message is worth reading skeptically — it's less "new analysis" and more "agent folding under pushback" 😅 Tells:

- **It resurrected its own kills.** ChirpLingo was executed in round 1 for "wrong category signal"; it's now #3. (Round 1 also killed ChirpSwitch, ChirpSwap, and ChirpTurn in the cuts section and then... shortlisted all three anyway. This agent's kill list is more of a suggestion 😄)
- **It forgot its own best argument.** Round 1 correctly said the Lark relationship belongs in the subtitle: "ChirpShift — Translate Lark notifications on-device." That solves the Play-Store-clarity problem completely — the store listing always pairs name with descriptor, so the name doesn't have to be self-explanatory. Under pressure it abandoned that and landed on exactly the "keyword salad" it originally mocked. ChirpTranslate is Foo Translator with a bird sticker.
- **Fun tournament note:** ChirpTranslate was explicitly killed by your first agent ("several already used by other products"). Every agent's crown is another agent's cut — Pinglish killed twice, Larklish crowned by agent 2 and killed by agent 3 for the same -lish concern I flagged. At this point you've extracted all the signal naming agents have to give 🏁

So, standing final ranking across the whole exercise:

1. **ChirpShift** — the round-1 pick was right the first time. Distinctive, zero collisions found, pronounceable, evokes Lark without touching the trademark, and sounds like a real tool. The "too opaque" concern is fully handled by the tagline.
2. **Larklish** — more personality and the bilingual wink, if you accept the Chinglish-suffix echo (real but arguably a feature for your audience).
3. **ChirpTranslate** — the safe fallback if this someday becomes a genuinely mass-market Play Store app and discoverability trumps taste. That's not v1.

And the tiebreaker hasn't changed: v1 has exactly one user, and he's the one reading the notification shade. com.vegerot.chirpshift or com.vegerot.larklish — pick the one that makes you grin and go build the listener 🐦⚙️

---

## Max

Would "Larklish" be hard to say for Chinese co-workers?

---

## Claude

Moderately, yes — Mandarin phonotactics fight it on two fronts 🀄

- "Lark" ends in the cluster /rk/, and Mandarin syllables can't end in stops or clusters (only -n and -ng). The standard fix is inserting vowels, loanword-style: like "Mark" → 马克 Mǎkè, "Lark" tends to come out lā-kè.
- "-lish" ends in /ʃ/, which Mandarin only allows at the start of a syllable — so it becomes lì-shì. (This is exactly why "English" gets jokingly transliterated as 英格力士 yīng-gé-lì-shì.)

Net result: your two-syllable name is likely to come out as ~four syllables, lā-kè-lì-shì. Sayable, but not smooth.

Three mitigating facts though:

- ChirpShift is worse — it has two final clusters (/rp/ and /ft/). Nobody in this tournament optimized for Mandarin.
- Your colleagues handle English tech names constantly; a slightly clunky loanword pronunciation is Tuesday for them. Also, at ByteDance the product is 飞书 anyway — "Lark" may come up less than you'd think.
- The names that were Mandarin-friendly (Feiyi, Yiniao, Niaoyu — all clean open syllables) were exactly the ones the agents killed for English pronounceability. The constraint is genuinely two-sided; no single name wins both.

Which suggests the actual Chinese-tech-industry solution: dual naming, like every real product there (Lark/飞书 itself, DingTalk/钉钉). Ship it as Larklish with 译鸟 ("translation bird") or 鸟语 as its Chinese name in the README. Your coworkers get a name that rolls off the tongue and a better joke than the English one 🐦

If one-name-for-everyone is a hard requirement, that's the strongest remaining argument for boring-but-universal ChirpTranslate... but for a personal app, I'd take the dual name and the grin.

---

## Max

I pick Larklish.

---

## Claude

Larklish it is 🐦🎉 Good pick — distinctive, honest about what it does, and the -lish wink is exactly right for its one launch user.

So the full stack is now decided:

- **Name:** Larklish (com.vegerot.larklish)
- **Architecture:** NotificationListenerService → ML Kit zh→en on-device → re-post with the original's contentIntent and actions → cancel the original
- **Why:** sits downstream of Lark's real rules engine, so your three notification tiers (including the blocked spammy bots) come free — no API scopes, no ByteDance tenant approval, no two-boolean approximation

Ready to scaffold it whenever you are: the manifest + service declaration, the notification-access permission flow, extracting MessagingStyle text, and the ML Kit model download on first launch. Want the skeleton? ⚙️

---

## Max

Does ByteDance/Vulcano/Lark have a translation API? Just want to consider options in case on-device ML is poor quality

---

## Claude

Yes — both exist, and they're arguably your best cloud options 🌋

**1. Lark Open Platform Translation API ✅**

The Lark/Feishu server API has a dedicated text-translation endpoint (POST /open-apis/translation/v1/text/translate, plus a language-detect sibling) — it's the same machinery behind the in-app translate you already use, so notification translations would match what you see when you open the chat. Big bonus for Larklish: your notification listener already has the text locally, so you'd only need a Feishu app with the translation scope — no message-reading scopes, which was the scary approval. Translation is a much easier ask than "read all my messages."

**2. Volcano Engine Machine Translation (火山翻译) ✅**

ByteDance's cloud translation service — it's ByteDance's MT brand and won 5 categories at the WMT 2020 international machine translation competition, and zh↔en is exactly its home turf. It supports 55 languages with a proper Machine Translation API at translate.volcengineapi.com with standard AK/SK auth. Catches: it requires real-name verification before you can use the translation API (China-region signup friction — check BytePlus, ByteDance's international cloud brand, for the non-CN equivalent), and it's pay-as-you-go past the free quota.

**Why these beat Google/DeepL for you 🔒:** your DLP concern mostly evaporates — the message content is ByteDance's data flowing to ByteDance's own infrastructure, which is a far easier compliance story than shipping work chat to a third party. And quality-wise, Chinese→English is the language pair these engines are most optimized for, including the internal-jargon-heavy register of Chinese workplace chat.

**Suggested architecture:** make translation a one-method interface —

```kotlin
interface Translator { suspend fun zhToEn(text: String): String }
```

— with MlKitTranslator as v1 and LarkApiTranslator / VolcanoTranslator behind a setting. Ship on-device first (free, offline, zero approvals), live with it for a week, and only wire up a cloud backend if the quality genuinely annoys you. Notification previews are short and context-light; gist-quality may be plenty. And you always have the escape hatch: tap through to Lark, where auto-translate gives the good translation anyway 🐦
