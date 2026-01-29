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

public class NadirSong {

    private static final String DECIMAL = "([0-9]+(?:[\\.,][0-9]+)?)";

    private static final Pattern P_ENERGY = Pattern.compile(
        "^total\\s+energy\\s+consumption\\s*\\(CPU\\+storage\\):\\s*" + DECIMAL + "\\s*kWh\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern P_TIME = Pattern.compile(
        "^total\\s+simulation\\s+time:\\s*" + DECIMAL + "\\s*sec\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    // NUEVO: cualquier indicio de fallo en el log invalida el fichero
    private static final Pattern P_FAIL = Pattern.compile("\\b(fail(?:ed)?|error|exception)\\b", Pattern.CASE_INSENSITIVE);

    private final List<Path> roots;
    private final List<Path> indexedFiles = new ArrayList<>(8192);
    private boolean printBanners = true;

    public NadirSong(List<Path> pathsIn) {
        if (pathsIn == null || pathsIn.isEmpty())
            throw new IllegalArgumentException("pathsIn no puede ser null ni vacío");
        this.roots = new ArrayList<>(pathsIn);
    }

    public NadirSong withBanners(boolean enabled) {
        this.printBanners = enabled;
        return this;
    }

    public List<Path> getIndexedFiles() {
        return Collections.unmodifiableList(indexedFiles);
    }

    public void indexOnly() {
        indexedFiles.clear();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            List<Path> files = listTcOutputFiles(root);
            if (printBanners) printIndexBanner(root, files);
            indexedFiles.addAll(files);
        }
    }

    private static final class Point {
        final double e, t;
        final Path path;
        Point(double e, double t, Path path) { this.e = e; this.t = t; this.path = path; }
    }

    public double[] computeNadir() {
        indexOnly();

        final List<Point> points = new ArrayList<>(indexedFiles.size());
        int parsedPairs = 0;
        for (Path f : indexedFiles) {
            double[] pair = readFirstEnergyTimePair(f);
            if (pair != null && pair[0] > 0.0 && pair[1] > 0.0) {
                points.add(new Point(pair[0], pair[1], f));
                parsedPairs++;
            }
        }

        if (printBanners) {
            System.out.printf(Locale.US,
                "[INFO] Parsed pairs: %d (from %d files)%n",
                parsedPairs, indexedFiles.size());
        }

        if (points.isEmpty()) {
            throw new IllegalStateException("No se encontraron pares (energy, time) > 0 con el formato esperado.");
        }

        List<Point> nd = nonDominated(points);

        Point maxE = null, maxT = null;
        for (Point p : nd) {
            if (maxE == null || p.e > maxE.e) maxE = p;
            if (maxT == null || p.t > maxT.t) maxT = p;
        }
        double nadirE = maxE.e;
        double nadirT = maxT.t;

        if (printBanners) {
            System.out.printf(Locale.US, "[RESULT] ND size = %d; NADIR = [%.6f, %.6f]%n", nd.size(), nadirE, nadirT);
            System.out.println("[ARGMAX] energy from: " + safeToString(maxE.path));
            System.out.println("[ARGMAX] time   from: " + safeToString(maxT.path));
            if (maxE.path.equals(maxT.path)) {
                System.out.println("[NOTE] El mismo individuo alcanza ambos componentes del nadir.");
            }
        }
        return new double[]{nadirE, nadirT};
    }

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
             .filter(NadirSong::isTcOutputFile)
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

    private static String safeToString(Path p) {
        try { return p.toAbsolutePath().toString(); } catch (Exception e) { return String.valueOf(p); }
    }

    /** Lee el PRIMER par (E,T); si el log contiene “failed”, “error” o “exception”, se descarta. */
    private static double[] readFirstEnergyTimePair(Path file) {
        try (BufferedReader br =
                 new BufferedReader(new InputStreamReader(openMaybeGz(file), StandardCharsets.UTF_8))) {
            String line;
            Double energy = null, time = null;
            boolean bad = false;
            while ((line = br.readLine()) != null) {
                String s = line.trim();

                // filtro de fallos
                if (!bad && P_FAIL.matcher(s).find()) {
                    bad = true;
                    // no hace falta seguir leyendo, pero seguimos por si quieres cambiar a futuro:
                    // break; // si prefieres cortar en cuanto detectes fallo, descomenta esta línea
                }

                Matcher mE = P_ENERGY.matcher(s);
                if (mE.matches()) { energy = parseNumber(mE.group(1)); continue; }
                Matcher mT = P_TIME.matcher(s);
                if (mT.matches()) { time = parseNumber(mT.group(1)); }
                if (energy != null && time != null && bad) break; // ya tenemos datos pero sabemos que es fallo
                if (energy != null && time != null && !bad) break;
            }
            if (bad) return null;                 // descartar fichero con fallo
            if (energy == null || time == null) return null;
            return new double[]{ energy, time };
        } catch (IOException ex) {
            System.err.printf("[WARN] No se pudo leer %s: %s%n", file, ex.getMessage());
            return null;
        }
    }

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

    private static List<Point> nonDominated(List<Point> pts) {
        if (pts.isEmpty()) return Collections.emptyList();

        List<Point> xs = new ArrayList<>(pts);
        xs.sort(Comparator.<Point>comparingDouble(p -> p.e)
                .thenComparingDouble(p -> p.t));

        List<Point> nd = new ArrayList<>();
        double bestTime = Double.POSITIVE_INFINITY;
        for (Point p : xs) {
            if (p.t < bestTime) {
                nd.add(p);
                bestTime = p.t;
            }
        }
        return nd;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java main_scico.hv.refpoint.NadirSong <dir1> [<dir2> ...]");
            System.exit(1);
        }
        List<Path> roots = new ArrayList<>();
        for (String a : args) roots.add(Paths.get(a));
        double[] nadir = new NadirSong(roots).withBanners(true).computeNadir();
        System.out.printf(Locale.US, "[NADIR] Global ND nadir = [%.6f, %.6f]%n", nadir[0], nadir[1]);
    }
}
