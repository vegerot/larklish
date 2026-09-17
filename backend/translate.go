package main

import (
	"context"
	"errors"
	"fmt"
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

type translation struct {
	English *string
	Failure string
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

// EnglishOf translates Han text and retries one transient failure. The Backend is the one owner of
// Lark translation now, so callers receive the final failure category rather than asking the phone
// to try the same API again.
func (t *Translator) EnglishOf(ctx context.Context, text string) translation {
	if !hasHan(text) {
		return translation{English: &text}
	}
	var last error
	for attempt := 0; attempt < 2; attempt++ {
		out, err := t.zhToEn(ctx, text)
		if err == nil {
			return translation{English: &out}
		}
		last = err
		log.Printf("translate failed: %v", err)
		if errors.Is(err, errNotTranslated) {
			break
		}
	}
	return translation{Failure: translationFailure(last)}
}

func translationFailure(err error) string {
	if errors.Is(err, errNotTranslated) {
		return "not-translated"
	}
	var larkErr larkError
	if errors.As(err, &larkErr) {
		return fmt.Sprintf("lark:%d", larkErr.code)
	}
	return "transport"
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

// isHanName mirrors Android's String.isHanName: keep the phone's useful romanization for bare
// Chinese personal names instead of replacing it with a Backend translation.
func isHanName(s string) bool {
	n := 0
	for _, r := range s {
		if !unicode.Is(unicode.Ideographic, r) {
			return false
		}
		n++
	}
	return n >= 2 && n <= 4
}

// cut keeps the first n characters, like the phone's `take(n)` before the translate call.
func cut(s string, n int) string {
	r := []rune(s)
	if len(r) <= n {
		return s
	}
	return string(r[:n])
}
