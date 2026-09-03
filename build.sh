#!/usr/bin/env bash
# SCM runs this at the repo root inside its Go image (Layer 8). Whatever is left in output/
# becomes the versioned tarball that ByteFaaS installs; run.sh is its entry point.
set -o errexit -o nounset -o pipefail -o xtrace

mkdir -p output
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go -C backend build -o ../output/larklish-backend .
cp run.sh output/run.sh
