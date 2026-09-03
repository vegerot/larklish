package main

import (
	"bufio"
	"os"
	"strings"
)

// readProperties reads `key=value` lines (Java properties as Gradle writes them); comments and
// blank lines are skipped. The app and the Backend share local.properties this way.
func readProperties(path string) map[string]string {
	props := map[string]string{}
	f, err := os.Open(path)
	if err != nil {
		return props
	}
	defer f.Close()
	sc := bufio.NewScanner(f)
	for sc.Scan() {
		line := strings.TrimSpace(sc.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		if k, v, ok := strings.Cut(line, "="); ok {
			props[strings.TrimSpace(k)] = strings.TrimSpace(v)
		}
	}
	return props
}

// first returns the first non-empty value.
func first(values ...string) string {
	for _, v := range values {
		if v != "" {
			return v
		}
	}
	return ""
}
