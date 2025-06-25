package algorithms.moga;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import core.Chromosome;

public class CartesianDistanceComparator<C extends Chromosome> implements Comparator<C> {
	private Map<EGAObjectives, Double> minReferencePoint;
	private Map<EGAObjectives, Double> maxReferencePoint;

	public CartesianDistanceComparator() {
		minReferencePoint = new EnumMap<>(EGAObjectives.class);
		maxReferencePoint = new EnumMap<>(EGAObjectives.class);
	}

	public void addValueMin(EGAObjectives obj, double value) {
		minReferencePoint.put(obj, value);
	}

	public void addValueMax(EGAObjectives obj, double value) {
		maxReferencePoint.put(obj, value);
	}

	@Override
	public int compare(C o1, C o2) {
		double[] normalized1 = calculatePoint(o1);
		double[] normalized2 = calculatePoint(o2);
		double distance1 = calculateDistance(normalized1);
		double distance2 = calculateDistance(normalized2);

		// TODO Auto-generated method stub
		int res = Double.compare(distance1, distance2);
		return res;
	}

	private double[] calculatePoint(C o) {
		double[] res = new double[2];
		int i = 0;
		for (EGAObjectives obj : minReferencePoint.keySet()) {
			res[i] = (o.getObjective(obj) - minReferencePoint.get(obj))
					/ (maxReferencePoint.get(obj) - minReferencePoint.get(obj));
			i++;
		}
		return res;
	}

	private double calculateDistance(double[] chrom) {
		double res = 0;
//reference point (0,0)
		for (int i = 0; i < chrom.length; i++) {
			res += chrom[i] * chrom[i];
		}
		return Math.sqrt(res);
	
	}

}
