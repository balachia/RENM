package eventnet.statistics;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import eventnet.EventNetwork;
import eventnet.EventNetwork.Direction;
import eventnet.statistics.DegreeStat.Base;

public class NormDirectRelationStat implements Statistic {

	private String m_attrName;
	private Direction m_direction;
	
	private double statsTime = -1;
	private DenseMatrix64F m_normStats;
	
	public NormDirectRelationStat(String attrName, Direction dir) {
		m_attrName = attrName;
		m_direction = dir;
	}
	
	@Override
	public String getName() {
		String res = "NormRelCov (";
		res += m_direction.toString() + " " + m_attrName + ")";
		return res;
	}

	@Override
	public double getValue(String source, String target, double currentTime,
			EventNetwork g) {
		
		if(currentTime > statsTime) {
			updateDegreeStats(currentTime, g);
		}
		
		int srcIndex = g.getNodeIndex(source);
		int trgIndex = g.getNodeIndex(target);
		
		return m_normStats.get(srcIndex, trgIndex);
	}

	@Override
	public DenseMatrix64F getAllValues(double currentTime, EventNetwork g) {
		if(currentTime > statsTime) {
			updateDegreeStats(currentTime, g);
		}
		
		return m_normStats.copy();
	}

	@Override
	public void reset() {
		statsTime = -1;
	}
	
	private void updateDegreeStats(double currentTime, EventNetwork g) {
		DenseMatrix64F edgeMatrix = g.getEdgeMatrix(m_attrName, currentTime);
		
		//get shit into row -> column format
		if(m_direction == Direction.IN) {
			CommonOps.transpose(edgeMatrix);
		} else if (m_direction == Direction.SYM){
			DenseMatrix64F edgeMatrixT = edgeMatrix.copy();
			CommonOps.trace(edgeMatrixT);
			CommonOps.addEquals(edgeMatrix, edgeMatrixT);
		}
		
		//get row sums
		DenseMatrix64F rowSum = new DenseMatrix64F(edgeMatrix.numRows, 1);
		DenseMatrix64F expander = new DenseMatrix64F(1, edgeMatrix.numCols);
		DenseMatrix64F rowSums = new DenseMatrix64F(edgeMatrix.numRows, edgeMatrix.numCols);
		
		CommonOps.set(expander,1);
		CommonOps.sumRows(edgeMatrix, rowSum);
		
		//0-check
		//there has got to be a cleverer way to do this
		for(int i = 0; i < rowSum.numRows; i++) {
			if(rowSum.get(i, 0) == 0.0) {
				rowSum.set(i, 0, 1.0);
			}
		}
		CommonOps.mult(rowSum, expander, rowSums);
		
		//NORMALIZE!
		CommonOps.elementDiv(edgeMatrix,rowSums);
		
		m_normStats = edgeMatrix;
			
		//REMEMBER TO UPDATE THE STATS TIME
		statsTime = currentTime;
	}

}
