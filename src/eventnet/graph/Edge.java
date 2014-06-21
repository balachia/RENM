package eventnet.graph;

import eventnet.graph.*;
// import org.apache.commons.lang3.tuple.Pair;

public class Edge {
    private static final long serialVersionUID = 1L;
    
    // private Pair<Node, Node> edge;
    private final Node src;
    private final Node trg;
    private final Graph parent;
    
    public Edge(Node mySrc, Node myTrg, Graph myParent) {
        src = mySrc;
        trg = myTrg;
        parent = myParent;
    }

}
