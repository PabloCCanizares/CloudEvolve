#!/usr/bin/env bash
set -euo pipefail

#Default configuration
PATH_BASE="/localSpace/cloudEnergy"   

ALGO_DEFAULT="eMOGA"
BASE_OUT_DEFAULT="${PATH_BASE}/cloudsimStorage/evolutionary"

SIMULATOR="eCloudSimStorage"
INITIAL_PATH="${PATH_BASE}/cloudsimStorage/evolutionary/InitialPopulation"
EXPERIMENT_NAME="Al_w3"
ITERATIONS=100
MUT_CODE=0          # 0=alta, 1=media, 2=baja
RULE_BASE=1
RERUNS=1
SIM_JAR="${PATH_BASE}/cloudsimStorage/cloudsimStorage-release.jar"
LAUNCHER_JAR="CloudEvolve-launcher.jar"

usage() {
  cat <<EOF
Uso:
  $0 [-a ALGORITMO] [-o RUTA_BASE_SALIDA] [-n EXPERIMENT_NAME] [-i ITERACIONES]
     [-m MUT_CODE] [-r RERUNS] [-p INITIAL_PATH] [-s SIM_JAR] [-l LAUNCHER_JAR]

Parámetros:
  -a  Algoritmo (por defecto: ${ALGO_DEFAULT})
  -o  Ruta base de salida (por defecto: ${BASE_OUT_DEFAULT})
  -n  Nombre de experimento/población inicial (por defecto: ${EXPERIMENT_NAME})
  -i  Iteraciones (por defecto: ${ITERATIONS})
  -m  Prob. mutación codificada: 0=alta,1=media,2=baja (por defecto: ${MUT_CODE})
  -r  Repeticiones (re-runs) (por defecto: ${RERUNS})
  -p  Ruta a InitialPopulation (por defecto: ${INITIAL_PATH})
  -s  Ruta al simulador .jar (por defecto: ${SIM_JAR})
  -l  Launcher JAR (por defecto: ${LAUNCHER_JAR})

Ejemplo:
  $0 -a eNSGAII -o /tmp/evos -n Al_w3 -i 200 -m 1 -r 30
EOF
  exit 1
}

ALGO="${ALGO_DEFAULT}"
BASE_OUT="${BASE_OUT_DEFAULT}"

while getopts ":a:o:n:i:m:r:p:s:l:h" opt; do
  case "$opt" in
    a) ALGO="$OPTARG" ;;
    o) BASE_OUT="$OPTARG" ;;
    n) EXPERIMENT_NAME="$OPTARG" ;;
    i) ITERATIONS="$OPTARG" ;;
    m) MUT_CODE="$OPTARG" ;;
    r) RERUNS="$OPTARG" ;;
    p) INITIAL_PATH="$OPTARG" ;;
    s) SIM_JAR="$OPTARG" ;;
    l) LAUNCHER_JAR="$OPTARG" ;;
    h|*) usage ;;
  esac
done

EXP_PATH="${INITIAL_PATH}/${EXPERIMENT_NAME}"

echo "Ejecutando con:"
echo "  Algoritmo        : ${ALGO}"
echo "  Simulador        : ${SIMULATOR}"
echo "  Exp inicial      : ${EXP_PATH}"
echo "  Iteraciones      : ${ITERATIONS}"
echo "  Mutación (0/1/2) : ${MUT_CODE}"
echo "  Rule base        : ${RULE_BASE}"
echo "  Salida base      : ${BASE_OUT}"
echo "  Re-runs          : ${RERUNS}"
echo "  Sim .jar         : ${SIM_JAR}"
echo "  Launcher .jar    : ${LAUNCHER_JAR}"
echo

exec java -jar "${LAUNCHER_JAR}" \
  "${ALGO}" \
  "${SIMULATOR}" \
  "${EXP_PATH}" \
  "${ITERATIONS}" \
  "${MUT_CODE}" \
  "${RULE_BASE}" \
  "${BASE_OUT}" \
  "${RERUNS}" \
  "${SIM_JAR}"

