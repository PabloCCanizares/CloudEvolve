package main_scico.hv;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Recorre una carpeta de experimento (la que contiene iterationlist.txt y subcarpetas 0000/, 0001/, ...),
 * extrae objetivos (Energía, Tiempo) desde output_*.tc, filtra factibles (>0), calcula:
 *  - frentes ND por iteración (IDs y valores),
 *  - hipervolumen por iteración (población y, opcionalmente, archivo virtual acumulado),
 *  - ficheros .dat y script gnuplot para graficar la evolución del HV.
 *
 * Uso:
 *   java algorithms.moga.ParetoAndHVFromLogs <experimentRoot> [--virtual-archive] [--ref E,T] [--ref-margin M]
 * Ejemplos:
 *   java ... ParetoAndHVFromLogs /path/exp --ref 1000,5000
 *   java ... ParetoAndHVFromLogs /path/exp --ref 1000,5000 --virtual-archive
 *   java ... ParetoAndHVFromLogs /path/exp --ref-margin 0.20
 */
public class ParetoAndHVFromLogs {

	/*
	    private static final Pattern P_ENERGY =
	        Pattern.compile("^total Energy consumption \\(CPU\\+storage\\):\\s*([0-9]+(?:\\.[0-9]+)?)\\s*kWh$", Pattern.CASE_INSENSITIVE);
	    private static final Pattern P_TIME =
	        Pattern.compile("^Total simulation time:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*sec$", Pattern.CASE_INSENSITIVE);
	    private static final Pattern P_ITERLINE =
	        Pattern.compile("^\\s*(\\d+)\\s*-\\s*\\[([^\\]]+)\\]\\s*$");
	    private static final Pattern P_OUTPUT_NAME =
	        Pattern.compile("^output_(\\d+)\\.tc$"); // acepta nº de dígitos variable
	        */
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

	// Cost Energy consumption: 3,31004 $
	private static final Pattern P_COST = Pattern.compile(
	    "^cost\\s+energy\\s+consumption:\\s*" + DECIMAL + "\\s*\\$\\s*$",
	    Pattern.CASE_INSENSITIVE
	);

	// Sin cambios (no dependen del separador decimal)
	private static final Pattern P_ITERLINE =
	    Pattern.compile("^\\s*(\\d+)\\s*-\\s*\\[([^\\]]+)\\]\\s*$");

	private static final Pattern P_OUTPUT_NAME =
	    Pattern.compile("^output_(\\d+)\\.tc$");
	
    private final Path root;           // .../ALG/CONF/<timestamp>_CONF
    private final Path iterListFile;   // iterationlist.txt

    // Índice dinámico ID -> ruta de output
    private final Map<Integer, Path> idToPath = new HashMap<>();
    // Caché de (E,T) por ID
    private final Map<Integer, double[]> idToPoint = new HashMap<>();

    // Flags/params
    private final boolean useVirtualArchive;
    private final double[] refFromCli;    // null si no se pasó --ref
    private final double refMargin;       // solo se usa si refFromCli == null

    double maxE=0.0;
    double maxT=0.0;
    
    public ParetoAndHVFromLogs(Path experimentRoot, boolean useVirtualArchive, double[] refFromCli, double refMargin) {
        this.root = experimentRoot;
        this.iterListFile = experimentRoot.resolve("iterationlist.txt");
        this.useVirtualArchive = useVirtualArchive;
        this.refFromCli = refFromCli;           // puede ser null
        this.refMargin  = refMargin;            // típicamente 0.10
        

        maxE=0.0;
        maxT=0.0;
    }

