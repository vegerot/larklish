package main

import (
	"crypto/subtle"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"runtime"
	"time"
)

// LookupRequest is what the phone sends for one cut Preview: the Original's title, its raw
// text (`Sender: message...`), `sbn.postTime`, and the user access token the phone holds.
type LookupRequest struct {
	Title     string `json:"title"`
	Text      string `json:"text"`
	WhenMs    int64  `json:"whenMs"`
	UserToken string `json:"userToken"`
	FlowID    string `json:"flowId,omitempty"`
}

// LookupResponse: `found` with the Full text and its English (null when Lark would not
// translate it), or `skipped` with the reason the phone records.
type LookupResponse struct {
	Outcome  string       `json:"outcome"`
	Reason   string       `json:"reason,omitempty"`
	MsgType  string       `json:"msgType,omitempty"`
	ChatID   string       `json:"chatId,omitempty"`
	FullText string       `json:"fullText,omitempty"`
	English  *string      `json:"english"`
	Timings  []TimingSpan `json:"timings"`
}

// Server is the Backend: one Lookup per request, the chat cache for the helper, and a liveness probe.
type Server struct {
	fetcher    *Fetcher
	translator *Translator
}

func (s *Server) routes(token string) http.Handler {
	if token == "" {
		panic("Backend token must not be empty")
	}
	authenticated := func(next http.HandlerFunc) http.HandlerFunc {
		return func(w http.ResponseWriter, r *http.Request) {
			if subtle.ConstantTimeCompare([]byte(r.Header.Get("Authorization")), []byte("Bearer "+token)) != 1 {
				w.Header().Set("WWW-Authenticate", "Bearer")
				http.Error(w, "unauthorized", http.StatusUnauthorized)
				return
			}
			next(w, r)
		}
	}
	mux := http.NewServeMux()
	mux.HandleFunc("POST /lookup", authenticated(s.handleLookup))
	mux.HandleFunc("POST /_/test/demo/larklish/lookup", authenticated(s.handleLookup))
	mux.HandleFunc("GET /chats", authenticated(func(w http.ResponseWriter, r *http.Request) { writeJSON(w, s.fetcher.Chats()) }))
	// ByteFaaS's liveness probe; the body names the Go that built the running binary, so a
	// deploy is visible from the outside (`tools/larklish-helper backend status`).
	mux.HandleFunc("GET /v1/ping", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "pong %s %s/%s", runtime.Version(), runtime.GOOS, runtime.GOARCH)
	})
	return mux
}

func (s *Server) handleLookup(w http.ResponseWriter, r *http.Request) {
	start := time.Now()
	var req LookupRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Title == "" || req.Text == "" || req.WhenMs == 0 || req.UserToken == "" {
		http.Error(w, "need title, text, whenMs, userToken", http.StatusBadRequest)
		return
	}
	timing := &lookupTiming{}
	defer func() {
		// Log successful and skipped Lookups alike for remote soaking; no message content or tokens.
		entry, _ := json.Marshal(struct {
			FlowID  string       `json:"flowId"`
			TotalMs int64        `json:"totalMs"`
			Spans   []TimingSpan `json:"timings"`
		}{req.FlowID, time.Since(start).Milliseconds(), timing.snapshot()})
		infoLog.Printf("lookup timing %s", entry)
	}()
	ctx := withTiming(withToken(r.Context(), req.UserToken), timing)
	var pick Pick
	timing.stage("lookup", func() { pick = s.fetcher.FullTextOf(ctx, req.Title, ParsePreview(req.Text), req.WhenMs) })
	if pick.Found == nil {
		logger := infoLog
		if pick.failed() {
			logger = log.Default()
		}
		logger.Printf("[%s] → %s", req.Title, pick.Reason)
		writeJSON(w, LookupResponse{Outcome: "skipped", Reason: pick.Reason, Timings: timing.snapshot()})
		return
	}
	full := pick.Found.Text
	var english *string
	timing.stage("translate", func() { english = s.translator.EnglishOf(ctx, cut(full, maxTranslateChars)) })
	infoLog.Printf("[%s] → found %s in %s, english: %v", req.Title, pick.Found.MsgType, pick.ChatID, english != nil)
	writeJSON(w, LookupResponse{Outcome: "found", MsgType: pick.Found.MsgType, ChatID: pick.ChatID, FullText: full, English: english, Timings: timing.snapshot()})
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	_ = json.NewEncoder(w).Encode(v)
}
