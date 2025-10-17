#!/usr/bin/env bash
set -euo pipefail

./launcherFullAlgorithm.sh -a "eVEGA"
./launcherFullAlgorithm.sh -a "eMOGA"
./launcherFullAlgorithm.sh -a "ePAES"
./launcherFullAlgorithm.sh -a "eNSGAII"
./launcherFullAlgorithm.sh -a "eSPEA2"

echo ">>> Experiment completed"

