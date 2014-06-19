package eventnet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
//import java.util.Iterator;
import java.util.LinkedList;


//import y.base.Edge;
import y.base.Graph;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixIO;
import org.ejml.ops.NormOps;

import eventnet.events.EdgeEvent;
import eventnet.helper.Pair;
//import eventnet.rate.EdgeMarker;
import eventnet.rate.RateCalculator;
//import eventnet.rate.LiveRateCalculator;
import eventnet.statistics.Statistic;

/**
 * Note: serialization is only possible before runSimulation, updateNetwork or prepareNetwork has been called
 * because the graph object has to be recreated from scratch
 * 
 * holds the main logic of event processing, data handling etc. 
 * In other words a Transformation object is used to put all the required
 * parts together and then execute it.
 * 
 * @author Michael Zinsmaier, University of Konstanz
 *
 */
public class Transformation implements Serializable{
	
	/** default serial id */
	private static final long serialVersionUID = 1L;
	/** defines the start time of the process each event with smaller {@link eventnet.events.Event#m_eventTime eventTime} will be executed without updating
	 * any statistics to prepare the network.*/
	private double m_startTime;
	/** counts the stochastic events */
	private int m_stochasticEventsCount;
	/** starting at start time events will be grouped together in m_timeRange intervals. E.g. if the time steps of the data
	 * is in seconds, a value of 60 would lead to a evaluation stepping in minutes */
	private double m_timeRange;
	/** the graph, that is used to calculate the statistics on it */
	private transient EventNetwork m_graph;
	/** all statistics that should be evaluated for edges */
	private Statistic[] m_edgeStatistics;
	/** the eventProcessor used to translate event types to attribute changes */
	private EventProcessor m_eventProcessor;
	/** linked list containing all edge events added to the process. */
	private LinkedList<EdgeEvent> m_edgeEvents = new LinkedList<EdgeEvent>();
	/** hashset containing all actors in the network. */
	private HashSet<String> m_uniqueActors = new HashSet<String>();
	
	/** horrific hack becuase java doesn't have complex return types */
	double m_llikelihood;
	int m_nevents;

	//just for serialization
	/** needed to recreate a graph object after serialization */
	private final String[] s_attributes;
	/** needed to recreate a graph object after serialization */
	private final double[] s_T_halfs;
	
	
	/**
	 * creates a new Transformation object that can be prepared with additional methods before finally being executed.
	 * 
	 * @param edgeStatistics {@link Transformation#m_edgeStatistics}
	 * @param eventProcessor {@link Transformation#m_eventProcessor}
	 * @param attributes names of the attributes that should be processed
	 * @param t_halfs the half times of the attributes that should be processed. t_half <= 0 indicates no decay
	 * @param timeRange {@link Transformation#m_timeRange}
	 * @param startTime {@link Transformation#m_startTime}
	 */
	public Transformation(Statistic[] edgeStatistics,
			EventProcessor eventProcessor, String[] attributes, double[] T_halfs, double timeRange, double startTime) {
		
		m_graph = new EventNetwork(attributes, T_halfs);
		m_timeRange = timeRange;
		m_edgeStatistics = edgeStatistics;
		m_eventProcessor = eventProcessor;
		m_startTime = startTime;
		
		//just for serialization
		//NOT TRUE I NEED THESE NOW
		s_attributes = attributes;
		s_T_halfs = T_halfs;
	}
	
	public LinkedList<EdgeEvent> getEdgeEvents(){
		return m_edgeEvents;
	}
	
	public int getEventCount(){
		return m_stochasticEventsCount;
	}
	
	public Statistic[] getStatisticts(){
		return m_edgeStatistics;
	}
	
	public String[] getAttributes(){	
		return s_attributes;
	}
	
