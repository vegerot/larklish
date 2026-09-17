package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	larkim "github.com/larksuite/oapi-sdk-go/v3/service/im/v1"
)

// fakeLark answers the four endpoints a Lookup touches, and records the user token it saw.
func fakeLark(t *testing.T) (*httptest.Server, *[]string) {
	var bearers []string
	mux := http.NewServeMux()
	reply := func(w http.ResponseWriter, body string) { // the SDK insists on the JSON content type
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		_, _ = w.Write([]byte(body))
	}
	mux.HandleFunc("/open-apis/auth/v3/tenant_access_token/internal", func(w http.ResponseWriter, r *http.Request) {
		reply(w, `{"code":0,"msg":"ok","tenant_access_token":"t-fake","expire":7200}`)
	})
	mux.HandleFunc("/open-apis/im/v1/chats/search", func(w http.ResponseWriter, r *http.Request) {
		bearers = append(bearers, r.Header.Get("Authorization"))
		reply(w, `{"code":0,"msg":"success","data":{"items":[
			{"chat_id":"oc_other","name":"Larklish 测试群 2"},
			{"chat_id":"oc_test","name":"Larklish 测试群"}]}}`)
	})
	mux.HandleFunc("/open-apis/im/v1/messages", func(w http.ResponseWriter, r *http.Request) {
		bearers = append(bearers, r.Header.Get("Authorization"))
		if r.URL.Query().Get("container_id") != "oc_test" || r.URL.Query().Get("start_time") == "" {
			t.Errorf("unexpected messages query: %s", r.URL.RawQuery)
		}
		reply(w, `{"code":0,"msg":"success","data":{"items":[
			{"message_id":"om_1","msg_type":"text","create_time":"1787869580000","deleted":false,
			 "body":{"content":"{\"text\":\"<p>这是一条很长的消息，前四十五个字符里没有边界</p><p>第二段 @_user_1</p>\"}"},
			 "mentions":[{"key":"@_user_1","name":"Max Coplan"}]}]}}`)
	})
	mux.HandleFunc("/open-apis/translation/v1/text/translate", func(w http.ResponseWriter, r *http.Request) {
		var body struct {
			Text string `json:"text"`
		}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			t.Fatal(err)
		}
		if strings.Contains(body.Text, "失败") {
			reply(w, `{"code":99991400,"msg":"rate limited"}`)
			return
		}
		reply(w, `{"code":0,"msg":"success","data":{"text":"This is a long message\nSecond paragraph @Max Coplan"}}`)
	})
	srv := httptest.NewServer(mux)
	t.Cleanup(srv.Close)
	return srv, &bearers
}

