package eventnet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
//import java.util.Map;

import eventnet.helper.Pair;
import eventnet.statistics.Statistic;
import eventnet.graph.*;
//import y.base.Edge;
//import eventnet.graph.Edge;
//import y.base.EdgeCursor;
//import y.base.Graph;
//import y.base.Node;
//import eventnet.graph.Node;
//import y.base.EdgeMap;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
//import org.ejml.ops.MatrixVisualization;


/**
 * represents the transformation graph. Internally a y-files graph is used and extended. The
 * EventNetwork provides simple assessor methods that can be used by self defined statistics.
 * 	
 * @author Michael Zinsmaier, University of Konstanz
 *
 */
public class EventNetwork {
	
	/** defines the 3 possible edge selectors outgoing edges, incoming and both. */
	public enum Direction {OUT, IN, SYM};

	/** y-files graph */
	private Graph m_graph = new Graph();
	/** hashes all names of decayed (half time > 0) attributes to their half time */
	private HashMap<String, Double> m_attrName2HalfTime = new HashMap<String, Double>();
	/** hashes node names (unique string identifiers) to the actual nodes in the graph */
	private HashMap<String, Node> m_name2node = new HashMap<String, Node>();
	/** hashes attribute names to edge maps containing the correct double attribute */
	private HashMap<String, EdgeMap> m_attrName2EdgeMap = new HashMap<String, EdgeMap>();
	/** int attribute that holds the last update time of the edge (0..currentTime)	 */
	private EdgeMap edgeTime = m_graph.createEdgeMap();
	
	/** EJML graph implementation. */
	/** registry of statistics to use 
	 * not sure yet whether this is useful/necessary
	 * It is undesirable, from the perspective of minimizing interconnections w/in code */
	private Statistic[] m_statistics;
	/** holds current time of graph */
	//TODO: Initialize somewhere to something sensible
	private double ejmlEdgeTime;
	/** hashes node names to row/col indices in various matrices */
	private HashMap<String, Integer> m_name2index = new HashMap<String, Integer>();
	/** hashes attribute name to the edge matrix containing its raw edge weights */
	private HashMap<String, DenseMatrix64F> m_attrName2EdgeMatrix = new HashMap<String, DenseMatrix64F>();
	
	
	public int getNetworkSize() {
		return m_name2index.size();
	}
	
	/**
	 * creates an EventNetwork. The parameters have to be pair wise [0] belongs to [0], [1] to [1]...
	 * 
	 * @param attributes names of the attributes that should be processed
	 * @param t_halfs the half times of the attributes that should be processed. t_half <= 0 indicates no decay
	 */
	public EventNetwork(String[] attributes, double[] t_halfs) {
		//create all simulated attributes and store their half times
		for (int i = 0; i < attributes.length; i++) {
			//System.out.println("Half-life " + attributes[i] + " " + t_halfs[i]);
			if (t_halfs[i] > 0) {
				m_attrName2HalfTime.put(attributes[i], t_halfs[i]);
				addEdgeAttribute(attributes[i]);				
			}
		}
	}
	
	/**
	 * Adds new actors to the event network. This should only be called before any work is done on the network, as it buffers empty spots with zeros. Which is obviously not reflective of anything at all.
	 *
	 * @param uniqueActors the unique actors
	 */
	public void addActors(HashSet<String> uniqueActors) {
		int numActors = m_name2index.size();
		
		// give each new actor an index
		for(String actor : uniqueActors) {
			if(!m_name2index.containsKey(actor)) {
				m_name2index.put(actor, numActors);
				numActors += 1;
			}
		}
		
		//rebuild edgemaps, buffering new nodes as 0's
		//TODO: because this code was changed it is now partially redundant; check for errors and fix
		//TODO: this is hackish, having no central attribute repository
		for(String attr : m_attrName2EdgeMatrix.keySet()) {
			DenseMatrix64F edgeMatrix = m_attrName2EdgeMatrix.get(attr);
			if(edgeMatrix != null && edgeMatrix.data.length > 0) {
				DenseMatrix64F newMatrix = new DenseMatrix64F(numActors,numActors);
				CommonOps.insert(edgeMatrix, newMatrix, 1, 1);
			} else {
				//put in new 0-matrix
				addEdgeAttribute(attr);
				//m_attrName2EdgeMatrix.put(attr, new DenseMatrix64F(numActors, numActors));
			}
		}
	}

