package eventnet.rate;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
//import java.util.Arrays.*;
import java.util.Random;

//import org.rosuda.JRI.REXP;
//import org.rosuda.JRI.Rengine;
//import org.jblas.DoubleMatrix;
//import org.jblas.Solve;
//import Jama.Matrix;


import eventnet.EventNetwork;
import eventnet.events.EdgeEvent;
import eventnet.statistics.Statistic;

public class LiveRateCalculator {
	
	private HashSet<EdgeMarker> _edgeMarkerSet = new HashSet<EdgeMarker>();
	private HashSet<String> _uniqueActors = new HashSet<String>();
	private ArrayList<DayEvents> _dayEventsList = new ArrayList<DayEvents>();
	
	//TODO: variable seed
	private Random _rng = new Random(0);
	
	private double[] _parameters;
	private double _learningRate;
	private double _SGDProbability;
	private double[] _currentGradient;
	
	/**
	 * Instantiates a new live rate calculator.
	 *
	 * @param initialParameters the initial parameters
	 */
	public LiveRateCalculator(double[] initialParameters) {
		setParameters(initialParameters);
		_learningRate = 0.01;
		_SGDProbability = 0.1;
	}
	
	public void clearDayEvents() {
		_dayEventsList = new ArrayList<DayEvents>();
	}
	
	/**
	 * Sets the parameters, and resets the operating gradient.
	 *
	 * @param newParameters the new parameters
	 */
	public void setParameters (double[] newParameters) {
		//System.out.println(java.util.Arrays.toString(_currentGradient));
		
		_parameters = newParameters.clone();
		_currentGradient = new double[_parameters.length];
	}
	
	/**
	 * Updates working parameters using gradient descent (ascent) and returns their new values. Does not change current working parameters.
	 *
	 * @return the new parameters per the gradient ascent method
	 */
	public double[] getUpdatedParameters () {
		double[] newParameters = new double[_parameters.length];
		for (int i = 0; i < _parameters.length; i++) {
			newParameters[i] = _parameters[i] + _learningRate * _currentGradient[i];
		}
		
		return newParameters;
	}
	
	/**
	 * Adds an edge marker to our edge tracker set.
	 *
	 * @param source the source node
	 * @param target the target node
	 */
	public void addEdgeMarker(String source, String target){
		_edgeMarkerSet.add(new EdgeMarker(source, target));
		_uniqueActors.add(source);
		_uniqueActors.add(target);
	}
	
	/**
	 * Gets the unique actors size.
	 *
	 * @return the unique actors size
	 */
	public int getUniqueActorsSize(){
		return _uniqueActors.size();
	}
	
