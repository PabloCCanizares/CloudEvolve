package platform.surrogate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal, dependency-free reader and evaluator for a LightGBM model saved in
 * its native text format ({@code Booster.save_model}).
 *
 * <p>It supports the {@code regression} objective with <b>numerical</b> splits,
 * which is exactly what the cloud surrogate uses: prediction is the plain sum of
 * the leaf values reached in every tree (the learning rate and the
 * boost-from-average base are already baked into the leaf values, so no link
 * function or extra base term is needed).</p>
 *
 * <p><b>Categorical splits are deliberately rejected.</b> The shipped surrogate
 * models train with {@code num_cat=0} (only numerical splits); the two nominal
 * inputs ({@code sto.type}, {@code work.name}) are never split on. If a future
 * retraining introduces a categorical split, {@link #load} throws rather than
 * silently mispredicting — mirroring the project's "fail fast on the unknown"
 * stance. The category lists from the {@code pandas_categorical} trailer are
 * still parsed so categorical inputs can be encoded to their integer codes.</p>
 */
public final class LightGbmModel {

    /** LightGBM decision_type bit masks (see LightGBM tree.h). */
    private static final int CATEGORICAL_MASK = 1;
    private static final int DEFAULT_LEFT_MASK = 2;

    private final String[] featureNames;
    private final Tree[] trees;
    /** Categories per categorical feature, in the order they appear in pandas_categorical. */
    private final List<List<String>> pandasCategorical;

    private LightGbmModel(String[] featureNames, Tree[] trees, List<List<String>> pandasCategorical) {
        this.featureNames = featureNames;
        this.trees = trees;
        this.pandasCategorical = pandasCategorical;
    }

    public String[] featureNames() {
        return featureNames;
    }

    public List<List<String>> pandasCategorical() {
        return pandasCategorical;
    }

    /** Number of input features the model expects ({@code max_feature_idx + 1}). */
    public int numFeatures() {
        return featureNames.length;
    }

    /** Sum of the leaf values reached in every tree for the given feature vector. */
    public double predict(double[] x) {
        double sum = 0.0;
        for (Tree t : trees) {
            sum += t.evaluate(x);
        }
        return sum;
    }

    // ───────────────────────────── parsing ─────────────────────────────

    public static LightGbmModel load(String path) throws IOException {
        String[] featureNames = null;
        List<List<String>> pandasCategorical = new ArrayList<>();
        List<Tree> trees = new ArrayList<>();
        java.util.Map<String, String> current = null; // accumulates the current Tree=... block

        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("feature_names=")) {
                    featureNames = line.substring("feature_names=".length()).trim().split("\\s+");
                } else if (line.startsWith("pandas_categorical:")) {
                    pandasCategorical = parsePandasCategorical(
                            line.substring("pandas_categorical:".length()).trim());
                } else if (line.startsWith("Tree=")) {
                    if (current != null) {
                        trees.add(buildTree(current));
                    }
                    current = new java.util.HashMap<>();
                } else if (current != null) {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        current.put(line.substring(0, eq), line.substring(eq + 1));
                    } else if (line.startsWith("end of trees")) {
                        trees.add(buildTree(current));
                        current = null;
                    }
                }
            }
            if (current != null) { // file ended right after the last tree block
                trees.add(buildTree(current));
            }
        }

        if (featureNames == null) {
            throw new IOException("LightGBM model has no feature_names: " + path);
        }
        if (trees.isEmpty()) {
            throw new IOException("LightGBM model has no trees: " + path);
        }
        return new LightGbmModel(featureNames, trees.toArray(new Tree[0]), pandasCategorical);
    }

    private static Tree buildTree(java.util.Map<String, String> b) throws IOException {
        int numLeaves = Integer.parseInt(b.getOrDefault("num_leaves", "1").trim());
        int numCat = Integer.parseInt(b.getOrDefault("num_cat", "0").trim());
        if (numCat != 0) {
            throw new IOException("Categorical splits are not supported (num_cat=" + numCat
                    + "). Retrain the surrogate with numerical-only features or extend LightGbmModel.");
        }
        double[] leafValue = parseDoubles(b.get("leaf_value"));
        int[] leafCount = parseInts(b.get("leaf_count"));
        if (numLeaves <= 1 || !b.containsKey("split_feature")) {
            return Tree.singleLeaf(leafValue.length > 0 ? leafValue[0] : 0.0,
                    leafCount.length > 0 ? leafCount[0] : 0);
        }
        int[] splitFeature = parseInts(b.get("split_feature"));
        double[] threshold = parseDoubles(b.get("threshold"));
        int[] decisionType = parseInts(b.get("decision_type"));
        int[] leftChild = parseInts(b.get("left_child"));
        int[] rightChild = parseInts(b.get("right_child"));
        for (int dt : decisionType) {
            if ((dt & CATEGORICAL_MASK) != 0) {
                throw new IOException("Categorical split encountered (decision_type=" + dt
                        + "); not supported by LightGbmModel.");
            }
        }
        return new Tree(splitFeature, threshold, decisionType, leftChild, rightChild, leafValue, leafCount);
    }

    private static List<List<String>> parsePandasCategorical(String s) {
        // Format: [["__UNK__", "hdd"], ["__UNK__", "mix_vm"]]  (or "null")
        List<List<String>> out = new ArrayList<>();
        if (s.isEmpty() || s.equals("null")) {
            return out;
        }
        int i = s.indexOf('[');
        if (i < 0) {
            return out;
        }
        s = s.substring(i + 1, s.lastIndexOf(']')); // strip outer brackets
        int depth = 0, start = -1;
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == '[') {
                if (depth == 0) {
                    start = k + 1;
                }
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0 && start >= 0) {
                    out.add(parseStringList(s.substring(start, k)));
                    start = -1;
                }
            }
        }
        return out;
    }

    private static List<String> parseStringList(String s) {
        List<String> out = new ArrayList<>();
        for (String tok : s.split(",")) {
            tok = tok.trim();
            if (tok.length() >= 2 && tok.charAt(0) == '"' && tok.charAt(tok.length() - 1) == '"') {
                tok = tok.substring(1, tok.length() - 1);
            }
            if (!tok.isEmpty()) {
                out.add(tok);
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static int[] parseInts(String s) {
        if (s == null) {
            return new int[0];
        }
        String[] p = s.trim().split("\\s+");
        int[] out = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            out[i] = Integer.parseInt(p[i]);
        }
        return out;
    }

    private static double[] parseDoubles(String s) {
        if (s == null) {
            return new double[0];
        }
        String[] p = s.trim().split("\\s+");
        double[] out = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            out[i] = Double.parseDouble(p[i]);
        }
        return out;
    }

    // ───────────────────────────── tree ─────────────────────────────

    private static final class Tree {
        private final int[] splitFeature;
        private final double[] threshold;
        private final int[] decisionType;
        private final int[] leftChild;
        private final int[] rightChild;
        private final double[] leafValue;
        private final int[] leafCount;   // training samples per leaf (for the novelty signal)

        private Tree(int[] splitFeature, double[] threshold, int[] decisionType,
                int[] leftChild, int[] rightChild, double[] leafValue, int[] leafCount) {
            this.splitFeature = splitFeature;
            this.threshold = threshold;
            this.decisionType = decisionType;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.leafValue = leafValue;
            this.leafCount = leafCount;
        }

        static Tree singleLeaf(double value, int count) {
            return new Tree(new int[0], new double[0], new int[0], new int[0], new int[0],
                    new double[] { value }, new int[] { count });
        }

        double evaluate(double[] x) {
            return leafValue[leafIndex(x)];
        }

        /** Index of the leaf the feature vector reaches. */
        int leafIndex(double[] x) {
            if (splitFeature.length == 0) { // single-leaf tree
                return 0;
            }
            int node = 0;
            while (node >= 0) {
                double fval = x[splitFeature[node]];
                int dt = decisionType[node];
                int missingType = (dt >> 2) & 3; // 0 none, 1 zero, 2 nan
                boolean defaultLeft = (dt & DEFAULT_LEFT_MASK) != 0;

                if (Double.isNaN(fval) && missingType != 2) {
                    fval = 0.0;
                }
                boolean isMissing = (missingType == 1 && fval == 0.0)
                        || (missingType == 2 && Double.isNaN(fval));

                boolean goLeft = isMissing ? defaultLeft : (fval <= threshold[node]);
                node = goLeft ? leftChild[node] : rightChild[node];
            }
            return ~node; // negative child encodes a leaf as ~leafIndex
        }

        /** 1/(leafSupport+1): high when the config lands in a sparsely-trained leaf. */
        double novelty(double[] x) {
            int li = leafIndex(x);
            int c = (leafCount != null && li >= 0 && li < leafCount.length) ? leafCount[li] : 0;
            return 1.0 / (c + 1.0);
        }
    }

    /**
     * Mean leaf-support novelty across the trees: higher means the input lands in
     * leaves backed by few training samples (a sparse, low-confidence region).
     */
    public double leafNovelty(double[] x) {
        double sum = 0.0;
        for (Tree t : trees) {
            sum += t.novelty(x);
        }
        return sum / trees.length;
    }
}