    public void run() throws IOException {
        // 0) Indexar outputs dinámicamente
        indexOutputs();

        // 1) Leer iterationlist
        List<GenEntry> gens = readIterationList(iterListFile);

        // 2) Determinar punto de referencia
        final double[] ref;
        if (refFromCli != null) {
            if (refFromCli.length != 2) {
                throw new IllegalArgumentException("--ref must have 2 values: energy,time (comma-separated)");
            }
            ref = Arrays.copyOf(refFromCli, 2);
            // Validación suave: avisar si hay puntos fuera del ref
            int outside = countPointsOutsideRef(gens, ref);
            if (outside > 0) {
                System.err.println("[WARN] " + outside + " points are not strictly better than the fixed reference point.");
                System.err.println("       HV will ignore contributions beyond ref (minimization). Consider a larger --ref if needed.");
            }
        } else {
            for (GenEntry g : gens) {
                for (int id : g.ids) {
                    double[] pt = getPointForId(id);
                    if (pt == null) continue;
                    if (pt[0] > 0 && pt[1] > 0) {
                        if (pt[0] > maxE) maxE = pt[0];
                        if (pt[1] > maxT) maxT = pt[1];
                    }
                }
            }
            if (maxE <= 0 || maxT <= 0) {
                throw new IllegalStateException("Cannot determine positive maxima for reference point.");
            }
            ref = new double[]{ maxE * (1.0 + refMargin), maxT * (1.0 + refMargin) };
            System.out.printf(Locale.US, "[INFO] Using empirical ref with margin %.2f: [%.6f, %.6f]%n",
                    refMargin, ref[0], ref[1]);
        }

        //Rutas de salida (instantáneo)
        Path idsTxt     = root.resolve("pareto_ids_per_generation.txt");
        Path valsTxt    = root.resolve("pareto_values_per_generation.txt");
        Path hvCsv      = root.resolve("hv_per_generation.csv");
        Path hvDat      = root.resolve("hv_evolution.dat");

        // 3b) Rutas de salida (virtual archive, si procede)
        Path idsVTxt    = root.resolve("pareto_ids_virtual_per_generation.txt");
        Path valsVTxt   = root.resolve("pareto_values_virtual_per_generation.txt");
        Path hvVCsv     = root.resolve("hv_virtual_per_generation.csv");
        Path hvVDat     = root.resolve("hv_virtual_evolution.dat");

        // 3c) GNUplot
        Path hvGnu      = root.resolve("hv_evolution.gnu");
        Path hvPng      = root.resolve("hv_evolution.png");
        Path hvEps      = root.resolve("hv_evolution.eps");

        // 4) Recorrido y generación de ficheros
        try (BufferedWriter wIds  = Files.newBufferedWriter(idsTxt);
             BufferedWriter wVals = Files.newBufferedWriter(valsTxt);
             BufferedWriter wHv   = Files.newBufferedWriter(hvCsv);
             BufferedWriter wDat  = Files.newBufferedWriter(hvDat);
             BufferedWriter wIdsV  = useVirtualArchive ? Files.newBufferedWriter(idsVTxt)  : null;
             BufferedWriter wValsV = useVirtualArchive ? Files.newBufferedWriter(valsVTxt) : null;
             BufferedWriter wHvV   = useVirtualArchive ? Files.newBufferedWriter(hvVCsv)   : null;
             BufferedWriter wDatV  = useVirtualArchive ? Files.newBufferedWriter(hvVDat)   : null) {

            wHv.write("generation,pop_size,nd_size,hypervolume,ref_energy,ref_time\n");
            wDat.write("# generation hypervolume\n");

            if (useVirtualArchive) {
                wHvV.write("generation,archive_size,hypervolume,ref_energy,ref_time\n");
                wDatV.write("# generation hypervolume_virtual\n");
            }

            VirtualArchive2D vArch = useVirtualArchive ? new VirtualArchive2D(ref) : null;

            for (GenEntry g : gens) {
                // Población factible de la generación
                List<Integer> keptIds = new ArrayList<>();
                List<double[]> pts = new ArrayList<>();
                for (int id : g.ids) {
                    double[] p = getPointForId(id);
                    if (p == null) continue;
                    if (p[0] > 0 && p[1] > 0) {
                        keptIds.add(id);
                        pts.add(p);
                    }
                }

                // Frente ND instantáneo
                List<Integer> ndIdx = HV2DUtil.nonDominatedIndices(pts);

                // (1) IDs ND (instante)
                wIds.write(g.iter + " - [ ");
                for (int i = 0; i < ndIdx.size(); i++) {
                    int gid = keptIds.get(ndIdx.get(i));
                    wIds.write(Integer.toString(gid));
                    if (i + 1 < ndIdx.size()) wIds.write("  ");
                }
                wIds.write(" ]\n");

                // (2) Valores ND (instante)
                wVals.write(g.iter + " - [ ");
                for (int i = 0; i < ndIdx.size(); i++) {
                    double[] p = pts.get(ndIdx.get(i));
                    wVals.write(String.format(Locale.US, "(%.6f,%.6f)", p[0], p[1]));
                    if (i + 1 < ndIdx.size()) wVals.write("  ");
                }
                wVals.write(" ]\n");

                // (3) HV instantáneo
                List<double[]> ndPts = new ArrayList<>(ndIdx.size());
                for (int idx : ndIdx) ndPts.add(pts.get(idx));
                double hv = HV2DUtil.compute(ndPts, ref);
                wHv.write(String.format(Locale.US, "%d,%d,%d,%.8f,%.6f,%.6f%n",
                        g.iter, keptIds.size(), ndIdx.size(), hv, ref[0], ref[1]));
                wDat.write(String.format(Locale.US, "%d %.10f%n", g.iter, hv));

                // Archivo virtual (opcional)
                if (useVirtualArchive) {
                    vArch.addBatch(keptIds, pts);
                    List<Integer> aIds = vArch.getIds();
                    List<double[]> aPts = vArch.getPoints();

                    wIdsV.write(g.iter + " - [ ");
                    for (int i = 0; i < aIds.size(); i++) {
                        wIdsV.write(Integer.toString(aIds.get(i)));
                        if (i + 1 < aIds.size()) wIdsV.write("  ");
                    }
                    wIdsV.write(" ]\n");

                    wValsV.write(g.iter + " - [ ");
                    for (int i = 0; i < aPts.size(); i++) {
                        double[] p = aPts.get(i);
                        wValsV.write(String.format(Locale.US, "(%.6f,%.6f)", p[0], p[1]));
                        if (i + 1 < aPts.size()) wValsV.write("  ");
                    }
                    wValsV.write(" ]\n");

                    double hvV = HV2DUtil.compute(aPts, ref);
                    wHvV.write(String.format(Locale.US, "%d,%d,%.8f,%.6f,%.6f%n",
                            g.iter, aPts.size(), hvV, ref[0], ref[1]));
                    wDatV.write(String.format(Locale.US, "%d %.10f%n", g.iter, hvV));
                }
            }
        }

        // Script gnuplot (una o dos curvas) y ejecución
        writeGnuplotScript(
            hvGnu,
            hvDat.getFileName().toString(),
            useVirtualArchive ? hvVDat.getFileName().toString() : null,
            hvPng.getFileName().toString(),
            hvEps.getFileName().toString()
        );
        runGnuplot(hvGnu, hvPng);

        System.out.println("[DONE] Outputs written under: " + root);
        
     // === Cálculo SIEMPRE del nadir ND global al finalizar ===
        try {
            List<GenEntry> gensForNadir = readIterationList(iterListFile); // ya existe el método
            double[] nadir = computeGlobalNadir(gensForNadir);
            double[] nadirPlus = new double[]{ nadir[0] * (1.0 + refMargin), nadir[1] * (1.0 + refMargin) };

            System.out.printf(Locale.US, "[NADIR] Global ND nadir = [%.6f, %.6f]%n", nadir[0], nadir[1]);
            System.out.printf(Locale.US, "[NADIR+ε] margin=%.2f -> [%.6f, %.6f]%n",
                    refMargin, nadirPlus[0], nadirPlus[1]);

            // Guardar a disco
            Path nadirTxt = root.resolve("nadir_global_nd.txt");
            try (BufferedWriter bw = Files.newBufferedWriter(nadirTxt)) {
                bw.write(String.format(Locale.US, "nadir_energy=%.6f%n", nadir[0]));
                bw.write(String.format(Locale.US, "nadir_time=%.6f%n",   nadir[1]));
            }
            Path nadirPlusTxt = root.resolve("nadir_global_nd_plus_margin.txt");
            try (BufferedWriter bw = Files.newBufferedWriter(nadirPlusTxt)) {
                bw.write(String.format(Locale.US, "epsilon=%.6f%n", refMargin));
                bw.write(String.format(Locale.US, "ref_energy=%.6f%n", nadirPlus[0]));
                bw.write(String.format(Locale.US, "ref_time=%.6f%n",   nadirPlus[1]));
            }
        } catch (Exception e) {
            System.err.println("[WARN] Could not compute/write global ND nadir: " + e.getMessage());
        }

    }

