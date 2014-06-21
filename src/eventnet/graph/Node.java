package eventnet.graph;

import eventnet.graph.*;

public class Node {
    private static final long serialVersionUID = 1L;
    protected final long nodeId;
    private final Graph parentGraph;
    
    public Node(long nid, Graph parent) {
        nodeId = nid;
        parentGraph = parent;
    }

    public Edge getEdgeTo(Node trg) {
        return parentGraph.getEdge(this, trg);
    }
}
