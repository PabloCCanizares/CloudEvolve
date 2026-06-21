#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# Surrogate vs real simulator, per fitness evaluation. Prints an accuracy table
# (surrogate prediction vs the recorded simulator output for every bundled
# config), the surrogate's throughput, and — unless --no-sim — a live wall-clock
# head-to-head on the smoke case. Non-destructive: the smoke fixture is restored
# afterwards.
#
#   ./compareSurrogate.sh            # full comparison incl. one live simulator run
#   ./compareSurrogate.sh --no-sim   # accuracy + speed only (instant, no jar needed)
# ---------------------------------------------------------------------------

REPRO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${REPRO_DIR}/.." && pwd)"

# Run from the repo root so lib/surrogate and repro/ resolve.
cd "${REPO_ROOT}"
exec java -Duser.language=en -Duser.country=US \
  -cp "${REPRO_DIR}/CloudEvolve-launcher.jar" \
  main.java.SurrogateComparison -m lib/surrogate -r repro "$@"