	/**
	 * Gets the edge marker set.
	 *
	 * @return the edge marker set
	 */
	public HashSet<EdgeMarker> getEdgeMarkerSet(){
		return _edgeMarkerSet;
	}
	
	
	/**
	 * Calculate the event and full graph statistics, updating the gradient and day events list.
	 *
	 * @param weightStats the weight stats
	 * @param events the events list
	 * @param currentTime the current time
	 * @param timePeriod the time period
	 * @param g the graph
	 */
	public void updateDayEventsList(Statistic[] weightStats, LinkedList<EdgeEvent> events, int currentTime, int timePeriod, EventNetwork g){
		//System.out.println(java.util.Arrays.toString(_currentGradient));
		
		double deltaT = 1.0;
		
		//System.out.println("updateDayEventsList <>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + currentTime);
		
		double[] eventRateStats;
		DayEvents dayEvents = new DayEvents();
		dayEvents.set_timePeriod(timePeriod);
		//dayEvents.set_timePeriod(currentTime);
		
		//boolean doCalculations = _rng.nextDouble() < _SGDProbability;
		
		//INSERT STOCHASTICITY EITHER HERE OR IN A PARAMETER TO THIS FUNCTION
		if(_rng.nextDouble() < _SGDProbability) {
			for(int i=0; i<events.size(); i++){
				eventRateStats = calculateRateStats(weightStats, events.get(i).getSource(), events.get(i).getTarget(), currentTime, g );
				//System.out.println("event: " + events.get(i).getSource() + " -> " + events.get(i).getTarget()+ ": " + eventRateStats[0] + " " + eventRateStats[1]);
				dayEvents.addEventRateStats(eventRateStats);
				
				//update gradient
				for(int j = 0; j < eventRateStats.length; j++) {
					_currentGradient[j] += eventRateStats[j];
				}
				//add constant statistic
				_currentGradient[eventRateStats.length] += 1.0;
			}
			
			ArrayList<double[]> ijRateStats = getAll_i_j_RateStatistics(weightStats, currentTime, g );
			dayEvents.set_ijRateStats(ijRateStats);
			
			int possiblePermutations = _uniqueActors.size() * (_uniqueActors.size()-1);
			int ijDyadsWithNoEdges = possiblePermutations - ijRateStats.size();
			dayEvents.set_ijDyadsWithNoEdges(ijDyadsWithNoEdges);
			
			//update gradient with ijStats
			//update with existing stats:
			for(double[] ijStat: ijRateStats) {
				double summand = 0.0;
				//build exponential term
				for(int i = 0; i < ijStat.length; i++) {
					summand += ijStat[i] * _parameters[i];
				}
				//constant statistic
				summand += 1.0 * _parameters[_parameters.length - 1];
				
				summand = Math.exp(summand);
				
				//add to gradient
				for(int i = 0; i < ijStat.length; i++) {
					_currentGradient[i] -= deltaT * ijStat[i] * summand;
				}
				//constant statistic
				_currentGradient[_currentGradient.length - 1] -= deltaT * 1.0 * summand;
			}
			
			//update gradient with empty edge statistics:
			//build exponential term, assuming only the constant statistic is nonzero
			double summand = Math.exp(_parameters[_parameters.length - 1]);
			
			//add only constant statistic to gradient, once for each edge
			_currentGradient[_currentGradient.length - 1] -= ijDyadsWithNoEdges * deltaT * 1.0 * summand;
		}
		
		
		
//		System.out.println("unique actors: " + _uniqueActors.size());
//		System.out.println("possiblePermutations: " + possiblePermutations); 
//		System.out.println("i_j stats: " + ijRateStats.size());
//		System.out.println("ijDyadsWithNoEdges: " + ijDyadsWithNoEdges);
		
		_dayEventsList.add(dayEvents);
		
	}
	
