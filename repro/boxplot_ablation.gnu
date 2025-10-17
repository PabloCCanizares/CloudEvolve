### Boxplots HV — 2 algoritmos (PAES2 vs PAES2_Ablation)
reset

# --- Salida EPS (vectorial)
set terminal epscairo color enhanced size 12cm,4.8cm font ",9"
set output "boxplot_hv_PAES2_vs_Ablation.eps"

# --- Tamaño de los puntos (outliers)
set pointsize 0.25

# --- Ejes y formato
#set title "Hypervolume (HV) – PAES2 vs Ablation"
set ylabel "HV"
set yrange [0:120000]
set ytics 0,20000,120000
set ytics out format "%.0f"
set xtics nomirror
set tics out
set border 3 lw 1

# --- Cuadrícula sutil
set grid ytics back lc rgb "#dddddd" lw 1

# --- Subgrupos
groups = "Al_w1 Al_w3 Bl_w1 Bl_w3"
N = words(groups)

# --- Algoritmos (solo estos dos) y colores
algos = "PAES PAES-Ablation"
# Prefijo antes del primer "_"
prefix(s) = (p = strstrt(s, "_")) > 0 ? substr(s, 1, p-1) : s
M = words(algos)

array A[M]
A[1] = "#009E73"  # PAES
A[2] = "#D55E00"  # PAES-Ablation

# --- Offsets para 2 cajas por grupo (simétricos)
array OFF[M]
OFF[1] = -0.12
OFF[2] =  0.12

# --- Estilo boxplot (EPS sin transparencia)
set style boxplot            # si tu versión soporta:  set style boxplot outliers
set style fill solid 1.0 border
set boxwidth 0.20 absolute   # algo más ancho al haber solo 2 cajas
# Si tu versión lo soporta:
# set style boxplot medianlinewidth 2

# --- Eje X
set xrange [0.5:N+0.5]
set xtics ("CloudA-w^s" 1, "CloudA-w^l" 2, "CloudB-w^s" 3, "CloudB-w^l" 4)

# --- Leyenda
set key outside right top spacing 1 samplen 1 width -2 opaque

# --- Plot: <algo>/hv__<grupo>.dat
plot \
    for [j=1:M] for [i=1:N] sprintf("%s/hv__%s.dat", word(algos,j), word(groups,i)) using (i+OFF[j]):1 \
        with boxplot lc rgb A[j] title (i==1 ? word(algos,j) : "")

unset output

