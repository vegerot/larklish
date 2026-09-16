package main

import (
	"context"
	"net/http"
	"sync"
	"time"
)

type TimingSpan struct {
	Name   string `json:"name"`
	Ms     int64  `json:"ms"`
	Status int    `json:"status,omitempty"`
	Failed bool   `json:"failed,omitempty"`
}

type lookupTiming struct {
	mu    sync.Mutex
	spans []TimingSpan
}

type timingKey struct{}

func withTiming(ctx context.Context, timing *lookupTiming) context.Context {
	return context.WithValue(ctx, timingKey{}, timing)
}

func (t *lookupTiming) add(span TimingSpan) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.spans = append(t.spans, span)
}

func (t *lookupTiming) snapshot() []TimingSpan {
	t.mu.Lock()
	defer t.mu.Unlock()
	return append([]TimingSpan{}, t.spans...)
}

func (t *lookupTiming) stage(name string, work func()) {
	start := time.Now()
	work()
	t.add(TimingSpan{Name: name, Ms: time.Since(start).Milliseconds()})
}

// The SDK uses this client for both user calls and its own tenant-token fetch.
type timedHTTPClient struct{ client *http.Client }

func (c timedHTTPClient) Do(req *http.Request) (*http.Response, error) {
	start := time.Now()
	resp, err := c.client.Do(req)
	if timing, ok := req.Context().Value(timingKey{}).(*lookupTiming); ok {
		span := TimingSpan{Name: req.Method + " " + req.URL.Path, Ms: time.Since(start).Milliseconds(), Failed: err != nil}
		if resp != nil {
			span.Status = resp.StatusCode
			span.Failed = span.Failed || resp.StatusCode >= 400
		}
		timing.add(span)
	}
	return resp, err
}
