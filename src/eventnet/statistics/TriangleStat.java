package eventnet.statistics;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import eventnet.EventNetwork;
import eventnet.EventNetwork.Direction;
//import eventnet.statistics.DegreeStat.Base;
//import y.base.Edge;
//import y.base.EdgeCursor;
//import y.base.Node;

public class TriangleStat implements Statistic {


		private static final long serialVersionUID = 1L;
		
		private Direction m_dir1;
		private Direction m_dir2;
		private String m_attr1;
		private String m_attr2;
		
		//the timestamp of the last statistics set we pulled out
		//TODO: not sure what to do about initial value here
		//problem if we want to reload stats after they get updated
		private double statsTime = -1;
		private DenseMatrix64F m_degreeStats;
		
		public TriangleStat(Direction dir1, String attr1, Direction dir2, String attr2) {
			m_dir1 = dir1;
			m_dir2 = dir2;
			m_attr1 = attr1;
			m_attr2 = attr2;
		}
		
		public String getName() {
			String res = "Triangle (";
			res += m_dir1.toString() + " " + m_attr1 + ", ";
			res += m_dir2.toString() + " " + m_attr2 + ")";
			return res;
		}
		
		public void reset() {
			statsTime = -1;
		}
		
		public DenseMatrix64F getAllValues(double currentTime, EventNetwork g) {
			//TODO: double check that we're onthe same network, e.g. thru hash?
			if(currentTime > statsTime) {
				updateDegreeStats(currentTime, g);
			}
			
			return m_degreeStats.copy();
		}
		
		@Override
		public double getValue(String source, String target, double currentTime,
				EventNetwork g) {
			
			double ret = 0;
			
			//TODO: check that we're working on the same network
			
			if(currentTime > statsTime) {
				updateDegreeStats(currentTime, g);
			}

			int srcIndex = g.getNodeIndex(source);
			int trgIndex = g.getNodeIndex(target);
			
			ret =  m_degreeStats.get(srcIndex, trgIndex);
			
			
			//System.out.println("Triangle get, " + currentTime + " " + ret + " " + source + " " + target);
			
			/*
			
			//retrieve the nodes
			Node n1 = g.getNode(source);
			Node n2 = g.getNode(target);
			String attr1, attr2;
			Direction dir1, dir2;
			
			
			//if necessary swap nodes
			if (g.getDegree(n1, m_dir1) <= g.getDegree(n2, m_dir2)) {
				attr1 = m_attr1; 
				attr2 = m_attr2;
				dir1 =  m_dir1;
				dir2 = m_dir2;
			} else {
				Node tmp = n1;
				n1 = n2;
				n2 = tmp;
				attr1 = m_attr2; 
				attr2 = m_attr1;
				dir1 =  m_dir2;
				dir2 = m_dir1;
			}

			//now 1 is guaranteed <= 2
			
			boolean debug = false;
			/*if(m_attr1.equals("neg") && m_attr2.equals("pos") && source.equals("UAR") && target.equals("KUW")){
				System.out.println("TriangularSTat.getValue: " + source + ", " + target + ", " + m_attr1 + ", " + m_attr2);
				debug = true;
			}
			*/
			
			/*
			if(debug){
				//System.out.println("n1, n2, attr1, attr2: " + n1.index() + ", " + n2.index() + ", " + attr1 + ", " + attr2);
			}
			
		    EdgeCursor ec = g.getEdges(n1, dir1);
			while (ec.ok()) {
				Edge e1 = ((Edge)ec.current());
				Node opp =  e1.opposite(n1);
				if(debug){
					System.out.println("e1: " + e1.source() + ", " + e1.target());
				}
				
				if (dir2 != Direction.SYM) {
					Edge e2 = null;
					switch (dir2) {
						case IN:  e2 = opp.getEdgeTo(n2); break;
						case OUT: e2 = opp.getEdgeFrom(n2); break;
					}
				
					if (e2 != null) {
						//such an edge exists the triangle is complete
						ret += (g.getEdgeValue(e1, attr1, currentTime) * g.getEdgeValue(e2, attr2, currentTime));
					}
				} else {
					Edge e2 = opp.getEdgeTo(n2);					
					if (e2 != null) {
						if(debug){
							System.out.println("e2: " + e2.source() + ", " + e2.target());
							System.out.println("e1.val: " + g.getEdgeValue(e1, attr1, currentTime));
							System.out.println("e2.val: " + g.getEdgeValue(e2, attr2, currentTime));
							System.out.println("ret +=: " + (g.getEdgeValue(e1, attr1, currentTime) * g.getEdgeValue(e2, attr2, currentTime)));
							System.out.println("");
						}
						//such an edge exists the triangle is complete
						ret += (g.getEdgeValue(e1, attr1, currentTime) * g.getEdgeValue(e2, attr2, currentTime));
						
					}
					Edge e3 = opp.getEdgeFrom(n2);					
					if (e3 != null) {
						if(debug){
							System.out.println("e3: " + e3.source() + ", " + e3.target());
							System.out.println("e1.val: " + g.getEdgeValue(e1, attr1, currentTime));
							System.out.println("e3.val: " + g.getEdgeValue(e3, attr2, currentTime));
							System.out.println("ret +=: " + (g.getEdgeValue(e1, attr1, currentTime) * g.getEdgeValue(e3, attr2, currentTime)));
							System.out.println("");
						}
						//such an edge exists the triangle is complete
						ret += (g.getEdgeValue(e1, attr1, currentTime) * g.getEdgeValue(e3, attr2, currentTime));
						
					}	

				}
				
				ec.next();
			}
			
			
			if(debug){
				System.out.println("");
				System.out.println("final ret: " + ret);
				System.out.println("Math.sqrt(ret): " + Math.sqrt(ret));
			}
			
			*/
			
			//return Math.sqrt(ret);
			return ret;
			
		}
		
