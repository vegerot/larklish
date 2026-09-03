package main

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"testing"
)

// The separators `tools/larklish-helper replay fetch` writes: items, paragraphs, fields of an element.
const (
	sepItem  = "\x01"
	sepPara  = "\x02"
	sepField = "\x03"
)

// unescaped undoes the escaping the corpus applies to every leaf string.
func unescaped(s string) string {
	var b strings.Builder
	for i := 0; i < len(s); i++ {
		c := s[i]
		if c == '\\' && i+1 < len(s) {
			i++
			switch s[i] {
			case 'n':
				b.WriteByte('\n')
			case 't':
				b.WriteByte('\t')
			default:
				b.WriteByte(s[i])
			}
		} else {
			b.WriteByte(c)
		}
	}
	return b.String()
}

func items(s string) []string {
	var out []string
	for _, it := range strings.Split(s, sepItem) {
		if it != "" {
			out = append(out, it)
		}
	}
	return out
}

type original struct {
	whenMs      int64
	title, text string
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

func rows(t *testing.T, dir, name string) [][]string {
	data, err := os.ReadFile(filepath.Join(dir, name))
	if err != nil {
		t.Fatal(err)
	}
	var out [][]string
	for _, line := range strings.Split(string(data), "\n") {
		if strings.TrimSpace(line) != "" {
			out = append(out, strings.Split(line, "\t"))
		}
	}
	return out
}

func loadCorpus(t *testing.T, dir string) *corpusSource {
	src := &corpusSource{titles: map[string][]string{}, dms: map[string]string{}, messages: map[string][]Candidate{}}
	for _, r := range rows(t, dir, "originals.tsv") {
		when, _ := strconv.ParseInt(r[0], 10, 64)
		src.originals = append(src.originals, original{when, unescaped(r[1]), unescaped(r[2])})
	}
	for _, r := range rows(t, dir, "titles.tsv") {
		ids := ""
		if len(r) > 1 {
			ids = r[1]
		}
		src.titles[unescaped(r[0])] = items(ids)
	}
	for _, r := range rows(t, dir, "dms.tsv") {
		src.dms[unescaped(r[0])] = r[1]
	}
	for _, r := range rows(t, dir, "messages.tsv") {
		chat, kind := r[0], r[1]
		created, _ := strconv.ParseInt(r[2], 10, 64)
		deleted := r[3] == "1"
		mentions := map[string]string{}
		for _, it := range items(r[4]) {
			f := strings.Split(it, sepField)
			mentions[unescaped(f[0])] = unescaped(f[1])
		}
		payload := ""
		if len(r) > 5 {
			payload = r[5]
		}
		text := ""
		switch {
		case deleted:
		case kind == "text":
			text = unwrapHtml(unescaped(payload))
		case kind == "post":
			parts := strings.Split(payload, sepPara)
			var paragraphs [][]PostElement
			for _, para := range parts[1:] {
				var elements []PostElement
				for _, it := range items(para) {
					f := strings.Split(it, sepField)
					elements = append(elements, PostElement{unescaped(f[0]), unescaped(f[1]), unescaped(f[2]), unescaped(f[3])})
				}
				paragraphs = append(paragraphs, elements)
			}
			text = postText(unescaped(parts[0]), paragraphs)
		}
		src.messages[chat] = append(src.messages[chat], Candidate{
			MsgType: kind, CreateTime: created, Deleted: deleted, Text: resolveMentions(text, mentions),
		})
	}
	return src
}

// TestReplayCorpus replays a day of real Originals through the Lookup the Backend ships, over
// a corpus pulled by `tools/larklish-helper replay fetch`. The corpus is real colleagues'
// messages, so it is never committed; without it this test skips. It writes one line per
// Original to outcomes-go.tsv, the file the gate diffs against the Kotlin replay's.
func TestReplayCorpus(t *testing.T) {
	dir := "../replay-corpus"
	if _, err := os.Stat(filepath.Join(dir, "originals.tsv")); err != nil {
		t.Skip("no corpus — run `tools/larklish-helper replay fetch`")
	}
	src := loadCorpus(t, dir)
	f := quiet(src)
	var lines []string
	counts := map[string]int{}
	for i, o := range src.originals {
		pick := f.FullTextOf(t.Context(), o.title, ParsePreview(o.text), o.whenMs)
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
