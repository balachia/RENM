package eventnet.statistics;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import eventnet.EventNetwork;
import eventnet.EventNetwork.Direction;

public class TwoPathsStat implements Statistic {

	private Direction m_dir1;
	private Direction m_dir2;
	private String m_attr1;
	private String m_attr2;
	
	private double statsTime = -1;
	private DenseMatrix64F m_twoPaths;
	
	public TwoPathsStat(Direction dir1, String attr1, Direction dir2, String attr2) {
		m_dir1 = dir1;
		m_dir2 = dir2;
		m_attr1 = attr1;
		m_attr2 = attr2;
	}
	
	@Override
	public String getName() {
		String res = "Two-Paths (";
		res += m_dir1.toString() + " " + m_attr1 + ", ";
		res += m_dir2.toString() + " " + m_attr2 + ")";
		return res;
	}

	@Override
	public double getValue(String source, String target, double currentTime,
			EventNetwork g) {

		double ret = 0;
		
		//TODO: check that we're working on the same network
		
		if(currentTime > statsTime) {
			updateStats(currentTime, g);
		}

		int srcIndex = g.getNodeIndex(source);
		int trgIndex = g.getNodeIndex(target);
		
		ret =  m_twoPaths.get(srcIndex, trgIndex);
		
		return ret;
	}

	@Override
	public DenseMatrix64F getAllValues(double currentTime, EventNetwork g) {
		if(currentTime > statsTime) {
			updateStats(currentTime, g);
		}
		
		return m_twoPaths.copy();
	}

	@Override
	public void reset() {
		statsTime = -1;
	}
	
	private void updateStats(double currentTime, EventNetwork g) {
		DenseMatrix64F edgeMatrix1 = g.getEdgeMatrix(m_attr1, currentTime);
		DenseMatrix64F edgeMatrix2 = g.getEdgeMatrix(m_attr2, currentTime);
		
		//System.out.println("Updating triangle statistics");
		
		//add all 2-paths in given direction
		//or for sym, add all 2-paths in all 4 direction combinations: in-in, in-out, out-n, out-out
		
		if( m_dir1 == Direction.IN ) {
			CommonOps.transpose(edgeMatrix1);
		}
		
		if( m_dir2 == Direction.IN ) {
			CommonOps.transpose(edgeMatrix2);
		}
		
		///HAHAHA MATRIX MULTIPLICATION IS DISTRIBUTIVE WAOOOSH
		if( m_dir1 == Direction.SYM ) {
			DenseMatrix64F edgeMatrix1T = edgeMatrix1.copy();
			CommonOps.transpose(edgeMatrix1T);
			CommonOps.addEquals(edgeMatrix1, edgeMatrix1T);
		}
		
		if( m_dir2 == Direction.SYM ) {
			DenseMatrix64F edgeMatrix2T = edgeMatrix2.copy();
			CommonOps.transpose(edgeMatrix2T);
			CommonOps.addEquals(edgeMatrix2, edgeMatrix2T);
		}
		
		//this just sets it to the right size
		m_twoPaths = new DenseMatrix64F(edgeMatrix1);
		//m_degreeStats = matrixSqrt(m_degreeStats);
		
		CommonOps.mult(edgeMatrix1, edgeMatrix2, m_twoPaths);

		statsTime = currentTime;
	}

}
