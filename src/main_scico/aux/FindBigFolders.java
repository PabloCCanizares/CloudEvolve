package main_scico.aux;


import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Devuelve la lista de carpetas que tiene más de X elementos. (Esto nos sirve para determinar donde están los experimentos)
 * @author j0hn
 *
 */
public class FindBigFolders {

    public static List<Path> main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java FindBigFolders <path_raiz> [min_subcarpetas]");
            System.exit(1);
        }

        Path root = Paths.get(args[0]);
        int minSubdirs = (args.length >= 2) ? Integer.parseInt(args[1]) : 100;

        if (!Files.isDirectory(root)) {
            System.err.println("El path no es un directorio: " + root);
            System.exit(2);
        }

        List<Path> result = new ArrayList<>();
        try {
            Files.walkFileTree(root,
                    java.util.EnumSet.of(FileVisitOption.FOLLOW_LINKS), // quita FOLLOW_LINKS si no quieres seguir symlinks
                    Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            // Cuenta subdirectorios inmediatos
                            int count = 0;
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                                for (Path p : stream) {
                                    if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                                        count++;
                                        if (count >= minSubdirs) {
                                            result.add(dir);
                                            break; // ya cumple
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                // Sin permisos u otro error: lo ignoramos y seguimos
                                System.err.println("Aviso: no se pudo leer " + dir + " -> " + e.getMessage());
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            // Evita abortar todo el recorrido por un fallo puntual
                            System.err.println("Aviso: fallo al acceder a " + file + " -> " + exc.getMessage());
                            return FileVisitResult.CONTINUE;
                        }
                    });

            // Mostrar resultados
            System.out.println("Carpetas con al menos " + minSubdirs + " subcarpetas inmediatas:");
            for (Path p : result) {
                System.out.println(p.toAbsolutePath());
            }
            System.out.println("Total: " + result.size());

           
        } catch (IOException e) {
            System.err.println("Error durante el recorrido: " + e.getMessage());
            System.exit(3);
        }
        return result;
    }
}
