package main

import (
	"encoding/json"
	"log"
	"net/http"
)

// LookupRequest is what the phone sends for one cut Preview: the Original's title, its raw
// text (`Sender: message...`), `sbn.postTime`, and the user access token the phone holds.
type LookupRequest struct {
	Title     string `json:"title"`
	Text      string `json:"text"`
	WhenMs    int64  `json:"whenMs"`
	UserToken string `json:"userToken"`
}

// LookupResponse: `found` with the Full text and its English (null when Lark would not
// translate it), or `skipped` with the reason the phone records.
type LookupResponse struct {
	Outcome  string  `json:"outcome"`
	Reason   string  `json:"reason,omitempty"`
	MsgType  string  `json:"msgType,omitempty"`
	ChatID   string  `json:"chatId,omitempty"`
	FullText string  `json:"fullText,omitempty"`
	English  *string `json:"english"`
}

// Server is the Backend: one Lookup per request, the chat cache for the helper, and a liveness probe.
type Server struct {
	fetcher    *Fetcher
	translator *Translator
}

func (s *Server) routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /lookup", s.handleLookup)
	mux.HandleFunc("GET /chats", func(w http.ResponseWriter, r *http.Request) { writeJSON(w, s.fetcher.Chats()) })
	mux.HandleFunc("GET /v1/ping", func(w http.ResponseWriter, r *http.Request) { _, _ = w.Write([]byte("pong")) })
	return mux
}

func (s *Server) handleLookup(w http.ResponseWriter, r *http.Request) {
	var req LookupRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Title == "" || req.Text == "" || req.WhenMs == 0 || req.UserToken == "" {
		http.Error(w, "need title, text, whenMs, userToken", http.StatusBadRequest)
		return
	}
	ctx := withToken(r.Context(), req.UserToken)
	pick := s.fetcher.FullTextOf(ctx, req.Title, ParsePreview(req.Text), req.WhenMs)
	if pick.Found == nil {
		log.Printf("[%s] → %s", req.Title, pick.Reason)
		writeJSON(w, LookupResponse{Outcome: "skipped", Reason: pick.Reason})
		return
	}
	full := pick.Found.Text
	english := s.translator.EnglishOf(ctx, cut(full, maxTranslateChars))
	log.Printf("[%s] → found %s in %s, english: %v", req.Title, pick.Found.MsgType, pick.ChatID, english != nil)
	writeJSON(w, LookupResponse{Outcome: "found", MsgType: pick.Found.MsgType, ChatID: pick.ChatID, FullText: full, English: english})
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	_ = json.NewEncoder(w).Encode(v)
}
