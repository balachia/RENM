package eventnet.statistics;

import java.io.Serializable;
import org.ejml.data.DenseMatrix64F;

import eventnet.EventNetwork;



public interface Statistic extends Serializable{ //should contain the functionality of Serializable
	
	public abstract String getName();
	
	public abstract double getValue(String source, String target, double currentTime, EventNetwork g);
	
	public abstract DenseMatrix64F getAllValues(double currentTime, EventNetwork g);
	
	public abstract void reset();
	
	//add another method that excepts node
	
}
 