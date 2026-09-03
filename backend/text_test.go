package main

import "testing"

// What a `text` body reads as once Lark's editor markup is off (Experiment 11).
func TestUnwrapHtml(t *testing.T) {
	cases := []struct{ name, in, want string }{
		{"plain text is left alone", "a < b, and 3 > 2", "a < b, and 3 > 2"},
		{"paragraphs become lines", "<p>TTP 故障应急最新信息同步：</p><p>为缓解TTP2 容量风险，请知悉～</p>",
			"TTP 故障应急最新信息同步：\n为缓解TTP2 容量风险，请知悉～"},
		{"entities are decoded", "<p>a &amp; b &lt;tag&gt;</p>", "a & b <tag>"},
		{"an escaped entity stays escaped", "<p>&amp;lt;</p>", "&lt;"}, // the text `&lt;`, not `<`
	}
	for _, c := range cases {
		if got := unwrapHtml(c.in); got != c.want {
			t.Errorf("%s: got %q, want %q", c.name, got, c.want)
		}
	}
}

// A `post` is title + paragraphs of tagged elements; one line per paragraph, like Lark's own Preview.
func TestPostText(t *testing.T) {
	got := postText("", [][]PostElement{
		{{Tag: "text", Text: "Hello "}, {Tag: "a", Text: "here"}},
		{{Tag: "at", UserName: "Max Coplan"}},
		{{Tag: "img"}},
		{{Tag: "emotion", EmojiType: "Delighted"}},
		{}, // an empty paragraph adds no line
		{{Tag: "media"}},
	})
	if want := "Hello here\n@Max Coplan\n[image]\n[Delighted]\n[media]"; got != want {
		t.Errorf("got %q, want %q", got, want)
	}
	if got := postText("Title", [][]PostElement{{{Tag: "emotion"}}}); got != "Title\n[emotion]" {
		t.Errorf("title + bare emotion: got %q", got)
	}
}
