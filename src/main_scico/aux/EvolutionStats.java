package main_scico.aux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * EvolutionStats con exportación a ficheros de datos + scripts gnuplot (boxplot y violin)
 * y ejecución automática de gnuplot. Todo se genera en la raíz indicada en la llamada.
 * 
 * ATENCIÓN: Es necesario que se haya ejecutado previamente ParetoAndHVFromLogs
 * Uso:
 *   java EvolutionStats "/ruta/a/carpeta/base"
 *
 * Estructura esperada:
 *   /base/
 *     Al_w1/
 *       experimentoX/
 *         evolution3.dat
 *         hv_virtual_evolution.dat (opcional)
 *         hv_evolution.dat         (alternativo)
 *     Al_w3/ ...
 *     Bl_w1/ ...
 *     Bl_w3/ ...
 *
 * Salidas generadas en /base/:
 *   - metric1__<grupo>.dat      (columna única)
 *   - metric2__<grupo>.dat
 *   - hv__<grupo>.dat
 *   - boxplot_metric1.gp  -> boxplot_metric1.png
 *   - boxplot_metric2.gp  -> boxplot_metric2.png
 *   - boxplot_hv.gp       -> boxplot_hv.png
 *   - violin_metric1.gp   -> violin_metric1.png
 *   - violin_metric2.gp   -> violin_metric2.png
 *   - violin_hv.gp        -> violin_hv.png
 */
public class EvolutionStats {