func TestALookupThroughTheServer(t *testing.T) {
	lark, bearers := fakeLark(t)
	client := newLark("cli_test", "secret", lark.URL)
	s := &Server{fetcher: NewFetcher(&liveSource{client}), translator: &Translator{client}}
	s.fetcher.Log = func(string, ...any) {}

	body := `{"title":"Larklish 测试群","text":"Bot: 这是一条很长的消息，前四十五个...","whenMs":1787869583000,"userToken":"u-test","flowId":"test-flow"}`
	rec := httptest.NewRecorder()
	req := httptest.NewRequest("POST", "/_/test/demo/larklish/lookup", strings.NewReader(body))
	req.Header.Set("Authorization", "Bearer test-backend-token")
	s.routes("test-backend-token").ServeHTTP(rec, req)
	if rec.Code != 200 {
		t.Fatalf("status %d: %s", rec.Code, rec.Body)
	}
	var got LookupResponse
	if err := json.Unmarshal(rec.Body.Bytes(), &got); err != nil {
		t.Fatal(err)
	}
	if got.Outcome != "found" || got.ChatID != "oc_test" || got.MsgType != "text" {
		t.Errorf("got %+v", got)
	}
	if got.Source != "full-text" {
		t.Errorf("source = %q, want full-text", got.Source)
	}
	if want := "这是一条很长的消息，前四十五个字符里没有边界\n第二段 @Max Coplan"; got.Message != want {
		t.Errorf("message = %q, want %q", got.Message, want)
	}
	if got.English == nil || !strings.HasPrefix(*got.English, "This is a long message") {
		t.Errorf("english = %v", got.English)
	}
	wantCalls := map[string]bool{
		"GET /open-apis/im/v1/chats/search":                    false,
		"GET /open-apis/im/v1/messages":                        false,
		"POST /open-apis/auth/v3/tenant_access_token/internal": false,
		"POST /open-apis/translation/v1/text/translate":        false,
		"lookup":            false,
		"translate.message": false,
		"translate.title":   false,
	}
	for _, span := range got.Timings {
		if span.Ms < 0 || span.Failed {
			t.Errorf("unexpected timing span: %+v", span)
		}
		if _, ok := wantCalls[span.Name]; ok {
			wantCalls[span.Name] = true
		}
	}
	for name, seen := range wantCalls {
		if !seen {
			t.Errorf("missing timing for %s: %+v", name, got.Timings)
		}
	}
	for _, b := range *bearers {
		if b != "Bearer u-test" {
			t.Errorf("a user-identity call carried %q", b)
		}
	}
	if len(*bearers) != 2 {
		t.Errorf("expected chats/search + messages under the user token, saw %d calls", len(*bearers))
	}

	rec = httptest.NewRecorder()
	req = httptest.NewRequest("POST", "/lookup", strings.NewReader(`{"title":"x"}`))
	req.Header.Set("Authorization", "Bearer test-backend-token")
	s.routes("test-backend-token").ServeHTTP(rec, req)
	if rec.Code != 400 {
		t.Errorf("incomplete request: status %d", rec.Code)
	}
}

func TestCompletePreviewSkipsLookupAndUserToken(t *testing.T) {
	lark, bearers := fakeLark(t)
	client := newLark("cli_test", "secret", lark.URL)
	s := &Server{fetcher: NewFetcher(&liveSource{client}), translator: &Translator{client}}
	s.fetcher.Log = func(string, ...any) {}

	rec := httptest.NewRecorder()
	req := httptest.NewRequest("POST", "/lookup", strings.NewReader(`{"title":"English group","text":"陈昱萌: 中文消息","flowId":"preview-flow"}`))
	req.Header.Set("Authorization", "Bearer test-backend-token")
	s.routes("test-backend-token").ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("status %d: %s", rec.Code, rec.Body)
	}
	var got LookupResponse
	if err := json.Unmarshal(rec.Body.Bytes(), &got); err != nil {
		t.Fatal(err)
	}
	if got.Outcome != "found" || got.Source != "preview" || got.MsgType != "preview" || got.English == nil {
		t.Fatalf("got %+v", got)
	}
	if got.Sender != nil { // bare Chinese names keep the phone's romanization
		t.Errorf("sender = %q, want nil", *got.Sender)
	}
	if len(*bearers) != 0 {
		t.Errorf("complete Preview made user calls: %v", *bearers)
	}
	for _, span := range got.Timings {
		if span.Name == "lookup" || strings.Contains(span.Name, "/im/v1/") || span.Name == "translate.sender" {
			t.Errorf("complete Preview unexpectedly ran %s", span.Name)
		}
	}
}

func TestTranslationFailuresKeepAUsableMessageSeparate(t *testing.T) {
	lark, _ := fakeLark(t)
	client := newLark("cli_test", "secret", lark.URL)
	s := &Server{fetcher: NewFetcher(&liveSource{client}), translator: &Translator{client}}
	s.fetcher.Log = func(string, ...any) {}
	handler := s.routes("test-backend-token")

	request := func(body string) LookupResponse {
		rec := httptest.NewRecorder()
		req := httptest.NewRequest("POST", "/lookup", strings.NewReader(body))
		req.Header.Set("Authorization", "Bearer test-backend-token")
		handler.ServeHTTP(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("status %d: %s", rec.Code, rec.Body)
		}
		var got LookupResponse
		if err := json.Unmarshal(rec.Body.Bytes(), &got); err != nil {
			t.Fatal(err)
		}
		return got
	}

	partial := request(`{"title":"失败标题","text":"Bot失败: 中文消息","flowId":"partial"}`)
	if partial.Outcome != "found" || partial.English == nil || len(partial.Failures) != 2 {
		t.Errorf("partial result = %+v", partial)
	}
	if partial.Title != nil || partial.Sender != nil {
		t.Errorf("failed fields must be nil: %+v", partial)
	}
	failed := request(`{"title":"English group","text":"Bot: 失败消息","flowId":"message"}`)
	if failed.Outcome != "translation-failed" || failed.Reason != "lark:99991400" || failed.English != nil || len(failed.Failures) != 1 || failed.Failures[0].Field != "message" {
		t.Errorf("message failure = %+v", failed)
	}
}