		private DenseMatrix64F matrixSqrt(DenseMatrix64F matrix) {
			DenseMatrix64F ret = matrix.copy();
			
			for(int i = 0; i < matrix.numRows; i++) {
				for(int j = 0; j < matrix.numCols; j++) {
					ret.set(i,j, Math.sqrt(matrix.get(i, j)));
				}
			}
			
			return ret;
		}
		
		private void updateDegreeStats(double currentTime, EventNetwork g) {
			DenseMatrix64F edgeMatrix1 = g.getEdgeMatrix(m_attr1, currentTime);
			DenseMatrix64F edgeMatrix2 = g.getEdgeMatrix(m_attr2, currentTime);
			
			//System.out.println("Updating triangle statistics");
			
			//add all 2-paths in given direction
			//or for sym, add all 2-paths in all 4 direction combinations: in-in, in-out, out-n, out-out
			
			if( m_dir1 == Direction.IN ) {
				CommonOps.transpose(edgeMatrix1);
			}
			
			if( m_dir2 == Direction.IN ) {
				CommonOps.transpose(edgeMatrix2);
			}
			
			///HAHAHA MATRIX MULTIPLICATION IS DISTRIBUTIVE WAOOOSH
			if( m_dir1 == Direction.SYM ) {
				DenseMatrix64F edgeMatrix1T = edgeMatrix1.copy();
				CommonOps.transpose(edgeMatrix1T);
				CommonOps.addEquals(edgeMatrix1, edgeMatrix1T);
			}
			
			if( m_dir2 == Direction.SYM ) {
				DenseMatrix64F edgeMatrix2T = edgeMatrix2.copy();
				CommonOps.transpose(edgeMatrix2T);
				CommonOps.addEquals(edgeMatrix2, edgeMatrix2T);
			}
			
			//this just sets it to the right size
			m_degreeStats = new DenseMatrix64F(edgeMatrix1);
			//m_degreeStats = matrixSqrt(m_degreeStats);
			
			CommonOps.mult(edgeMatrix1, edgeMatrix2, m_degreeStats);
			m_degreeStats = matrixSqrt(m_degreeStats);

			statsTime = currentTime;
		}
		

}

