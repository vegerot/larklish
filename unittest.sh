#!/usr/bin/env bash
# SCM's unit-test step: the repo is created with --unit-test, and SCM runs unittest.sh.
set -o errexit -o nounset -o pipefail -o xtrace

go -C backend test ./...
