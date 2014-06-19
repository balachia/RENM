package eventnet.rate;

public class EdgeMarker {
	
	private String _source;
	private String _target;
	
	public EdgeMarker(String source, String target){
		_source = source;
		_target = target;
	}
	
	public String getSource(){
		return _source;	
	}
	
	public String getTarget(){
		return _target;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_source == null) ? 0 : _source.hashCode());
		result = prime * result + ((_target == null) ? 0 : _target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EdgeMarker other = (EdgeMarker) obj;
		if (_source == null) {
			if (other._source != null)
				return false;
		} else if (!_source.equals(other._source))
			return false;
		if (_target == null) {
			if (other._target != null)
				return false;
		} else if (!_target.equals(other._target))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EdgeMarker [source=" + _source + ", target=" + _target + "]";
	}
	

}
