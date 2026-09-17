package main

import (
	"context"
	"fmt"
	"net/http"
	"time"

	lark "github.com/larksuite/oapi-sdk-go/v3"
	larkcore "github.com/larksuite/oapi-sdk-go/v3/core"
)

// newLark is the official SDK client. It fetches and renews the tenant token itself (the
// translate call); user-identity calls carry the phone's token per request (withToken).
func newLark(appID, appSecret, host string) *lark.Client {
	return lark.NewClient(appID, appSecret, lark.WithOpenBaseUrl(host), lark.WithReqTimeout(15*time.Second),
		lark.WithLogLevel(larkcore.LogLevelWarn), lark.WithHttpClient(timedHTTPClient{&http.Client{Timeout: 15 * time.Second}}))
}

// larkError preserves a Lark error code for the Backend response and timing record.
type larkError struct {
	path       string
	statusCode int
	code       int
	message    string
}

func (e larkError) Error() string {
	return fmt.Sprintf("Lark %s: http %d code %d %s", e.path, e.statusCode, e.code, e.message)
}

// larkErr words a failed call like the phone's LarkHttp did, so the record's `error:` rows read the same.
func larkErr(path string, status, code int, msg string) error {
	return larkError{path: path, statusCode: status, code: code, message: msg}
}

type tokenKey struct{}

// withToken puts the phone's user access token on the context of one Lookup.
func withToken(ctx context.Context, token string) context.Context {
	return context.WithValue(ctx, tokenKey{}, token)
}

// asUser is the request option every user-identity call takes.
func asUser(ctx context.Context) larkcore.RequestOptionFunc {
	token, _ := ctx.Value(tokenKey{}).(string)
	return larkcore.WithUserAccessToken(token)
}
