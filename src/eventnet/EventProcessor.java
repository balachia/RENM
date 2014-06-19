package eventnet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

import eventnet.functions.WeightFunction;
import eventnet.helper.Pair;



/**
 * 
 * an event processor object defines the amount of change for one or more
 * network attributes for a given eventType.<br><br>
 * An event provides an eventType, specifying which attributes should be
 * changed and the weight functions that should be used to change it<br><br>
 * An event provides an event weight, specifying the amount of change. Note however that
 * the weight will be transformed for each attribute by the according weight
 * function individually. 
 *  
 * @author Michael Zinsmaier, University of Konstanz
 *
 */
public class EventProcessor implements Serializable {
	
	
	/** default id */
	private static final long serialVersionUID = 1L;
	
	/** defines an invalid index indicating that an attribute should not change at all. */
	final Integer NO_CHANGE_INDEX = -1;
	/** reserved weight function identifier that indicates that an attribute should not change. */
	final String NO_CHANGE_IDENTIFIER = "None";
	
	
	/** hashes eventTypes 2 weight function index vectors	 */
	private HashMap<String, Integer[]> m_types2functions = new HashMap<String, Integer[]>();
	
	/** names of the attributes in the order of the function index vectors*/
	private String[] m_attrbNames;
	
	/** the weight functions */
	private WeightFunction[] m_weightFunctions;
	
	
	/**
	 * 
	 * creates an {@link EventProcessor eventProcessor}
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
	 * @param attrbNames header of the table defines which weight function affects which attribute (given an event type)
	 * @param eventTypes defines which weight functions should be used for a specific event type (given an event weight)
	 * @param weightFunctionIdentifirs content of the table defines which attributes should be modified by which weightFunctions<br>
	 * 			{@link EventProcessor#NO_CHANGE_IDENTIFIER the no change weight function identifier} indicates that an attribute should not change.
	 * @param weightFunctions (not part of the table) the weight functions that are referenced by the table content
	 */
	public EventProcessor(String[] attrbNames, String[] eventTypes, String[][] weightFunctionIdentifiers, WeightFunction[] weightFunctions) {
		//set up the weight functions array
		m_weightFunctions = weightFunctions;
		
		//associating each identifier with some function in the list?
		HashMap<String, Integer> weightFuncIdentifier2Index = new HashMap<String, Integer>();
		
		for (int i = 0; i < weightFunctions.length; i++) {
			weightFuncIdentifier2Index.put(weightFunctions[i].getName(), i);
		}
		
		//for each event type
		for (int i = 0; i < eventTypes.length; i++) {
			
			Integer[] tmp = new Integer[attrbNames.length];
			
			//build function index array
			for (int j = 0; j < attrbNames.length; j++) {
				if (weightFunctionIdentifiers[i][j].equals(NO_CHANGE_IDENTIFIER)) {
					tmp[j] = NO_CHANGE_INDEX;
				} else {
					tmp[j] = weightFuncIdentifier2Index.get(weightFunctionIdentifiers[i][j]);
				}
			}
			
			//add it
			m_types2functions.put(eventTypes[i], tmp);
		}
		
		m_attrbNames = attrbNames;
	}

	/**
	 * calculates the changes that one event of a certain eventType triggers
	 * 
	 * @param eventType the type of the event (specifies which weight function to use on which attribute)
	 * @param g a reference to the eventNetwork (do not write to it! read only)
	 * @param source unique string identifier of the source node
	 * @param target unique string identifier of the target node
	 * @param the current time of the network (not the exact time the event happened but the time of its timerange)
	 * @param eventWeight the weight of the event (input for the weight functions)
	 * 
	 * @return pairs of < attribute name, attribute change >
	 */
	public LinkedList<Pair<String, Double>> getChanges(String eventType, EventNetwork g, String source, String target, double currentTime, double eventWeight) {
		
		//System.out.println(">>>>>>>>>>>>> >>>>>>>>>>>>>>>>      >>>>>   eventType: " + eventType);
		Integer[] functionIndices = m_types2functions.get(eventType);
		LinkedList<Pair<String, Double>> changes = new LinkedList<Pair<String, Double>>();
		
		for (int i = 0; i < functionIndices.length; i++) {
			if (functionIndices[i] != NO_CHANGE_INDEX) {
				Double change = m_weightFunctions[functionIndices[i]].getIncrement(g, source, target, currentTime, eventWeight);
				changes.add(new Pair<String, Double>(m_attrbNames[i], change));
			}
		}
		
		return changes;
	}
	
}
