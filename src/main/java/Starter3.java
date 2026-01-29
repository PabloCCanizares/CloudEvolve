package main.java;

import java.util.concurrent.TimeUnit;

public class Starter3 {
	public static void main(String[] args) {
		/*Cloud_GA cga = new Cloud_GA(args);
		Cloud_MOGA cMoga = new Cloud_MOGA(args);
		Cloud_VEGA cVega = new Cloud_VEGA(args);
		Cloud_SPEA2 cSpea = new Cloud_SPEA2(args);
		Cloud_NSGAII cNsgaII = new Cloud_NSGAII(args);
		Cloud_PAES cPaes = new Cloud_PAES(args);

		// 6 iteraciones por algoritmo, 3 cambiando el nivel de probabilidad y 2 las
		// regals aplicadas
		String[] aux = new String[6];
		int i = 0;
		aux[0] = args[i + 0];
		aux[1] = args[i + 1];
		aux[2] = args[i + 2];
		aux[3] = args[i + 3];
		aux[4] = args[i + 4];
		aux[5] = args[i + 5];
		i += 6;
		System.out.println("Executing NSGAII");
		cNsgaII.start(aux);

		System.out.println("Executing GA");
		cga.start(aux);
		
		aux[0] = args[i + 0];
		aux[1] = args[i + 1];
		aux[2] = args[i + 2];
		aux[3] = args[i + 3];
		aux[4] = args[i + 4];
		aux[5] = args[i + 5];
		System.out.println("Executing NSGAII");
		cNsgaII.start(aux);

		System.out.println("Executing GA");
		cga.start(aux);
		
		for (i = 12; i < args.length - 5; i += 6) {
			aux[0] = args[i + 0];
			aux[1] = args[i + 1];
			aux[2] = args[i + 2];
			aux[3] = args[i + 3];
			aux[4] = args[i + 4];
			aux[5] = args[i + 5];

			System.out.println("Executing NSGAII");
			cNsgaII.start(aux);

			System.out.println("Executing SPEA2");
			cSpea.start(aux);

			System.out.println("Executing VEGA");
			//cVega.start(aux);

			System.out.println("Executing MOGA");
			cMoga.start(aux);

			System.out.println("Executing PAES");
			cPaes.start(aux);

			System.out.println("Executing GA");
			cga.start(aux);
		}
		System.out.println("end");
*/
	}
}
