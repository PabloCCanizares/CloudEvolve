package algorithms.moga;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Chromosome;

public class AdaptiveCellGrid<C extends Chromosome<C>> {
	private List<List<C>> data;
	private Map<Integer, Integer> cellCounts;
	private double[] minimum;
	private double[] maximum;
	private int capacity;
	private int numberOfDivisions;
	private int numberOfObjectives;
	private int actualSize;

	public AdaptiveCellGrid(int capacity, int numberOfDivisions, int numberOfObjectives) throws Exception {
		this.capacity = capacity;
		this.numberOfDivisions = numberOfDivisions;
		this.numberOfObjectives = numberOfObjectives;
		this.actualSize = 0;
		minimum = new double[numberOfObjectives];
		maximum = new double[numberOfObjectives];
		Arrays.fill(minimum, Double.POSITIVE_INFINITY);
		Arrays.fill(maximum, Double.NEGATIVE_INFINITY);

		// guard against integer overflow
		int tam = (int) Math.pow(numberOfDivisions, numberOfObjectives);
		if (tam < 0) {
			throw new Exception("number of divisions (bisections) too large for adaptive grid archive");
		}

		data = new ArrayList<>(numberOfDivisions * numberOfDivisions);
		for (int i = 0; i < numberOfDivisions * numberOfDivisions; i++) {
			data.add(new ArrayList<C>());
		}

		cellCounts = new HashMap<>();
	}

	// Resto de métodos de la clase...

	public List<C> get(C solution) {
		double[] indices = getIndicesFromSolution(solution);
		checkIndexOnRange(indices);
		int linearIndex = calculateLinearIndex(indices);
		return data.get(linearIndex);
	}

	public boolean add(C solution) {
		double[] indices = getIndicesFromSolution(solution);

		for (Map.Entry<Integer, Integer> entry : cellCounts.entrySet()) {
			int ind = entry.getKey();
			for (int i = 0; i < data.get(ind).size(); i++) {
				C oldSolution = data.get(ind).get(i);
				int flag = compare(solution, oldSolution);
				if (flag > 0) {
					// candidate dominates a member of the archive
					data.get(ind).remove(oldSolution);
					int aux = entry.getValue();
					aux--;
					entry.setValue(aux);
					actualSize--;
					if (entry.getValue() <= 0) {
						cellCounts.remove(entry);
					}
				} else if (flag < 0) {
					// candidate is dominated by a member of the archive
					return false;
				}
			}
		}
		if (!checkIndexOnRange(indices)) {
			adaptBounds(indices);
		}

		int linearIndex = calculateLinearIndex(indices);

		if (actualSize == 0) {
			data.get(linearIndex).add(solution);
			cellCounts.put(linearIndex, 1);
			actualSize++;
			return true;
		}
		List<C> cellData = data.get(linearIndex);

		if (actualSize >= capacity) {
			int mostDenseCellIndex = findMostDenseCell();
			if (mostDenseCellIndex == linearIndex) {
				// El elemento está en la celda más densa, no se inserta
				return false;
			} else {
				data.get(mostDenseCellIndex).remove(0);
				int value = cellCounts.get(mostDenseCellIndex);
				cellCounts.put(mostDenseCellIndex, data.get(mostDenseCellIndex).size());
				cellData.add(solution);
				cellCounts.put(linearIndex, cellData.size());
				actualSize++;
				return true;
			}
		}
		cellData.add(solution);
		cellCounts.put(linearIndex, cellData.size());
		actualSize++;

		return true;
	}

	private int compare(C solution, C oldSolution) {
		int nDominate;

		nDominate = 0;
		// Compare the objectives of two individuals
		Double dValue1, dValue2;
		for (EGAObjectives objective : EGAObjectives.values()) {

			dValue1 = solution.getObjective(objective);
			// Get objective
			dValue2 = oldSolution.getObjective(objective);
			if (dValue1 > dValue2)
				if (nDominate > 0)
					return 0;
				else
					nDominate--;
			else if (dValue1 < dValue2)
				if (nDominate < 0)
					return 0;
				else
					nDominate++;
			else {
				if (nDominate <= 0)
					nDominate--;
			}

		}
		return nDominate;
	}

	private int findMostDenseCell() {
		int mostDenseCellIndex = -1;
		int maxDensity = Integer.MIN_VALUE;
		for (Map.Entry<Integer, Integer> entry : cellCounts.entrySet()) {
			if (entry.getValue() > maxDensity) {
				mostDenseCellIndex = entry.getKey();
				maxDensity = entry.getValue();
			}
		}
		return mostDenseCellIndex;
	}

