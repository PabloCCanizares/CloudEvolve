package main.java;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Starter2 {
	public static void main(String[] args) {
		/*Cloud_GA cga = new Cloud_GA(args);
		Cloud_MOGA cMoga = new Cloud_MOGA(args);
		Cloud_VEGA cVega = new Cloud_VEGA(args);
		Cloud_SPEA2 cSpea = new Cloud_SPEA2(args);
		Cloud_NSGAII cNsgaII = new Cloud_NSGAII(args);
		Cloud_PAES cPaes = new Cloud_PAES(args);

		// 6 iteraciones por algoritmo, 3 cambiando el nivel de probabilidad y 2 las
		// regals aplicadas
		if (args.length == 7) {
			String[] aux = new String[7];
			int i = 0;
			for (i = 0; i < args.length - 6; i += 7) {
				aux[0] = args[i + 0];
				aux[1] = args[i + 1];
				aux[2] = args[i + 2];
				aux[3] = args[i + 3];
				aux[4] = args[i + 4];
				aux[5] = args[i + 5];
				aux[6] = args[i + 6];
				if (aux[0].toLowerCase().equalsIgnoreCase("nsgaii")) {
					System.out.println("Executing NSGAII");
					cNsgaII.start(Arrays.copyOfRange(aux, 1, args.length));;
				}
				
				if (aux[0].toLowerCase().equalsIgnoreCase("spea2")) {
					System.out.println("Executing SPEA2");
					cSpea.start(Arrays.copyOfRange(aux, 1, args.length));
				}
				
				if (aux[0].toLowerCase().equalsIgnoreCase("vega")) {
					System.out.println("Executing VEGA");
					//cVega.start(Arrays.copyOfRange(aux, 1, args.length));
				}
				
				if (aux[0].toLowerCase().equalsIgnoreCase("moga")) {
					System.out.println("Executing MOGA");
					cMoga.start(Arrays.copyOfRange(aux, 1, args.length));
				}
				
				if (aux[0].toLowerCase().equalsIgnoreCase("paes")) {
					System.out.println("Executing PAES");
					cPaes.start(Arrays.copyOfRange(aux, 1, args.length));
				}
				
				if (aux[0].toLowerCase().equalsIgnoreCase("ga")) {
					System.out.println("Executing GA");
					cga.start(Arrays.copyOfRange(aux, 1, args.length));
				}
			}
			System.out.println("end");

		}

		else {
			String[] aux = new String[6];
			int i = 0;
			for (i = 0; i < args.length - 5; i += 6) {
				aux[0] = args[i + 0];
				aux[1] = args[i + 1];
				aux[2] = args[i + 2];
				aux[3] = args[i + 3];
				aux[4] = args[i + 4];
				aux[5] = args[i + 5];
				if (aux[0].equalsIgnoreCase("nsgaii")) {
					System.out.println("Executing NSGAII");
					cNsgaII.start(aux);
				}

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
		}*/
	}
}
