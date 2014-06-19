package eventnet.debug;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.lang.IllegalStateException;

import com.martiansoftware.jsap.*;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.random.RandomDataImpl;
import org.apache.commons.math3.random.Well44497b;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import eventnet.EventNetwork.Direction;
import eventnet.helper.Pair;
import eventnet.statistics.ConstantStat;
import eventnet.statistics.NormDirectRelationStat;
import eventnet.statistics.Statistic;
import eventnet.statistics.TwoPathsStat;
import eventnet.statistics.ButtsTwoPathsStat;
import eventnet.EventNetwork;


public class simulator {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		//prep test file
		FileWriter fstreamExpVals = new FileWriter("expVals.txt", false);
		fstreamExpVals.close();
		
		String[] tableArgs = {"-a",
							"F:\\Users\\avashevko\\Dropbox\\Dropbox\\MTS Relational Event Network Modeling\\technical meeting\\relevent testing\\roptim\\Rattrs.txt",
							"-s",
							"F:\\Users\\avashevko\\Documents\\SONIC\\Eventnet\\SLChat\\stats.txt",
							"-t",
							"F:\\Users\\avashevko\\Dropbox\\Dropbox\\MTS Relational Event Network Modeling\\technical meeting\\relevent testing\\roptim\\Rtypes.txt",
							"-e",
							"out.txt",
							//"--verbose",
							};
		
