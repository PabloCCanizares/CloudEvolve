#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# Runs ONE evolution using the SURROGATE backend: instead of launching the
# CloudSim-Storage simulator, a trained LightGBM model predicts (energy, time)
# for each candidate. Everything else (algorithms, metamorphic testing, cloud
# topology) is identical to launcherSingleConf.sh — only the fitness evaluation
# is replaced, so runs finish in seconds instead of minutes/hours.
#
# Example:
#   ./launcherSurrogate.sh -a eNSGAII -n Al_w3 -i 100
# ---------------------------------------------------------------------------

REPRO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Same workspace/CWD setup as the real-simulator launcher so the ${workspace}
# token in the .mtc seeds and the relative work.path resolve here.
export CLOUDEVOLVE_WORKSPACE="${REPRO_DIR}"
cd "${REPRO_DIR}"

ALGO_DEFAULT="eMOGA"
INITIAL_PATH="${REPRO_DIR}/InitialPopulation"
EXPERIMENT_NAME="Al_w3"
ITERATIONS=100
MUT_CODE=0          # 0=high, 1=mid, 2=low
RULE_BASE=1
RERUNS=1
GUARD=""            # plausibility guard: none|nonneg|clamp (empty = model default)
BASE_OUT_DEFAULT="${REPRO_DIR}/out_surrogate"
# Surrogate models live in the repo's lib/surrogate; override with -s if you run
# this folder standalone (copy surrogate_*_lgbm.txt somewhere and point -s at it).
MODEL_DIR="${REPRO_DIR}/../lib/surrogate"
LAUNCHER_JAR="${REPRO_DIR}/CloudEvolve-launcher.jar"

usage() {
  cat <<EOF
Usage:
  $0 [-a ALGORITHM] [-n CONFIG] [-i ITERATIONS] [-m MUT_CODE] [-r RERUNS]
     [-o OUTPUT_BASE] [-p INITIAL_PATH] [-s SURROGATE_MODEL_DIR] [-l LAUNCHER_JAR]

  -a  Algorithm: eVEGA|eMOGA|ePAES|eNSGAII|eSPEA2   (default: ${ALGO_DEFAULT})
  -n  Configuration: Al_w1|Al_w3|Bl_w1|Bl_w3        (default: ${EXPERIMENT_NAME})
  -i  Iterations / generations                      (default: ${ITERATIONS})
  -m  Mutation config: 0=high, 1=mid, 2=low         (default: ${MUT_CODE})
  -r  Re-runs                                       (default: ${RERUNS})
  -o  Output base directory                         (default: ${BASE_OUT_DEFAULT})
  -p  InitialPopulation path                        (default: bundled)
  -s  Surrogate model directory                     (default: ../lib/surrogate)
  -l  Launcher .jar                                 (default: bundled)
EOF
  exit 1
}

ALGO="${ALGO_DEFAULT}"
BASE_OUT="${BASE_OUT_DEFAULT}"

while getopts ":a:n:i:m:r:o:p:s:l:g:h" opt; do
  case "$opt" in
    a) ALGO="$OPTARG" ;;
    n) EXPERIMENT_NAME="$OPTARG" ;;
    i) ITERATIONS="$OPTARG" ;;
    m) MUT_CODE="$OPTARG" ;;
    r) RERUNS="$OPTARG" ;;
    o) BASE_OUT="$OPTARG" ;;
    p) INITIAL_PATH="$OPTARG" ;;
    s) MODEL_DIR="$OPTARG" ;;
    g) GUARD="$OPTARG" ;;
    l) LAUNCHER_JAR="$OPTARG" ;;
    h|*) usage ;;
  esac
done

if [ ! -f "${MODEL_DIR}/surrogate_energy_lgbm.txt" ] || [ ! -f "${MODEL_DIR}/surrogate_time_lgbm.txt" ]; then
  echo "Surrogate models not found in '${MODEL_DIR}'."
  echo "Expected surrogate_energy_lgbm.txt and surrogate_time_lgbm.txt (override with -s)."
  exit 1
fi

EXP_PATH="${INITIAL_PATH}/${EXPERIMENT_NAME}"
mkdir -p "${BASE_OUT}"

echo "Running (SURROGATE backend):"
echo "  Algorithm     : ${ALGO}"
echo "  Configuration : ${EXPERIMENT_NAME}  (${EXP_PATH})"
echo "  Iterations    : ${ITERATIONS}"
echo "  Mutation      : ${MUT_CODE} (0=high,1=mid,2=low)"
echo "  Re-runs       : ${RERUNS}"
echo "  Output base   : ${BASE_OUT}"
echo "  Model dir     : ${MODEL_DIR}"
echo "  Workspace     : ${CLOUDEVOLVE_WORKSPACE}"
echo

# The last argument (normally the simulator .jar) is reused as the surrogate
# model directory: SurrogatePlatform reads it via the configured "simulator path".
GUARD_OPT=""
[ -n "${GUARD}" ] && GUARD_OPT="-Dcloudevolve.surrogate.guard=${GUARD}"

exec java -Duser.language=en -Duser.country=US ${GUARD_OPT} -jar "${LAUNCHER_JAR}" \
  "${ALGO}" \
  "eSURROGATE" \
  "${EXP_PATH}" \
  "${ITERATIONS}" \
  "${MUT_CODE}" \
  "${RULE_BASE}" \
  "${BASE_OUT}" \
  "${RERUNS}" \
  "${MODEL_DIR}"