func TestBackendAuthentication(t *testing.T) {
	s := &Server{fetcher: NewFetcher(nil)}
	handler := s.routes("test-backend-token")
	for _, tc := range []struct {
		name, method, path, authorization string
		status                            int
	}{
		{"public health probe", "GET", "/v1/ping", "", http.StatusOK},
		{"Lookup without token", "POST", "/lookup", "", http.StatusUnauthorized},
		{"Lookup with wrong token", "POST", "/lookup", "Bearer wrong", http.StatusUnauthorized},
		{"Lookup with valid token reaches validation", "POST", "/lookup", "Bearer test-backend-token", http.StatusBadRequest},
		{"prefixed Lookup without token", "POST", "/_/test/demo/larklish/lookup", "", http.StatusUnauthorized},
		{"prefixed Lookup with wrong token", "POST", "/_/test/demo/larklish/lookup", "Bearer wrong", http.StatusUnauthorized},
		{"prefixed Lookup with valid token reaches validation", "POST", "/_/test/demo/larklish/lookup", "Bearer test-backend-token", http.StatusBadRequest},
		{"prefixed cache stays hidden", "GET", "/_/test/demo/larklish/chats", "Bearer test-backend-token", http.StatusNotFound},
		{"prefixed health probe stays hidden", "GET", "/_/test/demo/larklish/v1/ping", "", http.StatusNotFound},
		{"prefixed Lookup rejects GET", "GET", "/_/test/demo/larklish/lookup", "Bearer test-backend-token", http.StatusMethodNotAllowed},
		{"cache without token", "GET", "/chats", "", http.StatusUnauthorized},
		{"cache with wrong token", "GET", "/chats", "Bearer wrong", http.StatusUnauthorized},
		{"cache with valid token", "GET", "/chats", "Bearer test-backend-token", http.StatusOK},
	} {
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequest(tc.method, tc.path, nil)
			req.Header.Set("Authorization", tc.authorization)
			rec := httptest.NewRecorder()
			handler.ServeHTTP(rec, req)
			if rec.Code != tc.status {
				t.Fatalf("status = %d, want %d: %s", rec.Code, tc.status, rec.Body)
			}
			if rec.Code == http.StatusUnauthorized && rec.Header().Get("WWW-Authenticate") != "Bearer" {
				t.Fatal("missing Bearer challenge")
			}
		})
	}
}

func TestBackendRejectsEmptyTokenConfiguration(t *testing.T) {
	defer func() {
		if recover() == nil {
			t.Fatal("empty Backend token must prevent startup")
		}
	}()
	(&Server{}).routes("")
}

func TestCandidateOfFlattensAPost(t *testing.T) {
	content := `{"title":"周报","content":[[{"tag":"text","text":"完成 "},{"tag":"a","text":"链接","href":"https://x"}],[{"tag":"at","user_id":"ou_1","user_name":"Max"}],[{"tag":"img","image_key":"img_1"}],[{"tag":"emotion","emoji_type":"Delighted"}]]}`
	kind, created, deleted := "post", "1787869580000", false
	m := &larkim.Message{MsgType: &kind, CreateTime: &created, Deleted: &deleted, Body: &larkim.MessageBody{Content: &content}}
	got := candidateOf(m)
	if want := "周报\n完成 链接\n@Max\n[image]\n[Delighted]"; got.Text != want || got.CreateTimeMs != 1787869580000 {
		t.Errorf("got %+v, want text %q", got, want)
	}
}
