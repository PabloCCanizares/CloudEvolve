package platform;

/**
 * Strategy that decides, per fitness evaluation, whether the hybrid backend
 * should ratify the surrogate's prediction with the real simulator. Implemented
 * entirely at the evaluation seam ({@link HybridPlatform#execute}) — it never
 * touches the GA engine.
 *
 * <p>The surrogate is evaluated first (it is cheap), so the decision can use both
 * the prediction and a novelty score. Implementations and composition live in
 * {@link RealEvaluationPolicies}.</p>
 */
public interface RealEvaluationPolicy {

    /**
     * @param prediction the surrogate output {@code {energy_kwh, sim_time_sec}}
     * @param ctx        per-evaluation context (generation, novelty score)
     * @return whether to run the real simulator to ratify this evaluation
     */
    boolean shouldRatify(double[] prediction, Context ctx);

    /** Per-evaluation context available to a policy. */
    final class Context {
        public final int generation;
        public final double novelty;

        public Context(int generation, double novelty) {
            this.generation = generation;
            this.novelty = novelty;
        }
    }
}
