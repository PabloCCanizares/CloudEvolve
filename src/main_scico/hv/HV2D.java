package main_scico.hv;

import java.util.*;

public final class HV2D {
    private HV2D(){}

    /** Exact 2D hypervolume for minimization, using non-dominated skyline. */
    public static double compute(List<double[]> points, double[] ref) {
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

    /** Non-dominated filter for minimization (returns points). */
    public static List<double[]> nonDominated(List<double[]> pts) {
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

    /** Non-dominated filter that returns indices of non-dominated points. */
    public static List<Integer> nonDominatedIndices(List<double[]> pts) {
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

    /** a dominates b iff a <= b in all and < in at least one (minimization). */
    private static boolean dominates(double[] a, double[] b) {
        boolean better = false;
        if (a[0] > b[0] || a[1] > b[1]) return false;
        if (a[0] < b[0]) better = true;
        if (a[1] < b[1]) better = true;
        return better;
    }
}
