package eventnet.debug;

//import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
//import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
//import java.io.Console;
import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedList;

import eventnet.RFacade;
import eventnet.Transformation;
import eventnet.rate.RateCalculator;
//import eventnet.events.EdgeEvent;
import eventnet.statistics.Statistic;

import com.martiansoftware.jsap.*;
//import net.sourceforge.sizeof.*;

/**
 * 
 * To be able to debug in a java environment and get more meaningful error messages the 
 * {@link RFacade} defines the method {@link RFacade#serialize} that serializes a Transformation
 * object. This Transformation object can then be loaded and executed with the main method defined
 * below.
 * 
 * @author Michael Zinsmaier, University of Konstanz
 *
 */
public class debugStart {

	/**
	 * loads and executes a serialized Transformation object.
	 * 
	 * @param args contains a single argument, that defines the path of the serialized Transformation
	 */
	public static void main(String[] args) {
		try {
			SimpleJSAP jsap = new SimpleJSAP(
					"eventNet starter",
					"Starts eventNet solver at a specified stage",
					new Parameter[]
							{
								new FlaggedOption( "transformation", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 't', "transform",
					                    "The transformation file." ),
			                    new FlaggedOption( "ijStats", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'i', "ij",
					                    "The file of ijStatistics." ),
			                    new FlaggedOption( "eventRateStats", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'e', "event",
					                    "The file of event rate statistics." ),
								new Switch( "loadstats", JSAP.NO_SHORTFLAG, "loadstats")
								//new Switch( "sgd", JSAP.NO_SHORTFLAG, "sgd")
							}
					);
			
			JSAPResult config = jsap.parse(args);
			
			//if (config.getBoolean("sgd")) {
			//	optimizeSGD(config.getString("transformation"));
			//} else 
			if(config.getBoolean("loadstats")) {
				loadStatistics(config.getString("eventRateStats"), config.getString("ijStats"));
			} else {
				loadTransformation(config.getString("transformation"));
			}
		
			//write2File(ret);
			
			System.out.println("done");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (JSAPException e) {
			e.printStackTrace();
		}

	}
	
	/*
	private static void optimizeSGD(String filename) throws FileNotFoundException,IOException,ClassNotFoundException {
		System.out.println("SGD OPTIMIZATION BRANCH");
		
		FileInputStream file;
		file = new FileInputStream( filename );
		ObjectInputStream o = new ObjectInputStream( file );
		System.out.println("Read in serialized transformation object: " + filename);
	
		Transformation sim = (Transformation) o.readObject();
		o.close();
		
		//System.out.println("Print basic info...");
		//printTransInfo(sim);
//		writeEventsToFile(sim, 40);
//		System.out.println();
		
		Date startTime = new Date(System.currentTimeMillis());
		System.out.println("Start Transformation: " + startTime);
		sim.transformConditionalTypeLive();
		System.out.println("Start Transformation: " + startTime);
		Date finishTime = new Date(System.currentTimeMillis());
		System.out.println("Transformation Finished: " + finishTime);
		
		//long totalSeconds = (finishTime.getTime() - startTime.getTime())/1000;
		//System.out.println("Time transpired: " + totalSeconds + " " + (double)(totalSeconds/60f));
		
	}
	*/
	
	private static void loadStatistics(String eventStatsFile, String ijStatsFile) {
		//TODO: fix this to read in events
		RateCalculator rc = new RateCalculator(new HashSet<String>(), false);
		
		//String initSize = SizeOf.humanReadable(SizeOf.deepSizeOf(rc));
		rc.loadStats(eventStatsFile, ijStatsFile);
		//String finSize = SizeOf.humanReadable(SizeOf.deepSizeOf(rc));
		
		//System.out.println(initSize + " => " + finSize);
		
		try{
			Thread.sleep(10000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		rc.executeMaximization(false);
	}
	
	private static void loadTransformation(String filename) throws FileNotFoundException,IOException,ClassNotFoundException {
		FileInputStream file;
		file = new FileInputStream( filename );
		ObjectInputStream o = new ObjectInputStream( file );
		System.out.println("Read in serialized transformation object: " + filename);
	
		Transformation sim = (Transformation) o.readObject();
		o.close();
		
		//System.out.println("Print basic info...");
		printTransInfo(sim);
//		writeEventsToFile(sim, 40);
//		System.out.println();
		
		Date startTime = new Date(System.currentTimeMillis());
		System.out.println("Start Transformation: " + startTime);
		double[][] ret = sim.transformConditionalType(false, true);
		Date finishTime = new Date(System.currentTimeMillis());
		System.out.println("Transformation Started: " + startTime);
		System.out.println("Transformation Finished: " + finishTime);
		
		long totalSeconds = (finishTime.getTime() - startTime.getTime())/1000;
		System.out.println("Time transpired: " + totalSeconds + " " + (double)(totalSeconds/60f));
	}
	
	
	private static void printTransInfo(Transformation trans){
		
		/*System.out.println("TRANS EVENT COUNT: " + trans.getEventCount());
		LinkedList<EdgeEvent> edgeEvents = trans.getEdgeEvents();
		System.out.println("EDGE EVENTS: " + edgeEvents.size());
		for(int i=0; i<10; i++){
			EdgeEvent e = edgeEvents.get(i);
			System.out.println( e.getEventTime() + ", " + e.getSource() + ", " + e.getTarget() + ", " + e.getEventType() + ", " + e.getEventWeight());
			
		}
		*/
		
		Statistic[] stats = trans.getStatisticts();
		System.out.println("Statistics: " + stats.length);
		for(int i=0; i<stats.length; i++){
			Statistic stat = stats[i];
			System.out.println("STAT: " + stat.getClass());
			
			
		}
		
		/*String[] attributes = trans.getAttributes();
		System.out.println("ATTRIBUTES: " + attributes.length);
		for(int i=0; i<attributes.length; i++){
			System.out.println("attribute: " + attributes[i]);
		}
		
		int[] tHalfs = trans.getTHalfs();
		System.out.println("T-HALFS: " + tHalfs.length);
		for(int i=0; i<tHalfs.length; i++){
			System.out.println("tHalfs: " + tHalfs[i]);
		}
		*/
	}
	
	/*
	private static void writeEventsToFile(Transformation trans, int max) throws IOException{
		
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("eventData.csv"));
		LinkedList<EdgeEvent> edgeEvents = trans.getEdgeEvents();
		
		HashMap<String,Integer> actorMap = new HashMap<String, Integer>();
		int actorIndex = 1;
		
		
		long normalizeIndex = 0;
		
		//for(int i=0; i<edgeEvents.size(); i++){
		for(int i=0; i<max; i++){
			EdgeEvent e = edgeEvents.get(i);
			//System.out.println(e.getEventTime() + ", " + e.getSource() + ", " + e.getTarget() + ", " + e.getEventType() + ", " + e.getEventWeight());
			//bufferedWriter.write(e.getEventTime() + ", " + e.getSource() + ", " + e.getTarget() + ", " + e.getEventType() + ", " + e.getEventWeight());
			//bufferedWriter.write(e.getEventTime() + "\t" + e.getSource() + "\t" + e.getTarget() );
			
			Integer sourceId = actorMap.get(e.getSource());
			if(sourceId==null){
				sourceId = actorIndex;
				actorMap.put(e.getSource(), actorIndex);
				actorIndex++;
			}
			Integer targetId = actorMap.get(e.getTarget());
			if(targetId==null){
				targetId = actorIndex;
				actorMap.put(e.getTarget(), actorIndex);
				actorIndex++;
			}
			
			if(i==0){
				normalizeIndex = e.getEventTime();
			}
			
			
			bufferedWriter.write((e.getEventTime()-normalizeIndex) + "\t" + sourceId + "\t" + targetId );
			bufferedWriter.newLine();
		}
		
		bufferedWriter.flush();
		bufferedWriter.close();
		
		
		System.out.println("MAP SIZE: " + actorMap.size());
		
	}
	*/
	
	/*
	private static void write2File(double[][] data) throws IOException {
		
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("transformationResults.data"));
      
		for (double[] ds : data) {
			for (double d : ds) {
				bufferedWriter.write(""+d + '\t');
			}
			bufferedWriter.newLine();
		}
		
		bufferedWriter.flush();
		bufferedWriter.close();
	}
	*/
}
