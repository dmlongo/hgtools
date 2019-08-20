package at.ac.tuwien.dbai.hgtools.hypergraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.dbai.hgtools.util.CombinationIterator;
import at.ac.tuwien.dbai.hgtools.util.PowerSetIterator;

public class Hypergraph {
	private Set<Edge> edges;
	private Set<String> vertices;

	public Hypergraph() {
		edges = new HashSet<>();
		vertices = new HashSet<>();
	}

	public void addEdge(Edge e) {
		edges.add(e);
		vertices.addAll(e.getVertices());
	}

	public Set<Edge> getEdges() {
		return edges;
	}
	
	public int cntVertices() {
		return vertices.size();
	}

	public int cntEdges() {
		return edges.size();
	}

	@Override
	public String toString() {
		String s = "";
		for (String line : toFile()) {
			s += line + System.getProperty("line.separator");
		}
		return s;
	}

	public List<String> toFile() {
		List<String> out = new LinkedList<String>();
		for (Iterator<Edge> it = edges.iterator(); it.hasNext();) {
			String e = it.next().toString();
			if (it.hasNext())
				e += ",";
			else
				e += ".";
			out.add(e);
		}
		return out;

	}

	public int degree() {
		int maxDegree = 0;

		for (String v : vertices) {
			int degree = 0;
			for (Edge e : edges)
				if (e.contains(v))
					degree++;
			if (degree > maxDegree)
				maxDegree = degree;
		}

		return maxDegree;
	}

	public int cntBip(int k) {
		int maxBip = 0;

		if (k <= edges.size()) {
			CombinationIterator<Edge> cit = new CombinationIterator<Edge>(edges, k);
			while (cit.hasNext()) {
				Collection<Edge> subset = cit.next();
				HashSet<String> inter = new HashSet<String>();

				Iterator<Edge> it = subset.iterator();
				Edge e = it.next();
				inter = new HashSet<String>(e.getVertices());

				while (it.hasNext()) {
					inter.retainAll(it.next().getVertices());
				}

				if (inter.size() > maxBip)
					maxBip = inter.size();
			}
		}

		return maxBip;
	}

	public int VCdimension() {
		int maxVC = (int) Math.floor(((Math.log(cntEdges()) / Math.log(2))));
		int i;

		// Find the maximum cardinality of a shattered subset of V
		for (i = 1; i <= maxVC; i++) {
			boolean shattered = false;

			// For each subset X of size vc check if it is shattered, if X is shattered then
			// vc is at least i
			CombinationIterator<String> cit = new CombinationIterator<String>(vertices, i);
			while (cit.hasNext() && !shattered) {
				boolean checkX = true;
				Collection<String> setX = cit.next();
				PowerSetIterator<String> itPSetX = new PowerSetIterator<String>(setX);

				// For each A \subseteq X check if there is an edge s.t. A = X \cap e
				// if there is a subset such that this check fails (checkX = false), then X is
				// not shattered.
				while (itPSetX.hasNext() && checkX) {
					Set<String> psetX = itPSetX.next();
					boolean edgeFound = false;

					for (Iterator<Edge> it = edges.iterator(); it.hasNext() && !edgeFound;) {
						Collection<String> helpX = new ArrayList<String>(setX);
						helpX.retainAll(it.next().getVertices());
						if (helpX.size() == psetX.size() && helpX.containsAll(psetX)) {
							edgeFound = true;
						}
					}

					if (!edgeFound)
						checkX = false;

				}

				if (checkX)
					shattered = true;

			}

			if (!shattered)
				return i - 1;
		}

		return i - 1;
	}

	/*
	 * Implementation might have errors.
	 * 
	 * public int VCdimension() { boolean found = false; int maxVC = (int)
	 * Math.floor(((Math.log(cntEdges())/Math.log(2)))); int vc;
	 * 
	 * for (vc = 1; vc <= maxVC; vc++ ) { found = false; CombinationIterator<String>
	 * cit = new CombinationIterator<String>(vertices,vc); while (cit.hasNext() &&
	 * !found) { PowerSetIterator<String> psetX = new
	 * PowerSetIterator<String>(cit.next()); LinkedList<Integer> usedE = new
	 * LinkedList<Integer>(); ArrayList<Edge> edges = new
	 * ArrayList<Edge>(this.getEdges()); if (VCdimHelper(psetX,edges,usedE)) found =
	 * true; } if (!found) return vc-1; }
	 * 
	 * 
	 * return vc-1; }
	 * 
	 * private boolean VCdimHelper(PowerSetIterator<String> itPSetX, ArrayList<Edge>
	 * edges, LinkedList<Integer> usedEdges) { if (itPSetX.hasNext()) { Set<String>
	 * setX = itPSetX.next(); for (int i=0; i<edges.size() &&
	 * !usedEdges.contains(i); i++) { if
	 * (edges.get(i).getVertices().containsAll(setX)) { usedEdges.push(i); if
	 * (VCdimHelper(itPSetX,edges,usedEdges)) { return true; } else {
	 * usedEdges.pop(); } } } return false; } else return true; }
	 */
}
