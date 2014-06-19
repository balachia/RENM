package eventnet.rate;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
//import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
//import java.util.Arrays.*;
//import java.util.Random;
//import java.util.List;

import org.apache.commons.lang3.tuple.*;

//import org.rosuda.JRI.REXP;
//import org.rosuda.JRI.Rengine;
//import Jama.Matrix;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
//import org.ejml.ops.MatrixFeatures;
//import org.ejml.ops.MatrixVisualization;
//import org.ejml.ops.MatrixIO;
import org.ejml.ops.NormOps;
//import com.db4o.*;
//import com.db4o.config.*;
//import com.db4o.activation.*;
//import com.db4o.ta.*;
//import com.db4o.query.Predicate;

import eventnet.EventNetwork;
import eventnet.events.EdgeEvent;
import eventnet.statistics.Statistic;

public class RateCalculator {
	
	private HashSet<EdgeMarker> _edgeMarkerSet = new HashSet<EdgeMarker>();
	private HashSet<String> _uniqueActors = new HashSet<String>();
	private ArrayList<DayEvents> _dayEventsList = new ArrayList<DayEvents>();
	
	private DenseMatrix64F m_currParameters;
	private double m_llikelihood;
	private DenseMatrix64F m_gradient;
	private DenseMatrix64F m_hessian;
	private boolean b_canSetParameters;
	private double m_lastProcessedTime;
	
	private boolean b_saveMode = false;
	private boolean b_weightCalculator;
	
	//weight calculator stuff
	private boolean b_wcInited = false;
	private DenseMatrix64F m_wcEqnMat;
	private DenseMatrix64F m_wcResMat;
	private LinkedList<Pair<DenseMatrix64F, Double>> m_wcDeviationList;
	
	/**
	 * Instantiates a new rate calculator.
	 *
	 * @param uniqueActors the set of unique actors
	 */
	public RateCalculator(HashSet<String> uniqueActors, boolean calculateWeights) {
		_uniqueActors.addAll(uniqueActors);
		b_canSetParameters = true;
		b_weightCalculator = calculateWeights;
	}
	
	public void addEdgeMarker(String source, String target){
		_edgeMarkerSet.add(new EdgeMarker(source, target));
		
		boolean added = _uniqueActors.add(source);
		added = added || _uniqueActors.add(target);
		
		if(added){
			System.out.println("WARNING: HAD TO ADD ACTORS " + source + " " + target);
		}
	}
	
	/**
	 * Sets the rate parameters for this iteration.
	 *
	 * @param newParameters the new parameters
	 * @throws IllegalStateException throws an illegal state exception if the parameters have already been set for this iteration. State resets after calculating the new parameters.
	 */
	public void setParameters(DenseMatrix64F newParameters) throws IllegalStateException {
		if(!b_canSetParameters) {
			throw new IllegalStateException("Parameters have already been set for this iteration");
		}
		
		m_currParameters = new DenseMatrix64F(newParameters);
		
		int size = m_currParameters.getNumElements();
		m_lastProcessedTime = 0;
		
		m_llikelihood = 0;
		m_gradient = new DenseMatrix64F(size,1);
		m_hessian = new DenseMatrix64F(size,size);
		
		b_canSetParameters = false;
	}
	
	public int getUniqueActorsSize(){
		return _uniqueActors.size();
	}
	
