#!/usr/bin/env bash
# SCM's unit-test step (the repo is created with --unit-test).
set -o errexit -o nounset -o pipefail -o xtrace

go -C backend test ./...