    /* ========= helpers para ref CLI ========= */

    private int countPointsOutsideRef(List<GenEntry> gens, double[] ref) throws IOException {
        int count = 0;
        for (GenEntry g : gens) {
            for (int id : g.ids) {
                double[] p = getPointForId(id);
                if (p == null) continue;
                if (!(p[0] > 0 && p[1] > 0)) continue;
                // punto “peor” que ref (mín): si p[d] >= ref[d], no aporta HV
                if (p[0] >= ref[0] || p[1] >= ref[1]) count++;
            }
        }
        return count;
    }

   // Calcula el nadir del frente ND global (minimización 2D) usando todos los puntos factibles
    private double[] computeGlobalNadir(List<GenEntry> gens) throws IOException {
        List<double[]> allPts = new ArrayList<>();
        for (GenEntry g : gens) {
            for (int id : g.ids) {
                double[] p = getPointForId(id);
                if (p == null) continue;
                if (p[0] > 0 && p[1] > 0) allPts.add(p);
            }
        }
        if (allPts.isEmpty()) {
            throw new IllegalStateException("No feasible points to compute global ND nadir.");
        }

        // ND global sobre todas las soluciones factibles
        List<Integer> ndIdx = HV2DUtil.nonDominatedIndices(allPts);
        if (ndIdx.isEmpty()) {
            throw new IllegalStateException("Global ND set is empty (unexpected).");
        }

        double nadirE = Double.NEGATIVE_INFINITY;
        double nadirT = Double.NEGATIVE_INFINITY;
        for (int idx : ndIdx) {
            double[] p = allPts.get(idx);
            if (p[0] > nadirE) nadirE = p[0];
            if (p[1] > nadirT) nadirT = p[1];
        }
        if (!(nadirE > 0 && nadirT > 0)) {
            throw new IllegalStateException("Computed nadir is not positive.");
        }
        return new double[]{nadirE, nadirT};
    }

