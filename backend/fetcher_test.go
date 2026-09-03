package main

import (
	"context"
	"errors"
	"strings"
	"testing"
)

// fakeSource is a Lark that answers from maps and remembers what was asked.
type fakeSource struct {
	titles   map[string][]string    // title → chats it names
	dms      map[string]string      // Sender → p2p chat
	messages map[string][]Candidate // chat → its newest messages
	broken   map[string]bool        // chats whose message list fails
	calls    []string
}

func (s *fakeSource) GroupChatIDs(_ context.Context, title string) ([]string, error) {
	s.calls = append(s.calls, "search:"+title)
	return s.titles[title], nil
}

func (s *fakeSource) DMChatID(_ context.Context, p Preview, whenMs int64) (string, error) {
	s.calls = append(s.calls, "dm:"+p.Sender)
	return s.dms[p.Sender], nil
}

func (s *fakeSource) Messages(_ context.Context, chatID string, whenMs int64) ([]Candidate, error) {
	s.calls = append(s.calls, "msgs:"+chatID)
	if s.broken[chatID] {
		return nil, errors.New("Lark /open-apis/im/v1/messages: http 400 code 230002 Bot/User can NOT be out of the chat.")
	}
	return s.messages[chatID], nil
}

func quiet(src Source) *Fetcher {
	f := NewFetcher(src)
	f.Log = func(string, ...any) {}
	return f
}

func count(calls []string, prefix string) int {
	n := 0
	for _, c := range calls {
		if strings.HasPrefix(c, prefix) {
			n++
		}
	}
	return n
}

func TestAnAmbiguousTitleAsksEachChatAndCachesOnlyTheConfirmedOne(t *testing.T) {
	src := &fakeSource{
		titles:   map[string][]string{"【bytedcli MR 处理】...": {"oc_a", "oc_b"}},
		messages: map[string][]Candidate{"oc_b": {text("m", at-3_000, "【bytedcli MR 处理】#3245 逾期提醒")}},
	}
	f := quiet(src)
	p := ParsePreview("Bits: 【bytedcli MR 处理】#3245 ...")
	pick := f.FullTextOf(t.Context(), "【bytedcli MR 处理】...", p, at)
	if pick.Found == nil || pick.ChatID != "oc_b" {
		t.Fatalf("got %+v, want found in oc_b", pick)
	}
	if f.chats["【bytedcli MR 处理】..."] != "oc_b" {
		t.Errorf("cache = %v, want the confirmed chat only", f.chats)
	}
	f.FullTextOf(t.Context(), "【bytedcli MR 处理】...", p, at)
	if count(src.calls, "search:") != 1 {
		t.Errorf("second Lookup searched again: %v", src.calls)
	}
}

func TestAnUnreadableChatIsThatChatsMissAndIsNeverCached(t *testing.T) {
	src := &fakeSource{
		titles:   map[string][]string{"ByteDance Research...": {"oc_bad", "oc_good"}, "Private...": {"oc_bad"}},
		broken:   map[string]bool{"oc_bad": true},
		messages: map[string][]Candidate{"oc_good": {text("m", at-3_000, "ByteDance Research 技术交流群 直播开启")}},
	}
	f := quiet(src)
	pick := f.FullTextOf(t.Context(), "ByteDance Research...", ParsePreview("Yifei: ByteDance Research 技术交流群 直播开启..."), at)
	if pick.Found == nil || pick.ChatID != "oc_good" {
		t.Fatalf("got %+v, want found in oc_good past the unreadable chat", pick)
	}
	pick = f.FullTextOf(t.Context(), "Private...", ParsePreview("Someone: 什么..."), at)
	if pick.Found != nil || !strings.HasPrefix(pick.Reason, "error: Lark") {
		t.Errorf("got %+v, want the chat's error as the reason", pick)
	}
	if _, cached := f.chats["Private..."]; cached {
		t.Errorf("a chat that answered 400 was cached: %v", f.chats)
	}
}

func TestATitleNamingNoChatIsFoundThroughTheChatsJustUsed(t *testing.T) {
	src := &fakeSource{
		titles: map[string][]string{"Seller Center - FE Oncall": {"oc_topic"}},
		messages: map[string][]Candidate{"oc_topic": {
			text("reply", at-2_000, "收到，我看一下"),
			text("root", at-9_000, "Seller Center 报警：5xx 比例超过 1%"),
		}},
	}
	f := quiet(src)
	if pick := f.FullTextOf(t.Context(), "Seller Center - FE Oncall", ParsePreview("A: Seller Center 报警：5xx 比例超过 1%..."), at-6_000); pick.Found == nil {
		t.Fatalf("first Lookup: %+v", pick)
	}
	// A reply in a thread carries the thread's root text as its title (Experiment 09).
	pick := f.FullTextOf(t.Context(), "Seller Center 报警：5xx 比例超过 1%", ParsePreview("B: 收到，我看一下..."), at)
	if pick.Found == nil || pick.ChatID != "oc_topic" {
		t.Fatalf("got %+v, want the reply through the LRU", pick)
	}
	if _, cached := f.chats["Seller Center 报警：5xx 比例超过 1%"]; cached {
		t.Errorf("an LRU hit was cached under the thread title: %v", f.chats)
	}
}

func TestADMOriginalUsesTheSenderKeyAndNeverTheGroupCache(t *testing.T) {
	src := &fakeSource{
		dms:      map[string]string{"Max Coplan's Feishu CLI": "oc_dm"},
		messages: map[string][]Candidate{"oc_dm": {text("m", at-3_000, "这是一条很长的消息，前四十五个字符里没有边界")}},
	}
	f := quiet(src)
	pick := f.FullTextOf(t.Context(), "Lark", ParsePreview("Max Coplan's Feishu CLI:..."), at)
	if pick.Found == nil || pick.ChatID != "oc_dm" {
		t.Fatalf("got %+v", pick)
	}
	if f.chats["dm:Max Coplan's Feishu CLI"] != "oc_dm" || len(f.chats) != 1 {
		t.Errorf("cache = %v, want only the dm: key", f.chats)
	}
}

func TestASingleCleanMissIsCached(t *testing.T) {
	src := &fakeSource{titles: map[string][]string{"Quiet group": {"oc_q"}}}
	f := quiet(src)
	pick := f.FullTextOf(t.Context(), "Quiet group", ParsePreview("A: 你好..."), at)
	if pick.Found != nil || pick.Reason != "no-message" {
		t.Fatalf("got %+v, want no-message", pick)
	}
	if f.chats["Quiet group"] != "oc_q" {
		t.Errorf("an unambiguous, readable chat was not cached: %v", f.chats)
	}
}