	public double[] getTHalfs(){	
		return s_T_halfs;
	}
	
	
	/**
	 * adds a additional bundle of edge events to the simulation. Order will be enforced
	 * by the simulation object.
	 *  
	 * @param events the events that should be added
	 * @param stochasticEventCount number of events that trigger statistic estimation
	 */
	public void addEdgeEvents(LinkedList<EdgeEvent> events, HashSet<String> uniqueActors, int stochasticEventCount) {
		//should be O(1) because of the LinkedList
		m_uniqueActors.addAll(uniqueActors);
		m_graph.addActors(uniqueActors);
		m_edgeEvents.addAll(events);
		m_stochasticEventsCount += stochasticEventCount;
	}	
	
	/**
	 * executes the main loop of estimation. Processes all events added so far under the 
	 * conditions set via the constructor...
	 * 
	 * @return a double array of statistic results one for each event
	 */
	public double[][] transformConditionalType(boolean calculateWeights, boolean verbose) {
		
		//back up events and redo graph at each iteration
		double[][] ret = new double[m_stochasticEventsCount][m_edgeStatistics.length];
		
		//TODO: FIX THIS SHIT
		//int numParameters = m_edgeStatistics.length + 1;
		//numParameters = 8;
		
		int numParameters = m_edgeStatistics.length;
		
		DenseMatrix64F parameters = new DenseMatrix64F(numParameters,1);
		DenseMatrix64F ejmlChange = parameters.copy();
		
		
		int maxIter = 100;
		int iter = 1;
		double P2Threshold = 1e-8;
		double P2Norm;
		double ll_diff;
		double last_llikelihood = Double.NEGATIVE_INFINITY;
		
		do {
			if(verbose) 
				System.out.println("ITERATION " + iter);
			//parameters = new DenseMatrix64F(8,1);
			
			//reset statistics
			for(Statistic stat : m_edgeStatistics) {
				stat.reset();
			}
			
			m_graph = new EventNetwork(s_attributes, s_T_halfs);
			m_graph.addActors(m_uniqueActors);
			
			LinkedList<EdgeEvent> edgeEvents = (LinkedList<EdgeEvent>) m_edgeEvents.clone();
	
			DenseMatrix64F nextParams = iterateTransform(edgeEvents, parameters, calculateWeights, verbose);
			
			CommonOps.sub(nextParams, parameters, ejmlChange);
			parameters = nextParams;
			
			//euclidean distance
			P2Norm = NormOps.normP2(ejmlChange);
			ll_diff = Math.abs(m_llikelihood - last_llikelihood);
            last_llikelihood = m_llikelihood;

			//let's see some norms!
			if(verbose)
			    System.out.println("|dX| (P2): " + P2Norm);
                System.out.println("|dLL|: " + ll_diff);
			
			iter += 1;
//		} while (iter < maxIter && P2Norm > P2Threshold);
	    } while (iter < maxIter && ll_diff > P2Threshold);
		
		if(!verbose)
			System.out.println(paramReport(m_edgeStatistics, parameters));
		
		return ret;
	}
	
