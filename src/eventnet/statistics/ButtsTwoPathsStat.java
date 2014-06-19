package eventnet.statistics;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import eventnet.EventNetwork;
import eventnet.EventNetwork.Direction;

public class ButtsTwoPathsStat implements Statistic {

	private Direction m_dir1;
	private Direction m_dir2;
	private String m_attr1;
	private String m_attr2;
	
	private double statsTime = -1;
	private DenseMatrix64F m_twoPaths;
	
	public ButtsTwoPathsStat(Direction dir1, String attr1, Direction dir2, String attr2) {
		m_dir1 = dir1;
		m_dir2 = dir2;
		m_attr1 = attr1;
		m_attr2 = attr2;
	}
	
	@Override
	public String getName() {
		String res = "Butts Two-Paths (";
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
		//carter butts style two paths
		//see his paper "A relational event framework for social action"
		//for a source and a target node
		//we look at all possible intermediaries and take the minimum of the path from source to inter, or inter to target
		//and all all those up
		
		//there's got to be a more efficient way to do this than i am
		
		DenseMatrix64F edgeMatrix1 = g.getEdgeMatrix(m_attr1, currentTime);
		DenseMatrix64F edgeMatrix2 = g.getEdgeMatrix(m_attr2, currentTime);
		
		//matrix building grunt work
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
		
		
		//and now let's do the adding up
		m_twoPaths = new DenseMatrix64F(edgeMatrix1.numRows, edgeMatrix2.numCols);
		for(int src = 0; src < edgeMatrix1.numRows; src++) {
			for(int trg = 0; trg < edgeMatrix2.numCols; trg++) {
				double summand = 0.0;
				for(int proxy = 0; proxy < edgeMatrix1.numCols; proxy++) {
					summand += Math.min(edgeMatrix1.get(src, proxy), edgeMatrix2.get(proxy, trg));
				}
				m_twoPaths.set(src, trg, summand);
			}
		}
		
		//VITAL SIDE EFFECT STEP
		statsTime = currentTime;
	}
}