	public HashSet<EdgeMarker> getEdgeMarkerSet(){
		return _edgeMarkerSet;
	}
	
	
	public void updateDayEventsList(Statistic[] weightStats, LinkedList<EdgeEvent> events, double currentTime, double baseDeltaT, double timeBinLength, EventNetwork g){
		//the time calculations are goofy as dick
		//it's some kind of uniformity assumption on intra-day events
		double delT = currentTime - m_lastProcessedTime;
		delT = baseDeltaT + 1 - (1.0 / (events.size() + 1));
		delT = baseDeltaT + 1 - (0.5 / events.size());
		
		//proper uniformity assumption with variable length time bins
		delT = baseDeltaT + timeBinLength * (1.0 - 1.0 / (events.size() + 1));
		
		m_lastProcessedTime = currentTime;
		
		
		//System.out.println("updateDayEventsList <>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + currentTime + " delT: " + delT);
		
		double[] eventRateStats;
		double[] eventRateStatsNoCons;
		DenseMatrix64F statsColumn;
		DenseMatrix64F statsRow;
		DenseMatrix64F statsMat;
		DayEvents dayEvents = new DayEvents();
		dayEvents.set_timePeriod(currentTime);
		
		for(int i=0; i<events.size(); i++){
			//eventRateStatsNoCons = calculateRateStats(weightStats, events.get(i).getSource(), events.get(i).getTarget(), currentTime, g );
			//eventRateStats = new double[eventRateStatsNoCons.length + 1];
			//System.arraycopy(eventRateStatsNoCons, 0, eventRateStats, 0, eventRateStatsNoCons.length);
			eventRateStats = calculateRateStats(weightStats, events.get(i).getSource(), events.get(i).getTarget(), currentTime, g );
			
			/*
			//add constant statistic
			eventRateStats[eventRateStats.length - 1] = 1;
			*/
			
			if(b_saveMode) {
				dayEvents.addEventRateStats(eventRateStats);
			}
			
			//add event rate statistics to gradient, remembering the constant term
			//^ fuck that guy
			//add event rate statistics to gradient
			// and to log likelihood
			for(int j = 0; j < eventRateStats.length; j++) {
			    m_llikelihood += eventRateStats[j] * m_currParameters.get(j,0);
				m_gradient.add(j, 0, eventRateStats[j]);
			}
			//m_gradient.add(m_gradient.getNumElements() - 1,0,1);
			
			//weight calculator bit
			if(b_weightCalculator) {
				if(!b_wcInited) {
					m_wcEqnMat = new DenseMatrix64F(eventRateStats.length,eventRateStats.length);
					m_wcResMat = new DenseMatrix64F(eventRateStats.length,1);
					m_wcDeviationList = new LinkedList<Pair<DenseMatrix64F,Double>>();
					
					b_wcInited = true;
				}
				
				statsColumn = new DenseMatrix64F(eventRateStats.length, 1, true, eventRateStats);
				statsRow = new DenseMatrix64F(1, eventRateStats.length, true, eventRateStats);
				statsMat = new DenseMatrix64F(eventRateStats.length, eventRateStats.length);
				
				//adding to deviations
				m_wcDeviationList.add(new ImmutablePair<DenseMatrix64F,Double>(statsColumn.copy(),events.get(i).getEventWeight()));
				
				//making A matrix
				CommonOps.mult(statsColumn, statsRow, statsMat);
				CommonOps.addEquals(m_wcEqnMat, statsMat);
				
				//making B matrix
				CommonOps.scale(events.get(i).getEventWeight(), statsColumn);
				CommonOps.addEquals(m_wcResMat, statsColumn);
			}
		}
		
		ArrayList<double[]> ijRateStats = getAll_i_j_RateStatistics(weightStats, currentTime, g );
		DenseMatrix64F[] ijMatrixRateStats = getAll_i_j_RateStatisticsMatrix(weightStats, currentTime, g);
		
		/*
		for(DenseMatrix64F temp: ijMatrixRateStats) {
			MatrixIO.print(System.out, temp, "%9.8f");
		}
		*/
		
		process_i_j_Statistics(ijMatrixRateStats, delT);
		
		
		if(b_saveMode) {
			dayEvents.set_ijRateStats(ijRateStats);
		}
		
		int possiblePermutations = _uniqueActors.size() * (_uniqueActors.size()-1);
		int ijDyadsWithNoEdges = possiblePermutations - ijRateStats.size();
		dayEvents.set_ijDyadsWithNoEdges(ijDyadsWithNoEdges);
		
		_dayEventsList.add(dayEvents);
	}
	
	
	public void executeMaximization(boolean persistData){
		System.out.println("Execute Maximization >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		
		if(persistData == true) {
			persistData();
		}
		
		double[] parameters = optimizeNR();
		System.out.println(java.util.Arrays.toString(parameters));
		
//		Rengine re=REngineManager.getInstance().getREngine();
//		executeNewtonRhapson(re);
//		re.end();
		
	}
	
	/**
	 * Adds ijStatistics to whatever functions we're building here
	 * 
	 * @param ijStatistics
	 * @param delT
	 */
	private void process_i_j_Statistics(DenseMatrix64F[] ijStatistics, double delT) {
		int size = ijStatistics[0].numCols;
		DenseMatrix64F summand = new DenseMatrix64F(size,size);
		
		//build parameter * statistic summand
		for(int i = 0; i < ijStatistics.length; i++) {
			CommonOps.addEquals(summand, m_currParameters.get(i, 0), ijStatistics[i]);
		}
		
		//exponentiate each term, and scale by delT
		for(int i = 0; i < summand.numRows; i++) {
			for(int j = 0; j < summand.numCols; j++) {
				if (i == j) {
					continue;
				}
				summand.set(i, j, delT * Math.exp(summand.get(i, j)));
			}
		}
		
		// subtract summand from log likelihood (\theta * s - t * exp(\sum \theta * s))
		m_llikelihood -= matrixSumWithoutSelfLoops(summand);
		
		//now add to gradient and hessian
		DenseMatrix64F iSummand = new DenseMatrix64F(summand);
		DenseMatrix64F jSummand = new DenseMatrix64F(summand);
		double sum;
		
		for(int si = 0; si < m_currParameters.getNumElements(); si++){
			//scale by first statistic
			iSummand = new DenseMatrix64F(summand);
			CommonOps.elementMult(iSummand, ijStatistics[si]);
			//CommonOps.scale(m_currParameters.get(si), iSummand);
			
			//subtract sum of all items from gradient
			sum = matrixSumWithoutSelfLoops(iSummand);
			m_gradient.add(si, 0, - sum);
			
			for(int sj = 0; sj < m_currParameters.getNumElements(); sj++) {
				//scale by second statistic
				jSummand = new DenseMatrix64F(iSummand);
				//CommonOps.scale(m_currParameters.get(sj), jSummand);
				CommonOps.elementMult(jSummand, ijStatistics[sj]);
				
				//subtract sum of all items from appropriate hessian cell
				sum = matrixSumWithoutSelfLoops(jSummand);
				m_hessian.add(si, sj, - sum);
			}
		}
	}
	
	/**
	 * Returns the sum of a matrix's elements, excluding the diagonal.
	 *
	 * @param matrix the matrix
	 * @return the sum
	 */
	private double matrixSumWithoutSelfLoops(DenseMatrix64F matrix) {
		double ret = 0;
		
		for(int i = 0; i < matrix.numRows; i++) {
			for(int j = 0; j < matrix.numCols; j++) {
				if(i == j) {
					continue;
				}
				
				ret += matrix.get(i, j);
			}
		}
		
		return ret;
	}
	
	/*
	private void executeNewtonRhapsonR(Rengine re){
		
		System.out.println("Execute Newton-Rhapson in R >>>>>>>>>>>>>>>>>>>>>>>>");
		REXP  rexp = re.eval("source('rateNR_4.R')");
		System.out.println("NewtonRhapson Results >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		System.out.println("REXP: " + rexp);
	
	}
	*/
	
	private double[] optimizeNR() {
		//this line of code is stupid, this shit should be a parameter in the object
		int size = 1 + _dayEventsList.get(0).getEventRateStatsList().get(0).length;
		
		
		double[] params = new double[size];
		double[] nextParams = new double[size];
		DenseMatrix64F ejmlParams = new DenseMatrix64F(size, 1, true, params);
		DenseMatrix64F ejmlNextParams = new DenseMatrix64F(size, 1, true, nextParams);
		DenseMatrix64F ejmlChange = ejmlParams.copy();
		//double[] params = {1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0};
		//params[size-1] = 1.0;
		System.out.println(java.util.Arrays.toString(params));
		
		int maxIter = 100;
		int iter = 1;
		double P2Threshold = 1e-8;
		double P2Norm;
		double last_llikelihood = Double.NEGATIVE_INFINITY;
		
		do {
		//for(int i = 0; i < 20; i++){
			System.out.println("ITERATION " + iter);
			System.out.println("Current Parameters:" + java.util.Arrays.toString(params));
			
			//catch db4o errors, let's hope this shit runs
			params = iterateNR(params);
			
			ejmlNextParams = new DenseMatrix64F(size, 1, true, params);
			CommonOps.sub(ejmlNextParams, ejmlParams, ejmlChange);
			ejmlParams = ejmlNextParams;
			
			//P2 Norm being of course the standard norm in Euclidean space.
			//What you didn't know that? Pfft, dummy.
			P2Norm = NormOps.normP2(ejmlChange);
			double ll_diff = Math.abs(m_llikelihood - last_llikelihood);
			last_llikelihood = m_llikelihood;
			
			//let's see some norms!
			System.out.println("|dX| (P2): " + P2Norm);
			System.out.println("|dLL|: " + ll_diff);
			
			//It's Pythonic!
			iter += 1;
		} while (iter < maxIter && P2Norm > P2Threshold);
		
		return params;
	}
	
	public double getLogLikelihood() {
	    return m_llikelihood;
	}
	
	public DenseMatrix64F solveForParameters() {
		b_canSetParameters = true;
		
		//System.out.println("Alt Gradient:");
		//m_gradient.print("%9.8f");
		
		//System.out.println("Alt Hessian:");
		//m_hessian.print("%9.8f");
		
		return solveForParameters(m_hessian, m_gradient, m_currParameters);
	}
	
	//I fucking hope this is how you get the standard errors
	public DenseMatrix64F getStandardErrors () {
		DenseMatrix64F varMatrix = new DenseMatrix64F(m_hessian.numRows, m_hessian.numCols);
		DenseMatrix64F variances = new DenseMatrix64F(m_hessian.numRows, 1);
		
		CommonOps.invert(m_hessian, varMatrix);
		CommonOps.changeSign(varMatrix);
		CommonOps.extractDiag(varMatrix, variances);
		DenseMatrix64F stdErrs = matrixSqrt(variances); 
		
		return stdErrs;
	}
	
	public DenseMatrix64F getWeightParameters() {
		DenseMatrix64F parameters = new DenseMatrix64F(m_wcResMat);
		
		//int rank = MatrixFeatures.rank(m_wcEqnMat);
		
		//System.out.println("Weight parameter matrix rank: " + rank);
		
		CommonOps.solve(m_wcEqnMat, m_wcResMat, parameters);
		
		double[] ret = new double[parameters.numRows];
		
		for(int i = 0; i < parameters.numRows; i++) {
			ret[i] = parameters.get(i, 0);
		}
		
		return parameters;
		//return ret;
	}
	
	/**
	 * Returns the MLE estimate of variance for weigh calculatios given the events already processed.
	 *
	 * @param parameters the statistic parameters, in the same order as statistics
	 * @return the weight variance
	 */
	public double getWeightVariance(DenseMatrix64F parameters) {
		double ret = 0.0;
		
		for(Pair<DenseMatrix64F, Double> statsAndWeight: m_wcDeviationList) {
			double weight = statsAndWeight.getRight();
			DenseMatrix64F stats = statsAndWeight.getLeft().copy();
			
			//dot product: multiplication, then addition
			CommonOps.elementMult(stats, parameters);
			double sumStats = CommonOps.elementSum(stats);
			double deviance = (weight - sumStats) * (weight - sumStats);
			
			ret += deviance;
		}
		
		//divide by N
		ret /= m_wcDeviationList.size();
		
		return ret;
	}
	
	public DenseMatrix64F getWeightCovarianceMatrix(double variance) {
		DenseMatrix64F cov = m_wcEqnMat.copy();
		
		CommonOps.scale((1/variance), cov);
		CommonOps.invert(cov);
		
		return cov;
	}
	
	public static DenseMatrix64F matrixSqrt(DenseMatrix64F matrix) {
		DenseMatrix64F ret = matrix.copy();
		
		for(int i = 0; i < matrix.numRows; i++) {
			for(int j = 0; j < matrix.numCols; j++) {
				ret.set(i,j, Math.sqrt(matrix.get(i, j)));
			}
		}
		
		return ret;
	}
	
	private DenseMatrix64F solveForParameters(DenseMatrix64F hessian, DenseMatrix64F gradient, DenseMatrix64F currentParameters) {
		DenseMatrix64F ejmlHessian = new DenseMatrix64F(hessian);
		DenseMatrix64F ejmlNegGradient = new DenseMatrix64F(gradient);
		CommonOps.changeSign(ejmlNegGradient);
		
		//ejmlHessian.print("%9.8f");
		//gradient.print("%9.8f");
		
		
		
		
		//H*(x1 - x0) = -G
		DenseMatrix64F ejmlDifference = new DenseMatrix64F(gradient.numRows,1);
		boolean b_couldInvert = CommonOps.solve(ejmlHessian, ejmlNegGradient, ejmlDifference);
		if(!b_couldInvert) {
			System.out.println("Unable to invert hessian");
		}
		
		DenseMatrix64F ejmlParams = new DenseMatrix64F(currentParameters);
		DenseMatrix64F ejmlNextParams = new DenseMatrix64F(ejmlParams);
		CommonOps.add(ejmlParams, ejmlDifference, ejmlNextParams);
		
		return ejmlNextParams;
	}
	
	private double[] iterateNR(double[] params) {
		//CONSTANT STATISTIC LOCATED IN PARAMETER <size - 1>
		int size = params.length;
		
		//TODO: let's use EJML here too
		//pray these are initialized to 0
		double[] gradient = new double[size];
		double[][] hessian = new double[size][size];
		
		//build NR equations
		//int count = 1;
		//TODO: setting initial time this way is rather hackish, should be passing an initial time parameter to rate calc
		double current_time = 0;
		for(DayEvents dayEvents : _dayEventsList) {
		//for(DayEvents dayEvents : dayEventsList) {
			//System.out.println(">>>>NEW DAY");
			
			//////////////////////////////////////////////
			//remember to include the constant statistics!
			//////////////////////////////////////////////
			
			//this should probably be a double but all other times are stored as ints
			//...so, go figure
			double delT = dayEvents.get_timePeriod() - current_time;
			if(delT < 0) {
				System.out.println("WARNING: DAY EVENTS ARE APPEARING OUT OF ORDER");
			}
			
			//double delT = (double) dayEvents.get_timePeriod() - current_time;
			current_time = dayEvents.get_timePeriod();
			
			if(delT == 0) {
				System.out.println("Delta T is 0 for day " + dayEvents.get_timePeriod());
			}
			
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
			int edgelessDyads = dayEvents.get_ijDyadsWithNoEdges();
			double summand = Math.exp(params[size - 1]);
			
			//build gradient
			gradient [size - 1] -= edgelessDyads * delT * 1.0 * summand;
			//build hessian
			hessian[size - 1][size - 1] -= edgelessDyads * delT * summand;
		}
		
		//System.out.println("Gradient:" + java.util.Arrays.toString(gradient));
		//System.out.println("Hessian:" + java.util.Arrays.deepToString(hessian));
		
		//EJML variant
		DenseMatrix64F ejmlHessian = new DenseMatrix64F(hessian);
		DenseMatrix64F ejmlGradient = new DenseMatrix64F(gradient.length,1,true,gradient);
		DenseMatrix64F ejmlParams = new DenseMatrix64F(params.length, 1, true, params);
		
		//System.out.println("Gradient:");
		//ejmlGradient.print("%9.8f");
		//System.out.println("Hessian:");
		//ejmlHessian.print("%9.8f");
		
		DenseMatrix64F ejmlNextParams = solveForParameters(ejmlHessian, ejmlGradient, ejmlParams);
		
		/*
		DenseMatrix64F ejmlHessian = new DenseMatrix64F(hessian);
		DenseMatrix64F ejmlNegGradient = new DenseMatrix64F(gradient.length,1,true,gradient);
		CommonOps.scale(-1.0, ejmlNegGradient);
		
		Matrix jaHessian = new Matrix(hessian);
		Matrix jaNegGradient = ((new Matrix(gradient, 1)).transpose()).timesEquals(-1.0);
		
		
		//H*(x1 - x0) = -G
		DenseMatrix64F ejmlDifference = new DenseMatrix64F(gradient.length,1);
		boolean b_couldInvert = CommonOps.solve(ejmlHessian, ejmlNegGradient, ejmlDifference);
		if(!b_couldInvert) {
			System.out.println("Unable to invert hessian");
		}
		
		DenseMatrix64F ejmlParams = new DenseMatrix64F(params.length, 1, true, params);
		DenseMatrix64F ejmlNextParams = new DenseMatrix64F(ejmlParams);
		CommonOps.add(ejmlParams, ejmlDifference, ejmlNextParams);
		*/
		
		//Matrix difference = jaHessian.solve(jaNegGradient);
		//Matrix jaParams = (new Matrix(params, 1)).transpose();
		//Matrix jaNextParams = difference.plus(jaParams);
		
		//double[] next_params = jaNextParams.getColumnPackedCopy();
		double[] next_params = new double[ejmlNextParams.getNumElements()];
		for(int i = 0; i < next_params.length; i++) {
			next_params[i] = ejmlNextParams.get(i,0);
		}
		
		
		
		//close db4o database
		//db.close();
		
		return next_params;
	}
	
	
	private DenseMatrix64F[] getAll_i_j_RateStatisticsMatrix(Statistic[] weightStats, double currentTime, EventNetwork g) {
		
		
		DenseMatrix64F[] weightStatMatrices = new DenseMatrix64F[weightStats.length];
		
		for(int i = 0; i < weightStats.length; i++) {
			weightStatMatrices[i] = weightStats[i].getAllValues(currentTime, g);
		}
		
		/*
		int size = weightStatMatrices[0].numCols;
		
		//account for constant
		//^ fuck that guy
		//DenseMatrix64F[] rateStatMatrices = new DenseMatrix64F[weightStats.length + 1];
		DenseMatrix64F[] rateStatMatrices = new DenseMatrix64F[weightStats.length];
		
		for(int i = 0; i < weightStats.length; i++) {
			rateStatMatrices[i] = new DenseMatrix64F(size,size);
			CommonOps.addEquals(rateStatMatrices[i], weightStatMatrices[i]);
		}
		rateStatMatrices[weightStats.length] = new DenseMatrix64F(size,size);
		CommonOps.set(rateStatMatrices[weightStats.length], 1);
		*/
		
		/*
		
		//combine weight stats into rate stats
		//include constant statistic
		//TODO: combine them elsewhere
		//TODO: fix up this magic number 8 shit
		DenseMatrix64F[] rateStatMatrices = new DenseMatrix64F[8];
		
		//reciprocity
		rateStatMatrices[0] = new DenseMatrix64F(size,size);
		CommonOps.addEquals(rateStatMatrices[0], weightStatMatrices[0]);
		CommonOps.addEquals(rateStatMatrices[0], weightStatMatrices[1]);
		//CommonOps.add(weightStatMatrices[0], weightStatMatrices[1], rateStatMatrices[0]);
		//inertia
		rateStatMatrices[1] = new DenseMatrix64F(size,size);
		CommonOps.addEquals(rateStatMatrices[1], weightStatMatrices[2]);
		CommonOps.addEquals(rateStatMatrices[1], weightStatMatrices[3]);
		//CommonOps.add(weightStatMatrices[2], weightStatMatrices[3], rateStatMatrices[1]);
		//triangle
		rateStatMatrices[2] = new DenseMatrix64F(size,size);
		//CommonOps.add(weightStatMatrices[4], weightStatMatrices[5], rateStatMatrices[2]);
		CommonOps.addEquals(rateStatMatrices[2], weightStatMatrices[4]);
		CommonOps.addEquals(rateStatMatrices[2], weightStatMatrices[5]);
		CommonOps.addEquals(rateStatMatrices[2], weightStatMatrices[6]);
		CommonOps.addEquals(rateStatMatrices[2], weightStatMatrices[7]);
		//activitySource
		rateStatMatrices[3] = new DenseMatrix64F(size,size);
		CommonOps.addEquals(rateStatMatrices[3], weightStatMatrices[8]);
		CommonOps.addEquals(rateStatMatrices[3], weightStatMatrices[9]);
		//CommonOps.add(weightStatMatrices[8], weightStatMatrices[9], rateStatMatrices[3]);
		//activityTarget
		rateStatMatrices[4] = new DenseMatrix64F(size,size);
		CommonOps.addEquals(rateStatMatrices[4], weightStatMatrices[10]);
		CommonOps.addEquals(rateStatMatrices[4], weightStatMatrices[11]);
		//CommonOps.add(weightStatMatrices[10], weightStatMatrices[11], rateStatMatrices[4]);
		//popularitySource
		rateStatMatrices[5] = new DenseMatrix64F(size,size);
		CommonOps.addEquals(rateStatMatrices[5], weightStatMatrices[12]);
		CommonOps.addEquals(rateStatMatrices[5], weightStatMatrices[13]);
		//CommonOps.add(weightStatMatrices[12], weightStatMatrices[13], rateStatMatrices[5]);
		//popularityTarget
		rateStatMatrices[6] = new DenseMatrix64F(size,size);
		CommonOps.addEquals(rateStatMatrices[6], weightStatMatrices[14]);
		CommonOps.addEquals(rateStatMatrices[6], weightStatMatrices[15]);
		//CommonOps.add(weightStatMatrices[14], weightStatMatrices[15], rateStatMatrices[6]);
		
		//constant
		rateStatMatrices[7] = new DenseMatrix64F(size,size);
		CommonOps.set(rateStatMatrices[7], 1);
		
		*/
		
		//return rateStatMatrices;
		return weightStatMatrices;
	}
	
	private ArrayList<double[]> getAll_i_j_RateStatistics(Statistic[] weightStats, double currentTime, EventNetwork g){
		
		ArrayList<double[]> statList = new ArrayList<double[]>();
		
		
		
		Iterator<String> sourceIterator = _uniqueActors.iterator();
		String source;
		String target;
		
		long lastTime = System.nanoTime();
		long currTime = System.nanoTime();
		
		int count = 0;
		int size = (_uniqueActors.size() * _uniqueActors.size() - _uniqueActors.size());
		
		while(sourceIterator.hasNext()) {
			source = sourceIterator.next();
			Iterator<String> targetIterator = _uniqueActors.iterator();
			
			while(targetIterator.hasNext()) {
				target = targetIterator.next();
				
				if(source.equals(target)) {
					continue;
				}
				
				double[] rateStats = calculateRateStats(weightStats, source, target, currentTime, g );
				
				statList.add(rateStats);
				
				//TODO: clean up debug code
				//debug
				//System.out.println(source + " " + target + " " + java.util.Arrays.toString(rateStats));
				
				count += 1;
				if(count % 1000 == 0){
					lastTime = currTime;
					currTime = System.nanoTime();
					
					//System.out.println(count + "/" + size + " " + ((currTime - lastTime) / 1000000000.0));
				}
				
			}
		}
		
		
		/*
		Iterator<EdgeMarker> it = _edgeMarkerSet.iterator();
		while(it.hasNext()){
			EdgeMarker edgeMarker = it.next();
			double[] rateStats = calculateRateStats(weightStats, edgeMarker.getSource(), edgeMarker.getTarget(), currentTime, g );
//			System.out.println("i,j RateStats[ " + edgeMarker.getSource() + ", " + edgeMarker.getTarget() + "]: "+ rateStats[0] + ", " + rateStats[1]+ ", " + rateStats[2]+ ", " + rateStats[3] + ", " + 
//					rateStats[4] + ", " + rateStats[5] + ", " + rateStats[6]);
			
			statList.add(rateStats);
		}
		*/
		
		
		return statList;
	}
	
	

	private double[] calculateRateStats(Statistic[] weightStats, String source, String target, double currentTime, EventNetwork g ){
		
		/*
		
		reciprocity = reciprocity^+ + reciprocity^-
		inertia = inertia^+ + inertia^-
		triangle = friendOfFriend+enemyOfFriend+friendOfEnemy+enemyOfEnemy
		activitySource = activitySource^+ + activitySource^-
		
		*/
		
		
		//SL Chat stuff
		//double[] rateStats = new double[10];
		double[] rateStats = new double[weightStats.length];
		for(int i = 0; i < weightStats.length; i++) {
			rateStats[i] = weightStats[i].getValue(source, target, currentTime, g);
		}
		
		
		/*
		
		double[] rateStats = new double[7];
		//double[] rateStats = new double[10];
		//double[] rateStats = new double[6];
		
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
		*/
		
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
