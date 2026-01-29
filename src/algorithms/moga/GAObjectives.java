package algorithms.moga;

import java.util.Map;
import java.util.WeakHashMap;

public class GAObjectives implements Comparable<GAObjectives> {

	private final Map<EGAObjectives, Double> map = new WeakHashMap<EGAObjectives, Double>();

	/*
	 * public int compareTo(GAObjectives o) {
    boolean aBetter = false;
    boolean bBetter = false;

    for (EGAObjectives obj : EGAObjectives.values()) {
        double va = this.getObjective(obj);
        double vb = o.getObjective(obj);

        if (va + EPS < vb) aBetter = true;      // this mejor (min)
        else if (va > vb + EPS) bBetter = true; // other mejor
        // si están “casi” iguales, no marcamos nada
        if (aBetter && bBetter) return 0; // no-dominados
    }

    if (aBetter && !bBetter) return -1; // this domina a o
    if (bBetter && !aBetter) return  1; // o domina a this
    return 0; // iguales (o casi)
	}
	 */
	
	@Override
	public int compareTo(GAObjectives o) {
		int nDominate;

		nDominate = 0;
		// Compare the objectives of two individuals
		Double dValue1, dValue2;
		for (Map.Entry<EGAObjectives, Double> set1 : map.entrySet()) {

			// Printing all elements of a Map
			//System.out.println("C1: " + set1.getKey() + " = " + set1.getValue());
			dValue1 = set1.getValue();
			// Get objective
			dValue2 = o.getObjective(set1.getKey());
			//System.out.println("C2: " + set1.getKey() + " = " + dValue2);
			if (dValue1 < dValue2)
				nDominate = 1;

		}

		return nDominate;
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

			// Printing all elements of a Map
			//System.out.println("C1: " + set1.getKey() + " = " + set1.getValue());
			if (set1.getValue() < 0)
				return false;
		}
		return true;
	}
}
