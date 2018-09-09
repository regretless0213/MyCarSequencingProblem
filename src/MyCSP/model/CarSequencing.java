/**
 * Copyright (c) 2016, chocoteam
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of samples nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package MyCSP.model;

import org.chocosolver.samples.AbstractProblem;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.kohsuke.args4j.Option;

import java.util.Scanner;

import static org.chocosolver.solver.search.strategy.Search.MyHeuristicSearch;
import static org.chocosolver.solver.search.strategy.Search.inputOrderLBSearch;

/**
 * CSPLib prob001:<br/>
 * "A number of cars are to be produced; they are not identical, because
 * different options are available as variants on the basic model. <br/>
 * The assembly line has different stations which install the various options
 * (air-conditioning, sun-roof, etc.). These stations have been designed to
 * handle at most a certain percentage of the cars passing along the assembly
 * line. Furthermore, the cars requiring a certain option must not be bunched
 * together, otherwise the station will not be able to cope. Consequently, the
 * cars must be arranged in a sequence so that the K of each station is never
 * exceeded. <br/>
 * For instance, if a particular station can only cope with at most half of the
 * cars passing along the line, the sequence must be built so that at most 1 car
 * in any 2 requires that option. <br/>
 * The problem has been shown to be NP-complete (Gent 1999)" <br/>
 *
 * @author Charles Prud'homme
 * @since 03/08/11
 */
public class CarSequencing extends AbstractProblem {

	@Option(name = "-d", aliases = "--data", usage = "Car sequencing data.", required = false)
//	Data data = Data.P4_72;
//	CSPLib data = CSPLib.ReginPuget2;
	MyData data = MyData.md05;

	IntVar[] cars;

	int nCars, nClasses, nOptions;

	int[] demands;
	int[][] optfreq, matrix, options, idleConfs;

	@Override
	public void buildModel() {
		model = new Model("CarSequencing");
		parse(data.source());
		// System.out.println(data.source());
		prepare();
		int max = nClasses - 1;
		cars = model.intVarArray("cars", nCars, 0, max, false);
		IntVar[] expArray = new IntVar[nClasses];

		for (int optNum = 0; optNum < options.length; optNum++) {
			int nbConf = options[optNum].length;
			for (int seqStart = 0; seqStart < (cars.length - optfreq[optNum][1]); seqStart++) {
				IntVar[] carSequence = extractor(cars, seqStart, optfreq[optNum][1]);

				// configurations that include given option may be chosen
				IntVar[] atMost = new IntVar[nbConf];
				for (int i = 0; i < nbConf; i++) {
					// optfreq[optNum][0] times AT MOST
					atMost[i] = model.intVar("atmost_" + optNum + "_" + seqStart + "_" + nbConf, 0, optfreq[optNum][0],
							true);
				}
				// System.out.println(atMost[0].getName()+":
				// "+atMost[0].getLB()+"--"+atMost[0].getUB());
				model.globalCardinality(carSequence, options[optNum], atMost, false).post();

				IntVar[] atLeast = model.intVarArray("atleast_" + optNum + "_" + seqStart, idleConfs[optNum].length, 0,
						max, true);
				model.globalCardinality(carSequence, idleConfs[optNum], atLeast, false).post();

				// all others configurations may be chosen
				IntVar sum = model.intVar("sum", optfreq[optNum][1] - optfreq[optNum][0], 99999999, true);
				model.sum(atLeast, "=", sum).post();
				// System.out.println(sum);
			}
		}

		int[] values = new int[expArray.length];
		for (int i = 0; i < expArray.length; i++) {
			expArray[i] = model.intVar("var_" + i, 0, demands[i], false);
			values[i] = i;
			// System.out.println(expArray[i]+" "+values[i]);
		}

		model.globalCardinality(cars, values, expArray, false).post();
	}

	private static IntVar[] extractor(IntVar[] cars, int initialNumber, int amount) {
		if ((initialNumber + amount) > cars.length) {
			amount = cars.length - initialNumber;
		}
		IntVar[] tmp = new IntVar[amount];
		System.arraycopy(cars, initialNumber, tmp, 0, initialNumber + amount - initialNumber);
		return tmp;
	}

	@Override
	public void configureSearch() {

		model.getSolver().setSearch(MyHeuristicSearch(cars, matrix, optfreq, demands));
		model.getSolver().limitTime("20m");
	}

	@Override
	public void solve() {
		model.getSolver().solve();
        model.getSolver().printStatistics();
//        for (int i = 0; i < nCars;i++) {
//            System.out.println(model.getVars()[i]);
//        }
		/*
		 * for (int i = 0; i < matrix.length; i++) { for (int j = 0; j <
		 * matrix[i].length; j++) { System.out.print(matrix[i][j]+ "  "); }
		 * System.out.println(); }
		 */
	}

	public static void main(String[] args) {
		new CarSequencing().execute(args);
	}

	private int[][] parse(String source) {
		int[][] data = null;
		Scanner sc = new Scanner(source);
		nCars = sc.nextInt();
		nOptions = sc.nextInt();
		nClasses = sc.nextInt();

		optfreq = new int[nOptions][2];
		// get frequencies
		for (int i = 0; i < nOptions; i++) {
			optfreq[i][0] = sc.nextInt();
		}
		for (int i = 0; i < nOptions; i++) {
			optfreq[i][1] = sc.nextInt();
		}

		// get the demand and options
		demands = new int[nClasses];
		matrix = new int[nClasses][nOptions];
		for (int i = 0; i < nClasses; i++) {
			sc.nextInt();
			demands[i] = sc.nextInt();
			for (int j = 0; j < nOptions; j++) {
				matrix[i][j] = sc.nextInt();
			}
		}
		sc.close();
		return data;
	}

	private void prepare() {
		options = new int[nOptions][];
		idleConfs = new int[nOptions][];
		for (int i = 0; i < matrix[0].length; i++) {
			int nbNulls = 0;
			int nbOnes = 0;
			for (int j = 0; j < matrix.length; j++) {
				if (matrix[j][i] == 1)
					nbOnes++;
				else
					nbNulls++;
			}
			options[i] = new int[nbOnes];
			idleConfs[i] = new int[nbNulls];
			int countOnes = 0;
			int countNulls = 0;
			for (int j = 0; j < matrix.length; j++) {
				if (matrix[j][i] == 1) {
					options[i][countOnes] = j;
					countOnes++;
				} else {
					idleConfs[i][countNulls] = j;
					countNulls++;
				}
			}
		}
		/*
		 * for (int j = 0; j < options.length; j++) { for (int i = 0;i <
		 * options[j].length; i++) { System.out.print(options[j][i] +"  "); }
		 * System.out.println(); } System.out.println("================="); for (int j =
		 * 0; j < idleConfs.length; j++) { for (int i = 0;i < idleConfs[j].length; i++)
		 * { System.out.print(idleConfs[j][i] +"  "); } System.out.println(); }
		 */
//		for (int j = 0; j < optfreq.length; j++) {
//			for (int i = 0; i < optfreq[j].length; i++) {
//				System.out.print(optfreq[j][i] + "  ");
//			}
//			System.out.println();
//		}
	}

	/////////////////////////////////// DATA
	/////////////////////////////////// //////////////////////////////////////////////////

	enum Data {
		myPb("10 5 6\n" + "1 2 1 2 1\n" + "2 3 3 5 5\n" + "0 1 1 0 1 1 0\n" + "1 1 0 0 0 1 0\n" + "2 2 0 1 0 0 1\n"
				+ "3 2 0 1 0 1 0\n" + "4 2 1 0 1 0 0\n" + "5 2 1 1 0 0 0"), P4_72(
						"80 5 22\n" + "1 2 1 2 1\n" + "2 3 3 5 5\n" + "0 6 1 0 0 1 0\n" + "1 10 1 1 1 0 0\n"
								+ "2 2 1 1 0 0 1\n" + "3 2 0 1 1 0 0\n" + "4 8 0 0 0 1 0\n" + "5 15 0 1 0 0 0\n"
								+ "6 1 0 1 1 1 0\n" + "7 5 0 0 1 1 0\n" + "8 2 1 0 1 1 0\n" + "9 3 0 0 1 0 0\n"
								+ "10 2 1 0 1 0 0\n" + "11 1 1 1 1 0 1\n" + "12 8 0 1 0 1 0\n" + "13 3 1 0 0 1 1\n"
								+ "14 10 1 0 0 0 0\n" + "15 4 0 1 0 0 1\n" + "16 4 0 0 0 0 1\n" + "17 2 1 0 0 0 1\n"
								+ "18 4 1 1 0 0 0\n" + "19 6 1 1 0 1 0\n" + "20 1 1 0 1 0 1\n"
								+ "21 1 1 1 1 1 1"), P6_76(
										"100 5 22\n" + "1 2 1 2 1\n" + "2 3 3 5 5\n" + "0 13 1 0 0 0 0\n"
												+ "1 8 0 0 0 1 0\n" + "2 7 0 1 0 0 0\n" + "3 1 1 0 0 1 0\n"
												+ "4 12 0 0 1 0 0\n" + "5 5 0 1 0 1 0\n" + "6 5 0 0 1 1 0\n"
												+ "7 6 0 1 1 0 0\n" + "8 3 1 0 0 0 1\n" + "9 12 1 1 0 0 0\n"
												+ "10 8 1 1 0 1 0\n" + "11 2 1 0 0 1 1\n" + "12 2 1 1 1 0 0\n"
												+ "13 1 0 1 0 1 1\n" + "14 4 1 0 1 0 0\n" + "15 4 0 1 0 0 1\n"
												+ "16 1 1 1 0 1 1\n" + "17 2 1 0 1 1 0\n" + "18 1 0 0 0 0 1\n"
												+ "19 1 1 1 1 1 0\n" + "20 1 1 1 0 0 1\n" + "21 1 0 1 1 1 0"), P10_93(
														"100 5 25\n" + "1 2 1 2 1\n" + "2 3 3 5 5\n" + "0 7 1 0 0 1 0\n"
																+ "1 11 1 1 0 0 0\n" + "2 1 0 1 1 1 1\n"
																+ "3 3 1 0 1 0 0\n" + "4 15 0 1 0 0 0\n"
																+ "5 2 1 0 1 1 0\n" + "6 8 0 1 0 1 0\n"
																+ "7 5 0 0 1 0 0\n" + "8 3 0 0 0 1 0\n"
																+ "9 4 0 1 1 1 0\n" + "10 5 1 0 0 0 0\n"
																+ "11 2 1 1 1 0 1\n" + "12 6 0 1 1 0 0\n"
																+ "13 2 0 0 1 0 1\n" + "14 2 0 1 0 0 1\n"
																+ "15 4 1 1 1 1 0\n" + "16 3 1 0 0 0 1\n"
																+ "17 5 1 1 0 1 0\n" + "18 2 1 1 1 0 0\n"
																+ "19 4 1 1 0 0 1\n" + "20 1 1 0 0 1 1\n"
																+ "21 1 1 1 0 1 1\n" + "22 1 0 1 0 1 1\n"
																+ "23 1 0 1 1 0 1\n" + "24 2 0 0 0 0 1"), P16_81(
																		"100 5 26\n" + "1 2 1 2 1\n" + "2 3 3 5 5\n"
																				+ "0 10 1 0 0 0 0\n" + "1 2 0 0 0 0 1\n"
																				+ "2 8 0 1 0 1 0\n" + "3 8 0 0 0 1 0\n"
																				+ "4 6 0 1 1 0 0\n" + "5 11 0 1 0 0 0\n"
																				+ "6 3 0 0 1 0 0\n" + "7 2 0 0 1 1 0\n"
																				+ "8 7 1 1 0 0 0\n" + "9 2 1 0 0 1 1\n"
																				+ "10 4 1 0 1 0 0\n"
																				+ "11 7 1 0 0 1 0\n"
																				+ "12 1 1 1 1 0 1\n"
																				+ "13 3 0 1 1 1 0\n"
																				+ "14 4 0 1 0 0 1\n"
																				+ "15 5 1 1 1 0 0\n"
																				+ "16 2 1 1 0 0 1\n"
																				+ "17 1 1 0 1 1 1\n"
																				+ "18 2 1 0 1 1 0\n"
																				+ "19 3 1 0 0 0 1\n"
																				+ "20 2 0 1 1 0 1\n"
																				+ "21 1 0 1 0 1 1\n"
																				+ "22 3 1 1 0 1 0\n"
																				+ "23 1 0 0 1 1 1\n"
																				+ "24 1 1 1 1 1 1\n"
																				+ "25 1 1 1 1 1 0"), P10_71(
																						"100 5 23\n" + "1 2 1 2 1\n"
																								+ "2 3 3 5 5\n"
																								+ "0 2 0 0 0 1 1\n"
																								+ "1 2 0 0 1 0 1\n"
																								+ "2 5 0 1 1 1 0\n"
																								+ "3 4 0 0 0 1 0\n"
																								+ "4 4 0 1 0 1 0\n"
																								+ "5 1 1 1 0 0 1\n"
																								+ "6 3 1 1 1 0 1\n"
																								+ "7 4 0 0 1 0 0\n"
																								+ "8 19 0 1 0 0 0\n"
																								+ "9 7 1 1 0 1 0\n"
																								+ "10 10 1 0 0 0 0\n"
																								+ "11 1 0 0 1 1 0\n"
																								+ "12 5 1 1 1 1 0\n"
																								+ "13 2 1 0 1 1 0\n"
																								+ "14 6 1 1 0 0 0\n"
																								+ "15 4 1 1 1 0 0\n"
																								+ "16 8 1 0 0 1 0\n"
																								+ "17 1 1 0 0 0 1\n"
																								+ "18 4 0 1 1 0 0\n"
																								+ "19 2 0 0 0 0 1\n"
																								+ "20 4 0 1 0 0 1\n"
																								+ "21 1 1 1 0 1 1\n"
																								+ "22 1 0 1 1 0 1"), P21_90(
																										"100 5 23\n"
																												+ "1 2 1 2 1\n"
																												+ "2 3 3 5 5\n"
																												+ "0 14 0 1 0 0 0\n"
																												+ "1 11 1 0 0 0 0\n"
																												+ "2 2 0 1 1 1 0\n"
																												+ "3 1 0 1 1 0 1\n"
																												+ "4 1 1 0 0 1 1\n"
																												+ "5 3 1 0 1 0 0\n"
																												+ "6 5 0 0 0 1 0\n"
																												+ "7 4 1 0 0 1 0\n"
																												+ "8 1 1 1 1 1 1\n"
																												+ "9 5 0 0 1 0 0\n"
																												+ "10 3 1 1 0 1 0\n"
																												+ "11 2 1 1 0 1 1\n"
																												+ "12 2 1 1 1 0 1\n"
																												+ "13 7 0 1 1 0 0\n"
																												+ "14 9 0 1 0 1 0\n"
																												+ "15 14 1 1 0 0 0\n"
																												+ "16 3 0 1 0 1 1\n"
																												+ "17 2 0 0 1 0 1\n"
																												+ "18 6 1 1 1 0 0\n"
																												+ "19 2 1 1 1 1 0\n"
																												+ "20 1 0 1 0 0 1\n"
																												+ "21 1 0 0 0 0 1\n"
																												+ "22 1 0 0 0 1 1"), P36_92(
																														"100 5 22\n"
																																+ "1 2 1 2 1\n"
																																+ "2 3 3 5 5\n"
																																+ "0 20 0 1 0 0 0\n"
																																+ "1 7 1 1 1 0 0\n"
																																+ "2 3 0 0 1 1 0\n"
																																+ "3 9 0 0 0 1 0\n"
																																+ "4 3 0 0 0 0 1\n"
																																+ "5 1 0 1 1 1 1\n"
																																+ "6 7 1 0 0 0 0\n"
																																+ "7 3 0 1 0 0 1\n"
																																+ "8 3 1 1 1 1 0\n"
																																+ "9 1 1 0 0 1 1\n"
																																+ "10 2 1 1 0 0 1\n"
																																+ "11 5 0 1 1 1 0\n"
																																+ "12 9 1 1 0 0 0\n"
																																+ "13 3 0 1 0 1 0\n"
																																+ "14 1 1 0 1 1 1\n"
																																+ "15 6 1 1 0 1 0\n"
																																+ "16 4 1 0 0 1 0\n"
																																+ "17 7 0 1 1 0 0\n"
																																+ "18 1 1 1 0 1 1\n"
																																+ "19 2 1 0 0 0 1\n"
																																+ "20 2 1 0 1 1 0\n"
																																+ "21 1 0 0 0 1 1"), P41_66(
																																		"100 5 19\n"
																																				+ "1 2 1 2 1\n"
																																				+ "2 3 3 5 5\n"
																																				+ "0 7 1 0 0 0 0\n"
																																				+ "1 9 0 1 1 0 0\n"
																																				+ "2 4 0 0 0 1 0\n"
																																				+ "3 2 0 1 0 1 1\n"
																																				+ "4 6 0 0 1 0 0\n"
																																				+ "5 18 0 1 0 0 0\n"
																																				+ "6 6 0 1 0 0 1\n"
																																				+ "7 6 0 0 0 0 1\n"
																																				+ "8 1 1 1 0 1 1\n"
																																				+ "9 10 1 1 0 0 0\n"
																																				+ "10 2 1 0 0 0 1\n"
																																				+ "11 11 0 1 0 1 0\n"
																																				+ "12 5 0 0 1 1 0\n"
																																				+ "13 1 0 1 1 1 0\n"
																																				+ "14 1 0 1 1 0 1\n"
																																				+ "15 3 1 0 1 0 0\n"
																																				+ "16 3 1 1 1 0 0\n"
																																				+ "17 3 1 1 0 1 0\n"
																																				+ "18 2 1 1 1 1 0"), P26_82(
																																						"100 5 24\n"
																																								+ "1 2 1 2 1\n"
																																								+ "2 3 3 5 5\n"
																																								+ "0 2 1 1 0 1 0\n"
																																								+ "1 13 0 1 0 0 0\n"
																																								+ "2 10 0 1 0 1 0\n"
																																								+ "3 14 1 1 0 0 0\n"
																																								+ "4 5 0 0 0 1 0\n"
																																								+ "5 2 0 1 0 1 1\n"
																																								+ "6 2 0 1 1 0 0\n"
																																								+ "7 8 1 0 0 1 0\n"
																																								+ "8 5 0 0 1 1 0\n"
																																								+ "9 3 1 1 1 0 0\n"
																																								+ "10 9 1 0 0 0 0\n"
																																								+ "11 6 1 1 0 0 1\n"
																																								+ "12 2 1 1 1 1 0\n"
																																								+ "13 2 0 0 0 0 1\n"
																																								+ "14 1 1 1 1 0 1\n"
																																								+ "15 2 0 1 1 1 0\n"
																																								+ "16 2 1 0 1 0 0\n"
																																								+ "17 1 1 0 0 0 1\n"
																																								+ "18 1 1 0 1 1 0\n"
																																								+ "19 6 0 0 1 0 0\n"
																																								+ "20 1 1 1 1 1 1\n"
																																								+ "21 1 0 0 1 1 1\n"
																																								+ "22 1 0 1 1 0 1\n"
																																								+ "23 1 0 0 1 0 1"),;

