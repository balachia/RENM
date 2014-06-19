package eventnet.functions;

import java.io.Serializable;

import eventnet.EventNetwork;

/**
 * the Identity weight function returns the same value as it
 * gets as parameter.
 * 
 * @author Michael Zinsmaier, University of Konstanz
 *
 */
public class Identity implements WeightFunction, Serializable {

	private static final long serialVersionUID = 1L;
	
	@Override
	public String getName() {
		return "Identity";
	}

	@Override
	public double getIncrement(EventNetwork g, String source, String target, double currentTime, double weight) {
		return weight;
	}

}
