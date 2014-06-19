package eventnet.functions;

import eventnet.EventNetwork;

public class Invert implements WeightFunction {
	private double m_pivot;
	
	public Invert(double pivot) {
		m_pivot = pivot;
	}
	
	@Override
	public String getName() {
		return "Invert " + m_pivot;
	}

	@Override
	public double getIncrement(EventNetwork g, String source, String target,
			double currentTime, double weight) {
		// TODO Auto-generated method stub
		return m_pivot - weight;
	}

}