	/**
	 * updates an edge. If the edge didn't exist so far it is inserted into the graph. If the attribute is new to the graph
	 * the attribute will be added ...
	 * 
	 * Updating an edge enforces the edge to get the current time.
	 * 
	 * @param source unique string identifier of the source node
	 * @param target unique string identifier of the target node
	 * @param changes pairs of < attribute name, value change > defines which attributes should be changed
	 * @param currentTime the current time of the simulation. 
	 */
	public void updateEdgeAttributes(String source, String target, LinkedList<Pair<String, Double>> changes, double currentTime) {
		
		//System.out.println("UPDATE EDGE ATTRIBUTES: " + source + ", " + target + ", " + changes.size() + ", " + currentTime);
		
		//creates the edge if necessary
		Edge e = getEdge(source, target);
		
		for (Pair<String, Double> change : changes) {
			
			String attr = change.firstValue;
			//System.out.println("attr: " + attr);

			//get the current edge value for the attribute name if necessary
			//update the time
			double oldVal = getEdgeValue(e, attr, currentTime);
			double ejmlOldVal = getEdgeValue(source, target, attr, currentTime);
			//System.out.println("oldVal: " + oldVal);
			//System.out.println("change: " + change.secondValue);

			EdgeMap edgeM = m_attrName2EdgeMap.get(attr);
			DenseMatrix64F edgeMatrix = m_attrName2EdgeMatrix.get(attr);
			
			//change the value
			if (!m_attrName2HalfTime.containsKey(attr)) {
				//System.out.println("ADD ATTRIBUTE TO MAP");
				edgeM.setDouble(e, oldVal + change.secondValue);
				setEdgeValue(source, target, attr, oldVal + change.secondValue);
			} else {
				//time dependent old value has already been updated now add new value
				double tHalf = m_attrName2HalfTime.get(attr);
				edgeM.setDouble(e, oldVal + (change.secondValue * Math.log(2) / tHalf ));
				setEdgeValue(source, target, attr, oldVal + (change.secondValue * Math.log(2) / tHalf ));
				
				//System.out.println("Update the oldValue "  + (oldVal + (change.secondValue * Math.log(2) / tHalf )) );
			}
			
			/*
			 * 
			 *  insert code for updating rate attribute. 
			 * 
			 * 
			 */
						
		}
	}
	
	/**
	 * Gets the edge matrix for a given attribute.
	 *
	 * @param attribute the edge attribute to return
	 * @param currentTime the current time
	 * @return the edge weight matrix
	 */
	public DenseMatrix64F getEdgeMatrix(String attribute, double currentTime) {
		if(currentTime != ejmlEdgeTime) {
			updateNetworkTime(currentTime);
		}
		
		if(!m_attrName2EdgeMatrix.containsKey(attribute)) {
			addEdgeAttribute(attribute);
		}
		
		DenseMatrix64F edgeMatrix = m_attrName2EdgeMatrix.get(attribute);
		return edgeMatrix.copy();
	}
	
	/**
	 * Sets the value of the edge from src to trg for the given attribute. Does NOT update to current time.
	 *
	 * @param src the edge source node
	 * @param trg the edge target node
	 * @param attrName the attribute name
	 * @param newValue the new value of the edge
	 */
	public void setEdgeValue(String src, String trg, String attrName, double newValue) {
		int srcIndex = getNodeIndex(src);
		int trgIndex = getNodeIndex(trg);
		
		if(!m_attrName2EdgeMatrix.containsKey(attrName)) {
			addEdgeAttribute(attrName);
		}
		
		DenseMatrix64F edgeMatrix = m_attrName2EdgeMatrix.get(attrName);
		edgeMatrix.set(srcIndex, trgIndex, newValue);
	}
	
