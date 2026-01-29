package main_scico.hv.refpoint;

import java.nio.file.Path;
import java.util.List;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class CalculateRefPoint {

    // ====== PATRONES IGUALES AL PROGRAMA DE REFERENCIA ======
    private static final String DECIMAL = "([0-9]+(?:[\\.,][0-9]+)?)";

    // total Energy consumption (CPU+storage): 18,77504 kWh
    private static final Pattern P_ENERGY = Pattern.compile(
        "^total\\s+energy\\s+consumption\\s*\\(CPU\\+storage\\):\\s*" + DECIMAL + "\\s*kWh\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    // Total simulation time: 123.45 sec  (también acepta 123,45)
    private static final Pattern P_TIME = Pattern.compile(
        "^total\\s+simulation\\s+time:\\s*" + DECIMAL + "\\s*sec\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private final List<Path> roots;                 // directorios de entrada
    private final List<Path> indexedFiles = new ArrayList<>(8192); // output_*.tc(.gz) encontrados
    private boolean printBanners = true;            // para emular tus logs

    public CalculateRefPoint(List<Path> pathsIn) {
        if (pathsIn == null || pathsIn.isEmpty())
            throw new IllegalArgumentException("pathsIn no puede ser null ni vacío");
        this.roots = new ArrayList<>(pathsIn);
    }

    /** Activa/desactiva los banners "Indexed N outputs. Sample: ..." */
    public CalculateRefPoint withBanners(boolean enabled) {
        this.printBanners = enabled;
        return this;
    }

    /** Devuelve los ficheros indexados (tras llamar a compute o indexOnly). */
    public List<Path> getIndexedFiles() {
        return Collections.unmodifiableList(indexedFiles);
    }

    /** Solo indexa (sin calcular Q99) por si quieres inspeccionar primero. */
    public void indexOnly() {
        indexedFiles.clear();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            List<Path> files = listTcOutputFiles(root);
            if (printBanners) printIndexBanner(root, files);
            indexedFiles.addAll(files);
        }
    }

    /** Hace todo: indexa, parsea pares (E,T) y calcula {Q99_E, Q99_T}. */
    public double[] compute() {
        // 1) Indexar
        indexOnly();

        // 2) Parsear
        final List<Double> energies = new ArrayList<>(indexedFiles.size());
        final List<Double> times    = new ArrayList<>(indexedFiles.size());

        int parsedPairs = 0;
        for (Path f : indexedFiles) {
            double[] pair = readFirstEnergyTimePair(f);
            if (pair != null && pair[0] > 0.0 && pair[1] > 0.0) {
                energies.add(pair[0]);
                times.add(pair[1]);
                parsedPairs++;
            }
        }

        if (printBanners) {
            System.out.printf(Locale.US,
                    "[INFO] Parsed pairs: %d (from %d files)%n",
                    parsedPairs, indexedFiles.size());
        }

        if (energies.isEmpty() || times.isEmpty()) {
            throw new IllegalStateException("No se encontraron pares (energy, time) > 0 con el formato esperado.");
        }

        // 3) Percentil 99 con interpolación lineal p*(n-1)
        double q99E = percentile(energies, 0.99);
        double q99T = percentile(times,    0.99);

        if (printBanners) {
            System.out.printf(Locale.US,
                    "[RESULT] Q99 energia = %.6f, Q99 tiempo = %.6f (sobre %d pares)%n",
                    q99E, q99T, Math.min(energies.size(), times.size()));
        }
        return new double[]{q99E, q99T};
    }

    // ------------------------ INDEXACIÓN ------------------------

    private static boolean isTcOutputFile(Path p) {
        String name = p.getFileName().toString();
        if (!(name.startsWith("output_") && (name.endsWith(".tc") || name.endsWith(".tc.gz")))) return false;
        String norm = p.toString().replace('\\', '/');
        return norm.contains("/TcOutput/");
    }

    private static List<Path> listTcOutputFiles(Path root) {
        List<Path> files = new ArrayList<>(4096);
        try (Stream<Path> s = Files.walk(root)) {
            s.filter(Files::isRegularFile)
             .filter(CalculateRefPoint::isTcOutputFile)
             .forEach(files::add);
        } catch (IOException e) {
            System.err.printf("[WARN] Error indexando %s: %s%n", root, e.getMessage());
        }
        files.sort(Comparator.comparing(Path::toString));
        return files;
    }

    private static void printIndexBanner(Path root, List<Path> files) {
        System.out.printf("Indexed %d outputs. Sample: ", files.size());
        int shown = 0;
        for (int i = 0; i < files.size() && shown < 5; i++) {
            Path rel = safeRelativize(root, files.get(i));
            System.out.printf("%d->%s", (i + 1), rel.toString().replace('\\', '/'));
            shown++;
            if (i < files.size() - 1 && shown < 5) System.out.print(", ");
        }
        if (files.isEmpty()) System.out.print("(none)");
        System.out.println();
    }

    private static Path safeRelativize(Path root, Path child) {
        try {
            return root.relativize(child);
        } catch (IllegalArgumentException iae) {
            return child.toAbsolutePath();
        }
    }

    // ------------------------ PARSEO (idéntico al de referencia) ------------------------

    /** Lee el PRIMER par con el mismo formato que ParetoAndHVFromLogs (P_ENERGY/P_TIME). */
    private static double[] readFirstEnergyTimePair(Path file) {
        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(openMaybeGz(file), StandardCharsets.UTF_8))) {
            String line;
            Double energy = null, time = null;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                Matcher mE = P_ENERGY.matcher(s);
                if (mE.matches()) { energy = parseNumber(mE.group(1)); continue; }
                Matcher mT = P_TIME.matcher(s);
                if (mT.matches()) { time = parseNumber(mT.group(1)); }
                if (energy != null && time != null) break;
            }
            if (energy == null || time == null) return null;
            return new double[]{ energy, time };
        } catch (IOException ex) {
            System.err.printf("[WARN] No se pudo leer %s: %s%n", file, ex.getMessage());
            return null;
        }
    }

    /** Igual que en el programa de referencia: cambia coma por punto. */
    private static double parseNumber(String s) {
        return Double.parseDouble(s.replace(',', '.'));
    }

    private static InputStream openMaybeGz(Path f) throws IOException {
        InputStream is = Files.newInputStream(f);
        if (f.getFileName().toString().endsWith(".gz")) {
            return new GZIPInputStream(is);
        }
        return is;
    }

    // ------------------------ PERCENTIL ------------------------

    /** Percentil con interpolación lineal p*(n-1), estilo NumPy 'linear'. */
    private static double percentile(List<Double> data, double p) {
        if (data.isEmpty()) throw new IllegalArgumentException("Empty data");
        if (p < 0.0 || p > 1.0) throw new IllegalArgumentException("p fuera de [0,1]");
        List<Double> xs = new ArrayList<>(data);
        Collections.sort(xs);
        if (xs.size() == 1) return xs.get(0);
        double pos = p * (xs.size() - 1);
        int i = (int) Math.floor(pos), j = (int) Math.ceil(pos);
        if (i == j) return xs.get(i);
        double w = pos - i;
        return xs.get(i) * (1.0 - w) + xs.get(j) * w;
    }
}
