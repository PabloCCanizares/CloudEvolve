#!/usr/bin/env bash
set -euo pipefail

# Runs the full study: the five MOGAs over the four cloud-workload configurations.
# Forwards -r/-i/-m to launcherFullAlgorithm.sh, so:
#   T2 (reduced):  ./launcherAllExperiments.sh -r 2 -i 20
#   T3 (full):     ./launcherAllExperiments.sh           (defaults: 30 runs, 100 iters, high mutation)

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

for algo in eVEGA eMOGA ePAES eNSGAII eSPEA2; do
  ./launcherFullAlgorithm.sh -a "${algo}" "$@"
done

echo ">>> All experiments completed"