	private DenseMatrix64F iterateTransform(LinkedList<EdgeEvent> edgeEvents, DenseMatrix64F parameters, boolean calculateWeights, boolean verbose) {
		//ensure proper ordering
		Collections.sort(edgeEvents);
		prepareNetwork(edgeEvents);
		m_nevents = edgeEvents.size();
		double lastTime = - m_timeRange;
		double currentTime = 0.0; //int should be sufficient. Its enough for 68 years in seconds
		//double lastTime = 0;	//tracks the timestamp of the last submitted event set
		boolean firstEventSet = true;
		
		double timeStepCarryover = 0;
		
		EdgeEvent e;
		
		LinkedList<EdgeEvent> currentEvents = new LinkedList<EdgeEvent>();
		
		RateCalculator rateCalculator = new RateCalculator(m_uniqueActors, calculateWeights);
		rateCalculator.setParameters(parameters);
		
		while ((e = edgeEvents.poll()) != null) {			
			//check if the network should go one step further in time
			//(TV: check if new event advances past the current time range)
			//i.e. sets a new day
			if (((e.getEventTime() - m_startTime) - currentTime) >= m_timeRange) {
				
				//we should be loading the time of the next event for the proper wait-time loading
				//currentTime = (int)(e.getEventTime() - m_startTime) + 1;
				
				if(firstEventSet) {
					//timeStepCarryover = -0.5 / currentEvents.size();
					timeStepCarryover = -m_timeRange / (currentEvents.size() + 1);
					firstEventSet = false;
				}
				
				double baseDeltaT = timeStepCarryover + currentTime - lastTime - 1;
				//timeStepCarryover = 1.0 / (currentEvents.size() + 1);
				//timeStepCarryover = 0.5 / currentEvents.size();
				
				//proper uniformity assumption under variable length time binning
				//even works with 0 length bins!
				//							...bitches
				baseDeltaT = timeStepCarryover + currentTime - lastTime - m_timeRange;
				timeStepCarryover = m_timeRange / (currentEvents.size() + 1);
				
				lastTime = currentTime;
				
				rateCalculator.updateDayEventsList(m_edgeStatistics, currentEvents, currentTime, baseDeltaT, m_timeRange, m_graph);
				
				//call updateNetwork for all event that belong to the current time range
				updateNetwork(currentEvents, currentTime, m_graph);
				
				//currentTime = (int)(e.getEventTime() - m_startTime) + 1;
				currentTime = e.getEventTime() - m_startTime;
				currentEvents = new LinkedList<EdgeEvent>();
			}
			
			currentEvents.add(e);
			rateCalculator.addEdgeMarker(e.getSource(), e.getTarget());
		}
		
		//process the final day
		double baseDeltaT = timeStepCarryover + currentTime - lastTime - 1;
		baseDeltaT = timeStepCarryover + currentTime - lastTime - m_timeRange;
		rateCalculator.updateDayEventsList(m_edgeStatistics, currentEvents, currentTime, baseDeltaT, m_timeRange, m_graph);
		updateNetwork(currentEvents, currentTime, m_graph);
		
		//System.out.println("unique actors " + rateCalculator.getUniqueActorsSize());
		//System.out.println("Number of edges: " + rateCalculator.getEdgeMarkerSet().size());
		//rateCalculator.executeMaximization();
		//rateCalculator.executeMaximization(true);
		
		DenseMatrix64F outParams = rateCalculator.solveForParameters();
		DenseMatrix64F stdErrs = rateCalculator.getStandardErrors();
		m_llikelihood = rateCalculator.getLogLikelihood();
		
		if(verbose)
			System.out.println(paramSEReport(m_edgeStatistics, outParams, stdErrs, m_llikelihood));
		
		if(calculateWeights) {
			DenseMatrix64F weightParams = rateCalculator.getWeightParameters();
			double variance = rateCalculator.getWeightVariance(weightParams);
			DenseMatrix64F weightCovariance = rateCalculator.getWeightCovarianceMatrix(variance);
			DenseMatrix64F weightSE = new DenseMatrix64F(weightParams);
			CommonOps.extractDiag(weightCovariance, weightSE);
			weightSE = RateCalculator.matrixSqrt(weightSE);
			
			System.out.println("WP: ");
			MatrixIO.print(System.out, weightParams, "%9.8f");
			System.out.println("WSE: ");
			MatrixIO.print(System.out, weightSE, "%9.8f");
		}
		
		//Runtime runtime = Runtime.getRuntime();
		//long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
		//System.out.println("TOTAL MEMORY USED: " + memoryUsed + ", " + (memoryUsed/1024) + "kb" + ", " + (memoryUsed/1048576 + "mb")) ;
		return outParams;
	}
	
