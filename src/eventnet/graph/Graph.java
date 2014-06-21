package eventnet.graph;

import java.util.HashMap;
import java.util.HashSet;
import eventnet.graph.*;

public class Graph {
    private static final long serialVersionUID = 1L;
    
    //private HashMap<Node, HashSet<Node>> graph;
    private HashMap<Node, HashMap<Node, Edge>> graph;
    private long nextNid = 0;
    
    public Graph() {
        graph = new HashMap<Node, HashMap<Node, Edge>>();
    }
    
    public Node createNode() {
        Node ret = new Node(nextNid, this);
        nextNid++;
        
        // add thing to node map
        graph.put(ret, new HashMap<Node, Edge>());
        
        return ret;
    }
    
    public Edge createEdge(Node src, Node trg) {
        //nope, i want this to fail horribly if you pass in a bad node
        //at least for now
        //TODO: fix this shit
        //if(!graph.containsKey(src))
        if(hasEdge(src,trg)) {
            return graph.get(src).get(trg);
        } else {
            Edge newEdge = new Edge(src, trg, this);
            graph.get(src).put(trg, newEdge);
            return newEdge;
        }
    }
    
    public EdgeMap createEdgeMap() {
        return new EdgeMap(this);
    }
    
    public void disposeEdgeMap(EdgeMap em) {
        // don't gotta do anything
    }
    
    public Edge getEdge(Node src, Node trg) {
        if(hasEdge(src,trg)) {
            return graph.get(src).get(trg);
            //return new Edge(src,trg);
        } else {
            return null;
        }
    }
    
    public boolean hasEdge(Node src, Node trg) {
        return graph.get(src).containsKey(trg);
    }

}