	public void remove(C value) {
		double[] indices = getIndicesFromSolution(value);
		checkIndexOnRange(indices);
		int linearIndex = calculateLinearIndex(indices);
		List<C> cellData = data.get(linearIndex);
		if (cellData.remove(value)) {
			cellCounts.put(linearIndex, cellData.size());
		}
	}

	public int getDensity(C solution) {
		double[] indices = getIndicesFromSolution(solution);
		checkIndexOnRange(indices);
		int linearIndex = calculateLinearIndex(indices);
		return cellCounts.getOrDefault(linearIndex, 0);
	}

	private boolean checkIndexOnRange(double... indices) {
		if (indices.length != minimum.length) {
			throw new IllegalArgumentException("Número incorrecto de índices");
		}
		for (int i = 0; i < indices.length; i++) {
			if (indices[i] < minimum[i] || indices[i] > maximum[i]) {
				return false;
			}
		}
		return true;
	}

	private void adaptBounds(double... indices) {
		boolean boundsChanged = false;
		for (int i = 0; i < indices.length; i++) {
			if (indices[i] < minimum[i]) {
				minimum[i] = indices[i];
				boundsChanged = true;
			}
			if (indices[i] > maximum[i]) {
				maximum[i] = indices[i];
				boundsChanged = true;
			}
		}
		if (boundsChanged) {
			if (actualSize > 0)
				transferData();
		}
	}

	private void transferData() {
		List<List<C>> newData = new ArrayList<>(numberOfDivisions * numberOfDivisions);
		for (int i = 0; i < numberOfDivisions * numberOfDivisions; i++) {
			newData.add(new ArrayList<C>());
		}
		Map<Integer, Integer> newCellCounts = new HashMap<>();

		double[] indices = new double[minimum.length];
		double[] newIndices = new double[minimum.length];
		for (int i = 0; i < cellCounts.size(); i++) {
			calculateIndices(i, indices, minimum);
			calculateIndices(i, newIndices, minimum);

			int linearIndex = calculateLinearIndex(newIndices);
			if (linearIndex > newData.size())
				System.out.println("problemas");
			newData.add(linearIndex, data.get(i));
			newCellCounts.put(linearIndex, cellCounts.getOrDefault(i, 0));
		}

		data = newData;
		cellCounts = newCellCounts;
	}

	private void calculateIndices(int linearIndex, double[] indices, double[] bounds) {
		double stride = 1;
		for (int i = indices.length - 1; i >= 0; i--) {
			indices[i] = (linearIndex / stride) % numberOfDivisions + bounds[i];
			stride *= numberOfDivisions;
		}
	}

	private int calculateLinearIndex(double... indices) {
		int linearIndex = 0;
		int product = 1;

		for (int i = numberOfObjectives - 1; i >= 0; i--) {
			int size = (int) (maximum[i] - minimum[i] + 1);
			int normalized = (int) (indices[i] - minimum[i]);
			linearIndex += normalized * product;
			product *= size;

		}
		return linearIndex;
	}

	private double[] getIndicesFromSolution(C solution) {
		// Aquí debes implementar la lógica para obtener los índices del cromosoma
		// `solution`
		// y devolverlos como un arreglo de tipo `double`
		// ...
		double[] ret = new double[numberOfObjectives];
		int i = 0;
		for (EGAObjectives obj : EGAObjectives.values()) {
			ret[i] = solution.getObjective(obj);
			i++;
		}
		return ret;
	}

	public int getSize() {
		return actualSize;
	}

	public boolean isInLessCrowdedRegion(C chrom, C mutated) {
		// Aquí debes implementar la lógica para determinar si el cromosoma `mutated`
		// está en una región menos poblada que el cromosoma `chrom`
		// y devolver `true` o `false` según corresponda
		// ...
		double aux1 = 0;
		double aux2 = 0;
		if (cellCounts.containsKey(chrom)) {
			aux1 = cellCounts.get(calculateLinearIndex(getIndicesFromSolution(chrom)));
		}
		if (cellCounts.containsKey(mutated)) {
			aux2 = cellCounts.get(calculateLinearIndex(getIndicesFromSolution(mutated)));
		}
		return aux1 < aux2;
	}

	public PopulationMO<C> getPopulation() {
		PopulationMO<C> pop = new PopulationMO<C>();
		for (Map.Entry<Integer, Integer> entry : cellCounts.entrySet()) {
			for (C sol : data.get(entry.getKey())) {
				pop.addChromosome(sol);
			}
		}
		return pop;
	}

}