	/**
	 * Executes a Newton Raphson optimization on the day event data. Can potentially become hot-pluggable with alternative optimization methods.
	 *
	 * @param persistData set to true to persist the day event data to hard drive
	 */
	public void executeMaximization(boolean persistData){
		System.out.println("Execute Maximization >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		
		if(persistData == true) {
			persistData();
		}
		
		//double[] parameters = optimizeNR();
		double[] parameters = _parameters;
		System.out.println("Final Parameters: " + java.util.Arrays.toString(parameters));
		
//		Rengine re=REngineManager.getInstance().getREngine();
//		executeNewtonRhapson(re);
//		re.end();
		
	}
	
	/**
	 * Execute newton rhapson method in R. Don't even consider using this function, it is broken.
	 *
	 * @param re R Engine
	 */
	/*
	private void executeNewtonRhapsonR(Rengine re){
		
		System.out.println("Execute Newton-Rhapson in R >>>>>>>>>>>>>>>>>>>>>>>>");
		REXP  rexp = re.eval("source('rateNR_4.R')");
		System.out.println("NewtonRhapson Results >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		System.out.println("REXP: " + rexp);
	
	}
	 */
	
	/**
	 * Runs a Newton Raphson optimization on this rate calculator's day event data, starting from the 0 vector.
	 *
	 * @return the optimized parameters
	 */
	/*
	private double[] optimizeNR() {
		//TODO: this line of code is stupid, this shit should be a parameter in the object
		int size = 1 + _dayEventsList.get(0).getEventRateStatsList().get(0).length;
		
		
		double[] params = new double[size];
		System.out.println(java.util.Arrays.toString(params));
		
		for(int i = 0; i < 20; i++){
			System.out.println(java.util.Arrays.toString(params));
			params = iterateNR(params);
		}
		
		return params;
	}
	*/
	
	/**
	 * Newton Raphson iteration, given parameters.
	 *
	 * @param params the current parameters
	 * @return the next iteration of parameters
	 */
	
	/*
	private double[] iterateNR(double[] params) {
		//CONSTANT STATISTIC LOCATED IN PARAMETER <size - 1>
		int size = params.length;
		
		//pray these are initialized to 0
		double[] gradient = new double[size];
		double[][] hessian = new double[size][size];
		
		//build NR equations
		int count = 1;
		for(DayEvents dayEvents : _dayEventsList) {
			//System.out.println(">>>>NEW DAY");
			
			//////////////////////////////////////////////
			//remember to include the constant statistics!
			//////////////////////////////////////////////
			
			//using a fixed time delta of 1 everywhere because we like to do things wrong
			//TODO: fix this shit, needs rewrite of persistence code and day events
			double delT = 1.0;
			
			//build event statistics
			//TODO: this shit should be easy, just read and glom into the gradient
			ArrayList<double[]> eventRateStats = dayEvents.getEventRateStatsList();
			for(double[] eventStat : eventRateStats) {
				for(int i = 0; i < size - 1; i++) {
					gradient[i] += eventStat[i];
				}
				gradient[size - 1] += 1;
			}
			
			//build ijstatistics
			ArrayList<double[]> ijRateStats = dayEvents.get_ijRateStats();
			for(double[] ijStat : ijRateStats) {
				///////////////
				//BUILD SUMMAND
				//dot product
				double summand = 0;
				for(int i = 0; i < size - 1; i++) {
					summand += params[i] * ijStat[i];
				}
				//constant term
				summand += params[size - 1] * 1.0;
				
				//exponentify
				summand = Math.exp(summand);
				
				////////////////
				//BUILD GRADIENT
				for(int i = 0; i < size - 1; i++) {
					gradient[i] -= delT * ijStat[i] * summand;
				}
				//constant term
				gradient [size - 1] -= delT * 1.0 * summand;
				
				///////////////
				//BUILD HESSIAN
				for(int i = 0; i < size - 1; i++) {
					for(int j = 0; j < size - 1; j++) {
						hessian[i][j] -= delT * ijStat[i] * ijStat[j] * summand;
					}
					//constant term
					hessian[i][size - 1] -= delT * ijStat[i] * summand;
				}
				for(int j = 0; j < size - 1; j++) {
					hessian[size - 1][j] -= delT * ijStat[j] * summand;
				}
				hessian[size - 1][size - 1] -= delT * summand;
			}
			
			
			//build empty ijstatistics
			//int edgelessDyads = dayEvents.get_ijDyadsWithNoEdges();
			
			
			
			
			//build gradient
			//build hessian
		}
		
		Matrix jaHessian = new Matrix(hessian);
		Matrix jaNegGradient = ((new Matrix(gradient, 1)).transpose()).timesEquals(-1.0);
		
		
		//H*(x1 - x0) = -G
		Matrix difference = jaHessian.solve(jaNegGradient);
		Matrix jaParams = (new Matrix(params, 1)).transpose();
		Matrix jaNextParams = difference.plus(jaParams);
		
		double[] next_params = jaNextParams.getColumnPackedCopy();
		
		
		
		//DoubleMatrix jbHessian = new DoubleMatrix(hessian);
		//DoubleMatrix jbNegGradient = (new DoubleMatrix(gradient)).neg();
		
		//DoubleMatrix difference = Solve.solve(jbHessian, jbNegGradient);
		
		//DoubleMatrix jbParams = new DoubleMatrix(params);
		//DoubleMatrix jbNextParams = difference.add(jbParams);
		
		//double[] next_params = jbNextParams.toArray();
		
		
		
		
		return next_params;
	}
	*/
	
	/**
	 * Returns the relevant weight statistics for a network.
	 *
	 * @param weightStats the weight statistics to calculate.
	 * @param currentTime the current time
	 * @param g the g
	 * @return the all_i_j_ rate statistics
	 */
	private ArrayList<double[]> getAll_i_j_RateStatistics(Statistic[] weightStats, int currentTime, EventNetwork g){
		
		ArrayList<double[]> statList = new ArrayList<double[]>();
		
		Iterator<EdgeMarker> it = _edgeMarkerSet.iterator();
		while(it.hasNext()){
			EdgeMarker edgeMarker = it.next();
			double[] rateStats = calculateRateStats(weightStats, edgeMarker.getSource(), edgeMarker.getTarget(), currentTime, g );
//			System.out.println("i,j RateStats[ " + edgeMarker.getSource() + ", " + edgeMarker.getTarget() + "]: "+ rateStats[0] + ", " + rateStats[1]+ ", " + rateStats[2]+ ", " + rateStats[3] + ", " + 
//					rateStats[4] + ", " + rateStats[5] + ", " + rateStats[6]);
			
			statList.add(rateStats);
		}
	
		
		return statList;
	}
	
	/**
	 * Calculate rate statistics, or more precisely, combine weight statistics into rate statistics.
	 * TODO: This function is extremely fragile and desperately needs generalization.
	 *
	 * @param weightStats the weight stats to use
	 * @param source the source node
	 * @param target the target node
	 * @param currentTime the current time
	 * @param g the graph
	 * @return the vector of rate statistics for the given node pair
	 */
	private double[] calculateRateStats(Statistic[] weightStats, String source, String target, int currentTime, EventNetwork g ){
		
		/*
		
		reciprocity = reciprocity^+ + reciprocity^-
		inertia = inertia^+ + inertia^-
		triangle = friendOfFriend+enemyOfFriend+friendOfEnemy+enemyOfEnemy
		activitySource = activitySource^+ + activitySource^-
		
		*/
		
		double[] rateStats = new double[7];
		
		//reciprocity
		rateStats[0] = weightStats[0].getValue(source, target, currentTime, g) + weightStats[1].getValue(source, target, currentTime, g);
		//inertia
		rateStats[1] = weightStats[2].getValue(source, target, currentTime, g) + weightStats[3].getValue(source, target, currentTime, g);
		//triangle
		rateStats[2] =  weightStats[4].getValue(source, target, currentTime, g) + weightStats[5].getValue(source, target, currentTime, g)
				        + weightStats[6].getValue(source, target, currentTime, g) + weightStats[7].getValue(source, target, currentTime, g);
		//activitySource
		rateStats[3] = weightStats[8].getValue(source, target, currentTime, g) + weightStats[9].getValue(source, target, currentTime, g);
		//activityTarget
		rateStats[4] = weightStats[10].getValue(source, target, currentTime, g) + weightStats[11].getValue(source, target, currentTime, g);
		//popularitySource
		rateStats[5] = weightStats[12].getValue(source, target, currentTime, g) + weightStats[13].getValue(source, target, currentTime, g);
		//popularityTarget
		rateStats[6] = weightStats[14].getValue(source, target, currentTime, g) + weightStats[15].getValue(source, target, currentTime, g);
		
		
		return rateStats;
	}
	
	// persists event data with time period timestamps
	private void persistData(){
		int final_ijDyadsWithNoEdges = 0;
		ArrayList<double[]> final_ijStats = new ArrayList<double[]>();
		ArrayList<double[]> finalEventRateStatsList = new ArrayList<double[]>();
	    
		
		int possiblePermutations = _uniqueActors.size() * (_uniqueActors.size()-1);
		System.out.println("unique number of actors: " + _uniqueActors.size());
		System.out.println("possible permutations: " + possiblePermutations);
		
		try {
		
			BufferedWriter eventWriter = new BufferedWriter(new FileWriter("eventRateStats.data"));
			BufferedWriter ijWriter = new BufferedWriter(new FileWriter("ijStats.data"));
			
			for(int i=0; i<_dayEventsList.size(); i++){
				//System.out.println("NEW DAY EVENT >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				DayEvents dayEvents = _dayEventsList.get(i);
				
				ArrayList<double[]> eventRateStatsList = dayEvents.getEventRateStatsList();
				ArrayList<double[]> ijStatsList = dayEvents.get_ijRateStats();
				double timePeriod = dayEvents.get_timePeriod();
				
				if( ijStatsList == null ) {
					continue;
				}
				
				int ijDyadsWithNoEdges = possiblePermutations - dayEvents.get_ijRateStats().size();
				
				//write event rate stats to file
				for(double[] rateStats: eventRateStatsList){
					eventWriter.write(timePeriod + "\t");
					for(int j=0; j<rateStats.length; j++){
						eventWriter.write(rateStats[j] + "\t");
					}
					eventWriter.newLine();
				}
				
				//write ij stats to file
				//TODO: fix 0 buffering here
				//BADBADBADBAD
				ijWriter.write(timePeriod + "\t" + ijDyadsWithNoEdges + "\t");
				ijWriter.write("\t\t\t\t\t\t");
				ijWriter.newLine();
				for(double[] ijStats: ijStatsList){
					ijWriter.write(timePeriod + "\t");
					for(int j=0; j<ijStats.length; j++){
						ijWriter.write(ijStats[j] + "\t");
					}
					ijWriter.newLine();
				}
				
				
				//finalEventRateStatsList.addAll(addMetaDataToStats(dayEvents.getEventRateStatsList(), new double[]{dayEvents.getEventRateStatsList().size()}));
				//final_ijStats.addAll(addMetaDataToStats(dayEvents.get_ijRateStats(), new double[]{dayEvents.get_ijRateStats().size(), ijDyadsWithNoEdges}));
				finalEventRateStatsList.addAll(dayEvents.getEventRateStatsList());
				final_ijStats.addAll(dayEvents.get_ijRateStats());
				
				//System.out.println("ijStats.size: " + dayEvents.get_ijRateStats().size());
				//System.out.println("ijDyadsWithNoEdges: " + ijDyadsWithNoEdges);
				//System.out.println("dayEvents.get_ijDyadsWithNoEdges(): " + dayEvents.get_ijDyadsWithNoEdges());
				//final_ijDyadsWithNoEdges = final_ijDyadsWithNoEdges + dayEvents.get_ijDyadsWithNoEdges();
				final_ijDyadsWithNoEdges = final_ijDyadsWithNoEdges + ijDyadsWithNoEdges;
			
			}
			
			eventWriter.flush();
			eventWriter.close();
			ijWriter.flush();
			ijWriter.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("final_ijDyadsWithNoEdges: " + final_ijDyadsWithNoEdges);
		System.out.println("finalEventRateStatsList.size: " + finalEventRateStatsList.size());
		System.out.println("final_ijStats.size: " + final_ijStats.size());
		
        //persistData(final_ijDyadsWithNoEdges, finalEventRateStatsList, final_ijStats); //debug
	}
	
	/*
	private ArrayList<double[]> addMetaDataToStats(ArrayList<double[]> statsList, double[] metaData){
		
		ArrayList<double[]> statsListWithMetaData = new ArrayList<double[]>();
		
		
		for(int i=0; i<statsList.size(); i++){
			double[] stats = statsList.get(i);
			double[] statsWithMetaData = new double[statsList.get(i).length + metaData.length];
			for(int j=0; j<stats.length; j++){
				statsWithMetaData[j] = stats[j];
			}
			for(int j=0; j<metaData.length; j++){
				if(i==0){ //tack meta data to end of first row
					statsWithMetaData[stats.length + j] = metaData[j];
				}
				else{
					statsWithMetaData[stats.length + j] = 0;
				}
			}
			statsListWithMetaData.add(statsWithMetaData);
		}
		
		return statsListWithMetaData;
	}
	*/
	
	/*
	private void persistData(int ijDyadsWithNoEdges, ArrayList<double[]> eventRateStatsList, ArrayList<double[]> ijStatsList){
		
		try {
		
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("eventRateStats.data"));
			for(double[] rateStats: eventRateStatsList){
				for(int i=0; i<rateStats.length; i++){
					bufferedWriter.write(rateStats[i] + "\t");
				}
				bufferedWriter.newLine();
			}
			
			bufferedWriter.flush();
			bufferedWriter.close();
			
			
			bufferedWriter = new BufferedWriter(new FileWriter("ijStats.data"));
			bufferedWriter.write(ijDyadsWithNoEdges + "\t");
			//for(int i=0; i<8; i++){
			for(int i=0; i<6; i++){
				bufferedWriter.write("0.0" + "\t");
			}
			bufferedWriter.newLine();
			for(double[] rateStats: ijStatsList){
				for(int i=0; i<rateStats.length; i++){
					bufferedWriter.write(rateStats[i] + "\t");
				}
				bufferedWriter.newLine();
			}
		
			bufferedWriter.flush();
			bufferedWriter.close();
		
		
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	*/
	
	public void loadStats(String eventStatsFile, String ijStatsFile) {
		_dayEventsList = readStats(eventStatsFile, ijStatsFile);
		//_dayEventsList = new ArrayList<DayEvents>();
		//_dayEventsList.add(dayEvents);	
	}
	
	
	private ArrayList<DayEvents> readStats(String eventStatsFile, String ijStatsFile) {
		ArrayList<DayEvents> dayEventsList = new ArrayList<DayEvents>();
		
		ArrayList<double[]> eventRateStatsList = new ArrayList<double[]>();
		ArrayList<double[]> ijStatsList = new ArrayList<double[]>();
		int ijDyadsWithNoEdges = 0;
		
		DayEvents dayEvents = new DayEvents();
		
		System.out.println("Reading in event stats");
		//read in event stats
		try{
			BufferedReader bufferedReader = new BufferedReader( new FileReader(eventStatsFile));
			
			String line = null;
	        while ((line = bufferedReader.readLine()) != null) {
	        	//NOTE!
	        	//first item is the time period indicator
	        	
	        	String[] stat_strings = line.split("\t");
	        	double[] stats = new double[stat_strings.length];
	        	for(int i = 0; i < stat_strings.length; i++)
	        		stats[i] = Double.parseDouble(stat_strings[i]);

	        	dayEvents.addEventRateStats(stats);
	        	//eventRateStatsList.add(stats);
	        }
			
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Counting lines in ij stats");
		long totalLines = countLines(ijStatsFile);
		System.out.println("Reading in ij stats");
		//read in ijstats
		//NOTE!
		// first item in each time period is the number of edgeless dyads
		try{
			BufferedReader bufferedReader = new BufferedReader( new FileReader(ijStatsFile));
			
			//boolean bFirstLine = true;
			long linecount = 1;
			String line = null;
	        while ((line = bufferedReader.readLine()) != null) {
	        	String[] stat_strings = line.split("\t");
	        	
	        	if( linecount == 1) {
	        		ijDyadsWithNoEdges = Integer.parseInt(stat_strings[0]);
	        		linecount++;
	        		//bFirstLine = false;
	        		continue;
	        	} else if (linecount % 10000 == 0) {
	        		System.out.println("ijStats line: " + (((double) linecount / totalLines) * 100.0));
	        	}
	        	
	        	double[] stats = new double[stat_strings.length];
	        	for(int i = 0; i < stat_strings.length; i++)
	        		stats[i] = Double.parseDouble(stat_strings[i]);

	        	ijStatsList.add(stats);
	        	
	        	linecount++;
	        }
			
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		dayEvents.set_ijRateStats(ijStatsList);
		dayEvents.set_ijDyadsWithNoEdges(ijDyadsWithNoEdges);
		
		dayEventsList.add(dayEvents);
		
		return dayEventsList;
	}
	
	private long countLines(String filename) {
		LineNumberReader lnr = null;
		try{
			lnr = new LineNumberReader(new FileReader(filename));
			lnr.skip(Long.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lnr.getLineNumber();
	}
}
