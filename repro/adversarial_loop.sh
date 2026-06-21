#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# Offline adversarial refinement loop (no GA-engine changes):
#
#   repeat K times:
#     1. EVOLVE with the surrogate (fast)                  -> candidate front
#     2. AUDIT the front with the REAL simulator           -> corrected front + increment
#     3. RETRAIN the surrogate on dataset + increment      -> improved model
#
# The GA generates the hard examples (it converges on the surrogate's blind
# spots); the simulator labels them; the model learns them; repeat. Works on a
# WORKING COPY of the models so the committed ones are untouched.
#
#   ./adversarial_loop.sh -d /path/surrogate_dataset.parquet -a eNSGAII -n Al_w1 -i 8 -k 3
# ---------------------------------------------------------------------------

REPRO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PY="${PY:-python3}"

DATASET=""
ALGO="eNSGAII"
CONFIG="Al_w1"
ITERS=8
ROUNDS=3
EMPHASIZE=1.0
GUARD="clamp"
SEED_MODELS="${REPRO_DIR}/../lib/surrogate"
WORKDIR="${REPRO_DIR}/out_adversarial"
SIM_JAR="${REPRO_DIR}/cloudsimStorage.jar"

usage() {
  cat <<EOF
Usage: $0 -d DATASET.parquet [-a ALGO] [-n CONFIG] [-i ITERS] [-k ROUNDS]
          [-e EMPHASIZE] [-g GUARD] [-m SEED_MODELS] [-o WORKDIR]
  -d  Original training set (.parquet)   (required)
  -a  Algorithm                          (default ${ALGO})
  -n  Configuration                      (default ${CONFIG})
  -i  Generations per round              (default ${ITERS})
  -k  Number of refinement rounds        (default ${ROUNDS})
  -e  Retrain emphasis on hard rows      (default ${EMPHASIZE})
  -g  Surrogate guard none|nonneg|clamp  (default ${GUARD})
  -m  Seed model directory               (default ../lib/surrogate)
  -o  Working directory                  (default ${WORKDIR})
EOF
  exit 1
}

while getopts ":d:a:n:i:k:e:g:m:o:h" opt; do
  case "$opt" in
    d) DATASET="$OPTARG" ;;
    a) ALGO="$OPTARG" ;;
    n) CONFIG="$OPTARG" ;;
    i) ITERS="$OPTARG" ;;
    k) ROUNDS="$OPTARG" ;;
    e) EMPHASIZE="$OPTARG" ;;
    g) GUARD="$OPTARG" ;;
    m) SEED_MODELS="$OPTARG" ;;
    o) WORKDIR="$OPTARG" ;;
    h|*) usage ;;
  esac
done
[ -n "${DATASET}" ] || usage
[ -f "${DATASET}" ] || { echo "Dataset not found: ${DATASET}"; exit 1; }

MODELS="${WORKDIR}/models"
INCREMENT="${WORKDIR}/increment.csv"
LAUNCHER_JAR="${REPRO_DIR}/CloudEvolve-launcher.jar"

rm -rf "${WORKDIR}"; mkdir -p "${MODELS}"
cp "${SEED_MODELS}"/surrogate_*_lgbm.txt "${SEED_MODELS}"/surrogate_feature_spec.json "${MODELS}/"
echo "Working copy of models: ${MODELS}"

for round in $(seq 1 "${ROUNDS}"); do
  echo "=================================================================="
  echo " Round ${round}/${ROUNDS}"
  echo "=================================================================="
  OUT="${WORKDIR}/round_${round}"

  echo "-- 1) evolve (surrogate, guard=${GUARD}) --"
  "${REPRO_DIR}/launcherSurrogate.sh" -a "${ALGO}" -n "${CONFIG}" -i "${ITERS}" \
      -s "${MODELS}" -o "${OUT}" -g "${GUARD}" > "${OUT}.evolve.log" 2>&1 || true

  RUN="$(find "${OUT}" -name iterationlist.txt -exec dirname {} \; | head -1)"
  if [ -z "${RUN}" ]; then echo "  evolve produced no run dir (see ${OUT}.evolve.log)"; break; fi

  echo "-- 2) audit the front with the real simulator --"
  java -cp "${LAUNCHER_JAR}" main.java.AuditFront \
    -r "${RUN}" -S "${SIM_JAR}" -w "${REPRO_DIR}" -c "${INCREMENT}"

  echo "-- 3) retrain on dataset + increment --"
  "${PY}" "${REPRO_DIR}/retrain_surrogate.py" \
    --dataset "${DATASET}" --increment "${INCREMENT}" \
    --models-dir "${MODELS}" --out "${MODELS}" --emphasize "${EMPHASIZE}"
  echo
done

echo ">>> Adversarial loop done. Refined models in ${MODELS}, increment in ${INCREMENT}."
echo "    Compare against the simulator with:"
echo "    repro/compareSurrogate.sh --evolve -m ${MODELS} -a ${ALGO} -n ${CONFIG} -i ${ITERS}"