	/**
	 * Returns the value of the edge from src to trg. Updates the whole network if necessary.
	 * If the attribute doesn't exist, adds it, and returns 0.0;
	 *
	 * @param src the edge source node
	 * @param trg the edge target node
	 * @param attrName the attribute name
	 * @param currentTime the current time
	 * @return the edge value at the current time
	 */
	public double getEdgeValue(String src, String trg, String attrName, double currentTime) {
		if(currentTime != ejmlEdgeTime) {
			updateNetworkTime(currentTime);
		}
		
		int srcIndex = getNodeIndex(src);
		int trgIndex = getNodeIndex(trg);
		
		DenseMatrix64F edgeMatrix = m_attrName2EdgeMatrix.get(attrName);
		if(edgeMatrix == null) {
			addEdgeAttribute(attrName);
			return 0.0;
		} else {
			return edgeMatrix.get(srcIndex, trgIndex);
		}
	}
	
	/**
	 * if the attribute is a decayed attribute the whole edge is updated to the current time.
	 * if not it is enough to query the attribute value. If the attribute doesn't exist a new
	 * attribute will be added and the return value is 0.0
	 * 
	 * @param e the edge in the graph
	 * @param attrName name of the attribute that should be read
	 * @param currentTime the current time of the simulation
	 * @return the value of the attribute at the current time
	 */
	public Double getEdgeValue(Edge e, String attrName, double currentTime) {
		
		EdgeMap edgeM = m_attrName2EdgeMap.get(attrName);
		if (edgeM != null) { //existing attr?
			if (m_attrName2HalfTime.containsKey(attrName)) {
				//attr should be decayed => check if the time is correct
				updateTime(e, currentTime);
			}
			return edgeM.getDouble(e);
		} else {
			addEdgeAttribute(attrName);
			return 0.0;
		}		
	}
	
	/**
	 * Update the whole network to the current time. Specific to the matrix implementation of the event network.
	 *
	 * @param currentTime the current time
	 */
	public void updateNetworkTime(double currentTime) {
		double tDiff = currentTime - ejmlEdgeTime;
		
		if(tDiff < 0) {
			System.out.println("WARNING: Attempting to update network to earlier time");
		}
		
		if(tDiff == 0) {
			return;
		}
		
		//update each half-life available edge matrix
		for(String attribute : m_attrName2HalfTime.keySet()) {
		//for(Map.Entry<String, DenseMatrix64F> edgeMatrixEntry : m_attrName2EdgeMatrix.entrySet()) {
			DenseMatrix64F edgeMatrix = m_attrName2EdgeMatrix.get(attribute);
			if(edgeMatrix == null) {
				System.out.println("Unable to update time for attribute " + attribute);
				continue;
			}
			
			double tHalf = m_attrName2HalfTime.get(attribute);
			
			double scaleFactor = Math.exp(-(tDiff) * Math.log(2) / tHalf);
			//System.out.println("Updating network '" + attribute + "' with factor " + scaleFactor);
			
			CommonOps.scale(scaleFactor, edgeMatrix);
			
			m_attrName2EdgeMatrix.put(attribute, edgeMatrix);
		}
		
		//VITAL TO REMEMBER THIS
		ejmlEdgeTime = currentTime;
		
		//TODO: consider informing each of the statistics of change via registry
	}
	
	
	
	/**
	 * updates an edge to the currentTime. To achieve this all simulated attributes
	 * will be recalculated (using their half time). This has to be done
	 * before adding a new value to a simulated attribute 
	 * 
	 * @param e the edge in the graph
	 * @param currentTime the current time of the process
	 */
	private void updateTime(Edge e, double currentTime) {
		
		double eTime = edgeTime.getDouble(e);
		
		if (eTime < currentTime) {
			//update all simulated attributes of the edge
			for (String key : m_attrName2HalfTime.keySet()) {
				
				double tHalf = m_attrName2HalfTime.get(key);
				EdgeMap edgeM = m_attrName2EdgeMap.get(key);
				
				double timeFactor = Math.exp(-(currentTime - eTime) * Math.log(2) / tHalf);
				double newValue = (edgeM.getDouble(e) * timeFactor);

				edgeM.setDouble(e, newValue);
			}
			edgeTime.setDouble(e, currentTime);
		}
	}

	/**
	 * returns the specified edge. If necessary creates it.
	 * 
	 * @param source unique string identifier of the source node
	 * @param target unique string identifier of the target node
	 * @return the edge in the graph
	 */
	public Edge getEdge(String source, String target) {
		
		Node src = m_name2node.get(source);
		Node tar = m_name2node.get(target);
		
		if (src==null || tar==null) {
			return addEdge(src, tar, source, target);
		} else {
			Edge ret = src.getEdgeTo(tar);
			if (ret == null) {
				return addEdge(src, tar, source, target);
			} else {
				return src.getEdgeTo(tar);
			}
		}
	}

