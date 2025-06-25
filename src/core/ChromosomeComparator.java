package core;

import java.util.List;

public interface ChromosomeComparator<C> {

	int sort(List<C> chromosomes);

	//T fit(C chromosome);
}
