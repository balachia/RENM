package eventnet;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.HashSet;




import eventnet.EventNetwork.Direction;
import eventnet.events.EdgeEvent;
import eventnet.functions.Identity;
import eventnet.functions.WeightFunction;
import eventnet.statistics.DegreeStat;
import eventnet.statistics.DirectRelationStat;
import eventnet.statistics.Statistic;
import eventnet.statistics.TriangleStat;
import eventnet.statistics.DegreeStat.Base;

/**
 * holds all methods that should be accessed from R. Note however that in principle any method in every class can
 * be accessed. The RFacaed is especially necessary to cast and to create objects from array collections
 * 
 * @author Michael Zinsmaier, University of Konstanz
 * 
  */
public class RFacade {


	/**
	 * 
	 * creates an  {@link EventProcessor eventProcessor}
	 * from an R perspective one should think of the parameters 1..3 as forming a table: <br><br>
	 * 
	 *  ----attrbNames-<br>
	 *  e-T------------<br>
	 *  v-y----weight--<br>
	 *  e-p---function-<br>
	 *  n-e-identifier-<br>
	 *  t-s------------<br>
	 * 
	 * 
	 * 
	 * @param attrbNames header of the table defines which weight function affects which attribute (given an event code)
	 * @param eventTypes defines which weight functions should be used for a specific event type (given an event weight)
	 * @param weightFunctionIdentifiers content of the table defines which attributes should be modified by which weightFunctions<br>
	 * 			{@link EventProcessor#NO_CHANGE_IDENTIFIER} indicates that an attribute should not change.
	 * @param weightFunctions (not part of the table) the weight functions that are referenced by the table content
	 * 
	 * @return an {@link EventProcessor}
	 */
	public EventProcessor createEventProcessor(String[] attrbNames, String[] eventTypes, String[] weightFunctionIdentifiers, WeightFunction[] weightFunctions) {
		//reformat weightFunctionIdentifiers
		String[][] funcIdent = new String[attrbNames.length][weightFunctionIdentifiers.length / attrbNames.length];
		for (int i = 0; i < funcIdent.length; i++) {
			for (int j = 0; j < funcIdent[0].length; j++) {
				funcIdent[i][j] = weightFunctionIdentifiers[i * attrbNames.length + j];
			}
		}

		return new EventProcessor(attrbNames, eventTypes, funcIdent, weightFunctions);
	}
	
	/**
	 * creates a transformation object {@link Transformation}  
	 * 
	 * @param edgeStatistics {@link Transformation#m_edgeStatistics}
	 * @param eventProcessor {@link Transformation#m_eventProcessor}
	 * @param attributes names of the attributes that should be processed
	 * @param t_halfs the half times of the attributes that should be processed. t_half <= 0 indicates no decay
	 * @param timeRange {@link Transformation#m_timeRange}
	 * @param startTime {@link Transformation#m_startTime}
	 * 
	 * @return a reference to the created Simulation object
	 */
	public Transformation setupTransformation(Statistic[] edgeStatistics, EventProcessor eventProcessor, String[] attributes, double[] tHalfs, int timeRange, long startTime) {	
		return new Transformation(edgeStatistics, eventProcessor, attributes, tHalfs, timeRange, startTime);
	}
	
	/**
	 * overloaded method to be able to accept a single halfTime and a single attribute
	 * 
	 * {@link RFacade#setupSimulation(Statistic[], EventProcessor, String[], int[], int, long) doced method}
	 */
	public Transformation setupTransformation(Statistic[] edgeStatistics, EventProcessor eventProcessor, String simAttribute, double tHalf, int timeRange, long startTime) {
		//create arrays of length one to match the simulation constructor
		String[] attr = new String[] {simAttribute};
		double[] half = new double[]{tHalf};
		
		return setupTransformation(edgeStatistics, eventProcessor, attr, half, timeRange, startTime);
	}
		
