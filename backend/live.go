package main

import (
	"context"
	"encoding/json"
	"html"
	"strconv"
	"strings"
	"time"

	lark "github.com/larksuite/oapi-sdk-go/v3"
	larkim "github.com/larksuite/oapi-sdk-go/v3/service/im/v1"
)

// a truncated title can name many chats (`【bytedcli MR 处理】` names 6); try this many (Experiment 11)
const prefixTries = 5

// the search index lags ~15 s behind the list (Experiment 06, E2 continued)
const searchTries = 4
const searchGap = 2 * time.Second

// liveSource is the Open API under the phone's user identity.
type liveSource struct {
	client *lark.Client
}

// GroupChatIDs: Lark truncates long titles with `...`; then every group whose name starts with
// the stem, else the exact name.
func (s *liveSource) GroupChatIDs(ctx context.Context, title string) ([]string, error) {
	stem := strings.TrimSuffix(title, ellipsis)
	resp, err := s.client.Im.V1.Chat.Search(ctx,
		larkim.NewSearchChatReqBuilder().Query(stem).PageSize(20).Build(), asUser(ctx))
	if err != nil {
		return nil, larkErr("/open-apis/im/v1/chats/search", 0, -1, err.Error())
	}
	if !resp.Success() {
		return nil, larkErr("/open-apis/im/v1/chats/search", resp.StatusCode, resp.Code, resp.Msg)
	}
	var ids []string
	for _, c := range resp.Data.Items {
		name := str(c.Name)
		if (title != stem && strings.HasPrefix(name, stem)) || name == title {
			ids = append(ids, str(c.ChatId))
		}
		if len(ids) == prefixTries {
			break
		}
	}
	return ids, nil
}

// DMChatID: `messages/search` around the Original's time; the p2p hit whose peer name (first
// line of `display_info`) is the Sender. `chat_type` is ignored without a `query`, so filter on
// `is_p2p_chat`. Polls because the index lags.
func (s *liveSource) DMChatID(ctx context.Context, p Preview, whenMs int64) (string, error) {
	window := larkim.NewTimeRangeBuilder().
		StartTime(time.UnixMilli(whenMs - 60_000).UTC().Format(time.RFC3339)).
		EndTime(time.UnixMilli(whenMs + 60_000).UTC().Format(time.RFC3339)).Build()
	body := larkim.NewSearchMessageReqBodyBuilder().Filter(larkim.NewMessageSearchFilterBuilder().TimeRange(window).Build())
	if stem := p.Stem(); stem != "" {
		body.Query(stem)
	}
	req := larkim.NewSearchMessageReqBuilder().PageSize(20).Body(body.Build()).Build()
	for attempt := 0; attempt < searchTries; attempt++ {
		if attempt > 0 {
			select {
			case <-time.After(searchGap * time.Duration(attempt)):
			case <-ctx.Done():
				return "", ctx.Err()
			}
		}
		resp, err := s.client.Im.V1.Message.Search(ctx, req, asUser(ctx))
		if err != nil {
			return "", larkErr("/open-apis/im/v1/messages/search", 0, -1, err.Error())
		}
		if !resp.Success() {
			return "", larkErr("/open-apis/im/v1/messages/search", resp.StatusCode, resp.Code, resp.Msg)
		}
		for _, hit := range resp.Data.Items {
			if hit.MetaData != nil && hit.MetaData.IsP2pChat != nil && *hit.MetaData.IsP2pChat && peerName(str(hit.DisplayInfo)) == p.Sender {
				return str(hit.MetaData.ChatId), nil
			}
		}
	}
	return "", nil
}

// peerName is the first line of a search hit's `display_info`, without Lark's `<h>` highlight tags.
func peerName(displayInfo string) string {
	first, _, _ := strings.Cut(displayInfo, "\n")
	return html.UnescapeString(tagRe.ReplaceAllString(first, ""))
}

// Messages: the chat's newest messages inside the widest window pickMessage uses, so a burst in
// a busy chat cannot push the message off the page.
func (s *liveSource) Messages(ctx context.Context, chatID string, whenMs int64) ([]Candidate, error) {
	resp, err := s.client.Im.V1.Message.List(ctx, larkim.NewListMessageReqBuilder().
		ContainerIdType("chat").ContainerId(chatID).SortType("ByCreateTimeDesc").PageSize(20).
		StartTime(strconv.FormatInt((whenMs-beforeDelayedMs)/1000, 10)).
		EndTime(strconv.FormatInt((whenMs+afterMs)/1000, 10)).Build(), asUser(ctx))
	if err != nil {
		return nil, larkErr("/open-apis/im/v1/messages", 0, -1, err.Error())
	}
	if !resp.Success() {
		return nil, larkErr("/open-apis/im/v1/messages", resp.StatusCode, resp.Code, resp.Msg)
	}
	items := make([]Candidate, 0, len(resp.Data.Items))
	for _, m := range resp.Data.Items {
		items = append(items, candidateOf(m))
	}
	return items, nil
}

// candidateOf reduces a message to what pickMessage needs. A recalled message stays in the list
// as "This message was recalled", so `deleted` blanks its text.
func candidateOf(m *larkim.Message) Candidate {
	kind := str(m.MsgType)
	deleted := m.Deleted != nil && *m.Deleted
	mentions := map[string]string{}
	for _, x := range m.Mentions {
		mentions[str(x.Key)] = str(x.Name)
	}
	content := ""
	if m.Body != nil {
		content = str(m.Body.Content)
	}
	text := ""
	switch {
	case deleted:
	case kind == "text":
		var body struct{ Text string }
		_ = json.Unmarshal([]byte(content), &body)
		text = unwrapHtml(body.Text)
	case kind == "post":
		text = postText(parsePost(content))
	}
	created, _ := strconv.ParseInt(str(m.CreateTime), 10, 64)
	return Candidate{ID: str(m.MessageId), MsgType: kind, CreateTime: created, Deleted: deleted, Text: resolveMentions(text, mentions)}
}

// parsePost maps Lark's JSON shape of a `post` — `{"title": "", "content": [[{tag, text, …}]]}` —
// onto postText's input.
func parsePost(content string) (string, [][]PostElement) {
	var post struct {
		Title   string `json:"title"`
		Content [][]struct {
			Tag       string `json:"tag"`
			Text      string `json:"text"`
			UserName  string `json:"user_name"`
			EmojiType string `json:"emoji_type"`
		} `json:"content"`
	}
	_ = json.Unmarshal([]byte(content), &post)
	paragraphs := make([][]PostElement, len(post.Content))
	for i, para := range post.Content {
		for _, e := range para {
			paragraphs[i] = append(paragraphs[i], PostElement{e.Tag, e.Text, e.UserName, e.EmojiType})
		}
	}
	return post.Title, paragraphs
}

func str(s *string) string {
	if s == nil {
		return ""
	}
	return *s
}
