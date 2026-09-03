// The Larklish Backend (Layer 7): the Lookup and the Full-text translate, off the phone.
//
//	go -C backend run .                       # this Mac, port 8787
//	curl -s localhost:8787/v1/ping            # liveness
//	curl -s localhost:8787/chats              # the chat cache
//
// The Lark app id and secret come from LARK_APP_ID / LARK_APP_SECRET, else from
// local.properties (`lark.appId`, `lark.appSecret`), the file the app builds from. The port
// comes from _BYTEFAAS_RUNTIME_PORT (ByteFaaS Native HTTP), PORT, or 8787; the Lark host from
// LARK_HOST (inside the IDC: https://fsopen.bytedance.net).
package main

import (
	"flag"
	"log"
	"net/http"
	"os"
)

func main() {
	port := flag.String("port", first(os.Getenv("_BYTEFAAS_RUNTIME_PORT"), os.Getenv("PORT"), "8787"), "listen port")
	host := flag.String("lark", first(os.Getenv("LARK_HOST"), "https://open.feishu.cn"), "Lark Open API host")
	props := flag.String("props", "../local.properties", "properties file with lark.appId and lark.appSecret")
	flag.Parse()

	p := readProperties(*props)
	appID, appSecret := first(os.Getenv("LARK_APP_ID"), p["lark.appId"]), first(os.Getenv("LARK_APP_SECRET"), p["lark.appSecret"])
	if appID == "" || appSecret == "" {
		log.Fatalf("no Lark app id/secret: set LARK_APP_ID and LARK_APP_SECRET, or lark.appId and lark.appSecret in %s", *props)
	}
	client := newLark(appID, appSecret, *host)
	server := &Server{fetcher: NewFetcher(&liveSource{client}), translator: &Translator{client}}
	log.Printf("Larklish Backend on :%s, Lark at %s", *port, *host)
	log.Fatal(http.ListenAndServe(":"+*port, server.routes()))
}
