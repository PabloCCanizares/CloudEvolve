#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# Runs ONE evolution with the HYBRID backend: the surrogate evaluates fitness,
# but every N generations the evaluations are routed to the REAL simulator, so
# the accurate values feed back into the search and are logged for re-training
# (surrogate_increment.csv). Combines speed (surrogate) with periodic accuracy.
#
#   ./launcherHybrid.sh -a eNSGAII -n Al_w1 -i 10 -e 3   # real simulator every 3 gens
# ---------------------------------------------------------------------------

REPRO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export CLOUDEVOLVE_WORKSPACE="${REPRO_DIR}"
cd "${REPRO_DIR}"

ALGO_DEFAULT="eMOGA"
INITIAL_PATH="${REPRO_DIR}/InitialPopulation"
EXPERIMENT_NAME="Al_w3"
ITERATIONS=100
MUT_CODE=0
RULE_BASE=1
RERUNS=1
REAL_EVERY=5
POLICY="novelty,implausible,everyN"   # ratify if OOD OR implausible OR every N gens
MAX_PER_GEN=8                          # real-simulator budget per generation
GUARD="clamp"                          # plausibility guard on surrogate predictions
BASE_OUT_DEFAULT="${REPRO_DIR}/out_hybrid"
MODEL_DIR="${REPRO_DIR}/../lib/surrogate"
SIM_JAR="${REPRO_DIR}/cloudsimStorage.jar"
LAUNCHER_JAR="${REPRO_DIR}/CloudEvolve-launcher.jar"
INCREMENT="${REPRO_DIR}/surrogate_increment.csv"

usage() {
  cat <<EOF
Usage:
  $0 [-a ALGORITHM] [-n CONFIG] [-i ITERATIONS] [-m MUT_CODE] [-r RERUNS]
     [-e REAL_EVERY] [-o OUTPUT_BASE] [-p INITIAL_PATH] [-s MODEL_DIR]
     [-S SIM_JAR] [-c INCREMENT_CSV] [-l LAUNCHER_JAR]

  -a  Algorithm: eVEGA|eMOGA|ePAES|eNSGAII|eSPEA2   (default: ${ALGO_DEFAULT})
  -n  Configuration: Al_w1|Al_w3|Bl_w1|Bl_w3        (default: ${EXPERIMENT_NAME})
  -i  Iterations / generations                      (default: ${ITERATIONS})
  -m  Mutation config: 0=high, 1=mid, 2=low         (default: ${MUT_CODE})
  -r  Re-runs                                       (default: ${RERUNS})
  -e  Use the REAL simulator every N generations    (default: ${REAL_EVERY})
  -o  Output base directory                         (default: ${BASE_OUT_DEFAULT})
  -p  InitialPopulation path                        (default: bundled)
  -s  Surrogate model directory                     (default: ../lib/surrogate)
  -S  Simulator .jar (real backend)                 (default: bundled)
  -c  Re-training increment CSV                      (default: ${INCREMENT})
  -l  Launcher .jar                                 (default: bundled)
EOF
  exit 1
}

ALGO="${ALGO_DEFAULT}"
BASE_OUT="${BASE_OUT_DEFAULT}"

while getopts ":a:n:i:m:r:e:o:p:s:S:c:l:P:C:g:h" opt; do
  case "$opt" in
    a) ALGO="$OPTARG" ;;
    n) EXPERIMENT_NAME="$OPTARG" ;;
    i) ITERATIONS="$OPTARG" ;;
    m) MUT_CODE="$OPTARG" ;;
    r) RERUNS="$OPTARG" ;;
    e) REAL_EVERY="$OPTARG" ;;
    o) BASE_OUT="$OPTARG" ;;
    p) INITIAL_PATH="$OPTARG" ;;
    s) MODEL_DIR="$OPTARG" ;;
    S) SIM_JAR="$OPTARG" ;;
    c) INCREMENT="$OPTARG" ;;
    l) LAUNCHER_JAR="$OPTARG" ;;
    P) POLICY="$OPTARG" ;;
    C) MAX_PER_GEN="$OPTARG" ;;
    g) GUARD="$OPTARG" ;;
    h|*) usage ;;
  esac
done

if [ ! -f "${MODEL_DIR}/surrogate_energy_lgbm.txt" ]; then
  echo "Surrogate models not found in '${MODEL_DIR}' (override with -s)."; exit 1
fi
if [ ! -f "${SIM_JAR}" ]; then
  echo "Simulator jar not found: ${SIM_JAR} (override with -S)."; exit 1
fi

EXP_PATH="${INITIAL_PATH}/${EXPERIMENT_NAME}"
mkdir -p "${BASE_OUT}"

echo "Running (HYBRID backend):"
echo "  Algorithm     : ${ALGO}"
echo "  Configuration : ${EXPERIMENT_NAME}"
echo "  Iterations    : ${ITERATIONS}"
echo "  Policy        : ${POLICY}  (cap ${MAX_PER_GEN}/gen, everyN=${REAL_EVERY})"
echo "  Guard         : ${GUARD}"
echo "  Model dir     : ${MODEL_DIR}"
echo "  Simulator jar : ${SIM_JAR}"
echo "  Increment CSV : ${INCREMENT}"
echo

# The last argument (simulator path) is the real backend's jar; the surrogate
# model dir and the ratification policy go via system properties.
exec java -Duser.language=en -Duser.country=US \
  -Dcloudevolve.surrogate.dir="${MODEL_DIR}" \
  -Dcloudevolve.surrogate.guard="${GUARD}" \
  -Dcloudevolve.hybrid.policy="${POLICY}" \
  -Dcloudevolve.hybrid.realEvery="${REAL_EVERY}" \
  -Dcloudevolve.hybrid.maxRealPerGen="${MAX_PER_GEN}" \
  -Dcloudevolve.hybrid.increment="${INCREMENT}" \
  -jar "${LAUNCHER_JAR}" \
  "${ALGO}" \
  "eHYBRID" \
  "${EXP_PATH}" \
  "${ITERATIONS}" \
  "${MUT_CODE}" \
  "${RULE_BASE}" \
  "${BASE_OUT}" \
  "${RERUNS}" \
  "${SIM_JAR}"
