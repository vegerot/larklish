package main

import (
	"context"
	"fmt"
	"time"

	lark "github.com/larksuite/oapi-sdk-go/v3"
	larkcore "github.com/larksuite/oapi-sdk-go/v3/core"
)

// newLark is the official SDK client. It fetches and renews the tenant token itself (the
// translate call); user-identity calls carry the phone's token per request (withToken).
func newLark(appID, appSecret, host string) *lark.Client {
	return lark.NewClient(appID, appSecret, lark.WithOpenBaseUrl(host), lark.WithReqTimeout(15*time.Second),
		lark.WithLogLevel(larkcore.LogLevelWarn))
}

// larkErr words a failed call like the phone's LarkHttp did, so the record's `error:` rows read the same.
func larkErr(path string, status, code int, msg string) error {
	return fmt.Errorf("Lark %s: http %d code %d %s", path, status, code, msg)
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
