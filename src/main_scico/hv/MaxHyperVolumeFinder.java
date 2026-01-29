package main_scico.hv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import main_scico.aux.FindBigFolders;

/**
 * Selecciona la carpeta cuyo hipervolumen final sea mayor.
 * Para cada carpeta del listado:
 *   - Intenta leer hv_virtual_evolution.dat; si no existe, hv_evolution.dat.
 *   - Toma la última línea NO vacía y extrae el SEGUNDO número (double).
 *   - Compara y devuelve la carpeta con mayor HV.
 *
 * Uso por CLI:
 *   javac HvBestFolder.java
 *   java HvBestFolder /ruta/exp1 /ruta/exp2 /ruta/exp3
 */
public class MaxHyperVolumeFinder {

    /** Devuelve la carpeta con mayor hipervolumen; Optional.empty() si ninguna es válida. */
    public static Optional<Path> selectBestFolder(List<Path> folders) {
        if (folders == null || folders.isEmpty()) {
            return Optional.empty();
        }
        Path bestFolder = null;
        Double bestHv = null;

        for (Path folder : folders) {
            if (folder == null || !Files.isDirectory(folder)) {
                warn("No es un directorio: " + folder);
                continue;
            }
            try {
                Double hv = readFinalHV(folder);
                if (hv == null || hv.isNaN() || hv.isInfinite()) {
                    warn("Sin HV válido en: " + folder);
                    continue;
                }
                if (bestHv == null || hv > bestHv) {
                    bestHv = hv;
                    bestFolder = folder;
                }
            } catch (IOException e) {
                warn("Error leyendo HV en " + folder + ": " + e.getMessage());
            }
        }
        return Optional.ofNullable(bestFolder);
    }

    /** Sobrecarga: acepta rutas como String. */
    public static Optional<Path> selectBestFolderFromStrings(List<String> folderPaths) {
        if (folderPaths == null) return Optional.empty();
        List<Path> paths = new ArrayList<>(folderPaths.size());
        for (String s : folderPaths) {
            paths.add(Paths.get(s));
        }
        return selectBestFolder(paths);
    }

    /**
     * Lee el HV final desde hv_virtual_evolution.dat (preferente) o hv_evolution.dat (alternativo)
     * dentro de 'folder'. Toma el SEGUNDO número de la última línea no vacía.
     * Devuelve null si no existe fichero válido o no se puede parsear.
     */
    public static Double readFinalHV(Path folder) throws IOException {
        Path hvVirtual = folder.resolve("hv_virtual_evolution.dat");
        Path hvPlain   = folder.resolve("hv_evolution.dat");
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

        // Segundo token = HV final
        return parseDoubleLenient(toks[1]);
    }

    /** Lee la última línea no vacía de un fichero de texto (UTF-8). */
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

    /** Intenta parsear double aceptando coma o punto decimal. */
    private static Double parseDoubleLenient(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
            try {
                return Double.parseDouble(s.replace(',', '.'));
            } catch (NumberFormatException nfe2) {
                return null;
            }
        }
    }

    private static void warn(String msg) {
        System.err.println("AVISO: " + msg);
    }

    /* =========================== CLI =========================== */
    /**
     * Con esto seleccionamos los indiviuos con más hipervolumen de un algoritmo
     * Lo utilizaremos para el grid.
     * @param args
     */
    public static void main(String[] args) {
    	List<Path> bestElements;
    	Path pathTemp;
		String strAlgorithmBase;
    	if (args.length == 0) {
            strAlgorithmBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/PAES2/";
        }
        else
        	strAlgorithmBase = args[0];
    	
    	System.out.println("Exploring folder: "+strAlgorithmBase);


    	bestElements = new ArrayList<Path>();
		try {
			strAlgorithmBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/PAES2/";
			//Tomamos la base, y de esta base extraemos las carpetas más densas (100 iteraciones debajo)
			pathTemp = selBestFolder(strAlgorithmBase, "Al_w1");
			bestElements.add(pathTemp);
			pathTemp = selBestFolder(strAlgorithmBase, "Al_w3");
			bestElements.add(pathTemp);
			pathTemp = selBestFolder(strAlgorithmBase, "Bl_w1");
			bestElements.add(pathTemp);
			pathTemp = selBestFolder(strAlgorithmBase, "Bl_w3");
			bestElements.add(pathTemp);
			
			for(Path path: bestElements)
				System.out.println("* "+path.toAbsolutePath());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		


    }

	private static Path selBestFolder(String strAlgorithmBase, String strWorkload) {
		
		String auxFolders[] = new String[2];
		Path pathRet;
		
		auxFolders[0] = strAlgorithmBase+strWorkload;
		auxFolders[1] = "100";
		
		pathRet = null;
		List<Path> folders = FindBigFolders.main(auxFolders);
        Optional<Path> best = selectBestFolder(folders);
        if (best.isPresent()) {
        	pathRet = best.get();
            System.out.println("Best HV in "+strWorkload+" >"+best.get().toAbsolutePath().normalize().toString());
        } else {
            System.err.println("No se encontró ninguna carpeta con HV válido.");
            System.exit(2);
        }
		
        return pathRet;
	}
}

