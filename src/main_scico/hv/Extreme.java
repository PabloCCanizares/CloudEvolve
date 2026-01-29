package main_scico.hv;

import java.io.*;
import java.nio.file.*;
import java.util.*;

class Extreme {
    double maxE = 0.0;
    double maxT = 0.0;
    void observe(double e, double t) {
        if (e > 0 && t > 0) {
            if (e > maxE) maxE = e;
            if (t > maxT) maxT = t;
        }
    }
    boolean valid() { return maxE > 0 && maxT > 0; }
}

class RefTracker {
    private final Map<String, Extreme> perCase = new HashMap<>();
    private final Extreme global = new Extreme();
    private final Path storePath;

    RefTracker(Path storePath) {
        this.storePath = storePath;
        load();
    }

    void observe(String caseKey, double e, double t) {
        perCase.computeIfAbsent(caseKey, k -> new Extreme()).observe(e, t);
        global.observe(e, t);
    }

    Extreme getCaseExtreme(String caseKey) {
        return perCase.getOrDefault(caseKey, new Extreme());
    }

    Extreme getGlobalExtreme() { return global; }

    void save() {
        // Persistimos en formato Properties: 
        // claves "case:<name>:maxE" / "case:<name>:maxT" y "global:maxE" / "global:maxT"
        Properties p = new Properties();
        for (Map.Entry<String, Extreme> e : perCase.entrySet()) {
            p.setProperty("case:"+e.getKey()+":maxE", Double.toString(e.getValue().maxE));
            p.setProperty("case:"+e.getKey()+":maxT", Double.toString(e.getValue().maxT));
        }
        p.setProperty("global:maxE", Double.toString(global.maxE));
        p.setProperty("global:maxT", Double.toString(global.maxT));
        try (OutputStream os = Files.newOutputStream(storePath)) {
            p.store(os, "Hypervolume reference maxima (auto-accumulated)");
        } catch (IOException ex) {
            System.err.println("[WARN] Could not save ref maxima: " + ex.getMessage());
        }
    }

    void load() {
        if (!Files.exists(storePath)) return;
        Properties p = new Properties();
        try (InputStream is = Files.newInputStream(storePath)) {
            p.load(is);
        } catch (IOException ex) {
            System.err.println("[WARN] Could not load ref maxima: " + ex.getMessage());
            return;
        }
        // Cargamos global
        String gE = p.getProperty("global:maxE");
        String gT = p.getProperty("global:maxT");
        if (gE != null) global.maxE = Double.parseDouble(gE);
        if (gT != null) global.maxT = Double.parseDouble(gT);
        // Cargamos casos
        for (String k : p.stringPropertyNames()) {
            if (!k.startsWith("case:")) continue;
            // k = case:<name>:maxE or maxT
            String[] parts = k.split(":", 3);
            if (parts.length != 3) continue;
            String caseKey = parts[1];
            String field = parts[2];
            Extreme ex = perCase.computeIfAbsent(caseKey, kk -> new Extreme());
            double v = Double.parseDouble(p.getProperty(k));
            if ("maxE".equals(field)) ex.maxE = v;
            else if ("maxT".equals(field)) ex.maxT = v;
        }
    }
}