	private String paramSEReport(Statistic[] stats, DenseMatrix64F paramsMat, DenseMatrix64F sesMat, double llikelihood) {
		String res = "Param\tSE\tStat\n";
		//res = "";
		double[] params = paramsMat.getData();
		double[] ses = sesMat.getData();
		
		for(int i = 0; i < stats.length; i++) {
		    String signif = "";
		    if (Math.abs(params[i]) > 1.96 * ses[i]) signif = "* ";
		    if (Math.abs(params[i]) > 2.57 * ses[i]) signif = "** ";
		    
			//res += stats[i].getName() + "\t" + params[i] + "\t" + ses[i] + "\n";
			res += params[i] + "\t" + ses[i] + "\t" + signif + stats[i].getName() + "\n";
			//res += params[i] + "\t" + ses[i] + "\t";
		}
		
		double k = stats.length;
		double n = m_nevents;
		
		double aic = 2 * k - 2 * llikelihood;
		double bic = k * Math.log(n) - 2 * llikelihood;
		double aicc = aic + (2 * k * (k+1)) / (n - k - 1);
		res += "LL:\t" + llikelihood + "\n";
		res += "AIC:\t" + aic + "\n";
		res += "AICc:\t" + aicc + "\n";
		res += "BIC:\t" + bic + "\n";
		
		return res;
	}
	
	private String paramReport(Statistic[] stats, DenseMatrix64F paramsMat) {
		String line1 = "";
		String line2 = "";
		
		for(int i = 0; i < stats.length; i++) {
			line1 += stats[i].getName() + "\t";
			line2 += paramsMat.get(i) + "\t";
		}
		
		String res = line1 + "\n" + line2 + "\n";
		res = line2;
		
		return res;
	}
	
	//Fossil Code!
	//put here by god to provide evidence that this project has been extensively worked on
	//but it actually hasn't
	
	/**
	 * executes the main loop of estimation. Processes all events added so far under the 
	 * conditions set via the constructor...
	 * Works on the fly, updating parameter estimates without committing to memory or HD.
	 * Side effect: sorts event list (by time?)
	 * 
	 * @return a double array of statistic results one for each event
	 */
	//public double[][] transformConditionalTypeLive() {
	
	/*
	public void transformConditionalTypeLive() {
		//ensure proper ordering
		Collections.sort(m_edgeEvents);
		prepareNetwork(m_edgeEvents);
		//int currentTime = 0; //int should be sufficient. Its enough for 68 years in seconds
		//int timePeriod = 0; //using this to keep track of how many new days we've had
		//EdgeEvent e;
		
		//int retIndex = 0;
		//double[][] ret = new double[m_stochasticEventsCount][m_edgeStatistics.length];
		//LinkedList<EdgeEvent> currentEvents = new LinkedList<EdgeEvent>();
		
		//RateCalculator rateCalculator = new RateCalculator();
		double[] parameters = new double[8];
		LiveRateCalculator rateCalculator = new LiveRateCalculator(parameters);
		
		System.out.println("ENTER MAIN LOOP " );
		
		//iterate some number of times or till predetermined endpoint
		int i = 0;
		while( i < 30 ) {
			//System.out.println(m_edgeEvents.size());
			iterateTransformation(rateCalculator);
			
			parameters = rateCalculator.getUpdatedParameters();
			
			System.out.println("New Parameters: " + java.util.Arrays.toString(parameters));
			
			rateCalculator.setParameters(parameters);
			
			i++;
		}
		
		System.out.println("unique actors " + rateCalculator.getUniqueActorsSize());
		System.out.println("Number of edges: " + rateCalculator.getEdgeMarkerSet().size());
		
		//rateCalculator.executeMaximization();
		rateCalculator.executeMaximization(true);
		
		
		Runtime runtime = Runtime.getRuntime();
		long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
		System.out.println("TOTAL MEMORY USED: " + memoryUsed + ", " + (memoryUsed/1024) + "kb" + ", " + (memoryUsed/1048576 + "mb")) ;
		//return ret;
	}
	*/
	

	/**
	 * Iterate once through the events in a transformation.
	 *
	 * @param rateCalculator the rate calculator
	 */
	
