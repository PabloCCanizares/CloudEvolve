#!/usr/bin/env bash
set -euo pipefail

# Algorithm to use for the 4 configurations
ALGO="${1:-eMOGA}"   # default: eMOGA if no argument is passed

# Repeat 30 times
for run in $(seq 1 30); do
  echo "=== Run ${run} with algorithm ${ALGO} ==="
  ./launcherSingleConf.sh -a "${ALGO}" -n Al_w1
  ./launcherSingleConf.sh -a "${ALGO}" -n Al_w3
  ./launcherSingleConf.sh -a "${ALGO}" -n Bl_w1
  ./launcherSingleConf.sh -a "${ALGO}" -n Bl_w3
  echo
done

echo ">>> Finished 30 runs with algorithm ${ALGO}"