    /* ===================== Archivo virtual 2D (minimización) ===================== */

    private static final class VirtualArchive2D {
        private final double[] ref;
        private final List<Integer> ids = new ArrayList<>();
        private final List<double[]> pts = new ArrayList<>();

        VirtualArchive2D(double[] ref) {
            this.ref = Arrays.copyOf(ref, ref.length);
            if (ref.length != 2) throw new IllegalArgumentException("VirtualArchive2D expects 2 objectives.");
        }

        /** Inserta lote: añade factibles que contribuyen, elimina dominados del archivo. */
        void addBatch(List<Integer> newIds, List<double[]> newPts) {
            for (int i = 0; i < newPts.size(); i++) {
                int id = newIds.get(i);
                double[] p = newPts.get(i);
                if (!(p[0] > 0 && p[1] > 0)) continue;
                if (!(p[0] < ref[0] && p[1] < ref[1])) continue;

                boolean dominatedByArchive = false;
                for (double[] a : pts) {
                    if (dominates(a, p)) { dominatedByArchive = true; break; }
                }
                if (dominatedByArchive) continue;

                for (int j = pts.size() - 1; j >= 0; j--) {
                    if (dominates(p, pts.get(j))) {
                        pts.remove(j);
                        ids.remove(j);
                    }
                }
                ids.add(id);
                pts.add(p);
            }
            sortSkyline();
        }