	/*
	public void iterateTransformation(LiveRateCalculator rateCalculator) {
		int currentTime = 0; //int should be sufficient. Its enough for 68 years in seconds
		int timePeriod = 0; //using this to keep track of how many new days we've had
		EdgeEvent e;
		LinkedList<EdgeEvent> currentEvents = new LinkedList<EdgeEvent>();
		
		Iterator<EdgeEvent> eventIterator = m_edgeEvents.iterator();
		rateCalculator.clearDayEvents();
		
		//start main loop
		//(TV: for every event in our database)
		while (eventIterator.hasNext()) {
			e = eventIterator.next();
			
			//check if the network should go one step further in time
			//(TV: check if new event advances past the current time range)
			//i.e. sets a new day
			if ((((e.getEventTime() - m_startTime)- currentTime) / m_timeRange) >= 1) {
				
				
				rateCalculator.updateDayEventsList(m_edgeStatistics, currentEvents, currentTime, timePeriod, m_graph);
				
				//call updateNetwork for all event that belong to the current time range
				updateNetwork(currentEvents, currentTime);
				
				currentTime = (int)(e.getEventTime() - m_startTime);
				currentEvents = new LinkedList<EdgeEvent>();
				timePeriod += 1;
			}
			
			currentEvents.add(e);
			rateCalculator.addEdgeMarker(e.getSource(), e.getTarget());
		}
	}
	*/

		
	/**
	 * called each time the main loop steps one unit of {@link Transformation#m_timeRange time} further.
	 * Updates the graph with the current events. 
	 * <br>
	 * To clarify the parameters assume timeRange equals one day. All events that happen during Monday
	 * should be included in "events" and currentTime will be set to Monday morning.
	 * 
	 * @param events all events belonging to the current time frame
	 * @param currentTime the point in time when all events of the current time frame happened.
	 */
	private void updateNetwork(LinkedList<EdgeEvent> events, double currentTime, EventNetwork graph) {
		
		//process edge events
		for (EdgeEvent edgeEvent : events) {
			LinkedList<Pair<String, Double>> changes = m_eventProcessor.getChanges(edgeEvent.getEventType(), graph, edgeEvent.getSource(), edgeEvent.getTarget(), currentTime, edgeEvent.getEventWeight());		
			graph.updateEdgeAttributes(edgeEvent.getSource(), edgeEvent.getTarget(), changes, currentTime);
		}
	}	
		
		
	/**
	 * method has a side effect on m_edgeEvents, the pre simulation events are cut
	 * 
	 * processes all events that happen before startTime to prepare the network
	 * 
	 * @param edgeEvents
	 */
	private void prepareNetwork(LinkedList<EdgeEvent> edgeEvents) {
		LinkedList<EdgeEvent> preSimEvents = new LinkedList<EdgeEvent>();
		
		double eTime;
		//double lastTime = m_timeRange;
		while ((eTime = edgeEvents.peek().getEventTime()) < m_startTime) {
			preSimEvents.add(edgeEvents.poll());
			//lastTime = eTime;
		}
		
		updateNetwork(preSimEvents, 0, m_graph);
		
		//return m_startTime - lastTime;
	}


	/**
	 * the graph object must be created manually 
	 * @param ois
	 * @throws IOException
	 */
    private void readObject( ObjectInputStream ois ) throws IOException 
	  { 
	    try 
	    { 
	      ois.defaultReadObject(); 
	      Graph m_graph2 = new Graph();
	      m_graph = new EventNetwork(s_attributes, s_T_halfs);
	      //TODO: This is incredibly hidden and obscure. Very bad. Need a better way to tell event network about actors, must make it more central.
	      m_graph.addActors(m_uniqueActors);
	    } 
	    catch ( ClassNotFoundException e ) 
	    { 
	      throw new IOException( "No class found. HELP!!" ); 
	    } 
	  } 

}
