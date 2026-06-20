#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# Turns the evolution outputs under repro/out into the hypervolume (HV) boxplots.
#   1. ParetoAndHVFromLogs computes HV-per-iteration for each run.
#   2. The final HV of each run is aggregated into <algo>/hv__<config>.dat.
#   3. gnuplot draws boxplot_all_algorithms.gnu -> boxplot_hv_all_algos.eps.
#
# Reference point:
#   default  -> empirical (max objective × 1.1 per run); correct for the bundled
#               20-trace workload subset.
#   --paper-ref -> the paper's fixed reference points (use only with the FULL
#                  workload, i.e. after `unzip workload.zip`).
# ---------------------------------------------------------------------------

REPRO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${REPRO_DIR}"

OUT="${REPRO_DIR}/out"
LAUNCHER_JAR="${REPRO_DIR}/CloudEvolve-launcher.jar"
GROUPS="Al_w1 Al_w3 Bl_w1 Bl_w3"
ALGOS="PAES SPEA2 NSGAII VEGA MOGA"
USE_PAPER_REF=0

paper_ref() {
  case "$1" in
    Al_w1) echo "18.826120,6075.100000" ;;
    Al_w3) echo "24.149430,6618.890000" ;;
    Bl_w1) echo "17.088820,6942.100000" ;;
    Bl_w3) echo "18.103740,10254.510400" ;;
  esac
}

while [ $# -gt 0 ]; do
  case "$1" in
    --paper-ref) USE_PAPER_REF=1 ;;
    -o) OUT="$2"; shift ;;
    -h|--help) echo "Usage: $0 [--paper-ref] [-o OUT_DIR]"; exit 0 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
  shift
done
OUT="${OUT%/}"

[ -d "$OUT" ] || { echo "No output dir: $OUT  (run the launchers first)"; exit 1; }

group_of() { echo "$1" | grep -oE 'Al_w1|Al_w3|Bl_w1|Bl_w3' | head -1; }

echo "== 1) HV per run ($([ "$USE_PAPER_REF" = 1 ] && echo 'paper ref' || echo 'empirical ref')) =="
while IFS= read -r itf; do
  run="$(dirname "$itf")"; grp="$(group_of "$run")"
  if [ "$USE_PAPER_REF" = 1 ] && [ -n "$grp" ]; then
    java -cp "$LAUNCHER_JAR" main_scico.hv.ParetoAndHVFromLogs "$run" --ref "$(paper_ref "$grp")" >/dev/null 2>&1 || true
  else
    java -cp "$LAUNCHER_JAR" main_scico.hv.ParetoAndHVFromLogs "$run" >/dev/null 2>&1 || true
  fi
  echo "   $(group_of "$run")  <-  ${run#"$OUT"/}"
done < <(find "$OUT" -name iterationlist.txt)

echo "== 2) Aggregating final HV -> <algo>/hv__<group>.dat =="
for a in $ALGOS; do for g in $GROUPS; do mkdir -p "$OUT/$a"; : > "$OUT/$a/hv__$g.dat"; done; done
while IFS= read -r hv; do
  run="$(dirname "$hv")"; grp="$(group_of "$run")"; [ -n "$grp" ] || continue
  rel="${run#"$OUT"/}"; algo="${rel%%/*}"
  final="$(grep -vE '^#' "$hv" | awk 'NF>=2{v=$2} END{if (v!="") print v}')"
  [ -n "$final" ] && echo "$final" >> "$OUT/$algo/hv__$grp.dat"
done < <(find "$OUT" -name hv_evolution.dat)

echo "== 3) Plotting =="
cp boxplot_all_algorithms.gnu "$OUT/"
if ( cd "$OUT" && gnuplot boxplot_all_algorithms.gnu ) 2>/tmp/gnuplot.err; then
  echo "   -> $OUT/boxplot_hv_all_algos.eps"
else
  echo "   gnuplot not available or some <algo>/hv__<group>.dat are empty (partial run). See /tmp/gnuplot.err"
fi
