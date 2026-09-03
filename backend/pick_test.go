package main

import "testing"

const at = 1_787_869_583_000 // an Original's `when`

func text(id string, ms int64, s string) Candidate {
	return Candidate{ID: id, MsgType: "text", CreateTime: ms, Text: s}
}

func post(id string, ms int64, s string) Candidate {
	return Candidate{ID: id, MsgType: "post", CreateTime: ms, Text: s}
}

// The picker gets the newest messages of the chat and must find the one behind the Original.
// One case per test of the phone's PickMessageTest.kt.
func TestPickMessage(t *testing.T) {
	cases := []struct {
		name  string
		items []Candidate
		stem  string
		found int    // index of the expected candidate, or -1
		skip  string // the expected reason when found < 0
	}{
		{"newest text in the window that starts with the stem wins",
			[]Candidate{
				text("new", at-1_000, "完全不同的另一条消息"),
				text("mine", at-2_000, "这是一条很长的消息，前四十五个字符里没有边界所以预览是空的"),
				text("old", at-60_000, "这是一条很长的消息"),
			}, "这是一条很长的消息，", 1, ""},
		{"empty stem takes the newest text in the window",
			[]Candidate{text("a", at+1_000, "后来的"), text("b", at-3_000, "先来的")}, "", 0, ""},
		{"messages outside the window never match",
			[]Candidate{text("future", at+6_000, "x"), text("past", at-61_000, "x")}, "", -1, "no-message"},
		{"the window reaches back the full minute (the delay runs to 43.9 s, Experiment 11)",
			[]Candidate{text("late", at-44_000, "还在窗口里")}, "还在窗口里", 0, ""},
		{"the stem matches without its case (Lark lower-cases @All, Experiment 11)",
			[]Candidate{post("p", at-1_000, "Hi @All Happy Friday!")}, "Hi @all Happy Friday!", 0, ""},
		{"only the opening of the stem has to agree (a trailing bold run was dropped)",
			[]Candidate{post("p", at-1_000, "Hi @All Happy Friday! FOR SAN JOSE OFFICE ONLY\nHope everyone has started packing")},
			"Hi @all Happy Friday!Hope everyone has started packing", 0, ""},
		{"a full-length stem reaches past a delayed push (3 m 59 s late on 2026-08-28)",
			[]Candidate{text("late", at-239_000, "How cool would it be if you could search lark and docs")},
			"How cool would it be if you could search lark and docs", 0, ""},
		{"a short stem does not reach that far",
			[]Candidate{text("late", at-239_000, "Yo")}, "Yo", -1, "no-message"},
		{"an empty stem never reaches past the minute",
			[]Candidate{text("late", at-120_000, "任意内容")}, "", -1, "no-message"},
		{"a short stem still has to agree in full",
			[]Candidate{text("other", at-1_000, "好的")}, "不好", -1, "no-match"},
		{"a mention Lark localizes still matches on its close (Experiment 12)",
			[]Candidate{post("p", at-1_000, "@Oncall 助手 MPA流水线发布卡住，重试之后失败")},
			"@Oncall Assistant MPA流水线发布卡住，重试之后失败", 0, ""},
		{"the @all mention is localized the same way",
			[]Candidate{post("p", at-1_000, "@所有人 正在直播中~\n会议链接：https://www.us.larkoffice.com/calendar/share?token=e975")},
			"@all 正在直播中~会议链接：https://www.us.larkoffice.com", 0, ""},
		{"the close only decides when both sides open with a mention",
			[]Candidate{text("other", at-1_000, "刚才 MPA流水线发布卡住，重试之后失败")},
			"@Oncall Assistant MPA流水线发布卡住，重试之后失败", -1, "no-match"},
		{"the opening still decides when it can",
			[]Candidate{
				text("newer", at-1_000, "@Someone 完全不同的开头 MPA流水线发布卡住，重试之后失败"),
				text("mine", at-2_000, "@Oncall Assistant MPA流水线发布卡住，重试之后失败"),
			}, "@Oncall Assistant MPA流水线发布卡住，重试之后失败", 1, ""},
		{"recalled messages are skipped",
			[]Candidate{{ID: "r", MsgType: "text", CreateTime: at - 1_000, Deleted: true}, text("ok", at-2_000, "还在")}, "", 1, ""},
		{"a textless message in the window reports its type",
			[]Candidate{{ID: "i", MsgType: "image", CreateTime: at - 1_000}}, "标题", -1, "type:image"},
		{"a flattened post matches the Preview that dropped its newlines (Experiment 08)",
			[]Candidate{post("p", at-1_000, "[image]\nCool performance by Sofia Alise in the courtyard!! Swing by!!!!")},
			"[image]Cool performance by Sofia Alise in the courtyard!!", 0, ""},
		{"a text that does not start with the stem is no match",
			[]Candidate{text("other", at-1_000, "别的内容")}, "这是", -1, "no-match"},
	}
	for _, c := range cases {
		got := pickMessage(c.items, c.stem, at)
		switch {
		case c.found >= 0 && (got.Found == nil || *got.Found != c.items[c.found]):
			t.Errorf("%s: got %+v, want candidate %d", c.name, got, c.found)
		case c.found < 0 && (got.Found != nil || got.Reason != c.skip):
			t.Errorf("%s: got %+v, want skipped %q", c.name, got, c.skip)
		}
	}
}
