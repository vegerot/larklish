package main

import (
	"context"
	"testing"
)

func TestEnglishOfPassesLatinThroughWithoutAClient(t *testing.T) {
	got := (&Translator{}).EnglishOf(context.Background(), "Sync Task Conflicted")
	if got.English == nil || *got.English != "Sync Task Conflicted" || got.Failure != "" {
		t.Errorf("got %+v", got)
	}
}

func TestIsHanNameMatchesThePhoneRule(t *testing.T) {
	if !isHanName("陈昱萌") || isHanName("Triton数据安全") || isHanName("王") || isHanName("国际电商风险运营平台") {
		t.Fatal("isHanName disagrees with the phone rule")
	}
}