    // Ajusta aquí los subgrupos que quieras recorrer
    private static final List<String> GROUPS = Arrays.asList("Al_w1", "Al_w3", "Bl_w1", "Bl_w3");

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java EvolutionStats \"/ruta/a/carpeta/base\"");
            System.exit(1);
        }

        Path base = Paths.get(args[0]);
        if (!Files.isDirectory(base)) {
            System.err.println("La ruta base no es un directorio: " + base.toAbsolutePath());
            System.exit(2);
        }

        Map<String, GroupAccumulator> data = new LinkedHashMap<>();
        for (String g : GROUPS) data.put(g, new GroupAccumulator(g));

        // ======== Recorrido de carpetas y lectura de valores ========
        for (String groupName : GROUPS) {
            Path groupDir = base.resolve(groupName);
            if (!Files.isDirectory(groupDir)) {
                System.err.println("AVISO: no existe el directorio del grupo: " + groupDir.toAbsolutePath());
                continue;
            }
            GroupAccumulator acc = data.get(groupName);

            // Buscamos evolution3.dat a profundidad 2 (grupo/exp/evolution3.dat)
            try (Stream<Path> walk = Files.walk(groupDir, 2)) {
                List<Path> evoFiles = walk
                        .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().equals("evolution3.dat"))
                        .collect(Collectors.toList());

                for (Path evoFile : evoFiles) {
                    try {
                        double[] pair = readLastLineFirstTwoNumbers(evoFile);
                        if (pair != null) {
                            acc.metric1.add(pair[0]); // p.ej., energía
                            acc.metric2.add(pair[1]); // p.ej., tiempo
                        } else {
                            System.err.println("AVISO: no se pudieron leer 2 números de la última línea: " + evoFile);
                        }
                        // HV final desde hv_virtual_evolution.dat o hv_evolution.dat
                        Path execFolder = evoFile.getParent();
                        Double hv = readFinalHV(execFolder);
                        if (hv != null) {
                            acc.hv.add(hv);
                        } else {
                            System.err.println("AVISO: sin HV para ejecución: " + execFolder);
                        }
                        acc.filesProcessed++;
                    } catch (IOException ex) {
                        System.err.println("ERROR leyendo " + evoFile + ": " + ex.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("ERROR recorriendo " + groupDir + ": " + e.getMessage());
            }
        }

        // ======== Estadísticos por grupo ========
        System.out.println("======= RESUMEN DE ESTADÍSTICAS =======");
        for (String groupName : GROUPS) {
            GroupAccumulator acc = data.get(groupName);
            System.out.println("\n=== Grupo: " + groupName + " ===");
            System.out.println("Ficheros procesados: " + acc.filesProcessed);

            printStats("Métrica #1", acc.metric1);
            printStats("Métrica #2", acc.metric2);
            printStats("Hipervolumen (HV)", acc.hv);
        }

        // ======== Escritura de datos por grupo/medida en la raíz ========
        try {
            writeMetricFiles(base, data, "metric1", GroupAccumulator::metric1);
            writeMetricFiles(base, data, "metric2", GroupAccumulator::metric2);
            writeMetricFiles(base, data, "hv",      GroupAccumulator::hv);
        } catch (IOException ioe) {
            System.err.println("ERROR escribiendo ficheros de datos: " + ioe.getMessage());
        }

        // ======== Generación de scripts de gnuplot (boxplot y violin) ========
        try {
            List<String> groupsWithMetric1 = groupsWithData(data, GroupAccumulator::metric1);
            List<String> groupsWithMetric2 = groupsWithData(data, GroupAccumulator::metric2);
            List<String> groupsWithHV      = groupsWithData(data, GroupAccumulator::hv);

            // BOX PLOTS
            if (!groupsWithMetric1.isEmpty()) {
                Path gp1 = base.resolve("boxplot_metric1.gp");
                generateBoxplotScriptGNU(gp1, "Métrica #1 - Boxplots por subgrupo", "Valor",
                        "metric1", groupsWithMetric1, "boxplot_metric1.png");
                runGnuplot(gp1);
            }
            if (!groupsWithMetric2.isEmpty()) {
                Path gp2 = base.resolve("boxplot_metric2.gp");
                generateBoxplotScriptGNU(gp2, "Métrica #2 - Boxplots por subgrupo", "Valor",
                        "metric2", groupsWithMetric2, "boxplot_metric2.png");
                runGnuplot(gp2);
            }
            if (!groupsWithHV.isEmpty()) {
                Path gp3 = base.resolve("boxplot_hv.gp");
                generateBoxplotScriptGNU(gp3, "Hipervolumen (HV) - Boxplots por subgrupo", "HV",
                        "hv", groupsWithHV, "boxplot_hv.png");
                runGnuplot(gp3);
            }

            // VIOLIN PLOTS (kernel density)
            if (!groupsWithMetric1.isEmpty()) {
                Path gv1 = base.resolve("violin_metric1.gp");
                generateViolinPrettyScriptGNU(gv1, "Métrica #1 - Violin por subgrupo", "Valor",
                        "metric1", groupsWithMetric1, "violin_metric1.png");
                runGnuplot(gv1);
            }
            if (!groupsWithMetric2.isEmpty()) {
                Path gv2 = base.resolve("violin_metric2.gp");
                generateViolinPrettyScriptGNU(gv2, "Métrica #2 - Violin por subgrupo", "Valor",
                        "metric2", groupsWithMetric2, "violin_metric2.png");
                runGnuplot(gv2);
            }
            if (!groupsWithHV.isEmpty()) {
                Path gv3 = base.resolve("violin_hv.gp");
                generateViolinPrettyScriptGNU(gv3, "Hipervolumen (HV) - Violin por subgrupo", "HV",
                        "hv", groupsWithHV, "violin_hv.png");
                runGnuplot(gv3);
            }

            System.out.println("\nFicheros de datos y gráficos generados en: " + base.toAbsolutePath());
        } catch (IOException ioe) {
            System.err.println("ERROR generando/ejecutando gnuplot: " + ioe.getMessage());
        }
    }

    /* ====================== LECTURA DE FICHEROS ====================== */

    /**
     * Lee la última línea no vacía del fichero y devuelve los 2 primeros elementos numéricos (double).
     * Devuelve null si no hay suficientes elementos parseables.
     */
    private static double[] readLastLineFirstTwoNumbers(Path file) throws IOException {
        String last = readLastNonEmptyLine(file);
        if (last == null) return null;

        String[] toks = last.trim().split("\\s+");
        if (toks.length < 2) return null;

        try {
            double a = Double.parseDouble(toks[0]);
            double b = Double.parseDouble(toks[1]);
            return new double[]{a, b};
        } catch (NumberFormatException nfe) {
            try {
                double a = Double.parseDouble(toks[0].replace(',', '.'));
                double b = Double.parseDouble(toks[1].replace(',', '.'));
                return new double[]{a, b};
            } catch (NumberFormatException nfe2) {
                return null;
            }
        }
    }

    /**
     * Intenta leer el HV final desde hv_virtual_evolution.dat (preferente)
     * o hv_evolution.dat (alternativo) en 'execFolder'.
     * Toma el SEGUNDO número de la última línea no vacía.
     * Devuelve null si no se encuentra o no se puede parsear.
     */
    private static Double readFinalHV(Path execFolder) throws IOException {
        Path hvVirtual = execFolder.resolve("hv_virtual_evolution.dat");
        Path hvPlain   = execFolder.resolve("hv_evolution.dat");
        Path selected  = null;

        if (Files.isRegularFile(hvVirtual)) {
            selected = hvVirtual;
        } else if (Files.isRegularFile(hvPlain)) {
            selected = hvPlain;
        } else {
            return null;
        }

        String last = readLastNonEmptyLine(selected);
        if (last == null) return null;

        String[] toks = last.trim().split("\\s+");
        if (toks.length < 2) return null;

        try {
            return Double.parseDouble(toks[1]);
        } catch (NumberFormatException nfe) {
            try {
                return Double.parseDouble(toks[1].replace(',', '.'));
            } catch (NumberFormatException nfe2) {
                return null;
            }
        }
    }

    /** Lee la última línea no vacía de un fichero de texto. */
    private static String readLastNonEmptyLine(Path file) throws IOException {
        String last = null;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) last = line;
            }
        }
        return last;
    }

    /* ====================== ESTADÍSTICOS ====================== */

    /** Imprime estadísticos para una lista de valores. */
    private static void printStats(String label, List<Double> values) {
        if (values.isEmpty()) {
            System.out.println(label + ": n=0 (sin datos)");
            return;
        }
        Stats st = Stats.from(values);
        System.out.printf(Locale.US,
                "%s: n=%d | min=%.6f | max=%.6f | media=%.6f | mediana=%.6f | IQR=%.6f (Q1=%.6f, Q3=%.6f)%n \n",
                label, st.n, st.min, st.max, st.mean, st.median, st.iqr, st.q1, st.q3);
        System.out.printf(Locale.US,
                "%s: n=%d &%.2f &%.2f &%.2f &%.2f &%.2f%n",
                label, st.n, st.min, st.max, st.mean, st.median, st.iqr);
    }

    private static class GroupAccumulator {
        final String name;
        final List<Double> metric1 = new ArrayList<>();
        final List<Double> metric2 = new ArrayList<>();
        final List<Double> hv      = new ArrayList<>();
        int filesProcessed = 0;

        GroupAccumulator(String name) { this.name = name; }

        List<Double> metric1() { return metric1; }
        List<Double> metric2() { return metric2; }
        List<Double> hv()      { return hv; }
    }

    private static class Stats {
        final int n;
        final double min, max, mean, median, q1, q3, iqr;

        private Stats(int n, double min, double max, double mean, double median, double q1, double q3) {
            this.n = n;
            this.min = min;
            this.max = max;
            this.mean = mean;
            this.median = median;
            this.q1 = q1;
            this.q3 = q3;
            this.iqr = q3 - q1;
        }

        static Stats from(List<Double> data) {
            int n = data.size();
            double[] arr = new double[n];
            for (int i = 0; i < n; i++) arr[i] = data.get(i);
            Arrays.sort(arr);

            double min = arr[0];
            double max = arr[n - 1];
            double mean = 0.0;
            for (double v : arr) mean += v;
            mean /= n;

            double median = medianOfSorted(arr, 0, n);

            int mid = n / 2;
            double q1, q3;
            if (n % 2 == 0) {
                q1 = medianOfSorted(arr, 0, mid);
                q3 = medianOfSorted(arr, mid, n);
            } else {
                q1 = medianOfSorted(arr, 0, mid);
                q3 = medianOfSorted(arr, mid + 1, n);
            }
            return new Stats(n, min, max, mean, median, q1, q3);
        }

        private static double medianOfSorted(double[] arr, int from, int to) {
            int len = to - from;
            if (len <= 0) return Double.NaN;
            int mid = from + len / 2;
            if (len % 2 == 0) {
                return (arr[mid - 1] + arr[mid]) / 2.0;
            } else {
                return arr[mid];
            }
        }
    }

    /* ====================== UTILIDADES I/O ====================== */

    @FunctionalInterface
    private interface MetricGetter {
        List<Double> get(GroupAccumulator acc);
    }

    private static void writeMetricFiles(Path base,
                                         Map<String, GroupAccumulator> data,
                                         String metricPrefix,
                                         MetricGetter getter) throws IOException {
        for (Map.Entry<String, GroupAccumulator> e : data.entrySet()) {
            String group = e.getKey();
            List<Double> values = getter.get(e.getValue());
            Path out = base.resolve(metricPrefix + "__" + group + ".dat");
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
                for (Double v : values) {
                    if (v != null && !v.isNaN() && !v.isInfinite()) {
                        pw.printf(Locale.US, "%.10f%n", v);
                    }
                }
            }
        }
    }

    private static List<String> groupsWithData(Map<String, GroupAccumulator> data, MetricGetter getter) {
        List<String> res = new ArrayList<>();
        for (Map.Entry<String, GroupAccumulator> e : data.entrySet()) {
            if (!getter.get(e.getValue()).isEmpty()) {
                res.add(e.getKey());
            }
        }
        return res;
    }

    /* ====================== GNUPlot (scripts) ====================== */

    /**
     * Genera script gnuplot para boxplots y lo guarda en 'scriptPath'.
     * Usa 'smooth boxplot' (gnuplot 5.x). Un fichero por grupo: <metricPrefix>__<grupo>.dat
     */
    /**
     * Genera un script de gnuplot (.gnu) para boxplots usando TU plantilla:
     * - with boxplot (sin smooth)
     * - set style fill solid 0.5 border
     * - set style boxplot outliers pointtype 7
     * - set boxwidth 0.5 absolute
     *
     * @param scriptPath  Ruta destino del .gnu (p.ej., base.resolve("boxplot_hv.gnu"))
     * @param title       Título del gráfico (p.ej., "Hipervolumen (HV) - Boxplots por subgrupo")
     * @param yLabel      Etiqueta eje Y (p.ej., "HV" o "Valor")
     * @param metricPrefix Prefijo de ficheros de datos (p.ej., "hv", "metric1", "metric2")
     * @param groups      Lista de subgrupos en orden (p.ej., ["Al_w1","Al_w3","Bl_w1","Bl_w3"])
     * @param outputPng   Nombre del PNG de salida (p.ej., "boxplot_hv.png")
     */
    private static void generateBoxplotScriptGNU(Path scriptPath,
                                                 String title,
                                                 String yLabel,
                                                 String metricPrefix,
                                                 List<String> groups,
                                                 String outputPng) throws IOException {
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("La lista de grupos no puede estar vacía.");
        }

        // groups = "Al_w1 Al_w3 Bl_w1 Bl_w3"
        String groupsStr = String.join(" ", groups);

        // set xtics ("Al_w1" 1, "Al_w3" 2, ...)
        StringBuilder xtics = new StringBuilder("(");
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) xtics.append(", ");
            xtics.append("\"").append(escapeForGnu(groups.get(i))).append("\" ").append(i + 1);
        }
        xtics.append(")");

        StringBuilder sb = new StringBuilder();
        sb.append("set terminal pngcairo size 1280,720 enhanced font \",10\"\n");
        sb.append("set output \"").append(escapeForGnu(outputPng)).append("\"\n");
        sb.append("set title \"").append(escapeForGnu(title)).append("\"\n");
        sb.append("set ylabel \"").append(escapeForGnu(yLabel)).append("\"\n");
        sb.append("set grid ytics\n\n");
        sb.append("set style fill solid 0.5 border\n");
        sb.append("set style boxplot outliers pointtype 7\n");
        sb.append("set boxwidth 0.5 absolute\n\n");
        sb.append("groups = \"").append(escapeForGnu(groupsStr)).append("\"\n");
        sb.append("N = words(groups)\n");
        sb.append("set xrange [0.5:N+0.5]\n");
        sb.append("set xtics ").append(xtics).append("\n\n");
        sb.append("plot for [i=1:N] sprintf(\"")
          .append(escapeForGnu(metricPrefix))
          .append("__%s.dat\", word(groups,i)) using (i):1 \\\n")
          .append("     with boxplot title word(groups,i)\n");

        Files.writeString(scriptPath, sb.toString(), StandardCharsets.UTF_8);
    }

    /** Escapa comillas para cadenas de gnuplot. */
    private static String escapeForGnu(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }


    /**
     * Genera un script GNUPlot (.gnu) de violines “estilo seaborn” para una métrica cualquiera.
     * - Lee <metricPrefix>__<grupo>.dat (1 columna, una muestra por línea)
     * - Genera/usa <metricPrefix>__<grupo>_kd.dat para las curvas KDE
     * - Dibuja violín relleno, contorno, boxplot interno y punto de media
     *
     * @param scriptPath Ruta del .gnu a generar
     * @param title      Título del gráfico
     * @param yLabel     Etiqueta del eje Y
     * @param metricPrefix Prefijo de ficheros (p.ej. "metric1", "metric2", "hv")
     * @param groups     Lista de grupos en orden
     * @param outputPng  Nombre del PNG de salida
     */
    private static void generateViolinPrettyScriptGNU(Path scriptPath,
                                                      String title,
                                                      String yLabel,
                                                      String metricPrefix,
                                                      List<String> groups,
                                                      String outputPng) throws IOException {
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("La lista de grupos no puede estar vacía.");
        }

        String groupsStr = String.join(" ", groups);

        // xtics dinámicos: ("Al_w1" 1, "Al_w3" 2, ...)
        StringBuilder xtics = new StringBuilder("(");
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) xtics.append(", \\\n      ");
            xtics.append("\"").append(escapeForGnu(groups.get(i))).append("\" ").append(i + 1);
        }
        xtics.append(")");

        StringBuilder sb = new StringBuilder();
        sb.append("# ============================================================\n");
        sb.append("# violin_").append(escapeForGnu(metricPrefix)).append(".gnu — Violines estilo \"seaborn\"\n");
        sb.append("# Requiere: ").append(escapeForGnu(metricPrefix)).append("__<grupo>.dat (1 columna, 1 muestra/linea)\n");
        sb.append("# ============================================================\n\n");

        sb.append("set terminal pngcairo size 1200,1200 enhanced font \",12\"\n");
        sb.append("set output \"").append(escapeForGnu(outputPng)).append("\"\n\n");

        sb.append("unset key\n");
        sb.append("set title \"").append(escapeForGnu(title)).append("\"\n");
        sb.append("set ylabel \"").append(escapeForGnu(yLabel)).append("\"\n");
        sb.append("set grid ytics\n");
        sb.append("set tics nomirror\n");
        sb.append("set border 3\n");
        sb.append("set datafile separator whitespace\n");
        sb.append("set samples 600         # resolución KDE\n");
        sb.append("set style fill solid 0.75 noborder  # relleno de los violines\n\n");

        // Grupos
        sb.append("groups = \"").append(escapeForGnu(groupsStr)).append("\"\n");
        sb.append("N = words(groups)\n\n");

        // Paleta de colores sin ternarios: ciclo sobre una lista de hex
        sb.append("# Paleta de colores cíclica\n");
        sb.append("colors = \"#1f77b4 #ff7f0e #2ca02c #9467bd #d62728 #8c564b #e377c2 #7f7f7f #bcbd22 #17becf\"\n");
        sb.append("ncolors = words(colors)\n");
        sb.append("rgb(i) = word(colors, 1 + ((i-1) % ncolors))\n\n");

        // KDE + global_max
        sb.append("global_max = 0.0\n");
        sb.append("do for [i=1:N] {\n");
        sb.append("    g = word(groups,i)\n");
        sb.append("    datafile = sprintf(\"").append(escapeForGnu(metricPrefix)).append("__%s.dat\", g)\n");
        sb.append("    kdfile   = sprintf(\"").append(escapeForGnu(metricPrefix)).append("__%s_kd.dat\", g)\n\n");
        sb.append("    set autoscale x\n");
        // Sin 'bandwidth 150' para dejar a gnuplot escoger el óptimo (más robusto entre métricas)
        sb.append("    set table kdfile\n");
        sb.append("    plot datafile using 1 smooth kdensity\n");
        sb.append("    unset table\n\n");
        sb.append("    stats kdfile using 2 nooutput\n");
        sb.append("    global_max = (STATS_max > global_max) ? STATS_max : global_max\n");
        sb.append("}\n\n");
        sb.append("if (global_max <= 0) global_max = 1.0\n");
        sb.append("scale = 0.40 / global_max   # ancho máx. violín en unidades X\n\n");

        // Rango Y
        sb.append("global_ymin = 1e30\n");
        sb.append("global_ymax = -1e30\n");
        sb.append("do for [i=1:N] {\n");
        sb.append("    kdfile = sprintf(\"").append(escapeForGnu(metricPrefix)).append("__%s_kd.dat\", word(groups,i))\n");
        sb.append("    stats kdfile using 1 nooutput\n");
        sb.append("    global_ymin = (STATS_min < global_ymin) ? STATS_min : global_ymin\n");
        sb.append("    global_ymax = (STATS_max > global_ymax) ? STATS_max : global_ymax\n");
        sb.append("}\n");
        sb.append("dy = (global_ymax - global_ymin)\n");
        sb.append("set yrange [global_ymin - 0.02*dy : global_ymax + 0.02*dy]\n\n");

        // Estadísticos
        sb.append("array med[N]; array q1[N]; array q3[N]; array avg[N]\n");
        sb.append("do for [i=1:N] {\n");
        sb.append("    g = word(groups,i)\n");
        sb.append("    datafile = sprintf(\"").append(escapeForGnu(metricPrefix)).append("__%s.dat\", g)\n");
        sb.append("    stats datafile using 1 nooutput\n");
        sb.append("    med[i] = STATS_median\n");
        sb.append("    q1[i]  = STATS_lo_quartile\n");
        sb.append("    q3[i]  = STATS_up_quartile\n");
        sb.append("    avg[i] = STATS_mean\n");
        sb.append("}\n\n");

        // Eje X
        sb.append("set xrange [0.5:N+0.5]\n");
        sb.append("set xtics \\\n    ").append(xtics).append("\n\n");

        // Estética boxplot
        sb.append("set style boxplot nooutliers\n");
        sb.append("set boxwidth 0.18 absolute\n\n");

        // Trazado
        sb.append("plot \\\n");
        sb.append("    for [i=1:N] sprintf(\"").append(escapeForGnu(metricPrefix)).append("__%s_kd.dat\", word(groups,i)) \\\n");
        sb.append("      using (i + scale*column(2)):(column(1)):(i - scale*column(2)) \\\n");
        sb.append("      with filledcurves lc rgb rgb(i) notitle, \\\n");
        sb.append("    for [i=1:N] sprintf(\"").append(escapeForGnu(metricPrefix)).append("__%s_kd.dat\", word(groups,i)) \\\n");
        sb.append("      using (i + scale*column(2)):(column(1)) with lines lc rgb \"#222222\" lw 1.2 notitle, \\\n");
        sb.append("    for [i=1:N] sprintf(\"").append(escapeForGnu(metricPrefix)).append("__%s_kd.dat\", word(groups,i)) \\\n");
        sb.append("      using (i - scale*column(2)):(column(1)) with lines lc rgb \"#222222\" lw 1.2 notitle, \\\n");
        sb.append("    for [i=1:N] sprintf(\"").append(escapeForGnu(metricPrefix)).append("__%s.dat\", word(groups,i)) \\\n");
        sb.append("      using (i):1 with boxplot lc rgb \"#333333\" notitle, \\\n");
        sb.append("    for [i=1:N] '+' using (i):(avg[i]) with points pt 7 ps 0.9 lc rgb \"white\" notitle\n");

        Files.writeString(scriptPath, sb.toString(), StandardCharsets.UTF_8);
    }



    private static String buildXtics(List<String> groups) {
        // Devuelve: ("Al_w1" 1, "Al_w3" 2, ...)
        StringBuilder x = new StringBuilder("(");
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) x.append(", ");
            x.append("\"").append(esc(groups.get(i))).append("\" ").append(i + 1);
        }
        x.append(")");
        return x.toString();
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    /* ====================== Ejecutar gnuplot ====================== */

    private static void runGnuplot(Path scriptPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("gnuplot", scriptPath.getFileName().toString());
            pb.directory(scriptPath.getParent().toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (Scanner sc = new Scanner(p.getInputStream(), StandardCharsets.UTF_8)) {
                while (sc.hasNextLine()) {
                    System.out.println("[gnuplot] " + sc.nextLine());
                }
            }
            int code = p.waitFor();
            if (code != 0) {
                System.err.println("AVISO: gnuplot devolvió código " + code + " para " + scriptPath);
            } else {
                System.out.println("OK: gnuplot ejecutado -> " + scriptPath.getFileName());
            }
        } catch (IOException | InterruptedException ex) {
            System.err.println("ERROR ejecutando gnuplot sobre " + scriptPath + ": " + ex.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
