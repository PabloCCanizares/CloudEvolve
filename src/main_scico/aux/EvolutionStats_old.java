package main_scico.aux;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Programa para recorrer una carpeta con subcarpetas fijas (Al_w1, Al_w3, Bl_w1),
 * localizar en cada subcarpeta los ficheros "evolution3.dat" dentro de sus carpetas-hija,
 * leer la última línea de cada fichero y extraer los 2 primeros elementos numéricos.
 * Cada elemento se acumula en un ArrayList distinto (métrica 1 y métrica 2) por cada grupo.
 *
 * Estadísticos por grupo y por métrica: mínimo, máximo, media, mediana e IQR (Q3-Q1).
 *
 * Uso:
 *   java EvolutionStats "/ruta/a/carpeta/base"
 */
public class EvolutionStats_old 
{

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

        // Recorremos cada grupo
        for (String groupName : GROUPS) {
            Path groupDir = base.resolve(groupName);
            if (!Files.isDirectory(groupDir)) {
                System.err.println("AVISO: no existe el directorio del grupo: " + groupDir.toAbsolutePath());
                continue;
            }
            GroupAccumulator acc = data.get(groupName);
            // Buscamos evolution3.dat a profundidad 2 (grupo/exp/evolution3.dat)
            try (Stream<Path> walk = Files.walk(groupDir, 2)) {
                List<Path> files = walk
                        .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().equals("evolution3.dat"))
                        .collect(Collectors.toList());
                for (Path evoFile : files) {
                    try {
                        double[] pair = readLastLineFirstTwoNumbers(evoFile);
                        if (pair != null) {
                            acc.metric1.add(pair[0]);
                            acc.metric2.add(pair[1]);
                            acc.filesProcessed++;
                        } else {
                            System.err.println("AVISO: no se pudo leer 2 números de la última línea: " + evoFile);
                        }
                    } catch (IOException ex) {
                        System.err.println("ERROR leyendo " + evoFile + ": " + ex.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("ERROR recorriendo " + groupDir + ": " + e.getMessage());
            }
        }

        // Imprimimos resultados
        System.out.println("======= RESUMEN DE ESTADÍSTICAS =======");
        for (String groupName : GROUPS) {
            GroupAccumulator acc = data.get(groupName);
            System.out.println("\n=== Grupo: " + groupName + " ===");
            System.out.println("Ficheros procesados: " + acc.filesProcessed);

            printStats("Métrica #1", acc.metric1);
            printStats("Métrica #2", acc.metric2);
        }
    }

    /**
     * Lee la última línea no vacía del fichero y devuelve los 2 primeros elementos numéricos (double).
     * Devuelve null si no hay suficientes elementos parseables.
     */
    private static double[] readLastLineFirstTwoNumbers(Path file) throws IOException {
        String last = null;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) last = line;
            }
        }
        if (last == null) return null;

        String[] toks = last.trim().split("\\s+");
        if (toks.length < 2) return null;

        try {
            double a = Double.parseDouble(toks[0]);
            double b = Double.parseDouble(toks[1]);
            return new double[]{a, b};
        } catch (NumberFormatException nfe) {
            // Intento adicional: sustituir coma decimal por punto si apareciera
            try {
                double a = Double.parseDouble(toks[0].replace(',', '.'));
                double b = Double.parseDouble(toks[1].replace(',', '.'));
                return new double[]{a, b};
            } catch (NumberFormatException nfe2) {
                return null;
            }
        }
    }

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
                "%s: n=%d &%.1f &%.1f &%.1f &%.1f &%.1f\n",
                label, st.n, st.min, st.max, st.mean, st.median, st.iqr);
        
    }

    /** Acumulador por grupo para dos métricas. */
    private static class GroupAccumulator {
        final String name;
        final List<Double> metric1 = new ArrayList<>();
        final List<Double> metric2 = new ArrayList<>();
        int filesProcessed = 0;

        GroupAccumulator(String name) { this.name = name; }
    }

    /** Estadísticos básicos + cuartiles. */
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

            // Cuartiles con método "mediana de mitades":
            // - Si n es impar, se excluye la mediana del cálculo de Q1 y Q3.
            // - Si n es par, se divide en dos mitades iguales.
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

        /**
         * Devuelve la mediana de un segmento [from, to) de un array ya ORDENADO.
         * Si el número de elementos es par, devuelve el promedio de los dos del centro.
         */
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
}
