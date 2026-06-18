package algorithms.moga;

import java.util.Map;
import java.util.WeakHashMap;

public class GAObjectives implements Comparable<GAObjectives> {

	/** Tolerance used to treat near-equal objective values as ties. */
	private static final double EPS = 1e-9;

	private final Map<EGAObjectives, Double> map = new WeakHashMap<EGAObjectives, Double>();

	/**
	 * Pareto comparison for a minimization problem (smaller objective = better).
	 *
	 * <p>Returns a value consistent with {@link Comparable}, where "less than"
	 * means "better / dominating":</p>
	 * <ul>
	 *   <li><b>-1</b> if {@code this} Pareto-dominates {@code o} (no worse on any
	 *       objective and strictly better on at least one),</li>
	 *   <li><b>+1</b> if {@code o} Pareto-dominates {@code this},</li>
	 *   <li><b>0</b> if the two are mutually non-dominated or equal within {@link #EPS}.</li>
	 * </ul>
	 */
	@Override
	public int compareTo(GAObjectives o) {
		boolean thisBetter = false;
		boolean otherBetter = false;

		for (EGAObjectives obj : EGAObjectives.values()) {
			double va = this.getObjective(obj);
			double vb = o.getObjective(obj);

			if (va + EPS < vb) {
				thisBetter = true;       // this is smaller -> better
			} else if (vb + EPS < va) {
				otherBetter = true;      // other is smaller -> better
			}
			// Better on both sides => neither dominates: stop early.
			if (thisBetter && otherBetter) {
				return 0;
			}
		}

		if (thisBetter && !otherBetter) {
			return -1; // this dominates o
		}
		if (otherBetter && !thisBetter) {
			return 1;  // o dominates this
		}
		return 0;      // equal (within EPS)
	}

	public void addObjective(EGAObjectives key, double value) {
		map.put(key, value);
	}

	public Double getObjective(EGAObjectives objIn) {
		Double dRet;
		if (map.containsKey(objIn))
			dRet = map.get(objIn);
		else
			dRet = -1.0;

		return dRet;
	}

	public boolean isValid() {
		for (Map.Entry<EGAObjectives, Double> set1 : map.entrySet()) {
			if (set1.getValue() < 0)
				return false;
		}
		return true;
	}
}
