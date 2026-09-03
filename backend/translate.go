package main

import (
	"context"
	"errors"
	"log"
	"unicode"

	lark "github.com/larksuite/oapi-sdk-go/v3"
	larktranslation "github.com/larksuite/oapi-sdk-go/v3/service/translation/v1"
)

// the translation API's limit; the phone cut here too
const maxTranslateChars = 1000

// Lark ran its own detection and decided there was nothing to translate. A retry cannot change that.
var errNotTranslated = errors.New("Lark returned the input unchanged")

// Translator is Lark's engine over the SDK (tenant token: the SDK's own cache).
type Translator struct {
	client *lark.Client
}

func (t *Translator) zhToEn(ctx context.Context, text string) (string, error) {
	resp, err := t.client.Translation.V1.Text.Translate(ctx, larktranslation.NewTranslateTextReqBuilder().
		Body(larktranslation.NewTranslateTextReqBodyBuilder().SourceLanguage("zh").TargetLanguage("en").Text(text).Build()).Build())
	if err != nil {
		return "", larkErr("/open-apis/translation/v1/text/translate", 0, -1, err.Error())
	}
	if !resp.Success() {
		return "", larkErr("/open-apis/translation/v1/text/translate", resp.StatusCode, resp.Code, resp.Msg)
	}
	// Lark runs its own language detection and returns Latin-heavy text untouched (Experiment 05).
	// The caller only sends Han text, so "unchanged" means "not translated".
	out := str(resp.Data.Text)
	if out == text {
		return "", errNotTranslated
	}
	return out, nil
}

// EnglishOf is the phone's `Translator.englishOf` plus its retry: only Han text goes to the
// engine, and a failed call is retried once (Lark's failures are transport-level and clustered,
// Experiment 11). nil means "no English": the phone falls back to its own path (Lark once more,
// then ML Kit with the `~` mark), so the record keeps its fallback rows.
func (t *Translator) EnglishOf(ctx context.Context, text string) *string {
	if !hasHan(text) {
		return &text
	}
	for attempt := 0; attempt < 2; attempt++ {
		out, err := t.zhToEn(ctx, text)
		if err == nil {
			return &out
		}
		log.Printf("translate failed: %v", err)
		if errors.Is(err, errNotTranslated) {
			break
		}
	}
	return nil
}

// hasHan: the same property as the phone's `Char.isHan` (Character.isIdeographic).
func hasHan(s string) bool {
	for _, r := range s {
		if unicode.Is(unicode.Ideographic, r) {
			return true
		}
	}
	return false
}

// cut keeps the first n characters, like the phone's `take(n)` before the translate call.
func cut(s string, n int) string {
	r := []rune(s)
	if len(r) <= n {
		return s
	}
	return string(r[:n])
}
