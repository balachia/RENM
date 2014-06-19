package eventnet.helper;

import java.io.Serializable;

/**
 * helper class that binds two arbitrary objects together
 * 
 * @author Michael Zinsmaier, University of Konstanz
 *
 * @param <T1> first pair value
 * @param <T2> second pair value
 */
public class Pair<T1,T2> implements Serializable{
	
	/** default id */
	private static final long serialVersionUID = 1L;
	/**
	 * @param first the first pair value
	 * @param second the second pair value
	 */ 
	public Pair(T1 first, T2 second) {
		firstValue = first;
		secondValue = second;
	}
	
	/** access the first pair value */
	public T1 firstValue;
	/** access the second pair value */
	public T2 secondValue;

}
