package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"testing"

	larkim "github.com/larksuite/oapi-sdk-go/v3/service/im/v1"
)

type original struct {
	WhenMs int64  `json:"whenMs"`
	Title  string `json:"title"`
	Text   string `json:"text"`
}

// corpusSource is a Lark that answers from a pulled corpus: what chats a title names, which
// p2p chat a Sender has, and what each chat said.
type corpusSource struct {
	originals []original
	titles    map[string][]string
	dms       map[string]string
	messages  map[string][]Candidate
}

func (c *corpusSource) GroupChatIDs(_ context.Context, title string) ([]string, error) {
	return c.titles[title], nil
}
func (c *corpusSource) DMChatID(_ context.Context, p Preview, whenMs int64) (string, error) {
	return c.dms[p.Sender], nil
}
func (c *corpusSource) Messages(_ context.Context, chatID string, whenMs int64) ([]Candidate, error) {
	return c.messages[chatID], nil
}

func readJSON(t *testing.T, dir, name string, into any) {
	data, err := os.ReadFile(filepath.Join(dir, name))
	if err != nil {
		t.Fatal(err)
	}
	if err := json.Unmarshal(data, into); err != nil {
		t.Fatalf("%s: %v", name, err)
	}
}

func jsonLines(t *testing.T, dir, name string) [][]byte {
	data, err := os.ReadFile(filepath.Join(dir, name))
	if err != nil {
		t.Fatal(err)
	}
	var out [][]byte
	for _, line := range bytes.Split(data, []byte("\n")) {
		if len(bytes.TrimSpace(line)) > 0 {
			out = append(out, line)
		}
	}
	return out
}

// loadCorpus reads what `tools/larklish-helper replay fetch` wrote. The messages are the raw
// API items, decoded into the SDK's struct and turned into Candidates by the same candidateOf
// the live Source uses — so the replay measures the shipped rules.
func loadCorpus(t *testing.T, dir string) *corpusSource {
	src := &corpusSource{titles: map[string][]string{}, dms: map[string]string{}, messages: map[string][]Candidate{}}
	for _, line := range jsonLines(t, dir, "originals.jsonl") {
		var o original
		if err := json.Unmarshal(line, &o); err != nil {
			t.Fatal(err)
		}
		src.originals = append(src.originals, o)
	}
	readJSON(t, dir, "titles.json", &src.titles)
	readJSON(t, dir, "dms.json", &src.dms)
	for _, line := range jsonLines(t, dir, "messages.jsonl") {
		var m larkim.Message
		if err := json.Unmarshal(line, &m); err != nil {
			t.Fatal(err)
		}
		chat := str(m.ChatId)
		src.messages[chat] = append(src.messages[chat], candidateOf(&m))
	}
	return src
}

// TestReplayCorpus replays a day of real Originals through the Lookup the Backend ships, over
// a corpus pulled by `tools/larklish-helper replay fetch`. The corpus is real colleagues'
// messages, so it is never committed; without it this test skips. It writes one line per
// Original to outcomes-go.tsv (the Layer 7 gate diffed it against the Kotlin replay's).
func TestReplayCorpus(t *testing.T) {
	dir := "../replay-corpus"
	if _, err := os.Stat(filepath.Join(dir, "originals.jsonl")); err != nil {
		t.Skip("no corpus — run `tools/larklish-helper replay fetch`")
	}
	src := loadCorpus(t, dir)
	f := quiet(src)
	var lines []string
	counts := map[string]int{}
	for i, o := range src.originals {
		pick := f.FullTextOf(t.Context(), o.Title, ParsePreview(o.Text), o.WhenMs)
		if pick.Found != nil {
			lines = append(lines, fmt.Sprintf("%d\tfound\t%s\t%d", i, pick.ChatID, pick.Found.CreateTime))
			counts["found"]++
		} else {
			lines = append(lines, fmt.Sprintf("%d\t%s\t\t", i, pick.Reason))
			counts[pick.Reason]++
		}
	}
	if err := os.WriteFile(filepath.Join(dir, "outcomes-go.tsv"), []byte(strings.Join(lines, "\n")+"\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	reasons := make([]string, 0, len(counts))
	for r := range counts {
		reasons = append(reasons, r)
	}
	sort.Slice(reasons, func(i, j int) bool { return counts[reasons[i]] > counts[reasons[j]] })
	t.Logf("replay: %d of %d Originals resolved", counts["found"], len(src.originals))
	for _, r := range reasons {
		t.Logf("  %4d  %s", counts[r], r)
	}
	// Experiment 11 measured 119 of 210 for these rules; a real regression drops far below half.
	if counts["found"]*2 < len(src.originals) {
		t.Errorf("only %d of %d resolved", counts["found"], len(src.originals))
	}
}
