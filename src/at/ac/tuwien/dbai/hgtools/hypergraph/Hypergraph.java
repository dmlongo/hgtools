package at.ac.tuwien.dbai.hgtools.hypergraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.dbai.hgtools.util.CombinationIterator;
import at.ac.tuwien.dbai.hgtools.util.PowerSetIterator;
import at.ac.tuwien.dbai.hgtools.util.Writable;

public class Hypergraph implements Writable {

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
		StringBuilder sb = new StringBuilder(200);
		for (String line : toFile()) {
			sb.append(line);
			sb.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}

	@Override
	public List<String> toFile() {
		List<String> out = new ArrayList<>(edges.size());
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

	public List<String> toPaceFile() {
		List<String> out = new ArrayList<>(edges.size() + 1);
		int n = cntVertices();
		int m = cntEdges();
		out.add("p htd " + n + " " + m + "\n");
		int nextEdge = 1;
		int nextVertex = 1;
		HashMap<String, Integer> s2i = new HashMap<>();
		for (Edge e : edges) {
			StringBuilder line = new StringBuilder(200);
			line.append(nextEdge++);
			for (Iterator<String> it = e.getVertices().iterator(); it.hasNext();) {
				String v = it.next();
				if (s2i.putIfAbsent(v, nextVertex) == null) {
					nextVertex++;
				}
				int iV = s2i.get(v);
				line.append(' ');
				line.append(iV);
				if (!it.hasNext()) {
					line.append('\n');
				}
			}
			out.add(line.toString());
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

	public int arity() {
		int maxArity = 0;

		for (Edge e : edges) {
			int arity = e.getVertices().size();
			if (arity > maxArity)
				maxArity = arity;
		}

		return maxArity;
	}

	public int cntBip(int k) {
		int maxBip = 0;

		if (k <= edges.size()) {
			CombinationIterator<Edge> cit = new CombinationIterator<>(edges, k);
			while (cit.hasNext()) {
				Collection<Edge> subset = cit.next();
				Iterator<Edge> it = subset.iterator();
				Edge e = it.next();
				HashSet<String> inter = new HashSet<>(e.getVertices());

				while (it.hasNext()) {
					inter.retainAll(it.next().getVertices());
				}

				if (inter.size() > maxBip)
					maxBip = inter.size();
			}
		}

		return maxBip;
	}

	public int vcDimension() {
		int maxVC = (int) Math.floor(Math.log(cntEdges()) / Math.log(2));
		int i;

		// Find the maximum cardinality of a shattered subset of V
		for (i = 1; i <= maxVC; i++) {
			boolean shattered = false;

			// For each subset X of size vc check if it is shattered, if X is shattered then
			// vc is at least i
			CombinationIterator<String> cit = new CombinationIterator<>(vertices, i);
			while (cit.hasNext() && !shattered) {
				boolean checkX = true;
				Collection<String> setX = cit.next();
				PowerSetIterator<String> itPSetX = new PowerSetIterator<>(setX);

				// For each A \subseteq X check if there is an edge s.t. A = X \cap e
				// if there is a subset such that this check fails (checkX = false), then X is
				// not shattered.
				while (itPSetX.hasNext() && checkX) {
					Set<String> psetX = itPSetX.next();
					boolean edgeFound = false;

					for (Iterator<Edge> it = edges.iterator(); it.hasNext() && !edgeFound;) {
						Collection<String> helpX = new ArrayList<>(setX);
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

}
