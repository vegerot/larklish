package main

import "strings"

// Mention is the suffix Lark puts on the Sender: `Sender@you: …` or `Sender@all: …`.
type Mention string

const (
	NoMention  Mention = ""
	MentionYou Mention = "you"
	MentionAll Mention = "all"
)

// ellipsis is what Lark appends when it cuts a message at 45 characters (Experiment 06).
const ellipsis = "..."

// Preview is a Lark Preview split into its parts. Lark posts `text` as `Sender: message`
// (Experiment 00); a mention makes it `Sender@you: message` or `Sender@all: message`.
// The phone parses the same way (Preview.kt) for the Relay; the Backend needs the Sender
// (DM lookups) and the stem (the match).
type Preview struct {
	Sender  string
	Mention Mention
	Message string
}

// Stem is the message without Lark's `...`: what the Full text must start with (Layer 5).
func (p Preview) Stem() string { return strings.TrimSuffix(p.Message, ellipsis) }

// Truncated reports that Lark cut the message, so the Full text holds more than the Preview.
func (p Preview) Truncated() bool { return strings.HasSuffix(p.Message, ellipsis) }

// ParsePreview splits a Preview's text. Every delimiter is ASCII, so byte offsets are exact.
func ParsePreview(text string) Preview {
	// No boundary in the first 45 characters → Lark posts `Sender:...` (Experiment 06 E5).
	if strings.HasSuffix(text, ":...") && !strings.Contains(text, ": ") {
		return ParsePreview(strings.TrimSuffix(text, ellipsis) + " " + ellipsis)
	}
	split := strings.Index(text, ": ")
	if split < 0 {
		return Preview{Message: text}
	}
	head, message := text[:split], text[split+2:]
	p := Preview{Sender: head, Message: message}
	switch {
	case strings.HasSuffix(head, "@you"):
		p.Sender, p.Mention = strings.TrimSuffix(head, "@you"), MentionYou
	case strings.HasSuffix(head, "@all"):
		p.Sender, p.Mention = strings.TrimSuffix(head, "@all"), MentionAll
	}
	return p
}
