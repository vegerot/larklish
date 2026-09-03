package main

import "testing"

// The cases of the phone's PreviewTest.kt, which stays: both sides parse a Preview.
func TestParsePreview(t *testing.T) {
	cases := []struct {
		name, text string
		want       Preview
	}{
		{"sender and message split at the first colon-space",
			"Yinghao Li: 哈喽，我发现有个问题", Preview{Sender: "Yinghao Li", Message: "哈喽，我发现有个问题"}},
		{"@you suffix on the Sender is split off",
			"Bits@you: Sync Task Conflicted", Preview{Sender: "Bits", Mention: MentionYou, Message: "Sync Task Conflicted"}},
		{"@all suffix on the Sender is split off",
			"Rachel Lam@all: Hello @all Please join us", Preview{Sender: "Rachel Lam", Mention: MentionAll, Message: "Hello @all Please join us"}},
		{"only the first colon-space splits",
			"Bits: 【告警】集群: 5xx 比例超过 1%", Preview{Sender: "Bits", Message: "【告警】集群: 5xx 比例超过 1%"}},
		{"text without colon-space is all message",
			"You have received a message.", Preview{Message: "You have received a message."}},
		{"the empty Preview is Sender:... (Experiment 06 E5)",
			"Bits:...", Preview{Sender: "Bits", Message: "..."}},
		{"emoji survives in Sender and message",
			"夏银璐😀: 好的👍🏼 收到", Preview{Sender: "夏银璐😀", Message: "好的👍🏼 收到"}},
		{"emoji alone after a mention",
			"Bits@you: 😀", Preview{Sender: "Bits", Mention: MentionYou, Message: "😀"}},
	}
	for _, c := range cases {
		if got := ParsePreview(c.text); got != c.want {
			t.Errorf("%s: ParsePreview(%q) = %+v, want %+v", c.name, c.text, got, c.want)
		}
	}
}

func TestStemDropsLarksEllipsis(t *testing.T) {
	if got := ParsePreview("Bits:...").Stem(); got != "" {
		t.Errorf("empty Preview stem = %q, want empty", got)
	}
	if got := ParsePreview("Bits: 这是一条很长的消息，...").Stem(); got != "这是一条很长的消息，" {
		t.Errorf("stem = %q", got)
	}
}

func TestLarksEllipsisIsWhatMarksAPreviewTruncated(t *testing.T) {
	cases := map[string]bool{
		"Bits: 这是一条很长的消息，...": true,
		"Bits:...":                     true, // the empty Preview is a cut too
		"Bits: Sync Task Conflicted":   false,
		"You have received a message.": false,
	}
	for text, want := range cases {
		if got := ParsePreview(text).Truncated(); got != want {
			t.Errorf("Truncated(%q) = %v, want %v", text, got, want)
		}
	}
}
