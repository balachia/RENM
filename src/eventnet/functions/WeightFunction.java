package eventnet.functions;

import eventnet.EventNetwork;

/**
 * a weight function defines a "mathematical" function from R -> R.
 * The simplest possible weight function is the identity function.
 * 
 * @author Michael Zinsmaier, University of Konstanz
 *
 */
public interface WeightFunction {
	
	/**
	 * @return the name of the weight function used to select it.
	 */
	public String getName();
	
	/**
	 * @param g a reference to the eventNetwork (do not write to it! read only)
	 * @param source unique string identifier of the source node
	 * @param target unique string identifier of the target node
	 * @param the current time of the network (not the exact time the event happened but the time of its timerange)
	 * @param eventWeight the weight of the event
	 *
	 * @return transformed weight
	 */
	public double getIncrement(EventNetwork g, String source, String target, double currentTime, double weight);

}
