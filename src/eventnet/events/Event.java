package eventnet.events;

import java.io.Serializable;


/**
 * Note: this class has a natural ordering that is inconsistent with equals
 * 
 * Defines common properties for all types of events. E.g. <br>
 * - events can be sorted in ascending {@link Event#m_eventTime} order
 * 
 * @author Michael Zinsmaier, University of Konstanz
 *
 */
public abstract class Event implements Comparable<Event>, Serializable{

	/** default id */
	private static final long serialVersionUID = 1L;
	/**  time the event happens. Determines the order of the events
	 * 	 e.g. a unix timestamp. */
	protected double m_eventTime;
	/** true => the event triggers the statistic set to be evaluated. */
	protected boolean m_stochastic;
	
	/**
	 * super constructor of events. Sets the member variables.
	 * 
	 * @param eventTime
	 * @param estimate
	 */
	public Event(double eventTime, boolean estimate) {
		m_eventTime = eventTime;
		m_stochastic = estimate;
	}
	
	/**
	 * @return {@link Event#m_eventTime}
	 */
	public double getEventTime() {
		return m_eventTime;
	}
	
	/**
	 * @return {@link Event#m_stochastic}
	 */
	public boolean stochastic() {
		return m_stochastic;
	}
	
	/**
	 * Events should be ordered by ascending {@link Event#m_eventTime}
	 */
	@Override
	public int compareTo(Event other) {
		if (this.m_eventTime < other.m_eventTime) {
			return -1;
		} else if (this.m_eventTime > other.m_eventTime) {
			return 1;
		} else {
			return 0;
		}
	}
	
}
