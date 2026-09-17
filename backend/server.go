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

// LookupRequest is what the phone sends for one Preview. Cut Previews also carry the notification
// time and user token because their Full text needs the Lookup.
type LookupRequest struct {
	Title     string `json:"title"`
	Text      string `json:"text"`
	WhenMs    int64  `json:"whenMs"`
	UserToken string `json:"userToken"`
	FlowID    string `json:"flowId,omitempty"`
}

type fieldFailure struct {
	Field  string `json:"field"`
	Reason string `json:"reason"`
}

// LookupResponse has a translated message from either the Preview or Full text. Individual title
// and Sender failures do not block a message Update. `translation-failed` leaves the ML Kit Relay.
type LookupResponse struct {
	Outcome  string         `json:"outcome"`
	Reason   string         `json:"reason,omitempty"`
	Source   string         `json:"source,omitempty"`
	MsgType  string         `json:"msgType,omitempty"`
	ChatID   string         `json:"chatId,omitempty"`
	Message  string         `json:"message,omitempty"`
	English  *string        `json:"english"`
	Title    *string        `json:"title"`
	Sender   *string        `json:"sender"`
	Failures []fieldFailure `json:"failures,omitempty"`
	Timings  []TimingSpan   `json:"timings"`
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
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Title == "" || req.Text == "" || req.FlowID == "" {
		http.Error(w, "need title, text, flowId", http.StatusBadRequest)
		return
	}
	preview := ParsePreview(req.Text)
	if preview.Truncated() && (req.WhenMs == 0 || req.UserToken == "") {
		http.Error(w, "cut Preview needs whenMs, userToken", http.StatusBadRequest)
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
	ctx := withTiming(r.Context(), timing)
	source, message, msgType := "preview", preview.Message, "preview"
	chatID := ""
	if preview.Truncated() {
		ctx = withToken(ctx, req.UserToken)
		var pick Pick
		timing.stage("lookup", func() { pick = s.fetcher.FullTextOf(ctx, req.Title, preview, req.WhenMs) })
		if pick.Found == nil {
			logger := infoLog
			if pick.failed() {
				logger = log.Default()
			}
			logger.Printf("[%s] → %s", req.Title, pick.Reason)
			writeJSON(w, LookupResponse{Outcome: "skipped", Reason: pick.Reason, Timings: timing.snapshot()})
			return
		}
		source, message, msgType, chatID = "full-text", pick.Found.Text, pick.Found.MsgType, pick.ChatID
	}
	var messageTranslation translation
	timing.stage("translate.message", func() { messageTranslation = s.translator.EnglishOf(ctx, cut(message, maxTranslateChars)) })
	if messageTranslation.English == nil {
		failure := fieldFailure{Field: "message", Reason: messageTranslation.Failure}
		infoLog.Printf("[%s] → translation-failed %s", req.Title, failure.Reason)
		writeJSON(w, LookupResponse{Outcome: "translation-failed", Reason: failure.Reason, Source: source, MsgType: msgType, ChatID: chatID, Message: message, Failures: []fieldFailure{failure}, Timings: timing.snapshot()})
		return
	}
	answer := LookupResponse{Outcome: "found", Source: source, MsgType: msgType, ChatID: chatID, Message: message, English: messageTranslation.English}
	if hasHan(req.Title) {
		var title translation
		timing.stage("translate.title", func() { title = s.translator.EnglishOf(ctx, req.Title) })
		answer.Title = title.English
		if title.English == nil {
			answer.Failures = append(answer.Failures, fieldFailure{Field: "title", Reason: title.Failure})
		}
	}
	if hasHan(preview.Sender) && !isHanName(preview.Sender) {
		var sender translation
		timing.stage("translate.sender", func() { sender = s.translator.EnglishOf(ctx, preview.Sender) })
		answer.Sender = sender.English
		if sender.English == nil {
			answer.Failures = append(answer.Failures, fieldFailure{Field: "sender", Reason: sender.Failure})
		}
	}
	infoLog.Printf("[%s] → found %s (%s), english: true", req.Title, msgType, source)
	answer.Timings = timing.snapshot()
	writeJSON(w, answer)
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	_ = json.NewEncoder(w).Encode(v)
}