		for(int i = 0; i < 100; i++) {
			System.out.print(i);
			
			double[] parameters = {-2.3, 1.5, 0.05, -0.07, 0.06, -0.08};
			//double[] parameters = {-2.3, 1.5};
			//double[] parameters = {0};
			
			simulatorRun(parameters);
		
			System.out.print("|\t");
			startFromTable.main(tableArgs);
		}
	}
	
	private static void simulatorRun(double[] parameters) throws Exception {
		String statFile = "F:\\Users\\avashevko\\Documents\\SONIC\\Eventnet\\SLChat\\stats.txt";

		Statistic[] stats = parseStatistics(statFile);
		
		//should be reading this stuff from a config file
		
		String[] attributes = {"chat"};
		double[] t_Halfs = {0.0};
		EventNetwork graph = new EventNetwork(attributes, t_Halfs);

		//let's make up some actors
		int size = 15;
		
		HashSet<String> actors = new HashSet<String>();
		for( int i = 0; i < size; i++) {
			actors.add(Integer.toString(i));
		}
		graph.addActors(actors);
		

		// Create file 
		FileWriter fstream = new FileWriter("out.txt");
		FileWriter fstreamExpVals = new FileWriter("expVals.txt", true);
		BufferedWriter out = new BufferedWriter(fstream);
		BufferedWriter expOut = new BufferedWriter(fstreamExpVals);
		
		//we go for the best prng
		RandomDataImpl unifGen = new RandomDataImpl(new Well44497b());
		
		int numEvents = 1000;
		String src = "-1";
		String trg = "-1";
		double wgt;
		double currentTime = 0.0;
		
		for(int i = 0; i < numEvents; i++) {
			//let's get some lambdas up
			//DenseMatrix64F[] statVals = new DenseMatrix64F[stats.length];
			DenseMatrix64F temp;
			DenseMatrix64F lambdas = new DenseMatrix64F(size, size);
			
			for(int statIndex = 0; statIndex < stats.length; statIndex++) {
				temp = stats[statIndex].getAllValues(currentTime, graph);
				CommonOps.addEquals(lambdas, parameters[statIndex], temp);
				
				//statVals[statIndex] = stats[statIndex].getAllValues(currentTime, graph);
				//CommonOps.scale(parameters[statIndex], statVals[statIndex]);
			}
			
			//set diagonal to 0 to prevent self loops
			//exponentiate everything, and might as well get some waiting times too
			//double minWaiting = 0.0;
			boolean first = true;
			//ExponentialDistribution expD;
			//DenseMatrix64F waitTimes = new DenseMatrix64F(size,size);
			
			double runningSum = 0;
			for(int row = 0; row < size; row++) {
				for(int col = 0; col < size; col++) {
					double val = lambdas.get(row, col);
					//val = Math.exp(val);
					if(row == col) {
						val = 0.0;
					} else {
						val = FastMath.exp(val);
					}
					
					lambdas.set(row, col, val);
					
					runningSum += val;
				}
			}
			
			//ExponentialDistribution expD = new ExponentialDistribution(1 / runningSum);
			//UniformRealDistribution unifD = new UniformRealDistribution(0.0, runningSum);
			
			//UniformRealDistribution invTranSample = new UniformRealDistribution();
			
			//standard java rng is periodic
			
			if(runningSum <= 0.0)
				System.out.println("runningSum le 0");
			
			if(Double.isNaN(runningSum))
				System.out.println("runningSum NaN");
			
			if(Double.isInfinite(runningSum))
				System.out.println("runningSum Infinite");
				
			
			//double minWaiting = expD.sample();
			//double unifSample = invTranSample.sample();
			double unifSample = unifGen.nextUniform(0.0, 1.0);
			//double minWaiting = (-Math.log(unifSample) / runningSum );
			double minWaiting = (-FastMath.log(unifSample) / runningSum );
			//double multinomialSelector = unifD.sample();
			double multinomialSelector = unifGen.nextUniform(0.0, runningSum);
			
			expOut.write(unifSample + "\t" + minWaiting + "\n");
			//expOut.write(runningSum + "\n");
			
			runningSum = 0.0;
			boolean multinomFound = false;
			for(int srcIndex = 0; srcIndex < size; srcIndex++) {
				for( int trgIndex = 0; trgIndex < size; trgIndex++) {
					if( multinomFound )
						continue;
					
					//avoid self loops
					if( srcIndex == trgIndex ) {
						continue;
					}
					
					int lambdaRow = graph.getNodeIndex(Integer.toString(srcIndex));
					int lambdaCol = graph.getNodeIndex(Integer.toString(trgIndex));
					
					double val = lambdas.get(lambdaRow, lambdaCol);
					
					if( runningSum + val > multinomialSelector ) {
						src = Integer.toString(srcIndex);
						trg = Integer.toString(trgIndex);
						multinomFound = true;
					} else {
						runningSum += val;
					}
				}
			}
			
			currentTime += minWaiting;
			
			wgt = 1.0;
			
			LinkedList<Pair<String, Double>> changes = new LinkedList<Pair<String, Double>>();
			changes.add(new Pair<String, Double>("chat", wgt));
			
			graph.updateEdgeAttributes(src, trg, changes, currentTime);
			
			//System.out.println("Event " + i + " " + currentTime + ": " + src + "->" + trg + " (" + minWaiting + ")");
			out.write(src + "\t" + trg + "\t" + currentTime + "\tchat\t1\tTRUE\n");
			
			if(src.equalsIgnoreCase(trg)) {
				System.out.println("src is trg");
				throw new IllegalStateException("src is trg");
			}
		}
		
		out.close();
		expOut.close();
	}
	
	
	
	private static Statistic[] parseStatistics(String statsFile) throws FileNotFoundException {
		//TODO: Make this shit actually parse a file
		
		LinkedList<Statistic> statsList = new LinkedList<Statistic>();
		//statsList.add(new ConstantStat());
		statsList.add(new NormDirectRelationStat("chat", Direction.OUT));
		statsList.add(new NormDirectRelationStat("chat", Direction.IN));
		//statsList.add(new ButtsTwoPathsStat(Direction.OUT, "chat", Direction.OUT, "chat"));
		//statsList.add(new ButtsTwoPathsStat(Direction.OUT, "chat", Direction.IN, "chat"));
		//statsList.add(new ButtsTwoPathsStat(Direction.IN, "chat", Direction.OUT, "chat"));
		//statsList.add(new ButtsTwoPathsStat(Direction.IN, "chat", Direction.IN, "chat"));
//		statsList.add(new TwoPathsStat(Direction.OUT, "chat", Direction.OUT, "chat"));
//		statsList.add(new TwoPathsStat(Direction.OUT, "chat", Direction.IN, "chat"));
//		statsList.add(new TwoPathsStat(Direction.IN, "chat", Direction.OUT, "chat"));
//		statsList.add(new TwoPathsStat(Direction.IN, "chat", Direction.IN, "chat"));
		
		Statistic[] stats = new Statistic[statsList.size()];
		statsList.toArray(stats);
		
		//Statistic[] stats = new Statistic[9];
		//stats = new Statistic[2];
		//stats = new Statistic[1];
		
		//stats[0] = new ConstantStat();
		
		//testcase1
		//stats[0] = new NormDirectRelationStat("chat", Direction.OUT);					//frpsndsnd
		//stats[1] = new NormDirectRelationStat("chat", Direction.IN);					//frprec
		//stats[1] = new TwoPathsStat(Direction.OUT, "chat", Direction.OUT, "chat");	//otp?
		//stats[1] = new TwoPathsStat(Direction.OUT, "chat", Direction.IN, "chat");		//osp?
		//stats[1] = new TwoPathsStat(Direction.IN, "chat", Direction.OUT, "chat");		//isp?
		//stats[1] = new TwoPathsStat(Direction.IN, "chat", Direction.IN, "chat");		//itp?
		
		return stats;
	}


}