	/**
	 * adds some edge events to a Transformation. The Transformation object ensures proper ordering.
	 * 
	 * all parameters except the Transformation object are arrays and each "row" of all arrays defines one {@link eventnet.events.EdgeEvent}
	 * 
	 * @param trans reference to a Transformation object
	 * @param source {@link eventnet.events.EdgeEvent#m_source}
	 * @param target {@link eventnet.events.EdgeEvent#m_target}
	 * @param time {@link eventnet.events.Event#m_eventTime}
	 * @param eventType {@link eventnet.events.EdgeEvent#m_eventCode}
	 * @param eventWeight {@link eventnet.events.EdgeEvent#m_eventWeight}
	 * @param stochastic {@link eventnet.events.Event#m_stochastic}
	 */
	public void addEdgeEvents(Transformation trans, String[] source, String[] target, long[] time, String[] eventType, double[] eventWeight, boolean[] stochastic) {
		
		LinkedList<EdgeEvent> events = new LinkedList<EdgeEvent>();
		HashSet<String> uniqueActors = new HashSet<String>();
		
		int stochasticEventCount = 0;
		for (int i = 0; i < source.length; i++) {
			events.add(new EdgeEvent(source[i], target[i], time[i], eventType[i], Math.abs(eventWeight[i]), stochastic[i]));
			uniqueActors.add(source[i]);
			uniqueActors.add(target[i]);
			
			if (stochastic[i]) {
				stochasticEventCount++;
			}
		}
		
		trans.addEdgeEvents(events, uniqueActors, stochasticEventCount);
	}
	
	/**
	 * executes the prepared transformation
	 * @param trans reference to a  java Transformation object
	 * @return double array containing the results for all defined edge statistics
	 */
	public double[][] computeTransformation(Transformation trans) {
		return trans.transformConditionalType(false, true);
	}
	
	/**
	 * serialize the Transformation object to the given path. Use this method to enable
	 * java debugging after initializing the simulation object.
	 * 
	 * @param filePath destination of the serialized object
	 * @param trans the Transformation object that should be serialized
	 */
	public void serialize(Transformation trans, String filePath)
	  {
	    try
	    {
	       FileOutputStream file = new FileOutputStream(filePath);
	       ObjectOutputStream o = new ObjectOutputStream(file);
	       o.writeObject(trans);
	       o.flush();
	       o.close();
	    }
	    catch ( IOException e ) {
	    	e.printStackTrace();
	    }
	  }
	
	
	/**
	 * accepts SYM, Sym, sym .. IN, in, In, ... Out, out, OUT and returns 
	 * the appropriate enum instance
	 * 
	 * @param val a string value that indicates which enum t ochoose
	 * @return the appropriate enum
	 */
	private Direction getEnum(String val) {
		if (val.toLowerCase().trim().equals("out")) {
			return Direction.OUT;
		} else
		if (val.toLowerCase().trim().equals("in")) {
			return Direction.IN;
		} else 
		if (val.toLowerCase().trim().equals("sym")) {
			return Direction.SYM;
		}
		
		//error ... guess sym
		return Direction.SYM;
	}
	
	/**
	 * Defines a simple statistic based on the past of the direct connection. Either incoming
	 * events or outgoing events are taken into account. => Inertia or Reciprocity statistic
	 * 
	 * @param attrName name of the attribute on which the statistic should be used
	 * @param dir the direction OUT, IN or SYM
	 * @return Inertia or Reciprocity statistic
	 */
	public Statistic createDirectRelationStatistic(String attrName, String dir) {
		return new DirectRelationStat(attrName, getEnum(dir));
	}
	

	/**
	 * Defines a triangle statistic. Something like enemy of friend ...
	 * 
	 * @param dir1 the direction of the first triangle edge (connected to the source) OUT, IN or SYM
	 * @param attr1 name of the attribute of the first triangle edge
	 * @param dir2 the direction of the second triangle edge (connected to the target) OUT, IN or SYM
	 * @param attr2 name of the attribute of the second triangle edge
	 * @return triangle statistic
	 */
	public Statistic createTriangleStatistic(String dir1, String attr1, String dir2, String attr2) {
		return new TriangleStat(getEnum(dir1), attr1, getEnum(dir2), attr2);
	}
	
	/**
	 * Defines a degree based statistic measuring e.g. the general hostility of a node
	 * 
	 * @param attrName name of the attribute on which the statistic should be used
	 * @param dir the direction OUT, IN or SYM
	 * @param base the node that should be evaluated accepted values are SOURCE or TARGET
	 * @return degree based statistic
	 */
	public Statistic createDegreeStatistic(String attrName, String dir, String base) {
		Base b;
		if (base.toLowerCase().trim().equals("source") || base.toLowerCase().trim().equals("src") ) {
			b = Base.SRC;
		} else {
			//default
			b = Base.TAR;
		}
		return new DegreeStat(attrName, getEnum(dir), b);
	}

	/**
	 * @param functionName name of the function that should be created (one of the predefined names)
	 * @return the weightFunction object
	 */
	public WeightFunction createWeightFunction(String functionName) {
		if (functionName.equals("Identity")) {
			return new Identity();
		}
		
		
		
		//Error invalid name
		return null;
	}
	
}
