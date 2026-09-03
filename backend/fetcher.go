package main

import (
	"fmt"
	"log"
	"strings"
	"sync"
)

// a bot DM's Original has this title; the Sender is the bot
const botDMTitle = "Lark"

// chats to re-try when the title names no chat; two is the whole prize (Experiment 10)
const recentTries = 2

// Source is where a Lookup reads: the live Open API (live.go), or a pulled corpus in tests.
type Source interface {
	// GroupChatIDs: the chats a group title names — the exact name, or every name starting
	// with the stem when Lark truncated the title with `...` (at most prefixTries).
	GroupChatIDs(title string) ([]string, error)
	// DMChatID: the p2p chat whose peer is the Sender, found around the Original's time; "" when none.
	DMChatID(p Preview, whenMs int64) (string, error)
	// Messages: the chat's newest messages inside the widest window pickMessage uses, newest first.
	Messages(chatID string, whenMs int64) ([]Candidate, error)
}

// Fetcher runs the Lookup (Layer 5): chat id → newest messages of the chat → pickMessage.
// Groups: title → chats/search. DMs: messages/search around the Original's time. Chat ids are
// cached in memory (`title` for groups, `dm:<Sender>` for DMs) and the chats that produced a
// Full text are kept most recent first, for thread replies whose title names no chat. Both die
// with the process and are re-learned, one search per chat.
type Fetcher struct {
	src    Source
	Log    func(format string, args ...any)
	mu     sync.Mutex // Originals arrive in bursts; the map and the LRU are shared
	chats  map[string]string
	recent []string
}

func NewFetcher(src Source) *Fetcher {
	return &Fetcher{src: src, Log: log.Printf, chats: map[string]string{}}
}

// FullTextOf finds the Full text behind an Original. A chat that cannot be read is just one
// chat's miss (pickIn); a failed *search* ends the Lookup, having no candidates to fall back on.
func (f *Fetcher) FullTextOf(title string, p Preview, whenMs int64) Pick {
	// A DM Original has title `Lark` — bots and people alike (Experiment 09) — or, in theory,
	// the Sender's own name; everything else is a group. The two caches never cross: the bot
	// that DMs Max also posts in groups.
	isDm := title == botDMTitle || title == p.Sender
	key := title
	if isDm {
		key = "dm:" + p.Sender
	}
	f.mu.Lock()
	cached, ok := f.chats[key]
	f.mu.Unlock()
	var candidates []string
	switch {
	case ok:
		candidates = []string{cached}
	case isDm:
		id, err := f.src.DMChatID(p, whenMs)
		if err != nil {
			return Pick{Reason: errorPrefix + err.Error()}
		}
		if id != "" {
			candidates = []string{id}
		}
	default:
		ids, err := f.src.GroupChatIDs(title)
		if err != nil {
			return Pick{Reason: errorPrefix + err.Error()}
		}
		candidates = ids
	}
	outcome := Pick{Reason: "no-chat"}
	// A truncated title can name several chats. Only the one holding the message is right,
	// so ask each in turn and remember that one — never a guess (Experiment 09).
	for i, chatID := range candidates {
		pick := f.pickIn(chatID, title, p, whenMs)
		if i == 0 {
			outcome = pick
		}
		if pick.Found != nil {
			f.remember(key, chatID)
			return pick
		}
	}
	// Unambiguous and readable: keep it even on a miss. A chat that answered 400 is never
	// cached — every later Original under this title would hit the same 400 forever.
	if len(candidates) == 1 && !outcome.failed() {
		f.remember(key, candidates[0])
	}
	// A reply inside a topic chat carries the *thread's* root text as its title, so no chat
	// search can ever find it (Experiment 09). Such a reply follows other traffic in the same
	// chat, so the chat is almost always one Larklish just used (Experiment 10).
	for _, chatID := range f.recentChats() {
		if pick := f.pickIn(chatID, title, p, whenMs); pick.Found != nil {
			return pick
		}
	}
	return outcome
}

// pickIn is one chat's answer. A failure here is this chat's answer, not the Lookup's:
// chats/search returns chats Max can find but not read (`230002 Bot/User can NOT be out of
// the chat`), and one of those must not skip the other candidates or the LRU (progress.md
// 2026-08-31). The reason still reaches the record through Pick.Reason.
func (f *Fetcher) pickIn(chatID, title string, p Preview, whenMs int64) Pick {
	items, err := f.src.Messages(chatID, whenMs)
	if err != nil {
		f.Log("[%s] %s unreadable: %v", title, chatID, err)
		return Pick{ChatID: chatID, Reason: errorPrefix + err.Error()}
	}
	ages := make([]string, len(items))
	for i, c := range items {
		ages[i] = fmt.Sprintf("%s %ds ago", c.MsgType, (whenMs-c.CreateTime)/1000)
	}
	f.Log("[%s] %s candidates: %s", title, chatID, strings.Join(ages, ", "))
	pick := pickMessage(items, p.Stem(), whenMs)
	pick.ChatID = chatID
	f.Log("[%s] %s → %s", title, chatID, pick)
	if pick.Found != nil {
		f.mu.Lock()
		f.recent = append([]string{chatID}, without(f.recent, chatID)...)
		f.mu.Unlock()
	}
	return pick
}

func (f *Fetcher) recentChats() []string {
	f.mu.Lock()
	defer f.mu.Unlock()
	n := min(recentTries, len(f.recent))
	return append([]string(nil), f.recent[:n]...)
}

func (f *Fetcher) remember(key, chatID string) {
	f.mu.Lock()
	f.chats[key] = chatID
	f.mu.Unlock()
}

// Chats is a copy of the cache, for `GET /chats` and the helper.
func (f *Fetcher) Chats() map[string]string {
	f.mu.Lock()
	defer f.mu.Unlock()
	out := make(map[string]string, len(f.chats))
	for k, v := range f.chats {
		out[k] = v
	}
	return out
}

func without(list []string, s string) []string {
	out := make([]string, 0, len(list))
	for _, x := range list {
		if x != s {
			out = append(out, x)
		}
	}
	return out
}

func (p Pick) String() string {
	if p.Found != nil {
		return fmt.Sprintf("Found(%s %d)", p.Found.MsgType, p.Found.CreateTime)
	}
	return "Skipped(" + p.Reason + ")"
}