		final String source;

		Data(String source) {
			this.source = source;
		}

		String source() {
			return source;
		}
	}
	
	enum CSPLib {
		random01("100 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 10 1 0 0 0 0\n" + 
				"1 2 0 0 0 0 1\n" + 
				"2 8 0 1 0 1 0\n" + 
				"3 8 0 0 0 1 0\n" + 
				"4 6 0 1 1 0 0\n" + 
				"5 11 0 1 0 0 0\n" + 
				"6 3 0 0 1 0 0\n" + 
				"7 2 0 0 1 1 0\n" + 
				"8 7 1 1 0 0 0\n" + 
				"9 2 1 0 0 1 1\n" + 
				"10 4 1 0 1 0 0\n" + 
				"11 7 1 0 0 1 0\n" + 
				"12 1 1 1 1 0 1\n" + 
				"13 3 0 1 1 1 0\n" + 
				"14 4 0 1 0 0 1\n" + 
				"15 5 1 1 1 0 0\n" + 
				"16 2 1 1 0 0 1\n" + 
				"17 1 1 0 1 1 1\n" + 
				"18 2 1 0 1 1 0\n" + 
				"19 3 1 0 0 0 1\n" + 
				"20 2 0 1 1 0 1\n" + 
				"21 1 0 1 0 1 1\n" + 
				"22 3 1 1 0 1 0\n" + 
				"23 1 0 0 1 1 1\n" + 
				"24 1 1 1 1 1 1\n" + 
				"25 1 1 1 1 1 0\n"),
				random02("100 5 23\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 14 0 1 0 0 0\n" + 
				"1 11 1 0 0 0 0\n" + 
				"2 2 0 1 1 1 0\n" + 
				"3 1 0 1 1 0 1\n" + 
				"4 1 1 0 0 1 1\n" + 
				"5 3 1 0 1 0 0\n" + 
				"6 5 0 0 0 1 0\n" + 
				"7 4 1 0 0 1 0\n" + 
				"8 1 1 1 1 1 1\n" + 
				"9 5 0 0 1 0 0\n" + 
				"10 3 1 1 0 1 0\n" + 
				"11 2 1 1 0 1 1\n" + 
				"12 2 1 1 1 0 1\n" + 
				"13 7 0 1 1 0 0\n" + 
				"14 9 0 1 0 1 0\n" + 
				"15 14 1 1 0 0 0\n" + 
				"16 3 0 1 0 1 1\n" + 
				"17 2 0 0 1 0 1\n" + 
				"18 6 1 1 1 0 0\n" + 
				"19 2 1 1 1 1 0\n" + 
				"20 1 0 1 0 0 1\n" + 
				"21 1 0 0 0 0 1\n" + 
				"22 1 0 0 0 1 1\n"),
				random03("100 5 22\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 20 0 1 0 0 0\n" + 
				"1 7 1 1 1 0 0\n" + 
				"2 3 0 0 1 1 0\n" + 
				"3 9 0 0 0 1 0\n" + 
				"4 3 0 0 0 0 1\n" + 
				"5 1 0 1 1 1 1\n" + 
				"6 7 1 0 0 0 0\n" + 
				"7 3 0 1 0 0 1\n" + 
				"8 3 1 1 1 1 0\n" + 
				"9 1 1 0 0 1 1\n" + 
				"10 2 1 1 0 0 1\n" + 
				"11 5 0 1 1 1 0\n" + 
				"12 9 1 1 0 0 0\n" + 
				"13 3 0 1 0 1 0\n" + 
				"14 1 1 0 1 1 1\n" + 
				"15 6 1 1 0 1 0\n" + 
				"16 4 1 0 0 1 0\n" + 
				"17 7 0 1 1 0 0\n" + 
				"18 1 1 1 0 1 1\n" + 
				"19 2 1 0 0 0 1\n" + 
				"20 2 1 0 1 1 0\n" + 
				"21 1 0 0 0 1 1\n"),
				random04("100 5 19\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 7 1 0 0 0 0\n" + 
				"1 9 0 1 1 0 0\n" + 
				"2 4 0 0 0 1 0\n" + 
				"3 2 0 1 0 1 1\n" + 
				"4 6 0 0 1 0 0\n" + 
				"5 18 0 1 0 0 0\n" + 
				"6 6 0 1 0 0 1\n" + 
				"7 6 0 0 0 0 1\n" + 
				"8 1 1 1 0 1 1\n" + 
				"9 10 1 1 0 0 0\n" + 
				"10 2 1 0 0 0 1\n" + 
				"11 11 0 1 0 1 0\n" + 
				"12 5 0 0 1 1 0\n" + 
				"13 1 0 1 1 1 0\n" + 
				"14 1 0 1 1 0 1\n" + 
				"15 3 1 0 1 0 0\n" + 
				"16 3 1 1 1 0 0\n" + 
				"17 3 1 1 0 1 0\n" + 
				"18 2 1 1 1 1 0\n"),
				random05("100 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 2 1 1 0 1 0\n" + 
				"1 13 0 1 0 0 0\n" + 
				"2 10 0 1 0 1 0\n" + 
				"3 14 1 1 0 0 0\n" + 
				"4 5 0 0 0 1 0\n" + 
				"5 2 0 1 0 1 1\n" + 
				"6 2 0 1 1 0 0\n" + 
				"7 8 1 0 0 1 0\n" + 
				"8 5 0 0 1 1 0\n" + 
				"9 3 1 1 1 0 0\n" + 
				"10 9 1 0 0 0 0\n" + 
				"11 6 1 1 0 0 1\n" + 
				"12 2 1 1 1 1 0\n" + 
				"13 2 0 0 0 0 1\n" + 
				"14 1 1 1 1 0 1\n" + 
				"15 2 0 1 1 1 0\n" + 
				"16 2 1 0 1 0 0\n" + 
				"17 1 1 0 0 0 1\n" + 
				"18 1 1 0 1 1 0\n" + 
				"19 6 0 0 1 0 0\n" + 
				"20 1 1 1 1 1 1\n" + 
				"21 1 0 0 1 1 1\n" + 
				"22 1 0 1 1 0 1\n" + 
				"23 1 0 0 1 0 1\n"),
				random06("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 1 0 0 1 0\n" + 
				"1 84 0 1 0 0 0\n" + 
				"2 5 0 1 0 1 0\n" + 
				"3 1 1 0 0 1 1\n" + 
				"4 8 1 1 0 0 0\n" + 
				"5 34 0 0 1 0 0\n" + 
				"6 3 1 1 1 1 0\n" + 
				"7 11 0 0 0 0 1\n" + 
				"8 3 0 1 1 0 0\n" + 
				"9 1 1 0 1 0 1\n" + 
				"10 4 1 1 0 1 0\n" + 
				"11 2 0 0 1 0 1\n" + 
				"12 1 1 1 0 0 1\n" + 
				"13 1 1 1 1 0 1\n" + 
				"14 3 0 0 1 1 0\n" + 
				"15 1 1 0 0 0 1\n" + 
				"16 12 0 0 0 1 0\n" + 
				"17 15 1 0 0 0 0\n" + 
				"18 2 0 1 1 1 0\n" + 
				"19 1 1 0 1 1 0\n" + 
				"20 1 1 1 0 1 1\n" + 
				"21 1 1 1 1 0 0\n" + 
				"22 1 0 0 1 1 1\n" + 
				"23 2 0 1 0 0 1\n"),
				random07("200 5 17\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 2 1 0 1 1 0\n" + 
				"1 2 1 0 0 0 1\n" + 
				"2 4 1 1 0 0 0\n" + 
				"3 1 1 1 1 0 0\n" + 
				"4 30 0 0 0 0 1\n" + 
				"5 1 1 1 0 1 0\n" + 
				"6 1 1 0 1 0 1\n" + 
				"7 39 0 0 1 0 0\n" + 
				"8 54 0 0 0 1 0\n" + 
				"9 1 0 1 1 0 1\n" + 
				"10 2 0 0 1 1 0\n" + 
				"11 3 1 0 1 0 0\n" + 
				"12 1 1 0 0 1 1\n" + 
				"13 53 0 1 0 0 0\n" + 
				"14 1 0 1 1 0 0\n" + 
				"15 1 0 1 0 0 1\n" + 
				"16 4 1 0 0 0 0\n"),
				random08("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 1 1 1 1 0\n" + 
				"1 2 1 0 0 0 1\n" + 
				"2 48 0 0 0 1 0\n" + 
				"3 1 1 1 1 0 1\n" + 
				"4 1 0 1 0 1 1\n" + 
				"5 1 1 1 0 1 0\n" + 
				"6 6 0 1 0 1 0\n" + 
				"7 1 0 0 1 0 1\n" + 
				"8 4 1 0 0 1 0\n" + 
				"9 32 0 0 1 0 0\n" + 
				"10 1 1 1 1 0 0\n" + 
				"11 58 0 1 0 0 0\n" + 
				"12 1 0 0 1 1 0\n" + 
				"13 4 1 0 1 0 0\n" + 
				"14 4 0 1 1 0 0\n" + 
				"15 2 1 1 0 0 1\n" + 
				"16 6 0 0 0 0 1\n" + 
				"17 12 1 0 0 0 0\n" + 
				"18 1 1 0 0 1 1\n" + 
				"19 1 0 1 0 0 1\n" + 
				"20 3 0 1 1 1 0\n" + 
				"21 1 0 0 0 1 1\n" + 
				"22 1 0 1 1 1 1\n" + 
				"23 8 1 1 0 0 0\n"),
				random09("200 5 18\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 30 0 0 0 0 1\n" + 
				"1 1 0 1 1 0 1\n" + 
				"2 7 0 1 0 1 0\n" + 
				"3 4 1 0 1 0 0\n" + 
				"4 1 0 0 0 1 1\n" + 
				"5 1 0 1 1 0 0\n" + 
				"6 2 0 0 1 0 1\n" + 
				"7 3 1 1 1 0 0\n" + 
				"8 7 1 1 0 0 0\n" + 
				"9 3 0 1 1 1 0\n" + 
				"10 8 0 0 1 1 0\n" + 
				"11 73 0 1 0 0 0\n" + 
				"12 2 1 0 0 1 0\n" + 
				"13 3 0 1 0 0 1\n" + 
				"14 33 1 0 0 0 0\n" + 
				"15 17 0 0 0 1 0\n" + 
				"16 1 1 0 1 1 0\n" + 
				"17 4 0 0 1 0 0\n"),
				random10("200 5 20\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 0 1 0 1 1\n" + 
				"1 1 0 1 1 0 0\n" + 
				"2 63 1 0 0 0 0\n" + 
				"3 6 1 1 0 0 0\n" + 
				"4 1 1 0 1 1 0\n" + 
				"5 2 1 0 0 0 1\n" + 
				"6 43 0 0 0 1 0\n" + 
				"7 1 1 1 1 0 0\n" + 
				"8 2 0 1 1 1 0\n" + 
				"9 7 1 0 1 0 0\n" + 
				"10 1 1 1 1 1 0\n" + 
				"11 16 0 0 0 0 1\n" + 
				"12 2 1 1 0 1 0\n" + 
				"13 31 0 1 0 0 0\n" + 
				"14 6 0 1 0 1 0\n" + 
				"15 2 0 0 1 1 0\n" + 
				"16 9 0 0 1 0 0\n" + 
				"17 1 0 1 1 1 1\n" + 
				"18 2 1 1 0 0 1\n" + 
				"19 1 0 1 0 0 1\n"),
				random11("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 1 1 0 0 1\n" + 
				"1 50 0 0 0 1 0\n" + 
				"2 1 0 1 0 0 1\n" + 
				"3 1 1 0 0 0 1\n" + 
				"4 45 1 0 0 0 0\n" + 
				"5 8 1 1 0 0 0\n" + 
				"6 2 0 0 1 1 0\n" + 
				"7 2 0 0 0 1 1\n" + 
				"8 2 1 0 0 1 0\n" + 
				"9 4 0 1 1 0 0\n" + 
				"10 2 0 0 1 1 1\n" + 
				"11 4 1 1 1 0 0\n" + 
				"12 1 0 1 1 1 1\n" + 
				"13 1 0 1 1 1 0\n" + 
				"14 2 1 0 1 1 0\n" + 
				"15 44 0 1 0 0 0\n" + 
				"16 7 0 0 0 0 1\n" + 
				"17 7 0 1 0 1 0\n" + 
				"18 1 1 0 1 0 0\n" + 
				"19 1 1 1 1 0 1\n" + 
				"20 2 1 1 0 1 0\n" + 
				"21 2 0 1 0 1 1\n" + 
				"22 9 0 0 1 0 0\n" + 
				"23 1 0 0 1 0 1\n"),
				random12("200 5 21\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 82 0 1 0 0 0\n" + 
				"1 20 0 0 0 0 1\n" + 
				"2 2 1 1 1 1 0\n" + 
				"3 3 0 1 0 0 1\n" + 
				"4 3 0 1 1 1 0\n" + 
				"5 1 1 0 1 0 1\n" + 
				"6 2 1 1 0 1 0\n" + 
				"7 28 0 0 0 1 0\n" + 
				"8 1 1 1 0 1 1\n" + 
				"9 2 1 0 1 0 0\n" + 
				"10 17 0 0 1 0 0\n" + 
				"11 2 1 1 0 0 1\n" + 
				"12 5 1 1 0 0 0\n" + 
				"13 12 1 0 0 0 0\n" + 
				"14 5 0 1 1 0 0\n" + 
				"15 4 1 1 1 0 0\n" + 
				"16 2 1 0 0 1 0\n" + 
				"17 1 0 0 1 1 0\n" + 
				"18 1 1 0 1 1 0\n" + 
				"19 5 0 1 0 1 0\n" + 
				"20 2 1 0 0 0 1\n"),
				random13("200 5 21\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 1 1 0 1 0\n" + 
				"1 1 0 0 0 1 1\n" + 
				"2 46 0 0 0 1 0\n" + 
				"3 4 1 0 0 1 0\n" + 
				"4 9 1 1 0 0 0\n" + 
				"5 63 0 1 0 0 0\n" + 
				"6 4 0 0 1 1 0\n" + 
				"7 13 0 0 0 0 1\n" + 
				"8 2 1 1 1 0 0\n" + 
				"9 2 1 0 1 0 0\n" + 
				"10 11 0 1 0 1 0\n" + 
				"11 4 1 0 0 0 1\n" + 
				"12 1 1 0 1 1 0\n" + 
				"13 1 0 1 1 0 1\n" + 
				"14 17 1 0 0 0 0\n" + 
				"15 9 0 0 1 0 0\n" + 
				"16 1 1 1 1 1 0\n" + 
				"17 1 1 1 1 0 1\n" + 
				"18 1 0 1 1 1 0\n" + 
				"19 4 0 1 1 0 0\n" + 
				"20 5 0 1 0 0 1\n"),
				random14("200 5 19\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 7 0 1 1 0 0\n" + 
				"1 56 0 0 0 1 0\n" + 
				"2 2 0 1 0 1 1\n" + 
				"3 2 1 1 1 0 0\n" + 
				"4 33 0 0 1 0 0\n" + 
				"5 39 1 0 0 0 0\n" + 
				"6 3 1 0 0 0 1\n" + 
				"7 1 1 1 0 0 1\n" + 
				"8 7 1 1 0 0 0\n" + 
				"9 1 0 0 1 1 0\n" + 
				"10 4 1 0 1 0 0\n" + 
				"11 1 1 0 1 0 1\n" + 
				"12 1 0 1 1 1 1\n" + 
				"13 24 0 1 0 0 0\n" + 
				"14 8 0 1 0 1 0\n" + 
				"15 7 0 0 0 0 1\n" + 
				"16 2 0 1 1 1 0\n" + 
				"17 1 1 0 0 1 0\n" + 
				"18 1 0 0 0 1 1\n"),
				random15("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 0 0 1 1 1\n" + 
				"1 1 1 1 1 0 1\n" + 
				"2 1 0 1 1 1 0\n" + 
				"3 2 0 1 1 0 1\n" + 
				"4 70 0 1 0 0 0\n" + 
				"5 2 0 1 0 0 1\n" + 
				"6 4 0 0 0 1 1\n" + 
				"7 2 1 0 1 1 0\n" + 
				"8 3 1 0 0 0 1\n" + 
				"9 2 1 1 1 0 0\n" + 
				"10 1 1 1 0 1 0\n" + 
				"11 4 1 0 1 0 0\n" + 
				"12 33 1 0 0 0 0\n" + 
				"13 15 1 1 0 0 0\n" + 
				"14 13 0 0 1 0 0\n" + 
				"15 23 0 0 0 1 0\n" + 
				"16 3 0 1 1 0 0\n" + 
				"17 1 1 0 0 1 1\n" + 
				"18 3 0 0 1 1 0\n" + 
				"19 3 1 0 0 1 0\n" + 
				"20 4 0 1 0 1 0\n" + 
				"21 1 0 0 1 0 1\n" + 
				"22 6 0 0 0 0 1\n" + 
				"23 2 1 1 1 1 0\n"),
				random16("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 1 0 1 0 0\n" + 
				"1 4 1 0 0 1 0\n" + 
				"2 72 0 1 0 0 0\n" + 
				"3 8 0 1 0 1 0\n" + 
				"4 1 1 0 0 1 1\n" + 
				"5 10 1 1 0 0 0\n" + 
				"6 30 0 0 1 0 0\n" + 
				"7 3 1 1 1 1 0\n" + 
				"8 10 0 0 0 0 1\n" + 
				"9 5 0 1 1 0 0\n" + 
				"10 1 1 0 1 0 1\n" + 
				"11 6 1 1 0 1 0\n" + 
				"12 2 0 0 1 0 1\n" + 
				"13 1 1 1 0 0 1\n" + 
				"14 1 1 1 1 0 1\n" + 
				"15 3 0 0 1 1 0\n" + 
				"16 1 1 0 0 0 1\n" + 
				"17 1 0 0 0 1 1\n" + 
				"18 13 0 0 0 1 0\n" + 
				"19 15 1 0 0 0 0\n" + 
				"20 2 0 1 1 1 0\n" + 
				"21 1 1 0 1 1 0\n" + 
				"22 1 1 1 0 1 1\n" + 
				"23 1 1 1 1 0 0\n" + 
				"24 1 0 0 1 0 1\n" + 
				"25 4 0 1 0 0 0\n"),
				random17("200 5 22\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 2 1 0 1 1 0\n" + 
				"1 3 1 0 0 0 1\n" + 
				"2 5 1 1 0 0 0\n" + 
				"3 3 1 1 1 0 0\n" + 
				"4 24 0 0 0 0 1\n" + 
				"5 2 1 1 0 0 1\n" + 
				"6 2 1 1 0 1 0\n" + 
				"7 2 0 1 0 1 0\n" + 
				"8 1 1 0 1 0 1\n" + 
				"9 32 0 0 1 0 0\n" + 
				"10 44 0 0 0 1 0\n" + 
				"11 2 0 1 1 0 1\n" + 
				"12 4 0 0 1 1 0\n" + 
				"13 6 1 0 1 0 0\n" + 
				"14 1 1 0 0 1 1\n" + 
				"15 1 0 0 1 1 1\n" + 
				"16 1 0 0 0 1 1\n" + 
				"17 53 0 1 0 0 0\n" + 
				"18 1 0 1 1 0 0\n" + 
				"19 1 0 1 0 0 1\n" + 
				"20 8 1 0 0 0 0\n" + 
				"21 2 1 0 0 1 0\n"),
				random18("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 1 1 1 1 0\n" + 
				"1 2 1 0 0 0 1\n" + 
				"2 40 0 0 0 1 0\n" + 
				"3 1 1 1 1 0 1\n" + 
				"4 1 0 1 0 1 1\n" + 
				"5 1 1 0 1 1 1\n" + 
				"6 1 1 1 0 1 0\n" + 
				"7 7 0 1 0 1 0\n" + 
				"8 1 0 0 1 0 1\n" + 
				"9 5 1 0 0 1 0\n" + 
				"10 29 0 0 1 0 0\n" + 
				"11 1 1 1 1 0 0\n" + 
				"12 56 0 1 0 0 0\n" + 
				"13 1 0 0 1 1 0\n" + 
				"14 5 1 0 1 0 0\n" + 
				"15 5 0 1 1 0 0\n" + 
				"16 2 1 1 0 0 1\n" + 
				"17 6 0 0 0 0 1\n" + 
				"18 13 1 0 0 0 0\n" + 
				"19 3 1 0 0 1 1\n" + 
				"20 1 0 1 0 0 1\n" + 
				"21 4 0 1 1 1 0\n" + 
				"22 2 0 0 0 1 1\n" + 
				"23 2 0 1 1 0 1\n" + 
				"24 10 1 1 0 0 0\n"),
				random19("200 5 21\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 26 0 0 0 0 1\n" + 
				"1 1 0 1 1 0 1\n" + 
				"2 10 0 1 0 1 0\n" + 
				"3 4 1 0 1 0 0\n" + 
				"4 1 0 0 0 1 1\n" + 
				"5 3 0 1 1 0 0\n" + 
				"6 3 0 0 1 0 1\n" + 
				"7 3 1 1 1 0 0\n" + 
				"8 9 1 1 0 0 0\n" + 
				"9 4 0 1 1 1 0\n" + 
				"10 8 0 0 1 1 0\n" + 
				"11 2 1 1 0 0 1\n" + 
				"12 59 0 1 0 0 0\n" + 
				"13 1 1 0 0 0 1\n" + 
				"14 3 1 0 0 1 0\n" + 
				"15 3 0 1 0 0 1\n" + 
				"16 32 1 0 0 0 0\n" + 
				"17 19 0 0 0 1 0\n" + 
				"18 1 1 0 1 1 0\n" + 
				"19 7 0 0 1 0 0\n" + 
				"20 1 1 1 0 1 0\n"),
				random20("200 5 23\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 0 1 0 1 1\n" + 
				"1 1 0 1 1 0 0\n" + 
				"2 49 1 0 0 0 0\n" + 
				"3 13 1 1 0 0 0\n" + 
				"4 3 1 0 1 1 0\n" + 
				"5 2 1 0 0 0 1\n" + 
				"6 33 0 0 0 1 0\n" + 
				"7 1 0 0 1 1 1\n" + 
				"8 1 1 1 1 0 0\n" + 
				"9 2 0 1 1 1 0\n" + 
				"10 8 1 0 1 0 0\n" + 
				"11 1 1 1 1 1 0\n" + 
				"12 1 0 0 1 0 1\n" + 
				"13 14 0 0 0 0 1\n" + 
				"14 1 1 0 0 1 0\n" + 
				"15 3 1 1 0 1 0\n" + 
				"16 35 0 1 0 0 0\n" + 
				"17 10 0 1 0 1 0\n" + 
				"18 4 0 0 1 1 0\n" + 
				"19 10 0 0 1 0 0\n" + 
				"20 1 0 1 1 1 1\n" + 
				"21 2 1 1 0 0 1\n" + 
				"22 2 0 1 0 0 1\n"),
				random21("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 1 1 0 0 1\n" + 
				"1 42 0 0 0 1 0\n" + 
				"2 3 0 1 0 0 1\n" + 
				"3 3 1 0 0 0 1\n" + 
				"4 36 1 0 0 0 0\n" + 
				"5 9 1 1 0 0 0\n" + 
				"6 4 0 0 1 1 0\n" + 
				"7 2 0 0 0 1 1\n" + 
				"8 3 1 0 0 1 0\n" + 
				"9 6 0 1 1 0 0\n" + 
				"10 2 0 0 1 1 1\n" + 
				"11 4 1 1 1 0 0\n" + 
				"12 1 0 1 1 1 1\n" + 
				"13 2 0 1 1 1 0\n" + 
				"14 2 1 0 1 1 0\n" + 
				"15 44 0 1 0 0 0\n" + 
				"16 8 0 0 0 0 1\n" + 
				"17 10 0 1 0 1 0\n" + 
				"18 1 1 0 1 0 0\n" + 
				"19 1 1 1 1 0 1\n" + 
				"20 2 1 1 0 1 0\n" + 
				"21 2 0 1 0 1 1\n" + 
				"22 11 0 0 1 0 0\n" + 
				"23 1 0 0 1 0 1\n"),
				random22("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 73 0 1 0 0 0\n" + 
				"1 2 0 0 0 1 1\n" + 
				"2 17 0 0 0 0 1\n" + 
				"3 2 1 1 1 1 0\n" + 
				"4 3 0 1 0 0 1\n" + 
				"5 3 0 1 1 1 0\n" + 
				"6 1 1 0 1 0 1\n" + 
				"7 3 1 1 0 1 0\n" + 
				"8 25 0 0 0 1 0\n" + 
				"9 1 1 1 0 1 1\n" + 
				"10 1 0 0 1 1 1\n" + 
				"11 2 1 0 1 0 0\n" + 
				"12 16 0 0 1 0 0\n" + 
				"13 2 1 1 0 0 1\n" + 
				"14 7 1 1 0 0 0\n" + 
				"15 14 1 0 0 0 0\n" + 
				"16 7 0 1 1 0 0\n" + 
				"17 6 1 1 1 0 0\n" + 
				"18 3 1 0 0 1 0\n" + 
				"19 1 0 0 1 1 0\n" + 
				"20 2 1 0 1 1 0\n" + 
				"21 1 0 1 0 1 1\n" + 
				"22 6 0 1 0 0 0\n" + 
				"23 2 1 0 0 0 1\n"),
				random23("200 5 21\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 2 1 1 0 1 0\n" + 
				"1 2 0 0 0 1 1\n" + 
				"2 41 0 0 0 1 0\n" + 
				"3 4 1 0 0 1 0\n" + 
				"4 13 1 1 0 0 0\n" + 
				"5 51 0 1 0 0 0\n" + 
				"6 4 0 0 1 1 0\n" + 
				"7 12 0 0 0 0 1\n" + 
				"8 2 1 1 1 0 0\n" + 
				"9 3 1 0 1 0 0\n" + 
				"10 12 0 1 0 1 0\n" + 
				"11 4 1 0 0 0 1\n" + 
				"12 1 1 0 1 1 0\n" + 
				"13 1 0 1 1 0 1\n" + 
				"14 19 1 0 0 0 0\n" + 
				"15 13 0 0 1 0 0\n" + 
				"16 1 1 1 1 1 0\n" + 
				"17 2 1 1 1 0 1\n" + 
				"18 2 0 1 1 1 0\n" + 
				"19 5 0 1 1 0 0\n" + 
				"20 6 0 1 0 0 1\n"),
				random24("200 5 22\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 8 0 1 1 0 0\n" + 
				"1 47 0 0 0 1 0\n" + 
				"2 3 0 1 0 1 1\n" + 
				"3 2 1 1 1 0 0\n" + 
				"4 27 0 0 1 0 0\n" + 
				"5 33 1 0 0 0 0\n" + 
				"6 3 1 0 0 0 1\n" + 
				"7 2 1 1 0 0 1\n" + 
				"8 11 1 1 0 0 0\n" + 
				"9 3 0 0 1 1 0\n" + 
				"10 5 1 0 1 0 0\n" + 
				"11 1 1 0 1 0 1\n" + 
				"12 1 0 1 1 1 1\n" + 
				"13 28 0 1 0 0 0\n" + 
				"14 1 0 0 1 0 1\n" + 
				"15 9 0 1 0 1 0\n" + 
				"16 1 1 1 0 1 1\n" + 
				"17 8 0 0 0 0 1\n" + 
				"18 2 0 1 1 1 0\n" + 
				"19 2 1 1 0 1 0\n" + 
				"20 2 1 0 0 1 0\n" + 
				"21 1 0 0 0 1 1\n"),
				random25("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 0 0 1 1 1\n" + 
				"1 1 1 1 1 0 1\n" + 
				"2 1 0 1 1 1 0\n" + 
				"3 2 0 1 1 0 1\n" + 
				"4 64 0 1 0 0 0\n" + 
				"5 4 0 1 0 0 1\n" + 
				"6 4 0 0 0 1 1\n" + 
				"7 5 1 0 1 1 0\n" + 
				"8 3 1 0 0 0 1\n" + 
				"9 2 1 1 1 0 0\n" + 
				"10 1 1 1 0 1 0\n" + 
				"11 4 1 0 1 0 0\n" + 
				"12 1 0 1 0 1 1\n" + 
				"13 26 1 0 0 0 0\n" + 
				"14 16 1 1 0 0 0\n" + 
				"15 14 0 0 1 0 0\n" + 
				"16 25 0 0 0 1 0\n" + 
				"17 4 0 1 1 0 0\n" + 
				"18 1 1 0 0 1 1\n" + 
				"19 4 0 0 1 1 0\n" + 
				"20 4 1 0 0 1 0\n" + 
				"21 4 0 1 0 1 0\n" + 
				"22 1 0 0 1 0 1\n" + 
				"23 6 0 0 0 0 1\n" + 
				"24 2 1 1 1 0 0\n"),
				random26("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 4 1 0 1 0 0\n" + 
				"1 6 1 0 0 1 0\n" + 
				"2 59 0 1 0 0 0\n" + 
				"3 10 0 1 0 1 0\n" + 
				"4 1 1 0 0 1 1\n" + 
				"5 13 1 1 0 0 0\n" + 
				"6 23 0 0 1 0 0\n" + 
				"7 3 1 1 1 1 0\n" + 
				"8 11 0 0 0 0 1\n" + 
				"9 6 0 1 1 0 0\n" + 
				"10 1 1 0 1 0 1\n" + 
				"11 7 1 1 0 1 0\n" + 
				"12 2 0 0 1 0 1\n" + 
				"13 1 1 1 0 0 1\n" + 
				"14 1 1 1 1 0 1\n" + 
				"15 5 0 0 1 1 0\n" + 
				"16 2 1 0 0 0 1\n" + 
				"17 1 0 0 0 1 1\n" + 
				"18 14 0 0 0 1 0\n" + 
				"19 17 1 0 0 0 0\n" + 
				"20 2 0 1 1 1 0\n" + 
				"21 2 1 0 1 1 0\n" + 
				"22 1 1 1 0 1 1\n" + 
				"23 1 1 1 1 0 0\n" + 
				"24 1 0 0 1 0 1\n" + 
				"25 6 0 1 0 0 0\n"),
				random27("200 5 23\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 1 0 1 1 0\n" + 
				"1 4 1 0 0 0 1\n" + 
				"2 12 1 1 0 0 0\n" + 
				"3 3 1 1 1 0 0\n" + 
				"4 19 0 0 0 0 1\n" + 
				"5 2 1 1 0 0 1\n" + 
				"6 3 1 1 0 1 0\n" + 
				"7 7 0 1 0 1 0\n" + 
				"8 2 0 0 1 0 1\n" + 
				"9 1 1 0 1 0 1\n" + 
				"10 26 0 0 1 0 0\n" + 
				"11 35 0 0 0 1 0\n" + 
				"12 2 0 1 1 0 1\n" + 
				"13 6 0 0 1 1 0\n" + 
				"14 7 1 0 1 0 0\n" + 
				"15 1 1 0 0 1 1\n" + 
				"16 1 0 0 1 1 1\n" + 
				"17 2 0 0 0 1 1\n" + 
				"18 48 0 1 0 0 0\n" + 
				"19 2 0 1 1 0 0\n" + 
				"20 2 0 1 0 0 1\n" + 
				"21 9 1 0 0 0 0\n" + 
				"22 3 1 0 0 1 0\n"),
				random28("200 5 27\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 1 1 1 1 0\n" + 
				"1 2 1 0 0 0 1\n" + 
				"2 1 1 0 1 0 1\n" + 
				"3 33 0 0 0 1 0\n" + 
				"4 1 1 1 1 0 1\n" + 
				"5 1 0 1 0 1 1\n" + 
				"6 1 1 0 1 1 1\n" + 
				"7 2 1 1 0 1 0\n" + 
				"8 9 0 1 0 1 0\n" + 
				"9 1 0 0 1 0 1\n" + 
				"10 6 1 0 0 1 0\n" + 
				"11 24 0 0 1 0 0\n" + 
				"12 1 1 1 1 0 0\n" + 
				"13 51 0 1 0 0 0\n" + 
				"14 1 0 0 1 1 0\n" + 
				"15 6 1 0 1 0 0\n" + 
				"16 7 0 1 1 0 0\n" + 
				"17 2 1 1 0 0 1\n" + 
				"18 8 0 0 0 0 1\n" + 
				"19 14 1 0 0 0 0\n" + 
				"20 4 1 0 0 1 1\n" + 
				"21 1 0 1 0 0 1\n" + 
				"22 4 0 1 1 1 0\n" + 
				"23 2 0 0 0 1 1\n" + 
				"24 2 0 1 1 0 0\n" + 
				"25 1 1 0 1 0 0\n" + 
				"26 14 1 1 0 0 0\n"),
				random29("200 5 23\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 21 0 0 0 0 1\n" + 
				"1 1 0 1 1 0 1\n" + 
				"2 15 0 1 0 1 0\n" + 
				"3 7 1 0 1 0 0\n" + 
				"4 1 0 0 0 1 1\n" + 
				"5 4 0 1 1 0 0\n" + 
				"6 4 0 0 1 0 1\n" + 
				"7 3 1 1 1 0 0\n" + 
				"8 10 1 1 0 0 0\n" + 
				"9 4 0 1 1 1 0\n" + 
				"10 8 0 0 1 1 0\n" + 
				"11 3 1 1 0 0 1\n" + 
				"12 49 0 1 0 0 0\n" + 
				"13 1 1 0 0 1 1\n" + 
				"14 2 1 0 0 0 1\n" + 
				"15 4 1 0 0 1 0\n" + 
				"16 3 0 1 0 0 1\n" + 
				"17 30 1 0 0 0 0\n" + 
				"18 18 0 0 0 1 0\n" + 
				"19 1 1 0 1 1 0\n" + 
				"20 9 0 0 1 0 0\n" + 
				"21 1 1 1 1 0 1\n" + 
				"22 1 1 1 0 1 0\n"),
				random30("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 0 1 0 1 1\n" + 
				"1 1 0 1 1 0 0\n" + 
				"2 41 1 0 0 0 0\n" + 
				"3 13 1 1 0 0 0\n" + 
				"4 4 1 0 1 1 0\n" + 
				"5 3 1 0 0 0 1\n" + 
				"6 26 0 0 0 1 0\n" + 
				"7 1 0 0 1 1 1\n" + 
				"8 1 1 1 1 0 0\n" + 
				"9 3 0 1 1 1 0\n" + 
				"10 1 1 0 0 1 1\n" + 
				"11 9 1 0 1 0 0\n" + 
				"12 1 1 1 1 1 0\n" + 
				"13 2 0 0 1 0 1\n" + 
				"14 12 0 0 0 0 1\n" + 
				"15 2 1 0 0 1 0\n" + 
				"16 4 1 1 0 1 0\n" + 
				"17 36 0 1 0 0 0\n" + 
				"18 12 0 1 0 1 0\n" + 
				"19 5 0 0 1 1 0\n" + 
				"20 13 0 0 1 0 0\n" + 
				"21 1 0 1 1 0 1\n" + 
				"22 3 1 1 0 0 1\n" + 
				"23 3 0 1 0 0 1\n"),
				random31("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 1 1 0 0 1\n" + 
				"1 36 0 0 0 1 0\n" + 
				"2 4 0 1 0 0 1\n" + 
				"3 4 1 0 0 0 1\n" + 
				"4 29 1 0 0 0 0\n" + 
				"5 11 1 1 0 0 0\n" + 
				"6 4 0 0 1 1 0\n" + 
				"7 2 0 0 0 1 1\n" + 
				"8 3 1 0 0 1 0\n" + 
				"9 7 0 1 1 0 0\n" + 
				"10 2 0 0 1 1 1\n" + 
				"11 5 1 1 1 0 0\n" + 
				"12 1 0 1 1 1 1\n" + 
				"13 3 0 1 1 1 0\n" + 
				"14 4 1 0 1 1 0\n" + 
				"15 43 0 1 0 0 0\n" + 
				"16 10 0 0 0 0 1\n" + 
				"17 12 0 1 0 1 0\n" + 
				"18 1 1 0 1 0 0\n" + 
				"19 1 1 1 1 0 1\n" + 
				"20 2 1 1 0 1 0\n" + 
				"21 2 0 1 0 1 1\n" + 
				"22 11 0 0 1 0 0\n" + 
				"23 1 1 1 1 0 0\n" + 
				"24 1 0 0 1 0 1\n"),
				random32("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 58 0 1 0 0 0\n" + 
				"1 2 0 0 0 1 1\n" + 
				"2 15 0 0 0 0 1\n" + 
				"3 2 1 1 1 1 0\n" + 
				"4 6 0 1 0 0 1\n" + 
				"5 4 0 1 1 1 0\n" + 
				"6 1 1 0 1 0 1\n" + 
				"7 4 1 1 0 1 0\n" + 
				"8 21 0 0 0 1 0\n" + 
				"9 1 1 1 0 1 1\n" + 
				"10 1 0 0 1 1 1\n" + 
				"11 3 1 0 1 0 0\n" + 
				"12 15 0 0 1 0 0\n" + 
				"13 2 1 1 0 0 1\n" + 
				"14 9 1 1 0 0 0\n" + 
				"15 19 1 0 0 0 0\n" + 
				"16 9 0 1 1 0 0\n" + 
				"17 6 1 1 1 0 0\n" + 
				"18 4 1 0 0 1 0\n" + 
				"19 3 0 0 1 1 0\n" + 
				"20 2 1 0 1 1 0\n" + 
				"21 1 0 1 0 1 1\n" + 
				"22 9 0 1 0 0 0\n" + 
				"23 3 1 0 0 0 1\n"),
				random33("200 5 22\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 1 1 0 1 0\n" + 
				"1 2 0 0 0 1 1\n" + 
				"2 34 0 0 0 1 0\n" + 
				"3 6 1 0 0 1 0\n" + 
				"4 16 1 1 0 0 0\n" + 
				"5 43 0 1 0 0 0\n" + 
				"6 5 0 0 1 1 0\n" + 
				"7 13 0 0 0 0 1\n" + 
				"8 1 1 0 1 0 1\n" + 
				"9 2 1 1 1 0 0\n" + 
				"10 4 1 0 1 0 0\n" + 
				"11 13 0 1 0 1 0\n" + 
				"12 4 1 0 0 0 1\n" + 
				"13 1 1 0 1 1 0\n" + 
				"14 2 0 1 1 0 1\n" + 
				"15 17 1 0 0 0 0\n" + 
				"16 16 0 0 1 0 0\n" + 
				"17 1 1 1 1 1 0\n" + 
				"18 2 1 1 1 0 1\n" + 
				"19 2 0 1 1 1 0\n" + 
				"20 5 0 1 1 0 0\n" + 
				"21 8 0 1 0 0 1\n"),
				random34("200 5 22\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 11 0 1 1 0 0\n" + 
				"1 40 0 0 0 1 0\n" + 
				"2 3 0 1 0 1 1\n" + 
				"3 2 1 1 1 0 0\n" + 
				"4 22 0 0 1 0 0\n" + 
				"5 27 1 0 0 0 0\n" + 
				"6 4 1 0 0 0 1\n" + 
				"7 3 1 1 0 0 1\n" + 
				"8 14 1 1 0 0 0\n" + 
				"9 3 0 0 1 1 0\n" + 
				"10 5 1 0 1 0 0\n" + 
				"11 1 1 0 1 0 1\n" + 
				"12 1 0 1 1 1 1\n" + 
				"13 30 0 1 0 0 0\n" + 
				"14 2 0 0 1 0 1\n" + 
				"15 9 0 1 0 1 0\n" + 
				"16 1 1 1 0 1 1\n" + 
				"17 8 0 0 0 0 1\n" + 
				"18 4 0 1 1 1 0\n" + 
				"19 5 1 1 0 1 0\n" + 
				"20 3 1 0 0 1 0\n" + 
				"21 2 0 0 0 1 1\n"),
				random35("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 0 0 1 1 1\n" + 
				"1 1 1 1 1 0 1\n" + 
				"2 1 0 1 1 1 0\n" + 
				"3 2 0 1 1 0 1\n" + 
				"4 55 0 1 0 0 0\n" + 
				"5 5 0 1 0 0 1\n" + 
				"6 4 0 0 0 1 1\n" + 
				"7 5 1 0 1 1 0\n" + 
				"8 6 1 0 0 0 1\n" + 
				"9 2 1 1 1 0 0\n" + 
				"10 1 1 1 0 1 0\n" + 
				"11 4 1 0 1 0 0\n" + 
				"12 1 0 1 0 1 1\n" + 
				"13 22 1 0 0 0 0\n" + 
				"14 16 1 1 0 0 0\n" + 
				"15 16 0 0 1 0 0\n" + 
				"16 25 0 0 0 1 0\n" + 
				"17 6 0 1 1 0 0\n" + 
				"18 1 1 0 0 1 1\n" + 
				"19 4 0 0 1 1 0\n" + 
				"20 5 1 0 0 1 0\n" + 
				"21 6 0 1 0 1 0\n" + 
				"22 1 0 0 1 0 1\n" + 
				"23 6 0 0 0 0 1\n" + 
				"24 4 1 1 1 0 0\n"),
				random36("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 6 1 0 1 0 0\n" + 
				"1 8 1 0 0 1 0\n" + 
				"2 48 0 1 0 0 0\n" + 
				"3 12 0 1 0 1 0\n" + 
				"4 1 1 0 0 1 1\n" + 
				"5 15 1 1 0 0 0\n" + 
				"6 17 0 0 1 0 0\n" + 
				"7 3 1 1 1 1 0\n" + 
				"8 10 0 0 0 0 1\n" + 
				"9 6 0 1 1 0 0\n" + 
				"10 1 1 0 1 0 1\n" + 
				"11 8 1 1 0 1 0\n" + 
				"12 4 0 0 1 0 1\n" + 
				"13 1 1 1 0 0 1\n" + 
				"14 1 1 1 1 0 1\n" + 
				"15 5 0 0 1 1 0\n" + 
				"16 3 1 0 0 0 1\n" + 
				"17 1 0 0 0 1 1\n" + 
				"18 15 0 0 0 1 0\n" + 
				"19 19 1 0 0 0 0\n" + 
				"20 3 0 1 1 1 0\n" + 
				"21 2 1 0 1 1 0\n" + 
				"22 1 1 1 0 1 1\n" + 
				"23 1 1 1 1 0 0\n" + 
				"24 1 0 0 1 0 1\n" + 
				"25 8 0 1 0 0 0\n"),
				random37("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 4 1 0 1 1 0\n" + 
				"1 4 1 0 0 0 1\n" + 
				"2 16 1 1 0 0 0\n" + 
				"3 1 1 1 1 0 1\n" + 
				"4 4 1 1 1 0 0\n" + 
				"5 14 0 0 0 0 1\n" + 
				"6 2 1 1 0 0 1\n" + 
				"7 5 1 1 0 1 0\n" + 
				"8 9 0 1 0 1 0\n" + 
				"9 3 0 0 1 0 1\n" + 
				"10 2 1 0 1 0 1\n" + 
				"11 19 0 0 1 0 0\n" + 
				"12 31 0 0 0 1 0\n" + 
				"13 2 0 1 1 0 1\n" + 
				"14 9 0 0 1 1 0\n" + 
				"15 7 1 0 1 0 0\n" + 
				"16 1 1 0 0 1 1\n" + 
				"17 1 0 0 1 1 1\n" + 
				"18 2 0 0 0 1 1\n" + 
				"19 43 0 1 0 0 0\n" + 
				"20 2 0 1 1 0 0\n" + 
				"21 4 0 1 0 0 1\n" + 
				"22 12 1 0 0 0 0\n" + 
				"23 3 1 0 0 1 0\n"),
				random38("200 5 27\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 1 1 1 1 0\n" + 
				"1 2 1 0 0 0 1\n" + 
				"2 2 1 0 1 0 1\n" + 
				"3 24 0 0 0 1 0\n" + 
				"4 1 1 1 1 0 1\n" + 
				"5 2 0 1 0 1 1\n" + 
				"6 1 1 0 1 1 1\n" + 
				"7 2 1 1 0 1 0\n" + 
				"8 12 0 1 0 1 0\n" + 
				"9 1 0 0 1 0 1\n" + 
				"10 9 1 0 0 1 0\n" + 
				"11 20 0 0 1 0 0\n" + 
				"12 2 1 1 1 0 0\n" + 
				"13 44 0 1 0 0 0\n" + 
				"14 3 0 0 1 1 0\n" + 
				"15 6 1 0 1 0 0\n" + 
				"16 7 0 1 1 0 0\n" + 
				"17 4 1 1 0 0 1\n" + 
				"18 8 0 0 0 0 1\n" + 
				"19 17 1 0 0 0 0\n" + 
				"20 4 1 0 0 1 1\n" + 
				"21 1 0 1 0 0 1\n" + 
				"22 4 0 1 1 1 0\n" + 
				"23 2 0 0 0 1 1\n" + 
				"24 2 0 1 1 0 0\n" + 
				"25 1 1 0 1 0 0\n" + 
				"26 18 1 1 0 0 0\n"),
				random39("200 5 23\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 17 0 0 0 0 1\n" + 
				"1 2 0 1 1 0 1\n" + 
				"2 18 0 1 0 1 0\n" + 
				"3 8 1 0 1 0 0\n" + 
				"4 1 0 0 0 1 1\n" + 
				"5 5 0 1 1 0 0\n" + 
				"6 5 0 0 1 0 1\n" + 
				"7 3 1 1 1 0 0\n" + 
				"8 11 1 1 0 0 0\n" + 
				"9 4 0 1 1 1 0\n" + 
				"10 9 0 0 1 1 0\n" + 
				"11 4 1 1 0 0 1\n" + 
				"12 40 0 1 0 0 0\n" + 
				"13 1 1 0 0 1 1\n" + 
				"14 2 1 0 0 0 1\n" + 
				"15 6 1 0 0 1 0\n" + 
				"16 3 0 1 0 0 1\n" + 
				"17 29 1 0 0 0 0\n" + 
				"18 18 0 0 0 1 0\n" + 
				"19 1 1 0 1 1 0\n" + 
				"20 9 0 0 1 0 0\n" + 
				"21 2 1 1 1 0 1\n" + 
				"22 2 1 1 0 1 0\n"),
				random40("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 0 1 0 1 1\n" + 
				"1 3 0 1 1 0 0\n" + 
				"2 35 1 0 0 0 0\n" + 
				"3 14 1 1 0 0 0\n" + 
				"4 4 1 0 1 1 0\n" + 
				"5 3 1 0 0 0 1\n" + 
				"6 22 0 0 0 1 0\n" + 
				"7 1 0 0 1 1 1\n" + 
				"8 2 1 1 1 0 0\n" + 
				"9 3 0 1 1 1 0\n" + 
				"10 3 1 0 0 1 1\n" + 
				"11 9 1 0 1 0 0\n" + 
				"12 1 1 1 1 1 0\n" + 
				"13 2 0 0 1 0 1\n" + 
				"14 11 0 0 0 0 1\n" + 
				"15 4 1 0 0 1 0\n" + 
				"16 4 1 1 0 1 0\n" + 
				"17 33 0 1 0 0 0\n" + 
				"18 14 0 1 0 1 0\n" + 
				"19 5 0 0 1 1 0\n" + 
				"20 16 0 0 1 0 0\n" + 
				"21 2 0 1 1 0 1\n" + 
				"22 3 1 1 0 0 1\n" + 
				"23 3 0 1 0 0 1\n"),
				random41("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 1 1 0 0 1\n" + 
				"1 25 0 0 0 1 0\n" + 
				"2 6 0 1 0 0 1\n" + 
				"3 5 1 0 0 0 1\n" + 
				"4 23 1 0 0 0 0\n" + 
				"5 11 1 1 0 0 0\n" + 
				"6 5 0 0 1 1 0\n" + 
				"7 3 0 0 0 1 1\n" + 
				"8 5 1 0 0 1 0\n" + 
				"9 7 0 1 1 0 0\n" + 
				"10 2 0 0 1 1 1\n" + 
				"11 6 1 1 1 0 0\n" + 
				"12 1 0 1 1 1 1\n" + 
				"13 3 0 1 1 1 0\n" + 
				"14 5 1 0 1 1 0\n" + 
				"15 43 0 1 0 0 0\n" + 
				"16 9 0 0 0 0 1\n" + 
				"17 14 0 1 0 1 0\n" + 
				"18 1 1 0 1 0 0\n" + 
				"19 1 1 1 1 0 1\n" + 
				"20 5 1 1 0 1 0\n" + 
				"21 2 0 1 0 1 1\n" + 
				"22 14 0 0 1 0 0\n" + 
				"23 1 1 1 1 0 0\n" + 
				"24 2 0 0 1 0 1\n"),
				random42("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 46 0 1 0 0 0\n" + 
				"1 2 0 0 0 1 1\n" + 
				"2 14 0 0 0 0 1\n" + 
				"3 2 1 1 1 1 0\n" + 
				"4 7 0 1 0 0 1\n" + 
				"5 4 0 1 1 1 0\n" + 
				"6 2 1 0 1 0 1\n" + 
				"7 6 1 1 0 1 0\n" + 
				"8 19 0 0 0 1 0\n" + 
				"9 1 1 1 0 1 1\n" + 
				"10 1 0 0 1 1 1\n" + 
				"11 5 1 0 1 0 0\n" + 
				"12 16 0 0 1 0 0\n" + 
				"13 3 1 1 0 0 1\n" + 
				"14 10 1 1 0 0 0\n" + 
				"15 21 1 0 0 0 0\n" + 
				"16 9 0 1 1 0 0\n" + 
				"17 6 1 1 1 0 0\n" + 
				"18 5 1 0 0 1 0\n" + 
				"19 3 0 0 1 1 0\n" + 
				"20 2 1 0 1 1 0\n" + 
				"21 1 0 1 0 1 1\n" + 
				"22 12 0 1 0 0 0\n" + 
				"23 3 1 0 0 0 1\n"),
				random43("200 5 23\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 4 1 1 0 1 0\n" + 
				"1 2 0 0 0 1 1\n" + 
				"2 26 0 0 0 1 0\n" + 
				"3 9 1 0 0 1 0\n" + 
				"4 1 0 0 1 0 1\n" + 
				"5 18 1 1 0 0 0\n" + 
				"6 33 0 1 0 0 0\n" + 
				"7 5 0 0 1 1 0\n" + 
				"8 12 0 0 0 0 1\n" + 
				"9 1 1 0 1 0 1\n" + 
				"10 2 1 1 1 0 0\n" + 
				"11 5 1 0 1 0 0\n" + 
				"12 17 0 1 0 1 0\n" + 
				"13 5 1 0 0 0 1\n" + 
				"14 1 1 0 1 1 0\n" + 
				"15 2 0 1 1 0 1\n" + 
				"16 17 1 0 0 0 0\n" + 
				"17 19 0 0 1 0 0\n" + 
				"18 1 1 1 1 1 0\n" + 
				"19 2 1 1 1 0 1\n" + 
				"20 2 0 1 1 1 0\n" + 
				"21 7 0 1 1 0 0\n" + 
				"22 9 0 1 0 0 1\n"),
				random44("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 12 0 1 1 0 0\n" + 
				"1 1 0 0 1 1 1\n" + 
				"2 32 0 0 0 1 0\n" + 
				"3 3 0 1 0 1 1\n" + 
				"4 2 1 1 1 0 0\n" + 
				"5 17 0 0 1 0 0\n" + 
				"6 21 1 0 0 0 0\n" + 
				"7 4 1 0 0 0 1\n" + 
				"8 2 1 0 1 1 0\n" + 
				"9 3 1 1 0 0 1\n" + 
				"10 19 1 1 0 0 0\n" + 
				"11 3 0 0 1 1 0\n" + 
				"12 3 0 1 0 0 1\n" + 
				"13 6 1 0 1 0 0\n" + 
				"14 1 1 0 1 0 1\n" + 
				"15 1 0 1 1 1 1\n" + 
				"16 30 0 1 0 0 0\n" + 
				"17 2 0 0 1 0 1\n" + 
				"18 12 0 1 0 1 0\n" + 
				"19 1 1 1 0 1 1\n" + 
				"20 9 0 0 0 0 1\n" + 
				"21 4 0 1 1 1 0\n" + 
				"22 5 1 1 0 1 0\n" + 
				"23 5 1 0 0 1 0\n" + 
				"24 2 0 0 0 0 1\n"),
				random45("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 0 0 1 1 1\n" + 
				"1 1 1 1 1 0 1\n" + 
				"2 2 0 1 1 1 0\n" + 
				"3 2 0 1 1 0 1\n" + 
				"4 43 0 1 0 0 0\n" + 
				"5 5 0 1 0 0 1\n" + 
				"6 4 0 0 0 1 1\n" + 
				"7 5 1 0 1 1 0\n" + 
				"8 6 1 0 0 0 1\n" + 
				"9 3 1 1 1 0 0\n" + 
				"10 2 1 1 0 1 0\n" + 
				"11 5 1 0 1 0 0\n" + 
				"12 1 0 1 0 1 1\n" + 
				"13 18 1 0 0 0 0\n" + 
				"14 18 1 1 0 0 0\n" + 
				"15 16 0 0 1 0 0\n" + 
				"16 27 0 0 0 1 0\n" + 
				"17 7 0 1 1 0 0\n" + 
				"18 1 1 0 0 1 1\n" + 
				"19 4 0 0 1 1 0\n" + 
				"20 8 1 0 0 1 0\n" + 
				"21 7 0 1 0 1 0\n" + 
				"22 2 0 0 1 0 1\n" + 
				"23 8 0 0 0 0 1\n" + 
				"24 4 1 1 1 0 0\n"),
				random46("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 6 1 0 1 0 0\n" + 
				"1 8 1 0 0 1 0\n" + 
				"2 39 0 1 0 0 0\n" + 
				"3 14 0 1 0 1 0\n" + 
				"4 1 1 0 0 1 1\n" + 
				"5 15 1 1 0 0 0\n" + 
				"6 13 0 0 1 0 0\n" + 
				"7 4 1 1 1 1 0\n" + 
				"8 11 0 0 0 0 1\n" + 
				"9 9 0 1 1 0 0\n" + 
				"10 1 1 0 1 0 1\n" + 
				"11 9 1 1 0 1 0\n" + 
				"12 4 0 0 1 0 1\n" + 
				"13 2 1 1 0 0 1\n" + 
				"14 1 1 1 1 0 1\n" + 
				"15 6 0 0 1 1 0\n" + 
				"16 3 1 0 0 0 1\n" + 
				"17 2 0 0 0 1 1\n" + 
				"18 14 0 0 0 1 0\n" + 
				"19 20 1 0 0 0 0\n" + 
				"20 3 0 1 1 1 0\n" + 
				"21 3 1 0 1 1 0\n" + 
				"22 1 1 1 0 1 1\n" + 
				"23 1 1 1 1 0 0\n" + 
				"24 1 0 0 1 0 1\n" + 
				"25 9 0 1 0 0 0\n"),
				random47("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 4 1 0 1 1 0\n" + 
				"1 6 1 0 0 0 1\n" + 
				"2 19 1 1 0 0 0\n" + 
				"3 1 1 1 1 0 1\n" + 
				"4 6 1 1 1 0 0\n" + 
				"5 8 0 0 0 0 1\n" + 
				"6 2 1 1 0 0 1\n" + 
				"7 6 1 1 0 1 0\n" + 
				"8 13 0 1 0 1 0\n" + 
				"9 3 0 0 1 0 1\n" + 
				"10 4 1 0 1 0 1\n" + 
				"11 12 0 0 1 0 0\n" + 
				"12 25 0 0 0 1 0\n" + 
				"13 3 0 1 1 0 1\n" + 
				"14 12 0 0 1 1 0\n" + 
				"15 7 1 0 1 0 0\n" + 
				"16 1 1 0 0 1 1\n" + 
				"17 1 0 1 0 1 1\n" + 
				"18 1 0 0 1 1 1\n" + 
				"19 2 0 0 0 1 1\n" + 
				"20 42 0 1 0 0 0\n" + 
				"21 2 0 1 1 0 0\n" + 
				"22 4 0 1 0 0 0\n" + 
				"23 13 1 0 0 0 0\n" + 
				"24 3 1 0 0 1 0\n"),
				random48("200 5 28\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 1 0 1 1 0 1\n" + 
				"1 1 1 1 1 1 0\n" + 
				"2 3 1 0 0 0 1\n" + 
				"3 2 1 0 1 0 1\n" + 
				"4 20 0 0 0 1 0\n" + 
				"5 1 1 1 1 0 1\n" + 
				"6 2 0 1 0 1 1\n" + 
				"7 1 1 0 1 1 1\n" + 
				"8 2 1 1 0 1 0\n" + 
				"9 13 0 1 0 1 0\n" + 
				"10 1 0 0 1 0 1\n" + 
				"11 9 1 0 0 1 0\n" + 
				"12 17 0 0 1 0 0\n" + 
				"13 3 1 1 1 0 0\n" + 
				"14 37 0 1 0 0 0\n" + 
				"15 3 0 0 1 1 0\n" + 
				"16 6 1 0 1 0 0\n" + 
				"17 7 0 1 1 0 0\n" + 
				"18 5 1 1 0 0 1\n" + 
				"19 9 0 0 0 0 1\n" + 
				"20 21 1 0 0 0 0\n" + 
				"21 4 1 0 0 1 1\n" + 
				"22 1 0 1 0 0 1\n" + 
				"23 5 0 1 1 1 0\n" + 
				"24 2 0 0 0 1 0\n" + 
				"25 2 0 1 1 0 0\n" + 
				"26 3 1 0 0 0 0\n" + 
				"27 19 1 0 0 0 0\n"),
				random49("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 12 0 0 0 0 1\n" + 
				"1 2 0 1 1 0 1\n" + 
				"2 22 0 1 0 1 0\n" + 
				"3 10 1 0 1 0 0\n" + 
				"4 1 0 0 0 1 1\n" + 
				"5 5 0 1 1 0 0\n" + 
				"6 5 0 0 1 0 1\n" + 
				"7 4 1 1 1 0 0\n" + 
				"8 2 1 0 1 0 1\n" + 
				"9 14 1 1 0 0 0\n" + 
				"10 5 0 1 1 1 0\n" + 
				"11 10 0 0 1 1 0\n" + 
				"12 4 1 1 0 0 1\n" + 
				"13 30 0 1 0 0 0\n" + 
				"14 1 1 0 0 1 1\n" + 
				"15 2 1 0 0 0 1\n" + 
				"16 6 1 0 0 1 0\n" + 
				"17 6 0 1 0 0 1\n" + 
				"18 25 1 0 0 0 0\n" + 
				"19 19 0 0 0 1 0\n" + 
				"20 1 1 0 1 1 0\n" + 
				"21 10 0 0 1 0 0\n" + 
				"22 2 1 1 1 0 1\n" + 
				"23 2 1 1 0 1 0\n"),
				random50("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 0 1 0 1 1\n" + 
				"1 3 0 1 1 0 0\n" + 
				"2 26 1 0 0 0 0\n" + 
				"3 1 0 0 0 1 1\n" + 
				"4 18 1 1 0 0 0\n" + 
				"5 5 1 0 1 1 0\n" + 
				"6 4 1 0 0 0 1\n" + 
				"7 14 0 0 0 1 0\n" + 
				"8 1 0 0 1 1 1\n" + 
				"9 2 1 1 1 0 0\n" + 
				"10 3 0 1 1 1 0\n" + 
				"11 4 1 0 0 1 1\n" + 
				"12 9 1 0 1 0 0\n" + 
				"13 1 1 1 1 1 0\n" + 
				"14 3 0 0 1 0 1\n" + 
				"15 8 0 0 0 0 1\n" + 
				"16 5 1 0 0 1 0\n" + 
				"17 5 1 1 0 1 0\n" + 
				"18 35 0 1 0 0 0\n" + 
				"19 15 0 1 0 1 0\n" + 
				"20 7 0 0 1 0 0\n" + 
				"21 19 0 0 1 0 0\n" + 
				"22 2 0 1 1 0 1\n" + 
				"23 3 1 1 0 0 1\n" + 
				"24 4 0 1 0 0 1\n"),
				random51("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 2 1 1 0 0 1\n" + 
				"1 20 0 0 0 1 0\n" + 
				"2 6 0 1 0 0 1\n" + 
				"3 6 1 0 0 0 1\n" + 
				"4 19 1 0 0 0 0\n" + 
				"5 11 1 1 0 0 0\n" + 
				"6 6 0 0 1 1 0\n" + 
				"7 4 0 0 0 1 1\n" + 
				"8 6 1 0 0 1 0\n" + 
				"9 8 0 1 1 0 0\n" + 
				"10 2 0 0 1 1 1\n" + 
				"11 6 1 1 1 0 0\n" + 
				"12 1 0 1 1 1 1\n" + 
				"13 3 0 1 1 1 0\n" + 
				"14 5 1 0 1 1 0\n" + 
				"15 43 0 1 0 0 0\n" + 
				"16 8 0 0 0 0 1\n" + 
				"17 14 0 1 0 1 0\n" + 
				"18 1 1 0 1 0 0\n" + 
				"19 2 1 1 1 0 1\n" + 
				"20 6 1 1 0 1 0\n" + 
				"21 2 0 1 0 1 1\n" + 
				"22 15 0 0 1 0 0\n" + 
				"23 1 1 1 1 0 0\n" + 
				"24 3 0 0 1 0 1\n"),
				random52("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 40 0 1 0 0 0\n" + 
				"1 3 0 0 0 1 1\n" + 
				"2 11 0 0 0 0 1\n" + 
				"3 2 1 1 1 1 0\n" + 
				"4 7 0 1 0 0 1\n" + 
				"5 4 0 1 1 1 0\n" + 
				"6 2 1 0 1 0 1\n" + 
				"7 7 1 1 0 1 0\n" + 
				"8 19 0 0 0 1 0\n" + 
				"9 2 1 1 0 1 1\n" + 
				"10 2 0 0 1 1 1\n" + 
				"11 7 1 0 1 0 0\n" + 
				"12 14 0 0 1 0 0\n" + 
				"13 4 1 1 0 0 1\n" + 
				"14 11 1 1 0 0 0\n" + 
				"15 21 1 0 0 0 0\n" + 
				"16 9 0 1 1 0 0\n" + 
				"17 6 1 1 1 0 0\n" + 
				"18 5 1 0 0 1 0\n" + 
				"19 3 0 0 1 1 0\n" + 
				"20 3 1 0 1 1 0\n" + 
				"21 1 0 1 0 1 1\n" + 
				"22 14 0 1 0 0 0\n" + 
				"23 3 1 0 0 0 1\n"),
				random53("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 6 1 1 0 1 0\n" + 
				"1 2 0 0 0 1 1\n" + 
				"2 23 0 0 0 1 0\n" + 
				"3 10 1 0 0 1 0\n" + 
				"4 1 0 0 1 0 1\n" + 
				"5 18 1 1 0 0 0\n" + 
				"6 31 0 1 0 0 0\n" + 
				"7 5 0 0 1 1 0\n" + 
				"8 10 0 0 0 0 1\n" + 
				"9 1 1 0 1 0 1\n" + 
				"10 3 1 1 1 0 0\n" + 
				"11 6 1 0 1 0 0\n" + 
				"12 16 0 1 0 1 0\n" + 
				"13 5 1 0 0 0 1\n" + 
				"14 1 1 0 1 1 0\n" + 
				"15 3 0 1 1 0 1\n" + 
				"16 2 1 1 0 0 1\n" + 
				"17 17 1 0 0 0 0\n" + 
				"18 17 0 0 1 0 0\n" + 
				"19 1 1 1 1 1 0\n" + 
				"20 2 1 1 1 0 1\n" + 
				"21 3 0 1 1 1 0\n" + 
				"22 8 0 1 1 0 0\n" + 
				"23 9 0 1 0 0 1\n"),
				random54("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 0 1 0 0 1\n" + 
				"1 1 1 1 1 1 1\n" + 
				"2 7 1 0 0 0 1\n" + 
				"3 8 1 1 0 1 0\n" + 
				"4 6 0 1 1 0 0\n" + 
				"5 1 1 1 1 0 1\n" + 
				"6 2 1 1 1 1 0\n" + 
				"7 1 0 0 0 1 1\n" + 
				"8 2 0 0 1 1 1\n" + 
				"9 5 1 1 0 0 1\n" + 
				"10 6 1 1 0 0 0\n" + 
				"11 14 0 0 1 0 0\n" + 
				"12 4 1 0 1 0 0\n" + 
				"13 3 0 1 0 1 1\n" + 
				"14 8 1 0 0 1 0\n" + 
				"15 41 0 1 0 0 0\n" + 
				"16 8 0 0 0 0 1\n" + 
				"17 1 1 0 1 0 1\n" + 
				"18 5 1 1 1 0 0\n" + 
				"19 3 0 1 1 0 1\n" + 
				"20 1 0 0 1 0 0\n" + 
				"21 20 0 0 0 1 0\n" + 
				"22 14 0 1 0 1 0\n" + 
				"23 27 1 0 0 0 0\n" + 
				"24 9 0 0 1 1 0\n"),
				random55("200 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 7 1 0 0 0 1\n" + 
				"1 4 1 0 1 1 0\n" + 
				"2 2 1 1 1 0 0\n" + 
				"3 1 1 1 1 1 0\n" + 
				"4 1 0 1 1 1 1\n" + 
				"5 6 1 0 0 1 0\n" + 
				"6 10 0 0 1 1 0\n" + 
				"7 6 1 0 1 0 0\n" + 
				"8 16 0 0 0 1 0\n" + 
				"9 4 0 1 0 0 1\n" + 
				"10 8 0 1 1 0 0\n" + 
				"11 11 0 1 0 1 0\n" + 
				"12 4 0 0 0 1 1\n" + 
				"13 24 1 0 0 0 0\n" + 
				"14 1 0 1 0 1 1\n" + 
				"15 36 0 1 0 0 0\n" + 
				"16 3 0 1 1 1 0\n" + 
				"17 5 1 1 0 1 0\n" + 
				"18 18 0 0 1 0 0\n" + 
				"19 15 1 1 0 0 0\n" + 
				"20 1 1 0 1 0 1\n" + 
				"21 2 0 1 1 0 1\n" + 
				"22 10 0 0 0 0 1\n" + 
				"23 4 1 1 0 0 1\n" + 
				"24 1 1 0 0 0 1\n"),
				random56("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 7 1 0 1 0 0\n" + 
				"1 9 1 0 0 1 0\n" + 
				"2 38 0 1 0 0 0\n" + 
				"3 14 0 1 0 1 0\n" + 
				"4 3 1 0 0 1 1\n" + 
				"5 16 1 1 0 0 0\n" + 
				"6 11 0 0 1 0 0\n" + 
				"7 4 1 1 1 1 0\n" + 
				"8 9 0 0 0 0 1\n" + 
				"9 10 0 1 1 0 0\n" + 
				"10 1 1 0 1 0 1\n" + 
				"11 9 1 1 0 1 0\n" + 
				"12 4 0 0 1 0 1\n" + 
				"13 2 1 1 0 0 1\n" + 
				"14 2 1 1 1 0 1\n" + 
				"15 6 0 0 1 1 0\n" + 
				"16 3 1 0 0 0 1\n" + 
				"17 1 0 0 0 1 1\n" + 
				"18 13 0 0 0 1 0\n" + 
				"19 18 1 0 0 0 0\n" + 
				"20 3 0 1 1 1 0\n" + 
				"21 5 1 0 1 1 0\n" + 
				"22 1 1 1 0 1 1\n" + 
				"23 1 1 1 1 0 0\n" + 
				"24 1 0 0 1 0 1\n" + 
				"25 9 0 1 0 0 0\n"),
				random57("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 0 1 0 1 1\n" + 
				"1 8 0 0 0 0 1\n" + 
				"2 39 0 1 0 0 0\n" + 
				"3 8 0 1 1 0 0\n" + 
				"4 6 1 0 1 0 1\n" + 
				"5 10 1 1 0 1 0\n" + 
				"6 3 0 1 1 0 1\n" + 
				"7 8 1 0 1 0 0\n" + 
				"8 12 0 0 1 0 0\n" + 
				"9 2 0 0 1 1 1\n" + 
				"10 3 1 1 1 1 0\n" + 
				"11 22 1 1 0 0 0\n" + 
				"12 8 1 0 0 1 0\n" + 
				"13 14 0 1 0 1 0\n" + 
				"14 2 1 1 1 0 0\n" + 
				"15 3 0 0 0 1 1\n" + 
				"16 4 0 1 0 0 1\n" + 
				"17 1 1 0 0 1 1\n" + 
				"18 2 1 1 0 0 1\n" + 
				"19 1 0 0 1 0 1\n" + 
				"20 2 0 0 1 1 0\n" + 
				"21 4 1 0 1 1 0\n" + 
				"22 14 0 0 0 0 0\n" + 
				"23 1 1 0 0 0 1\n" + 
				"24 4 0 1 1 0 0\n" + 
				"25 16 1 0 0 0 0\n"),
				random58("200 5 30\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 31 0 1 0 0 0\n" + 
				"1 14 0 1 1 0 0\n" + 
				"2 1 0 1 1 1 0\n" + 
				"3 4 1 0 1 1 0\n" + 
				"4 1 1 1 1 0 1\n" + 
				"5 1 1 0 0 1 1\n" + 
				"6 2 1 0 1 0 0\n" + 
				"7 2 1 1 1 1 0\n" + 
				"8 5 0 0 0 0 1\n" + 
				"9 1 0 0 1 1 1\n" + 
				"10 20 1 1 0 0 0\n" + 
				"11 3 0 0 1 0 1\n" + 
				"12 1 0 1 1 1 1\n" + 
				"13 1 1 1 0 1 1\n" + 
				"14 5 1 1 0 0 1\n" + 
				"15 3 1 1 1 0 0\n" + 
				"16 3 0 1 0 1 1\n" + 
				"17 1 1 1 1 1 1\n" + 
				"18 5 1 0 0 0 1\n" + 
				"19 1 1 0 1 0 0\n" + 
				"20 11 0 0 1 0 0\n" + 
				"21 6 1 1 0 1 0\n" + 
				"22 14 0 0 0 1 0\n" + 
				"23 1 0 0 0 0 0\n" + 
				"24 17 0 0 0 0 0\n" + 
				"25 1 0 0 0 0 0\n" + 
				"26 7 0 0 0 0 0\n" + 
				"27 23 1 0 0 0 0\n" + 
				"28 5 0 0 0 0 0\n" + 
				"29 10 0 0 0 0 0\n"),
				random59("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 6 0 0 1 0 1\n" + 
				"1 1 0 0 1 1 1\n" + 
				"2 40 0 1 0 0 0\n" + 
				"3 15 1 0 0 0 0\n" + 
				"4 10 1 0 0 1 0\n" + 
				"5 1 1 0 0 0 1\n" + 
				"6 1 0 1 0 1 1\n" + 
				"7 8 1 1 0 0 1\n" + 
				"8 22 1 1 0 0 0\n" + 
				"9 1 0 1 1 0 1\n" + 
				"10 8 0 1 0 0 1\n" + 
				"11 6 0 1 1 0 0\n" + 
				"12 3 1 0 0 1 1\n" + 
				"13 6 0 1 1 1 0\n" + 
				"14 1 1 1 0 1 1\n" + 
				"15 1 1 1 1 1 0\n" + 
				"16 1 1 0 1 0 1\n" + 
				"17 6 1 0 1 0 0\n" + 
				"18 6 1 0 1 1 0\n" + 
				"19 18 0 1 0 1 0\n" + 
				"20 11 0 0 0 1 0\n" + 
				"21 3 0 0 0 0 1\n" + 
				"22 13 0 0 1 0 0\n" + 
				"23 2 1 1 1 0 0\n" + 
				"24 5 0 0 1 0 0\n" + 
				"25 5 1 1 0 0 0\n"),
				random60("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 5 1 1 1 0 0\n" + 
				"1 22 1 0 0 0 0\n" + 
				"2 12 1 0 0 1 0\n" + 
				"3 3 0 1 0 1 1\n" + 
				"4 3 0 0 0 1 1\n" + 
				"5 3 0 1 1 1 0\n" + 
				"6 2 1 0 1 0 1\n" + 
				"7 7 0 1 0 0 1\n" + 
				"8 2 1 0 1 1 0\n" + 
				"9 1 1 0 0 0 1\n" + 
				"10 9 0 0 0 1 0\n" + 
				"11 5 1 0 1 0 0\n" + 
				"12 6 0 0 1 1 0\n" + 
				"13 19 1 1 0 0 0\n" + 
				"14 1 1 1 1 1 0\n" + 
				"15 12 0 1 1 0 0\n" + 
				"16 2 0 1 1 0 1\n" + 
				"17 3 0 0 0 0 1\n" + 
				"18 4 1 1 0 0 1\n" + 
				"19 14 0 0 1 0 0\n" + 
				"20 8 1 1 0 1 0\n" + 
				"21 3 0 0 1 0 1\n" + 
				"22 34 0 1 0 0 0\n" + 
				"23 15 0 1 0 1 0\n" + 
				"24 3 1 0 0 1 1\n" + 
				"25 2 1 1 0 0 1\n"),
				random61("200 5 27\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 2 1 0 1 1 0\n" + 
				"1 1 1 0 1 0 1\n" + 
				"2 12 0 1 0 1 0\n" + 
				"3 21 0 0 0 1 0\n" + 
				"4 1 1 1 1 0 1\n" + 
				"5 6 1 1 0 1 0\n" + 
				"6 3 0 0 0 1 1\n" + 
				"7 8 1 0 0 1 0\n" + 
				"8 2 0 1 1 1 0\n" + 
				"9 17 1 0 0 0 0\n" + 
				"10 3 0 1 0 1 1\n" + 
				"11 13 0 1 1 0 0\n" + 
				"12 6 0 0 1 0 1\n" + 
				"13 2 0 1 1 0 1\n" + 
				"14 1 0 0 1 1 1\n" + 
				"15 4 1 0 1 0 0\n" + 
				"16 3 1 1 1 0 0\n" + 
				"17 3 1 1 0 0 1\n" + 
				"18 4 1 0 0 0 1\n" + 
				"19 8 0 0 1 1 0\n" + 
				"20 12 0 0 1 0 0\n" + 
				"21 24 1 1 0 0 0\n" + 
				"22 5 0 1 0 0 1\n" + 
				"23 6 0 0 0 0 0\n" + 
				"24 3 1 1 1 1 0\n" + 
				"25 29 0 1 0 0 0\n" + 
				"26 1 1 0 0 0 0\n"),
				random62("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 1 1 0 0 1\n" + 
				"1 2 1 0 1 0 1\n" + 
				"2 36 0 1 0 0 0\n" + 
				"3 8 1 1 0 1 0\n" + 
				"4 1 0 1 1 0 1\n" + 
				"5 5 1 1 1 0 0\n" + 
				"6 15 1 0 0 0 0\n" + 
				"7 24 1 1 0 0 0\n" + 
				"8 16 0 0 1 0 0\n" + 
				"9 4 1 0 1 1 0\n" + 
				"10 6 0 1 0 0 1\n" + 
				"11 6 0 0 0 0 1\n" + 
				"12 7 0 1 1 0 0\n" + 
				"13 6 0 0 0 1 1\n" + 
				"14 6 1 0 0 1 0\n" + 
				"15 15 0 1 0 1 0\n" + 
				"16 2 1 0 0 1 1\n" + 
				"17 1 0 0 1 1 1\n" + 
				"18 3 1 0 0 0 1\n" + 
				"19 6 1 0 1 0 0\n" + 
				"20 12 0 0 0 1 0\n" + 
				"21 3 0 0 1 0 1\n" + 
				"22 1 1 1 1 1 0\n" + 
				"23 3 1 1 0 1 0\n" + 
				"24 2 0 1 1 0 0\n" + 
				"25 7 0 0 1 0 0\n"),
				random63("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 5 0 1 1 1 0\n" + 
				"1 2 0 0 1 1 1\n" + 
				"2 1 1 1 1 0 1\n" + 
				"3 3 1 0 1 0 1\n" + 
				"4 3 1 1 0 1 0\n" + 
				"5 15 0 1 0 1 0\n" + 
				"6 11 0 1 1 0 0\n" + 
				"7 6 1 0 0 0 1\n" + 
				"8 3 0 1 0 1 1\n" + 
				"9 4 0 1 1 0 1\n" + 
				"10 5 1 1 1 0 0\n" + 
				"11 7 1 0 0 1 0\n" + 
				"12 5 0 1 0 0 1\n" + 
				"13 23 0 0 0 1 0\n" + 
				"14 4 1 0 1 1 0\n" + 
				"15 36 0 1 0 0 0\n" + 
				"16 1 0 0 0 1 1\n" + 
				"17 10 0 0 1 0 0\n" + 
				"18 1 0 1 1 1 1\n" + 
				"19 7 1 0 1 0 0\n" + 
				"20 21 1 0 0 0 0\n" + 
				"21 3 1 1 1 1 0\n" + 
				"22 4 1 1 0 0 1\n" + 
				"23 14 1 0 0 0 0\n" + 
				"24 5 0 0 0 0 1\n" + 
				"25 1 1 0 0 0 0\n"),
				random64("200 5 24\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 1 0 1 0 1\n" + 
				"1 3 1 1 1 1 0\n" + 
				"2 4 0 0 1 0 1\n" + 
				"3 9 0 0 1 0 0\n" + 
				"4 4 0 1 1 0 1\n" + 
				"5 8 1 0 0 1 0\n" + 
				"6 9 0 0 1 1 0\n" + 
				"7 25 1 1 0 0 0\n" + 
				"8 17 1 0 0 0 0\n" + 
				"9 6 0 0 0 0 1\n" + 
				"10 1 1 0 1 1 0\n" + 
				"11 21 0 0 0 1 0\n" + 
				"12 11 1 0 1 0 0\n" + 
				"13 17 0 1 0 1 0\n" + 
				"14 1 1 0 0 0 1\n" + 
				"15 4 1 1 0 0 1\n" + 
				"16 1 0 0 1 1 1\n" + 
				"17 3 0 1 1 1 0\n" + 
				"18 6 1 1 0 1 0\n" + 
				"19 10 0 1 0 0 1\n" + 
				"20 28 0 1 0 0 0\n" + 
				"21 1 1 1 1 0 1\n" + 
				"22 1 1 1 0 1 1\n" + 
				"23 7 0 1 1 0 0\n"),
				random65("200 5 27\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 2 1 1 1 0 1\n" + 
				"1 7 1 1 0 1 0\n" + 
				"2 20 1 1 0 0 0\n" + 
				"3 2 0 1 1 0 1\n" + 
				"4 6 1 1 1 0 0\n" + 
				"5 22 0 0 0 1 0\n" + 
				"6 13 1 0 0 0 0\n" + 
				"7 12 0 1 0 1 0\n" + 
				"8 1 1 0 1 1 1\n" + 
				"9 1 1 1 1 1 0\n" + 
				"10 37 0 1 0 0 0\n" + 
				"11 8 1 0 0 1 0\n" + 
				"12 13 0 0 1 0 0\n" + 
				"13 6 0 1 0 0 1\n" + 
				"14 8 0 1 1 0 0\n" + 
				"15 6 1 0 1 0 0\n" + 
				"16 2 1 1 0 0 1\n" + 
				"17 3 0 0 0 1 1\n" + 
				"18 4 1 0 1 1 0\n" + 
				"19 2 0 1 1 1 0\n" + 
				"20 1 0 0 1 1 1\n" + 
				"21 1 1 1 0 1 1\n" + 
				"22 7 1 0 0 0 1\n" + 
				"23 3 0 0 0 0 1\n" + 
				"24 4 0 0 0 0 0\n" + 
				"25 4 0 0 1 0 0\n" + 
				"26 5 0 0 1 0 0\n"),
				random66("200 5 27\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 11 1 0 1 0 0\n" + 
				"1 9 1 0 0 1 0\n" + 
				"2 36 0 1 0 0 0\n" + 
				"3 20 0 1 0 1 0\n" + 
				"4 2 1 0 0 1 1\n" + 
				"5 21 1 1 0 0 0\n" + 
				"6 9 0 0 1 0 0\n" + 
				"7 4 1 1 1 1 0\n" + 
				"8 7 0 0 0 0 1\n" + 
				"9 9 0 1 1 0 0\n" + 
				"10 2 1 0 1 0 1\n" + 
				"11 10 1 1 0 1 0\n" + 
				"12 3 0 0 1 0 1\n" + 
				"13 4 1 1 0 0 1\n" + 
				"14 2 1 1 1 0 1\n" + 
				"15 7 0 0 1 1 0\n" + 
				"16 3 1 0 0 0 1\n" + 
				"17 1 0 0 0 1 1\n" + 
				"18 6 0 0 0 1 0\n" + 
				"19 12 1 0 0 0 0\n" + 
				"20 3 0 1 1 1 0\n" + 
				"21 5 1 0 1 1 0\n" + 
				"22 1 0 1 1 0 1\n" + 
				"23 2 1 1 0 1 1\n" + 
				"24 2 1 1 1 0 0\n" + 
				"25 1 0 0 1 0 0\n" + 
				"26 8 0 1 0 0 0\n"),
				random67("200 5 27\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 7 0 1 1 1 0\n" + 
				"1 1 0 0 1 0 1\n" + 
				"2 20 1 1 0 0 0\n" + 
				"3 6 1 1 0 1 0\n" + 
				"4 12 1 0 0 1 0\n" + 
				"5 18 1 0 0 0 0\n" + 
				"6 6 1 0 1 0 0\n" + 
				"7 13 0 1 1 0 0\n" + 
				"8 29 0 1 0 0 0\n" + 
				"9 7 1 1 1 0 0\n" + 
				"10 1 1 0 1 1 0\n" + 
				"11 6 0 0 1 0 0\n" + 
				"12 1 1 1 1 0 1\n" + 
				"13 4 0 0 1 1 0\n" + 
				"14 2 0 0 0 1 1\n" + 
				"15 18 0 1 0 1 0\n" + 
				"16 6 0 0 0 0 1\n" + 
				"17 1 1 0 1 0 1\n" + 
				"18 7 1 0 0 0 1\n" + 
				"19 3 0 1 1 0 1\n" + 
				"20 2 0 1 1 1 1\n" + 
				"21 13 0 0 0 1 0\n" + 
				"22 1 0 1 0 1 1\n" + 
				"23 3 1 1 1 0 0\n" + 
				"24 5 0 1 0 0 1\n" + 
				"25 1 1 0 0 0 0\n" + 
				"26 7 1 0 0 0 0\n"),
				random68("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 9 0 0 0 0 1\n" + 
				"1 1 0 1 1 1 1\n" + 
				"2 4 0 1 0 0 1\n" + 
				"3 7 1 0 0 1 0\n" + 
				"4 1 0 1 0 1 1\n" + 
				"5 5 0 1 1 1 0\n" + 
				"6 11 1 0 0 0 0\n" + 
				"7 11 0 1 1 0 0\n" + 
				"8 1 1 0 0 1 1\n" + 
				"9 16 0 0 0 1 0\n" + 
				"10 23 1 1 0 0 0\n" + 
				"11 6 1 0 0 0 1\n" + 
				"12 6 0 0 1 1 0\n" + 
				"13 31 0 1 0 0 0\n" + 
				"14 14 0 1 0 1 0\n" + 
				"15 9 0 0 1 0 0\n" + 
				"16 4 1 0 1 0 0\n" + 
				"17 5 1 0 1 1 0\n" + 
				"18 3 0 0 0 1 1\n" + 
				"19 3 1 1 1 1 0\n" + 
				"20 3 1 1 0 0 1\n" + 
				"21 10 1 1 1 0 0\n" + 
				"22 9 1 1 0 0 0\n" + 
				"23 1 0 1 1 0 1\n" + 
				"24 1 0 0 1 0 1\n" + 
				"25 6 1 0 0 0 1\n"),
				random69("200 5 30\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 9 0 1 1 0 0\n" + 
				"1 5 1 0 0 0 1\n" + 
				"2 9 1 0 0 1 0\n" + 
				"3 2 1 1 0 1 1\n" + 
				"4 31 0 1 0 0 0\n" + 
				"5 8 0 0 1 0 0\n" + 
				"6 2 0 1 0 1 1\n" + 
				"7 19 0 1 0 1 0\n" + 
				"8 1 1 1 1 1 0\n" + 
				"9 5 0 0 1 1 0\n" + 
				"10 28 1 1 0 0 0\n" + 
				"11 5 1 1 0 0 1\n" + 
				"12 1 0 0 1 1 1\n" + 
				"13 7 1 0 1 0 0\n" + 
				"14 3 1 0 1 1 0\n" + 
				"15 6 0 0 0 0 1\n" + 
				"16 1 1 1 1 0 1\n" + 
				"17 8 1 1 1 0 0\n" + 
				"18 2 0 0 0 1 1\n" + 
				"19 7 0 1 1 1 0\n" + 
				"20 2 0 0 1 0 1\n" + 
				"21 12 1 0 0 0 0\n" + 
				"22 4 0 1 0 0 1\n" + 
				"23 1 1 0 1 0 0\n" + 
				"24 1 0 1 1 0 0\n" + 
				"25 6 1 0 0 1 0\n" + 
				"26 1 1 0 0 0 0\n" + 
				"27 1 0 0 0 0 0\n" + 
				"28 1 0 0 0 0 0\n" + 
				"29 12 0 0 0 0 0\n"),
				random70("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 4 0 0 1 0 1\n" + 
				"1 10 0 1 1 0 0\n" + 
				"2 3 1 0 1 1 0\n" + 
				"3 6 0 1 0 0 1\n" + 
				"4 8 0 1 1 1 0\n" + 
				"5 3 1 0 1 0 1\n" + 
				"6 1 0 1 0 1 1\n" + 
				"7 5 0 0 0 0 1\n" + 
				"8 7 0 0 1 1 0\n" + 
				"9 2 0 1 1 0 1\n" + 
				"10 8 0 0 0 1 0\n" + 
				"11 17 0 1 0 1 0\n" + 
				"12 31 0 1 0 0 0\n" + 
				"13 2 1 1 1 1 0\n" + 
				"14 9 0 0 1 0 0\n" + 
				"15 5 1 0 0 0 1\n" + 
				"16 3 0 0 0 1 1\n" + 
				"17 15 1 1 0 0 0\n" + 
				"18 7 1 0 0 1 0\n" + 
				"19 14 1 1 0 1 0\n" + 
				"20 21 1 0 0 0 0\n" + 
				"21 1 1 1 1 0 1\n" + 
				"22 4 1 1 0 0 1\n" + 
				"23 2 1 0 0 1 1\n" + 
				"24 9 1 0 1 0 0\n" + 
				"25 3 1 1 1 0 0\n"),
				random71("200 5 28\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 3 0 0 1 1 0\n" + 
				"1 8 1 0 0 1 0\n" + 
				"2 3 1 0 0 0 1\n" + 
				"3 1 1 0 1 1 1\n" + 
				"4 2 1 1 1 0 1\n" + 
				"5 5 1 1 1 1 0\n" + 
				"6 5 1 0 1 1 0\n" + 
				"7 1 1 0 0 1 1\n" + 
				"8 1 0 1 0 1 1\n" + 
				"9 29 0 1 0 0 0\n" + 
				"10 20 0 0 0 1 0\n" + 
				"11 1 1 1 0 1 1\n" + 
				"12 1 0 0 0 0 1\n" + 
				"13 2 0 0 1 0 1\n" + 
				"14 14 0 1 1 0 0\n" + 
				"15 4 1 0 1 0 0\n" + 
				"16 9 0 1 0 0 1\n" + 
				"17 11 0 0 1 0 0\n" + 
				"18 3 0 1 1 1 0\n" + 
				"19 5 1 1 1 0 0\n" + 
				"20 9 1 1 0 0 1\n" + 
				"21 17 1 0 0 0 0\n" + 
				"22 1 1 1 1 1 1\n" + 
				"23 1 1 0 1 0 0\n" + 
				"24 13 0 1 0 0 0\n" + 
				"25 2 0 1 0 0 0\n" + 
				"26 7 0 0 0 0 0\n" + 
				"27 22 0 0 0 0 0\n"),
				random72("200 5 27\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 11 0 1 1 0 0\n" + 
				"1 2 0 0 1 1 1\n" + 
				"2 5 0 0 1 0 0\n" + 
				"3 8 1 1 1 0 0\n" + 
				"4 4 1 1 0 0 1\n" + 
				"5 10 1 0 0 1 0\n" + 
				"6 4 0 1 0 0 1\n" + 
				"7 2 1 1 1 1 0\n" + 
				"8 2 0 1 1 0 1\n" + 
				"9 4 0 0 1 1 0\n" + 
				"10 16 0 1 0 1 0\n" + 
				"11 36 0 1 0 0 0\n" + 
				"12 1 1 0 1 0 1\n" + 
				"13 3 1 0 0 1 1\n" + 
				"14 13 1 0 0 0 0\n" + 
				"15 2 0 1 0 1 1\n" + 
				"16 10 1 1 0 1 0\n" + 
				"17 1 1 1 1 0 1\n" + 
				"18 3 1 0 1 1 0\n" + 
				"19 6 0 0 0 0 1\n" + 
				"20 24 1 1 0 0 0\n" + 
				"21 8 0 0 0 1 0\n" + 
				"22 6 0 1 1 1 0\n" + 
				"23 2 1 0 0 0 1\n" + 
				"24 4 0 0 1 0 1\n" + 
				"25 4 0 0 0 0 0\n" + 
				"26 9 1 0 1 0 0\n"),
				random73("200 5 27\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 4 0 1 1 1 0\n" + 
				"1 4 1 0 1 1 0\n" + 
				"2 1 0 0 1 0 1\n" + 
				"3 11 1 1 0 1 0\n" + 
				"4 4 0 1 0 1 1\n" + 
				"5 3 0 0 0 0 1\n" + 
				"6 3 1 0 0 1 1\n" + 
				"7 3 1 1 1 1 0\n" + 
				"8 10 0 1 1 0 0\n" + 
				"9 4 1 0 0 0 1\n" + 
				"10 3 0 0 1 1 1\n" + 
				"11 1 0 1 1 1 1\n" + 
				"12 4 0 0 1 1 0\n" + 
				"13 15 0 1 0 1 0\n" + 
				"14 13 0 0 1 0 0\n" + 
				"15 23 1 1 0 0 0\n" + 
				"16 4 1 0 1 0 0\n" + 
				"17 3 1 1 0 0 1\n" + 
				"18 2 0 1 1 0 1\n" + 
				"19 6 1 1 1 0 0\n" + 
				"20 17 1 0 0 0 0\n" + 
				"21 9 0 1 0 0 1\n" + 
				"22 1 1 0 1 0 1\n" + 
				"23 29 0 1 0 0 0\n" + 
				"24 10 1 0 0 1 0\n" + 
				"25 12 0 0 0 0 0\n" + 
				"26 1 1 0 0 0 0\n"),
				random74("200 5 28\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 6 1 0 1 1 0\n" + 
				"1 1 1 0 1 1 1\n" + 
				"2 9 0 1 1 0 0\n" + 
				"3 1 0 0 1 1 1\n" + 
				"4 2 1 1 1 1 0\n" + 
				"5 1 1 1 1 1 1\n" + 
				"6 10 0 0 1 0 0\n" + 
				"7 2 1 0 0 1 1\n" + 
				"8 18 0 1 0 1 0\n" + 
				"9 6 0 1 0 0 1\n" + 
				"10 4 0 0 0 1 1\n" + 
				"11 1 0 1 1 1 1\n" + 
				"12 7 1 1 1 0 0\n" + 
				"13 3 0 1 1 1 0\n" + 
				"14 4 1 0 0 0 1\n" + 
				"15 15 1 0 0 0 0\n" + 
				"16 2 1 1 0 1 1\n" + 
				"17 13 1 1 0 0 0\n" + 
				"18 8 1 0 1 0 0\n" + 
				"19 6 1 0 0 0 0\n" + 
				"20 36 0 1 0 0 0\n" + 
				"21 2 1 0 1 0 1\n" + 
				"22 21 1 1 0 0 0\n" + 
				"23 5 0 0 1 0 0\n" + 
				"24 2 0 1 0 0 1\n" + 
				"25 1 0 0 0 0 0\n" + 
				"26 8 0 0 0 0 0\n" + 
				"27 6 0 0 0 0 0\n"),
				random75("200 5 26\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 5 0 1 0 0 1\n" + 
				"1 16 0 1 0 1 0\n" + 
				"2 10 0 1 1 0 0\n" + 
				"3 8 0 0 1 0 0\n" + 
				"4 1 1 1 1 1 0\n" + 
				"5 15 1 0 0 0 0\n" + 
				"6 3 0 0 0 1 1\n" + 
				"7 8 0 1 1 1 0\n" + 
				"8 5 1 0 1 0 0\n" + 
				"9 1 1 0 1 1 1\n" + 
				"10 5 0 0 1 1 0\n" + 
				"11 9 1 1 0 1 0\n" + 
				"12 2 0 0 1 0 1\n" + 
				"13 25 1 1 0 0 0\n" + 
				"14 9 1 1 1 0 0\n" + 
				"15 28 0 1 0 0 0\n" + 
				"16 1 0 0 1 1 1\n" + 
				"17 3 0 1 0 1 1\n" + 
				"18 1 0 1 1 1 1\n" + 
				"19 2 0 1 1 0 1\n" + 
				"20 11 0 0 0 0 1\n" + 
				"21 4 1 1 0 0 1\n" + 
				"22 4 1 0 0 0 0\n" + 
				"23 11 0 0 0 1 0\n" + 
				"24 5 1 0 1 0 0\n" + 
				"25 8 1 0 0 0 0\n"),
				ReginPuget0("100 5 18\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 5 1 1 0 0 1\n" + 
				"1 3 1 1 0 1 0\n" + 
				"2 7 1 1 1 0 0\n" + 
				"3 1 0 1 1 1 0\n" + 
				"4 10 1 1 0 0 0\n" + 
				"5 2 1 0 0 0 1\n" + 
				"6 11 1 0 0 1 0\n" + 
				"7 5 1 0 1 0 0\n" + 
				"8 4 0 1 0 0 1\n" + 
				"9 6 0 1 0 1 0\n" + 
				"10 12 0 1 1 0 0\n" + 
				"11 1 0 0 1 0 1\n" + 
				"12 1 0 0 1 1 0\n" + 
				"13 5 1 0 0 0 0\n" + 
				"14 9 0 1 0 0 0\n" + 
				"15 5 0 0 0 0 1\n" + 
				"16 12 0 0 0 1 0\n" + 
				"17 1 0 0 1 0 0\n"),
				ReginPuget1("100 5 22\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 6 1 0 0 1 0\n" + 
				"1 10 1 1 1 0 0\n" + 
				"2 2 1 1 0 0 1\n" + 
				"3 2 0 1 1 0 0\n" + 
				"4 8 0 0 0 1 0\n" + 
				"5 15 0 1 0 0 0\n" + 
				"6 1 0 1 1 1 0\n" + 
				"7 5 0 0 1 1 0\n" + 
				"8 2 1 0 1 1 0\n" + 
				"9 3 0 0 1 0 0\n" + 
				"10 2 1 0 1 0 0\n" + 
				"11 1 1 1 1 0 1\n" + 
				"12 8 0 1 0 1 0\n" + 
				"13 3 1 0 0 1 1\n" + 
				"14 10 1 0 0 0 0\n" + 
				"15 4 0 1 0 0 1\n" + 
				"16 4 0 0 0 0 1\n" + 
				"17 2 1 0 0 0 1\n" + 
				"18 4 1 1 0 0 0\n" + 
				"19 6 1 1 0 1 0\n" + 
				"20 1 1 0 1 0 1\n" + 
				"21 1 1 1 1 1 1\n"),
				ReginPuget2("100 5 22\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 13 1 0 0 0 0\n" + 
				"1 8 0 0 0 1 0\n" + 
				"2 7 0 1 0 0 0\n" + 
				"3 1 1 0 0 1 0\n" + 
				"4 12 0 0 1 0 0\n" + 
				"5 5 0 1 0 1 0\n" + 
				"6 5 0 0 1 1 0\n" + 
				"7 6 0 1 1 0 0\n" + 
				"8 3 1 0 0 0 1\n" + 
				"9 12 1 1 0 0 0\n" + 
				"10 8 1 1 0 1 0\n" + 
				"11 2 1 0 0 1 1\n" + 
				"12 2 1 1 1 0 0\n" + 
				"13 1 0 1 0 1 1\n" + 
				"14 4 1 0 1 0 0\n" + 
				"15 4 0 1 0 0 1\n" + 
				"16 1 1 1 0 1 1\n" + 
				"17 2 1 0 1 1 0\n" + 
				"18 1 0 0 0 0 1\n" + 
				"19 1 1 1 1 1 0\n" + 
				"20 1 1 1 0 0 1\n" + 
				"21 1 0 1 1 1 0\n"),
				ReginPuget3("100 5 25\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 7 1 0 0 1 0\n" + 
				"1 11 1 1 0 0 0\n" + 
				"2 1 0 1 1 1 1\n" + 
				"3 3 1 0 1 0 0\n" + 
				"4 15 0 1 0 0 0\n" + 
				"5 2 1 0 1 1 0\n" + 
				"6 8 0 1 0 1 0\n" + 
				"7 5 0 0 1 0 0\n" + 
				"8 3 0 0 0 1 0\n" + 
				"9 4 0 1 1 1 0\n" + 
				"10 5 1 0 0 0 0\n" + 
				"11 2 1 1 1 0 1\n" + 
				"12 6 0 1 1 0 0\n" + 
				"13 2 0 0 1 0 1\n" + 
				"14 2 0 1 0 0 1\n" + 
				"15 4 1 1 1 1 0\n" + 
				"16 3 1 0 0 0 1\n" + 
				"17 5 1 1 0 1 0\n" + 
				"18 2 1 1 1 0 0\n" + 
				"19 4 1 1 0 0 1\n" + 
				"20 1 1 0 0 1 1\n" + 
				"21 1 1 1 0 1 1\n" + 
				"22 1 0 1 0 1 1\n" + 
				"23 1 0 1 1 0 1\n" + 
				"24 2 0 0 0 0 1\n"),
				ReginPuget4("100 5 23\n" + 
				"1 2 1 2 1\n" + 
				"2 3 3 5 5\n" + 
				"0 2 0 0 0 1 1\n" + 
				"1 2 0 0 1 0 1\n" + 
				"2 5 0 1 1 1 0\n" + 
				"3 4 0 0 0 1 0\n" + 
				"4 4 0 1 0 1 0\n" + 
				"5 1 1 1 0 0 1\n" + 
				"6 3 1 1 1 0 1\n" + 
				"7 4 0 0 1 0 0\n" + 
				"8 19 0 1 0 0 0\n" + 
				"9 7 1 1 0 1 0\n" + 
				"10 10 1 0 0 0 0\n" + 
				"11 1 0 0 1 1 0\n" + 
				"12 5 1 1 1 1 0\n" + 
				"13 2 1 0 1 1 0\n" + 
				"14 6 1 1 0 0 0\n" + 
				"15 4 1 1 1 0 0\n" + 
				"16 8 1 0 0 1 0\n" + 
				"17 1 1 0 0 0 1\n" + 
				"18 4 0 1 1 0 0\n" + 
				"19 2 0 0 0 0 1\n" + 
				"20 4 0 1 0 0 1\n" + 
				"21 1 1 1 0 1 1\n" + 
				"22 1 0 1 1 0 1\n"),
;

		final String source;

		CSPLib(String source) {
			this.source = source;
		}

		String source() {
			return source;
		}
	}


}

