package eventnet.graph;

import java.util.HashMap;
import eventnet.graph.*;

public class EdgeMap {
    private static final long serialVersionUID = 1L;
    private final Graph parent;
    
    private HashMap<Edge, Double> doubleVals;
    
    public EdgeMap(Graph myParent) {
        parent = myParent;
        doubleVals = new HashMap<Edge, Double>();
    }
    
    public void setDouble(Edge e, double val) {
        doubleVals.put(e, val);
    }
    
    public double getDouble(Edge e) {
        if(!doubleVals.containsKey(e)) {
            setDouble(e, 0);
        }
        
        return doubleVals.get(e);
    }
}
