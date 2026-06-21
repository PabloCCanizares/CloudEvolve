package platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import platform.surrogate.SurrogateModel;

/**
 * Factory and implementations of {@link RealEvaluationPolicy}. The hybrid backend
 * builds one from configuration (see {@link #fromConfig}); the pieces are
 * composable so you can mix triggers without if-spaghetti.
 *
 * <p>Available triggers: {@code everyN} (calendar), {@code probability},
 * {@code implausible} (prediction out of the training target range) and
 * {@code novelty} (out-of-distribution, validated as the best signal — see
 * {@code repro/novelty_validation}). Combine with {@link #anyOf} and bound the
 * real-simulator budget with {@link #capped}.</p>
 */
public final class RealEvaluationPolicies {

    public static final String POLICY_PROPERTY = "cloudevolve.hybrid.policy";
    public static final String EVERY_PROPERTY = "cloudevolve.hybrid.realEvery";
    public static final String PROBABILITY_PROPERTY = "cloudevolve.hybrid.probability";
    public static final String NOVELTY_PROPERTY = "cloudevolve.hybrid.noveltyThreshold";
    public static final String MAX_PER_GEN_PROPERTY = "cloudevolve.hybrid.maxRealPerGen";

    private RealEvaluationPolicies() {
    }

    /** Ratify on generations that are a (non-zero) multiple of {@code n}. */
    public static RealEvaluationPolicy everyN(int n) {
        return (pred, ctx) -> n > 0 && ctx.generation % n == 0;
    }

    /** Ratify a random fraction {@code p} of evaluations. */
    public static RealEvaluationPolicy probability(double p) {
        return (pred, ctx) -> ThreadLocalRandom.current().nextDouble() < p;
    }

    /** Ratify when the prediction falls outside the plausible (training) range. */
    public static RealEvaluationPolicy implausible(double[] energyBounds, double[] timeBounds) {
        return (pred, ctx) -> outside(pred[0], energyBounds) || outside(pred[1], timeBounds);
    }

    /** Ratify when the novelty (out-of-distribution) score exceeds {@code threshold}. */
    public static RealEvaluationPolicy novelty(double threshold) {
        return (pred, ctx) -> ctx.novelty > threshold;
    }

    /** Ratify if any of the given policies fires. */
    public static RealEvaluationPolicy anyOf(List<RealEvaluationPolicy> policies) {
        return (pred, ctx) -> {
            for (RealEvaluationPolicy p : policies) {
                if (p.shouldRatify(pred, ctx)) {
                    return true;
                }
            }
            return false;
        };
    }

    /** Bound the real-simulator budget to at most {@code maxPerGen} ratifications per generation. */
    public static RealEvaluationPolicy capped(RealEvaluationPolicy inner, int maxPerGen) {
        return new RealEvaluationPolicy() {
            private int generation = -1;
            private int used = 0;

            @Override
            public synchronized boolean shouldRatify(double[] pred, Context ctx) {
                if (ctx.generation != generation) {
                    generation = ctx.generation;
                    used = 0;
                }
                if (used >= maxPerGen) {
                    return false;
                }
                if (inner.shouldRatify(pred, ctx)) {
                    used++;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Builds the policy from system properties: {@code cloudevolve.hybrid.policy}
     * is a comma-separated subset of {everyN, probability, implausible, novelty}
     * (default {@code everyN}), combined with {@link #anyOf} and optionally
     * wrapped by {@link #capped} ({@code cloudevolve.hybrid.maxRealPerGen}).
     */
    public static RealEvaluationPolicy fromConfig(SurrogateModel model) {
        String spec = System.getProperty(POLICY_PROPERTY, "everyN").toLowerCase();
        List<RealEvaluationPolicy> parts = new ArrayList<>();
        for (String token : spec.split(",")) {
            switch (token.trim()) {
                case "everyn":
                    parts.add(everyN(intProp(EVERY_PROPERTY, 5)));
                    break;
                case "probability":
                    parts.add(probability(doubleProp(PROBABILITY_PROPERTY, 0.1)));
                    break;
                case "implausible":
                    parts.add(implausible(model.energyBounds(), model.timeBounds()));
                    break;
                case "novelty": {
                    double thr = doubleProp(NOVELTY_PROPERTY, model.noveltyThreshold());
                    if (Double.isNaN(thr)) {
                        System.out.println("HybridPlatform: novelty policy requested but no threshold "
                                + "(set -D" + NOVELTY_PROPERTY + " or add novelty_threshold to the spec); skipping.");
                    } else {
                        parts.add(novelty(thr));
                    }
                    break;
                }
                case "":
                    break;
                default:
                    System.out.println("HybridPlatform: unknown policy '" + token + "', ignored.");
            }
        }
        if (parts.isEmpty()) {
            parts.add(everyN(intProp(EVERY_PROPERTY, 5)));
        }
        RealEvaluationPolicy combined = parts.size() == 1 ? parts.get(0) : anyOf(parts);
        int cap = intProp(MAX_PER_GEN_PROPERTY, 0);
        return cap > 0 ? capped(combined, cap) : combined;
    }

    private static boolean outside(double v, double[] bounds) {
        return bounds != null && (v < bounds[0] || v > bounds[1]);
    }

    private static int intProp(String key, int dflt) {
        try {
            String v = System.getProperty(key);
            return (v == null || v.isEmpty()) ? dflt : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    private static double doubleProp(String key, double dflt) {
        try {
            String v = System.getProperty(key);
            return (v == null || v.isEmpty()) ? dflt : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return dflt;
        }
    }
}
