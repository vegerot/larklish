package main

import (
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

type failingTransport struct{}

func (failingTransport) RoundTrip(*http.Request) (*http.Response, error) {
	return nil, errors.New("offline")
}

func TestTimedHTTPClientRecordsFailureWithoutURLSecrets(t *testing.T) {
	timing := &lookupTiming{}
	client := timedHTTPClient{&http.Client{Transport: failingTransport{}}}
	req := httptest.NewRequest("POST", "https://example.test/open-apis/translation/v1/text/translate?token=secret", strings.NewReader("private text"))
	_, err := client.Do(req.WithContext(withTiming(req.Context(), timing)))
	if err == nil {
		t.Fatal("expected transport error")
	}
	spans := timing.snapshot()
	if len(spans) != 1 || !spans[0].Failed || spans[0].Ms < 0 || spans[0].Name != "POST /open-apis/translation/v1/text/translate" {
		t.Fatalf("unexpected span: %+v", spans)
	}
}
