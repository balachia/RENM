package eventnet.events;

/**
 * Data container class, encapsulates an attribute edge event.
 * Either the change of an estimated variable or the change of a covar
 *  
 * @author Michael Zinsmaier, University of Konstanz
 *
 */
public class EdgeEvent extends Event{

	/** default id */
	private static final long serialVersionUID = 1L;
	/** specifies which attributes should be changed by the event (see {@link EventProcessor})*/
	private String m_eventType;
	/** the weight of the current event */
	private double m_eventWeight;
	/** unique string identifier of the target node */
	private String m_target;
	/** unique string identifier of the source node */
	private String m_source;

	/**
	 * creates a new attribute edge event. Sets the member variables.
	 * 
	 * @param source {@link EdgeEvent#m_source}
	 * @param target {@link EdgeEvent#m_target}
	 * @param eventTime {@link Event#m_eventTime}
	 * @param eventType {@link EdgeEvent#m_eventType}
	 * @param eventWeight {@link EdgeEvent#m_eventWeight}
	 * @param estimate {@link Event#stochastic()}
	 */
	public EdgeEvent(String source, String target, double eventTime, String eventType, double eventWeight, boolean estimate) {
		super(eventTime, estimate);
		m_source = source;
		m_target = target;
		m_eventType = eventType;
		m_eventWeight = eventWeight;
	}
	
	/**
	 * @return {@link EdgeEvent#m_eventType}
	 */
	public String getEventType() {
		return m_eventType;
	}
	
	/**
	 * @return {@link EdgeEvent#m_target}
	 */
	public String getTarget() {
		return m_target;
	}
	
	/**
	 * @return {@link EdgeEvent#m_source}
	 */
	public String getSource() {
		return m_source;
	}

	/**
	 * @return {@link EdgeEvent#m_weight}
	 */
	public double getEventWeight() {
		return m_eventWeight;
	}
	
}
