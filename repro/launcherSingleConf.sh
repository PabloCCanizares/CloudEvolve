#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# Runs ONE evolution (one algorithm × one cloud-workload configuration) using
# the self-contained data bundled in this folder. No machine-specific setup:
# the script roots everything at its own directory.
# ---------------------------------------------------------------------------

# Absolute path to this repro/ folder; everything resolves from here.
REPRO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Make the ${workspace} token in the .mtc seeds resolve to this folder, and run
# with this folder as the working directory so the relative work.path in the
# .tc files (workload/io_mix/cpu) resolves to repro/workload.
export CLOUDEVOLVE_WORKSPACE="${REPRO_DIR}"
cd "${REPRO_DIR}"

# Defaults (all bundled, all overridable via flags)
ALGO_DEFAULT="eMOGA"
SIMULATOR="eCloudSimStorage"
INITIAL_PATH="${REPRO_DIR}/InitialPopulation"
EXPERIMENT_NAME="Al_w3"
ITERATIONS=100
MUT_CODE=0          # 0=high, 1=mid, 2=low  (the paper's best results use high=0)
RULE_BASE=1
RERUNS=1
BASE_OUT_DEFAULT="${REPRO_DIR}/out"
SIM_JAR="${REPRO_DIR}/cloudsimStorage.jar"
LAUNCHER_JAR="${REPRO_DIR}/CloudEvolve-launcher.jar"

usage() {
  cat <<EOF
Usage:
  $0 [-a ALGORITHM] [-n CONFIG] [-i ITERATIONS] [-m MUT_CODE] [-r RERUNS]
     [-o OUTPUT_BASE] [-p INITIAL_PATH] [-s SIM_JAR] [-l LAUNCHER_JAR]

  -a  Algorithm: eVEGA|eMOGA|ePAES|eNSGAII|eSPEA2   (default: ${ALGO_DEFAULT})
  -n  Configuration: Al_w1|Al_w3|Bl_w1|Bl_w3        (default: ${EXPERIMENT_NAME})
        Al=CloudA (low-perf), Bl=CloudB (high-perf); w1=small workload, w3=large
  -i  Iterations / generations                      (default: ${ITERATIONS})
  -m  Mutation config: 0=high, 1=mid, 2=low         (default: ${MUT_CODE})
  -r  Re-runs                                       (default: ${RERUNS})
  -o  Output base directory                         (default: ${BASE_OUT_DEFAULT})
  -p  InitialPopulation path                        (default: bundled)
  -s  Simulator .jar                                (default: bundled)
  -l  Launcher .jar                                 (default: bundled)

Example (quick check of the pipeline):
  $0 -a eNSGAII -n Al_w3 -i 5
EOF
  exit 1
}

ALGO="${ALGO_DEFAULT}"
BASE_OUT="${BASE_OUT_DEFAULT}"

while getopts ":a:n:i:m:r:o:p:s:l:h" opt; do
  case "$opt" in
    a) ALGO="$OPTARG" ;;
    n) EXPERIMENT_NAME="$OPTARG" ;;
    i) ITERATIONS="$OPTARG" ;;
    m) MUT_CODE="$OPTARG" ;;
    r) RERUNS="$OPTARG" ;;
    o) BASE_OUT="$OPTARG" ;;
    p) INITIAL_PATH="$OPTARG" ;;
    s) SIM_JAR="$OPTARG" ;;
    l) LAUNCHER_JAR="$OPTARG" ;;
    h|*) usage ;;
  esac
done

EXP_PATH="${INITIAL_PATH}/${EXPERIMENT_NAME}"
mkdir -p "${BASE_OUT}"

echo "Running:"
echo "  Algorithm     : ${ALGO}"
echo "  Configuration : ${EXPERIMENT_NAME}  (${EXP_PATH})"
echo "  Iterations    : ${ITERATIONS}"
echo "  Mutation      : ${MUT_CODE} (0=high,1=mid,2=low)"
echo "  Re-runs       : ${RERUNS}"
echo "  Output base   : ${BASE_OUT}"
echo "  Workspace     : ${CLOUDEVOLVE_WORKSPACE}"
echo

exec java -Duser.language=en -Duser.country=US -jar "${LAUNCHER_JAR}" \
  "${ALGO}" \
  "${SIMULATOR}" \
  "${EXP_PATH}" \
  "${ITERATIONS}" \
  "${MUT_CODE}" \
  "${RULE_BASE}" \
  "${BASE_OUT}" \
  "${RERUNS}" \
  "${SIM_JAR}"
