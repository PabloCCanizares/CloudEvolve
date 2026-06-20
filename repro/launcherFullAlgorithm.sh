#!/usr/bin/env bash
set -euo pipefail

# Runs ONE algorithm over the four cloud-workload configurations, repeated RUNS
# times (each run is an independent execution used for the statistics/boxplots).
#   T2 (reduced):  ./launcherFullAlgorithm.sh -a eNSGAII -r 2 -i 20
#   T3 (full):     ./launcherFullAlgorithm.sh -a eNSGAII            (defaults: 30 runs, 100 iters)

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ALGO="eMOGA"; RUNS=30; ITERS=100; MUT=0
while getopts ":a:r:i:m:h" opt; do
  case "$opt" in
    a) ALGO="$OPTARG" ;;
    r) RUNS="$OPTARG" ;;
    i) ITERS="$OPTARG" ;;
    m) MUT="$OPTARG" ;;
    h|*) echo "Usage: $0 [-a ALGO] [-r RUNS] [-i ITERS] [-m MUT(0=high,1=mid,2=low)]"; exit 1 ;;
  esac
done

for run in $(seq 1 "${RUNS}"); do
  echo "=== ${ALGO}: run ${run}/${RUNS} (iters=${ITERS}, mut=${MUT}) ==="
  for cfg in Al_w1 Al_w3 Bl_w1 Bl_w3; do
    ./launcherSingleConf.sh -a "${ALGO}" -n "${cfg}" -i "${ITERS}" -m "${MUT}" -r 1
  done
  echo
done

echo ">>> Finished ${RUNS} run(s) of ${ALGO}"
