package eventnet.functions;

import eventnet.EventNetwork;

public class Threshold implements WeightFunction {
	private double m_threshold;
	
	public Threshold(double threshold) {
		m_threshold = threshold;
	}
	
	@Override
	public String getName() {
		return "Threshold " + m_threshold;
	}

	@Override
	public double getIncrement(EventNetwork g, String source, String target,
			double currentTime, double weight) {
		if(weight > m_threshold) {
			return 1.0;
		} else {
			return 0.0;
		}
	}

}
