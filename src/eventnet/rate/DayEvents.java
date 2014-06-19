package eventnet.rate;

import java.util.ArrayList;

public class DayEvents {
	
	private ArrayList<double[]> _eventRateStatsList;
	private ArrayList<double[]> _ijRateStats;
	private int _ijDyadsWithNoEdges;
	private double _timePeriod;
	
	public double get_timePeriod() {
		return _timePeriod;
	}

	public void set_timePeriod(double timePeriod) {
		_timePeriod = timePeriod;
	}
	
	public int get_ijDyadsWithNoEdges() {
		return _ijDyadsWithNoEdges;
	}

	public void set_ijDyadsWithNoEdges(int ijDyadsWithNoEdges) {
		_ijDyadsWithNoEdges = ijDyadsWithNoEdges;
	}

	public void addEventRateStats(double[] eventRateStats){
		if(_eventRateStatsList == null)
			_eventRateStatsList = new ArrayList<double[]>();
		
		_eventRateStatsList.add(eventRateStats);
	}
	
	public ArrayList<double[]> getEventRateStatsList() {
		return _eventRateStatsList;
	}

	public ArrayList<double[]> get_ijRateStats() {
		return _ijRateStats;
	}


	public void set_ijRateStats(ArrayList<double[]> ijRateStats) {
		_ijRateStats = ijRateStats;
	}
	

}
