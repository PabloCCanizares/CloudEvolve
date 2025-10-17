### Boxplots HV — 5 algoritmos agrupados por subgrupo (script en el directorio padre)
reset

# --------- Salida compacta (menos alta)
# --- Salida EPS (vectorial)
set terminal epscairo color enhanced size 12cm,4.8cm font ",9"   # ajusta tamaño si quieres
set output "boxplot_hv_all_algos.eps"

# --- Tamaño de los puntos (outliers)
set pointsize 0.25

# --------- Ejes y formato
#set title "Hypervolume (HV) – Algoritmos por subgrupo"
set ylabel "HV"
set yrange [0:120000]
set ytics 0,20000,120000
set ytics out format "%.0f"
set xtics nomirror
set tics out
set border 3 lw 1

# --------- Cuadrícula sutil
set grid ytics back lc rgb "#dddddd" lw 1

# --------- Subgrupos (posiciones base en X)
groups = "Al_w1 Al_w3 Bl_w1 Bl_w3"
N = words(groups)

# --------- Algoritmos y colores (Okabe–Ito, apto daltónicos)
algos = "PAES2 SPEA2 NSGAII VEGA MOGA"
M = words(algos)

array A[M]
A[1] = "#009E73"  # PAES2 - bluish green
A[2] = "#D55E00"  # SPEA2 - vermillion
A[3] = "#56B4E9"  # NSGAII - sky blue
A[4] = "#CC79A7"  # VEGA - reddish purple
A[5] = "#0072B2"  # MOGA - blue

# --------- Offsets para agrupar cajas dentro de cada subgrupo (simétricos)
# Pensados para 5 algos y boxwidth pequeño
array OFF[M]
OFF[1] = -0.36
OFF[2] = -0.18
OFF[3] =  0.00
OFF[4] =  0.18
OFF[5] =  0.36

# --------- Estilo boxplot (modo compatible)
set style boxplot
# Si tu versión soporta 'outliers', puedes usar:
# set style boxplot outliers
set style fill solid 0.6 border
set boxwidth 0.14 absolute   # más estrecho para que quepan los 5 por grupo
# Si tu versión soporta la línea de mediana:
# set style boxplot medianlinewidth 2

# --------- Eje X
set xrange [0.5:N+0.5]
set xtics ("CloudA-w^s" 1, "CloudA-w^l" 2, "CloudB-w^s" 3, "CloudB-w^l" 4)

# --------- Leyenda por algoritmo (una sola entrada por algoritmo)
set key outside right top spacing 1 samplen 1 width -2 opaque

# --------- Plot:
# Para cada algoritmo j y cada grupo i, leemos: <algo>/hv__<grupo>.dat
# Títulos: solo en i==1 para no duplicar entradas de leyenda.
plot \
    for [j=1:M] for [i=1:N] sprintf("%s/hv__%s.dat", word(algos,j), word(groups,i)) using (i+OFF[j]):1 \
        with boxplot lc rgb A[j] title (i==1 ? word(algos,j) : "")

unset output

