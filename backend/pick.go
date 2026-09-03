package main

import (
	"regexp"
	"strings"
	"unicode"
)

// the message is posted before its Original: p50 5 s, p95 24 s, max 44 s (Experiment 11)
const beforeMs = 60_000

// Lark can deliver a backlog minutes late — 3 m 59 s observed, three Originals in two seconds.
// Only a full-length needle may reach that far back: it names one message on its own, so the
// extra reach cannot mistake a neighbour for it.
const beforeDelayedMs = 300_000

// clock slack between the phone and Lark. No message ever arrives after its Original (Experiment 11).
const afterMs = 5_000

// enough characters to name one message inside the window; 0 false positives in 35 chats (Experiment 11)
const matchChars = 12

// Candidate is one message from the `messages` list, reduced to what pickMessage needs. Mentions resolved.
type Candidate struct {
	ID         string
	MsgType    string
	CreateTime int64 // epoch ms
	Deleted    bool
	Text       string // `text` and flattened `post` messages; "" for every other type
}

// Pick is one answer: the candidate found in ChatID, or the reason there is none —
// `no-chat`, `no-message` (nothing in the window), `no-match`, `type:<msg_type>`, or
// `error: …` when a chat could not be read. The reasons reach the phone's record unchanged.
type Pick struct {
	Found  *Candidate
	ChatID string
	Reason string
}

const errorPrefix = "error: "

// failed: the call failed, not the match. Such a chat answers for itself only.
func (p Pick) failed() bool { return p.Found == nil && strings.HasPrefix(p.Reason, errorPrefix) }

// pickMessage finds the newest message with text just before the Original whose text opens
// with the Preview stem. Only the first matchChars folded characters must agree: the Preview
// is not a faithful prefix of the Full text — it writes `[Delighted]` for an emoji, lower-cases
// `@All`, and can drop a trailing bold run (Experiment 11). A message that opens with a mention
// is matched on the close of the stem instead, because Lark localizes the name. A textless
// message in the window is reported by type (`image`, `sticker`, `interactive`, …).
//
// How far back the window reaches depends on how much needle there is. A full-length one
// identifies the message by itself, so it may look past a delayed push; a short or empty stem
// leans on the window to tell messages apart, so it stays close to the Original.
//
// Characters are runes here; the phone's Kotlin counted UTF-16 units, which differs only for
// an emoji inside the first 12 characters (5 of 257 corpus stems, Experiment 15).
func pickMessage(items []Candidate, stem string, whenMs int64) Pick {
	folded := []rune(fold(stem))
	needle := folded
	if len(needle) > matchChars {
		needle = needle[:matchChars]
	}
	before := int64(beforeMs)
	if len(needle) == matchChars {
		before = beforeDelayedMs
	}
	var window []*Candidate
	for i := range items {
		c := &items[i]
		if !c.Deleted && c.CreateTime >= whenMs-before && c.CreateTime <= whenMs+afterMs {
			window = append(window, c)
		}
	}
	for _, c := range window {
		if c.Text != "" && strings.HasPrefix(fold(c.Text), string(needle)) {
			return Pick{Found: c}
		}
	}
	// A mention shows whichever of an account's names the reader prefers, so the two sides
	// disagree on the opening and on nothing else: the phone writes `@Oncall Assistant`,
	// `@all` and `@Yan Fan`, the API `@Oncall 助手`, `@所有人` and `@樊艳` (Experiment 12).
	// Both sides must open with a mention, or a shared close — a link, a sign-off — could
	// name the wrong message.
	if len(folded) >= matchChars && folded[0] == '@' {
		close := string(folded[len(folded)-matchChars:])
		for _, c := range window {
			if strings.HasPrefix(c.Text, "@") && strings.Contains(fold(c.Text), close) {
				return Pick{Found: c}
			}
		}
	}
	if len(window) == 0 {
		return Pick{Reason: "no-message"}
	}
	if newest := window[0]; newest.Text != "" {
		return Pick{Reason: "no-match"}
	} else {
		return Pick{Reason: "type:" + newest.MsgType}
	}
}

// fold is how a stem and a message text are compared: no whitespace, no case (Lark's Preview keeps neither).
func fold(s string) string {
	var b strings.Builder
	for _, r := range s {
		if !unicode.IsSpace(r) {
			b.WriteRune(r)
		}
	}
	return strings.ToLower(b.String())
}

var mentionRe = regexp.MustCompile(`@_(user_\d+|all)`)

// resolveMentions turns `@_user_1` placeholders into `@Name`, from the message's `mentions`
// (key → name). `@_all` has no entry there and reads `@all` in the Preview, so it is spelled
// that way here too.
func resolveMentions(text string, mentions map[string]string) string {
	return mentionRe.ReplaceAllStringFunc(text, func(m string) string {
		if m == "@_all" {
			return "@all"
		}
		if name, ok := mentions[m]; ok {
			return "@" + name
		}
		return m
	})
}

var tagRe = regexp.MustCompile(`</?[a-z][^>]*>`)

// unwrapHtml strips the paragraphs Lark's editor wraps a `text` body in — `<p>line</p><p>line</p>` —
// which Lark's own Preview never shows (11 of 198 text messages, Experiment 11).
func unwrapHtml(text string) string {
	if !strings.HasPrefix(text, "<p>") {
		return text
	}
	s := tagRe.ReplaceAllString(strings.ReplaceAll(text, "</p><p>", "\n"), "")
	s = strings.ReplaceAll(s, "&lt;", "<")
	s = strings.ReplaceAll(s, "&gt;", ">")
	return strings.ReplaceAll(s, "&amp;", "&") // `&amp;` last, or `&amp;lt;` decodes twice
}

// PostElement is one tagged piece of a `post` paragraph, with only the fields postText reads.
type PostElement struct {
	Tag, Text, UserName, EmojiType string
}

// postText flattens a `post` — `title` + paragraphs of tagged elements (Experiment 08):
// `{"title": "", "content": [[{"tag": "text", "text": "…"}], [{"tag": "img", "image_key": "…"}]]}`.
// One line per paragraph; images become `[image]` and emoji `[Delighted]`, like Lark's own Preview.
func postText(title string, paragraphs [][]PostElement) string {
	var lines []string
	if title != "" {
		lines = append(lines, title)
	}
	for _, p := range paragraphs {
		var b strings.Builder
		for _, e := range p {
			switch e.Tag {
			case "text", "a":
				b.WriteString(e.Text)
			case "at":
				b.WriteString("@" + e.UserName)
			case "img":
				b.WriteString("[image]")
			case "emotion":
				if e.EmojiType != "" {
					b.WriteString("[" + e.EmojiType + "]")
				} else {
					b.WriteString("[" + e.Tag + "]")
				}
			default:
				b.WriteString("[" + e.Tag + "]")
			}
		}
		if b.Len() > 0 {
			lines = append(lines, b.String())
		}
	}
	return strings.Join(lines, "\n")
}