	/**
	 * Gets the index of the named node. Creates the new node if necessary.
	 *
	 * @param name the unique name of the node
	 * @return the index of the node with the given name
	 */
	public int getNodeIndex(String name) {
		if(!m_name2index.containsKey(name)) {
			//really this should probably throw an error as this is BAD BAD BAD behavior
			//it would invalidate certain prior ijstatistic calculations if used after start of simulation
			
			System.out.println("getNodeIndex adding actor " + name);
			HashSet<String> newActors = new HashSet<String>();
			newActors.add(name);
			
			addActors(newActors);
		}
		
		int nodeIndex = m_name2index.get(name);
		return nodeIndex;
	}
	
	/**
	 * @param name unique name of the node
	 * @return the node with the specified name. If not available a new node will be created.
	 */
	public Node getNode(String name) {
		Node n = m_name2node.get(name);	
		if (n == null) {
			n = addNode(name);
		}
		
		return n;
	}
	
	/**
	 * @param n an EXISTING node of the graph use getNode to get such a node
	 * @param dir one of the possible edge selectors
	 * @return the inDegree, outDegree, symetricDegree of the node
	 */
	/*public int getDegree(Node n, Direction dir) {
		switch (dir) {
			case IN: return n.inDegree();
			case OUT: return n.outDegree();
			case SYM: return n.degree();
		}
		//error default
		return 0;
	}*/
	
	/**
	 * @param n an EXISTING node of the graph use getNode to get such a node
	 * @param dir one of the possible edge selectors
	 * @return cursor over all incoming, outgoing, all edges of a node
	 */	
	/*public EdgeCursor getEdges(Node n, Direction dir) {
		switch (dir) {
			case IN: return n.inEdges();
			case OUT: return n.outEdges();
			case SYM: return n.edges();
		}
		//error
		return null;
	}*/
	
		
	/**
	 * creates a new edge in the graph
	 * 
	 * @param src the source node in the graph
	 * @param tar the target node in the graph
	 * @param source the unique string identifier of the source node
	 * @param target the unique string identifier of the target node
	 */
	private Edge addEdge(Node src, Node tar, String source, String target) {
		if (src == null) {
			src = addNode(source);
		}
		if (tar == null) {
			tar = addNode(target);
		}
		
		return m_graph.createEdge(src, tar); 
	}
	
	
	/**
	 * adds a node to the graph
	 * 
	 * @param name the unique string identifier of the node
	 * @return the node in the graph
	 */
	private Node addNode(String name) {
		Node tmp = m_graph.createNode();
		m_name2node.put(name, tmp);
		
		//System.out.println("EventNetwork.addNode: " + name + ", " + tmp.index());
		
		return tmp;
	}
	
	/**
	 * adds an edge attribute to the graph
	 * @param attrName the unique attribute name
	 * @return a edgemap to iterate over the newly created attribute
	 */
	private EdgeMap addEdgeAttribute(String attrName) {
//	private void addEdgeAttribute(String attrName) {	    
		EdgeMap edgeM = m_graph.createEdgeMap();
		m_attrName2EdgeMap.put(attrName, edgeM);
		
		//make sure not to overwrite existing edgeMatrix
		if(!m_attrName2EdgeMatrix.containsKey(attrName)) {
			int numActors = m_name2index.size();
			
			//don't add 0-sized matrices, they're bad
			//but this will cause problems if certain methods assume this creation
			if(numActors < 1) {
				return edgeM;
			}
			
			DenseMatrix64F newMatrix = new DenseMatrix64F(numActors,numActors);
			m_attrName2EdgeMatrix.put(attrName, newMatrix);
		}
		
		return edgeM;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void finalize() {		
		//according to the yfiles doc the maps have to be disposed
		//not sure if this holds true even for garbage collection
		//but safety first
		for (String key : m_attrName2EdgeMap.keySet()) {
			m_graph.disposeEdgeMap(m_attrName2EdgeMap.get(key));			
		}
	}
	
	
	
	
	
}
