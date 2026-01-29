package main_scico.hv.refpoint;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import main_scico.aux.FindBigFolders;


public class main_nadir {

    // Base por defecto si no se pasan args
    private static final String DEFAULT_ALGO_BASE = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/NSGAII/";

    public static void main(String[] args) {
        // 1) Reunir bases de entrada
        List<String> bases = new ArrayList<>();
        if (args != null && args.length > 0) {
            for (String a : args) if (a != null && !a.isBlank()) bases.add(a);
        } else {
            // fallback al ejemplo que diste
            bases.add("/localSpace/cloudEnergy/cloudsimStorage/evolutionary/PAES2/Bl_w3");
            bases.add("/localSpace/cloudEnergy/cloudsimStorage/evolutionary/SPEA2/Bl_w3");
            bases.add("/localSpace/cloudEnergy/cloudsimStorage/evolutionary/VEGA/Bl_w3");
            bases.add("/localSpace/cloudEnergy/cloudsimStorage/evolutionary/MOGA/Bl_w3");
            //bases.add("/localSpace/cloudEnergy/cloudsimStorage/evolutionary/NSGAII/Al_w3");
        }

        if (bases.isEmpty()) {
            System.err.println("[ERROR] No se recibieron bases de búsqueda.");
            System.exit(1);
        }

        // 2) Llamar a FindBigFolders.main(base) por cada base y acumular sin duplicados
        Set<Path> allRoots = new LinkedHashSet<>();
        for (String base : bases) {
            try {
        		String auxFolders[] = new String[2];
        		
        		auxFolders[0] = base;
        		auxFolders[1] = "100";
        		
                // según especificación del usuario: FindBigFolders.main(String) -> List<Path>
                List<Path> found = FindBigFolders.main(auxFolders);
                if (found != null && !found.isEmpty()) {
                    allRoots.addAll(found);
                    System.out.printf(Locale.US, "[INFO] Base: %s -> %d folders%n", base, found.size());
                } else {
                    System.out.printf(Locale.US, "[WARN] Base: %s -> 0 folders devueltos%n", base);
                }
            } catch (Exception ex) {
                System.err.printf("[WARN] Falló FindBigFolders.main(%s): %s%n", base, ex.getMessage());
            }
        }

        if (allRoots.isEmpty()) {
            System.err.println("[ERROR] Tras explorar las bases no hay folders a procesar.");
            System.exit(2);
        }

        // 3) Pasar todos los paths a CalculateRefPoint y calcular Q99
        List<Path> rootsList = new ArrayList<>(allRoots);
        NadirSong nadir = new NadirSong(rootsList).withBanners(true);

        double[] q99 = nadir.computeNadir(); // q99[0]=energía, q99[1]=tiempo
        System.out.printf(Locale.US, "[DONE] RefPoint (Q99): energy=%.6f, time=%.6f%n", q99[0], q99[1]);
    }
}
