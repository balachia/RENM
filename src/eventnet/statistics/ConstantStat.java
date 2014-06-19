package eventnet.statistics;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import eventnet.EventNetwork;

public class ConstantStat implements Statistic {
	DenseMatrix64F m_constants;

	@Override
	public String getName() {
		return "Constant";
	}

	@Override
	public double getValue(String source, String target, double currentTime,
			EventNetwork g) {
		return 1.0;
	}

	@Override
	public DenseMatrix64F getAllValues(double currentTime, EventNetwork g) {
		if(m_constants == null) {
			int size = g.getNetworkSize();
			m_constants = new DenseMatrix64F(size,size);
			CommonOps.set(m_constants,1.0);
		}
		
		return m_constants;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

}
