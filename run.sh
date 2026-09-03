#!/usr/bin/env bash
# ByteFaaS runs this as /opt/bytefaas/run.sh with the tarball as the working directory.
# exec, so the Backend is the process that gets the platform's signals.
set -o errexit -o nounset -o pipefail

exec ./larklish-backend
