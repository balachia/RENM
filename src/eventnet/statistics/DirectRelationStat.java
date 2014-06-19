package eventnet.statistics;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import eventnet.EventNetwork;
import eventnet.EventNetwork.Direction;

/**
 * statistics that work with the direct relation of source and target.
 * Basically inertia or reciprocity = what did I do or what did you do
 * 
 * @author Michael Zinsmaier, University of Konstanz
 *
 */
public class DirectRelationStat implements Statistic {

	private static final long serialVersionUID = 1L;
	
	private String m_attrName; //pos or neg
	private Direction m_direction;
	
	public DirectRelationStat(String attrName, Direction dir) {
		m_attrName = attrName;
		m_direction = dir;
	}
	
	public String getName() {
		String res = "RelCov (";
		res += m_direction.toString() + " " + m_attrName + ")";
		return res;
	}
	
	public void reset() {
		//not keeping state so irrelevant
	}
	
	public DenseMatrix64F getAllValues(double currentTime, EventNetwork g) {
		DenseMatrix64F ret = g.getEdgeMatrix(m_attrName, currentTime);
		
		if(m_direction == Direction.OUT) {
			return ret;
		}
		
		DenseMatrix64F retT = ret.copy();
		CommonOps.transpose(ret, retT);
		
		if(m_direction == Direction.IN) {
			return retT;
		} else { //Direction.SYM
			CommonOps.addEquals(ret, retT);
		}
		
		return ret;
	}

	public double getValue(String source, String target, double currentTime,
			EventNetwork g) {
		
		if (m_direction == Direction.OUT) {
			//get value of queried edge
			return g.getEdgeValue(source, target, m_attrName, currentTime);
			//return g.getEdgeValue(g.getEdge(source, target), m_attrName, currentTime);
		} else 
		if (m_direction == Direction.IN){
			//get value of edge in opposite direction
			return g.getEdgeValue(target, source, m_attrName, currentTime);
			//return g.getEdgeValue(g.getEdge(target, source), m_attrName, currentTime);
		} 
		if (m_direction == Direction.SYM) {
			double retVal = g.getEdgeValue(source, target, m_attrName, currentTime);
			retVal += g.getEdgeValue(target, source, m_attrName, currentTime);
			
			return retVal;
		}
		
		return 0;
	}
	
}
