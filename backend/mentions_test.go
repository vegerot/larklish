package main

import "testing"

func TestResolveMentions(t *testing.T) {
	cases := []struct {
		name, text string
		mentions   map[string]string
		want       string
	}{
		{"placeholders become @names", "@_user_1 这条消息 @_user_2 谢谢",
			map[string]string{"@_user_1": "Max Coplan", "@_user_2": "Yinghao Li"}, "@Max Coplan 这条消息 @Yinghao Li 谢谢"},
		{"the tenth mention is not eaten by the first", "@_user_10 @_user_1",
			map[string]string{"@_user_1": "One", "@_user_10": "Ten"}, "@Ten @One"},
		{"unknown placeholders stay", "@_user_3 hi", nil, "@_user_3 hi"},
		// A `text` body spells @all as `@_all`, and the `mentions` array never names it (soak 2026-09-02).
		{"the @all mention has no entry and reads like the Preview", "Hi @_all Labor Day event setup is ready!", nil,
			"Hi @all Labor Day event setup is ready!"},
	}
	for _, c := range cases {
		if got := resolveMentions(c.text, c.mentions); got != c.want {
			t.Errorf("%s: got %q, want %q", c.name, got, c.want)
		}
	}
}