        List<Integer> getIds()   { return new ArrayList<>(ids); }
        List<double[]> getPoints(){
            List<double[]> out = new ArrayList<>(pts.size());
            for (double[] p : pts) out.add(Arrays.copyOf(p, p.length));
            return out;
        }

        private void sortSkyline() {
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < pts.size(); i++) order.add(i);
            order.sort(Comparator.comparingDouble(i -> pts.get(i)[0]));

            List<Integer> idsSky = new ArrayList<>();
            List<double[]> ptsSky = new ArrayList<>();
            double bestY = Double.POSITIVE_INFINITY;
            for (int idx : order) {
                double[] p = pts.get(idx);
                if (p[1] < bestY) {
                    idsSky.add(ids.get(idx));
                    ptsSky.add(p);
                    bestY = p[1];
                }
            }
            ids.clear(); ids.addAll(idsSky);
            pts.clear(); pts.addAll(ptsSky);
        }

        private static boolean dominates(double[] a, double[] b) {
            boolean better = false;
            if (a[0] > b[0] || a[1] > b[1]) return false;
            if (a[0] < b[0]) better = true;
            if (a[1] < b[1]) better = true;
            return better;
        }
    }

    /* ===================== Indexación dinámica ===================== */

    /** Recorre subdirectorios y registra todos los output_*.tc (ID → Path). */
    private void indexOutputs() throws IOException {
        idToPath.clear();

        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(root)) {
            for (Path sub : dirs) {
                if (!Files.isDirectory(sub)) continue;
                Path tcOut = sub.resolve("TcOutput");
                if (!Files.isDirectory(tcOut)) continue;

                try (DirectoryStream<Path> outs = Files.newDirectoryStream(tcOut, "output_*.tc")) {
                    for (Path f : outs) {
                        String name = f.getFileName().toString();
                        Matcher m = P_OUTPUT_NAME.matcher(name);
                        if (m.matches()) {
                            int id = Integer.parseInt(m.group(1)); // nº de dígitos variable
                            idToPath.put(id, f);
                        }
                    }
                }
            }
        }

        if (idToPath.isEmpty()) {
            throw new IllegalStateException("No output_*.tc files found under: " + root);
        }

        String sample = idToPath.entrySet().stream().limit(5)
                .map(e -> e.getKey() + "->" + root.relativize(e.getValue()))
                .collect(Collectors.joining(", "));
        System.out.println("Indexed " + idToPath.size() + " outputs. Sample: " + sample);
    }

    /* ========================= Helpers ========================= */
    private static double parseNumber(String s) {
        return Double.parseDouble(s.replace(',', '.'));
    }
    private double[] getPointForId(int globalId) throws IOException {
        double[] cached = idToPoint.get(globalId);
        if (cached != null) return cached;

        Path file = idToPath.get(globalId);
        if (file == null) return null; // ID no indexado

        Double energy = null, time = null;
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                Matcher mE = P_ENERGY.matcher(s);
                if (mE.matches()) 
                { 
                	energy = parseNumber(mE.group(1)); continue; 
                }
                Matcher mT = P_TIME.matcher(s);
                if (mT.matches()) { 
                	time = parseNumber(mT.group(1)); }
                if (energy != null && time != null) break;
            }
        }
        if (energy == null || time == null) return null;
        double[] pt = new double[]{ energy, time };
        idToPoint.put(globalId, pt);
        return pt;
    }

    private static List<GenEntry> readIterationList(Path file) throws IOException {
        List<GenEntry> gens = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = P_ITERLINE.matcher(line);
                if (!m.matches()) continue;
                int iter = Integer.parseInt(m.group(1));
                String body = m.group(2).trim();
                // IDs separados por espacios (en tus ejemplos con doble espacio, split \\s+ lo cubre)
                String[] toks = body.split("\\s+");
                List<Integer> ids = new ArrayList<>();
                for (String tok : toks) {
                    try { ids.add(Integer.parseInt(tok)); } catch (NumberFormatException ignored) {}
                }
                gens.add(new GenEntry(iter, ids));
            }
        }
        gens.sort(Comparator.comparingInt(g -> g.iter));
        return gens;
    }

    private static final class GenEntry {
        final int iter;
        final List<Integer> ids;
        GenEntry(int iter, List<Integer> ids) { this.iter = iter; this.ids = ids; }
    }

    /* ===================== GNUPlot: script + ejecución ===================== */

    private void writeGnuplotScript(Path gnuFile, String datInst, String datVirt,
                                    String pngName, String epsName) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(gnuFile)) {
            bw.write("# Auto-generated by ParetoAndHVFromLogs\n");
            bw.write("set term pngcairo size 1200,800 enhanced\n");
            bw.write("set output '" + pngName + "'\n");
            bw.write("set title 'Hypervolume evolution'\n");
            bw.write("set xlabel 'Generation'\n");
            bw.write("set ylabel 'Hypervolume'\n");
            bw.write("set grid\n");
            bw.write("set key left top\n");
            if (datVirt != null) {
                bw.write("plot \\\n");
                bw.write("  '" + datInst + "' using 1:2 with linespoints lw 2 pt 7 title 'HV (population)', \\\n");
                bw.write("  '" + datVirt + "' using 1:2 with lines lw 3 title 'HV (virtual archive)'\n");
            } else {
                bw.write("plot '" + datInst + "' using 1:2 with linespoints lw 2 pt 7 title 'HV (population)'\n");
            }
            bw.write("\n");
            bw.write("set term postscript eps enhanced color font 'Arial,14'\n");
            bw.write("set output '" + epsName + "'\n");
            bw.write("replot\n");
            bw.write("set output\n");
        }
    }

    private void runGnuplot(Path script, Path pngOut) {
        try {
            Process p = new ProcessBuilder("gnuplot", script.getFileName().toString())
                    .directory(root.toFile())
                    .inheritIO()
                    .start();
            int code = p.waitFor();
            if (code != 0) {
                System.err.println("gnuplot exited with code " + code + ". You can run it manually:");
                System.err.println("  (cd " + root + " && gnuplot " + script.getFileName() + ")");
                return;
            }
            // Intentar abrir el PNG (opcional; ignora fallo si no hay GUI)
            /*try {
                new ProcessBuilder("xdg-open", pngOut.getFileName().toString())
                        .directory(root.toFile())
                        .start();
            } catch (Exception ignore) {}*/
        } catch (Exception e) {
            System.err.println("Could not run gnuplot automatically. Run manually:");
            System.err.println("  (cd " + root + " && gnuplot " + script.getFileName() + ")");
        }
    }

    /* ========================= main ========================= */

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: ParetoAndHVFromLogs <experimentRoot> [--virtual-archive] [--ref E,T] [--ref-margin M]");
            System.err.println("  Example (fixed ref):       ParetoAndHVFromLogs /path/exp --ref 1000,5000");
            System.err.println("  Example (empirical +20%):  ParetoAndHVFromLogs /path/exp --ref-margin 0.20");
            System.exit(1);
        }
        Path root = Paths.get(args[0]);
        boolean useVirtual = false;
        double[] refCli = null;
        double refMargin = 0.10;

        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if ("--virtual-archive".equalsIgnoreCase(a)) {
                useVirtual = true;
            } else if ("--ref".equalsIgnoreCase(a) && i + 1 < args.length) {
                refCli = parseRef(args[++i]);
            } else if ("--ref-margin".equalsIgnoreCase(a) && i + 1 < args.length) {
                refMargin = Double.parseDouble(args[++i]);
                if (refMargin < 0) throw new IllegalArgumentException("--ref-margin must be >= 0");
            }
        }

        new ParetoAndHVFromLogs(root, useVirtual, refCli, refMargin).run();
    }

    private static double[] parseRef(String s) {
        String[] parts = s.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("--ref must be 'E,T' (e.g., --ref 1000,5000)");
        }
        double e = Double.parseDouble(parts[0].trim());
        double t = Double.parseDouble(parts[1].trim());
        if (!(e > 0 && t > 0)) {
            throw new IllegalArgumentException("--ref values must be > 0");
        }
        return new double[]{e, t};
    }

    /* ===================== Utilidad HV 2D interna (exacta) ===================== */

    /** Versión embebida para que el archivo sea autocontenido. */
    private static final class HV2DUtil {
        private HV2DUtil(){}

        /** Exact 2D hypervolume for minimization, using non-dominated skyline. */
        static double compute(List<double[]> points, double[] ref) {
            // Keep feasible and strictly better than ref
            List<double[]> S = new ArrayList<>();
            for (double[] p : points) {
                if (p.length != 2) throw new IllegalArgumentException("2D expected");
                if (p[0] > 0 && p[1] > 0 && p[0] < ref[0] && p[1] < ref[1]) {
                    S.add(p);
                }
            }
            if (S.isEmpty()) return 0.0;

            List<double[]> nd = nonDominated(S);

            // Sort by x asc; build decreasing y skyline
            nd.sort(Comparator.comparingDouble(a -> a[0]));
            List<double[]> sky = new ArrayList<>();
            double bestY = Double.POSITIVE_INFINITY;
            for (double[] p : nd) {
                if (p[1] < bestY) {
                    sky.add(p);
                    bestY = p[1];
                }
            }

            double hv = 0.0;
            for (int i = 0; i < sky.size(); i++) {
                double[] p = sky.get(i);
                double xRight = (i + 1 < sky.size()) ? sky.get(i + 1)[0] : ref[0];
                double width  = xRight - p[0];
                double height = ref[1] - p[1];
                if (width > 0 && height > 0) hv += width * height;
            }
            return hv;
        }

        /** Non-dominated filter for minimization (returns indices). */
        static List<Integer> nonDominatedIndices(List<double[]> pts) {
            List<Integer> ndIdx = new ArrayList<>();
            for (int i = 0; i < pts.size(); i++) {
                double[] p = pts.get(i);
                boolean dominated = false;
                for (int j = 0; j < pts.size(); j++) {
                    if (i == j) continue;
                    double[] q = pts.get(j);
                    if (dominates(q, p)) { dominated = true; break; }
                }
                if (!dominated) ndIdx.add(i);
            }
            return ndIdx;
        }

        private static List<double[]> nonDominated(List<double[]> pts) {
            List<double[]> nd = new ArrayList<>();
            for (int i = 0; i < pts.size(); i++) {
                double[] p = pts.get(i);
                boolean dominated = false;
                for (int j = 0; j < pts.size(); j++) {
                    if (i == j) continue;
                    double[] q = pts.get(j);
                    if (dominates(q, p)) { dominated = true; break; }
                }
                if (!dominated) nd.add(p);
            }
            return nd;
        }

        /** a dominates b iff a <= b in all and < in at least one (minimization). */
        private static boolean dominates(double[] a, double[] b) {
            boolean better = false;
            if (a[0] > b[0] || a[1] > b[1]) return false;
            if (a[0] < b[0]) better = true;
            if (a[1] < b[1]) better = true;
            return better;
        }
    }

    public static void calcNadir(String parentCaseDir) {
        String cmd =
            "LC_ALL=C find '" + parentCaseDir + "' -type f -name 'nadir_global_nd.txt' -print0 "
          + "| xargs -0 awk -F= '"
          + "/^nadir_energy=/{e=$2+0} "
          + "/^nadir_time=/{t=$2+0} "
          + "ENDFILE{ if(e!=\"\" && t!=\"\") printf(\"%.6f %.6f\\n\", e, t); e=\"\"; t=\"\" }' "
          + "| awk '{ if($1>E) E=$1; if($2>T) T=$2 } END { printf(\"[NADIR] Global ND nadir = [%.6f, %.6f]\\n\", E, T) }'";

        try {
            Process p = new ProcessBuilder("bash", "-lc", cmd).inheritIO().start();
            int code = p.waitFor();
            if (code != 0) {
                System.err.println("[WARN] Global NADIR command exited with code " + code);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Could not compute global NADIR: " + e.getMessage());
        }
    }

}
