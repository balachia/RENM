package eventnet.statistics;

import eventnet.EventNetwork;
import eventnet.EventNetwork.Direction;
//import y.base.EdgeCursor;
//import y.base.Node;
//import y.base.Edge;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

public class DegreeStat implements Statistic {
	
	private static final long serialVersionUID = 1L;

	public enum Base {SRC, TAR};
	
	private String m_attrName;
	private Direction m_direction;
	private Base m_base;
	
	//the timestamp of the last statistics set we pulled out
	//TODO: not sure what to do about initial value here
	//problem if we want to reload stats after they get updated
	private double statsTime = -1;
	private DenseMatrix64F m_degreeStats;
	private DenseMatrix64F m_fullDegreeStats;
	
	
	public DegreeStat(String attrName, Direction dir, Base base) {
		m_attrName = attrName;
		m_direction = dir;
		m_base = base;
	}
	
	public String getName() {
		String res = "Degree (";
		res += m_base.toString() + "/" + m_direction.toString() + " " + m_attrName + ")";
		return res;
	}
	
	public void reset() {
		statsTime = -1;
	}
	
	public DenseMatrix64F getAllValues(double currentTime, EventNetwork g) {
		if(currentTime > statsTime) {
			updateDegreeStats(currentTime, g);
		}
		
		DenseMatrix64F ret = m_fullDegreeStats.copy();
		if(m_base == Base.SRC) {
			return ret;
		} else{
			CommonOps.transpose(ret);
			return ret;
		}
		
		//return m_fullDegreeStats.copy();
	}
	
	@Override
	public double getValue(String source, String target, double currentTime,
			EventNetwork g) {
		
		if(currentTime > statsTime) {
			updateDegreeStats(currentTime, g);
		}
		
		int nodeIndex;
		if (m_base == Base.SRC) {
			nodeIndex = g.getNodeIndex(source);
		} else {
			nodeIndex = g.getNodeIndex(target);
		}
		
		return m_degreeStats.get(nodeIndex, 0);
		
		//System.out.println("DegreeStat " + source +", " + target +"," + currentTime);
		
		/*
		double ret = 0;
		
		//retrieve the node
		Node n;
		if (m_base == Base.SRC) {
			n = g.getNode(source);
		} else {
			n = g.getNode(target);
		}
		
		//m_direction = EventNetwork.Direction.SYM;
		EdgeCursor ec = g.getEdges(n, m_direction);
		//System.out.println("ec.size: " + ec.size());
		
		//((Edge)ec.current()).source().
		
		while (ec.ok()) {
			//System.out.println("((Edge)ec.current()).source(): " + ((Edge)ec.current()).source() );
			//System.out.println("m_attrName: " + m_attrName);
			//System.out.println("m_direction: " + m_direction);
			//System.out.println("edge value: " + g.getEdgeValue((Edge)ec.current(), m_attrName, currentTime));
			ret += g.getEdgeValue((Edge)ec.current(), m_attrName, currentTime);
			ec.next();
		}
		
		return ret;
		
		/* */
	
	}
	
	private void updateDegreeStats(double currentTime, EventNetwork g) {
		DenseMatrix64F edgeMatrix = g.getEdgeMatrix(m_attrName, currentTime);
		m_degreeStats = new DenseMatrix64F(edgeMatrix.numRows, 1);
		DenseMatrix64F expander = new DenseMatrix64F(1, edgeMatrix.numCols);
		m_fullDegreeStats = new DenseMatrix64F(edgeMatrix.numRows, edgeMatrix.numCols);
		
		CommonOps.set(expander, 1);
		
		if(m_direction == Direction.IN) {
			CommonOps.sumCols(edgeMatrix, m_degreeStats);
		} else if(m_direction == Direction.OUT) {
			CommonOps.sumRows(edgeMatrix, m_degreeStats);
		} else if(m_direction == Direction.SYM) {
			DenseMatrix64F statsMatrix2 = new DenseMatrix64F(edgeMatrix.numRows, 1);
			
			CommonOps.sumCols(edgeMatrix, m_degreeStats);
			CommonOps.sumRows(edgeMatrix, statsMatrix2);
			
			CommonOps.addEquals(m_degreeStats, statsMatrix2);
		}
		
		//expand to full matrix with a vector of 1's
		CommonOps.mult(m_degreeStats, expander, m_fullDegreeStats);
			
		//REMEMBER TO UPDATE THE STATS TIME
		statsTime = currentTime;
	}

}
