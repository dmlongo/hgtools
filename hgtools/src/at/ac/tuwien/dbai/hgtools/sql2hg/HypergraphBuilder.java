package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.alg.util.UnionFind;

import at.ac.tuwien.dbai.hgtools.hypergraph.Edge;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;

public class HypergraphBuilder {

	private static String getFullName(String table, String col) {
		return table + "." + col;
	}

	private Hypergraph h;
	private int nextVar;
	private HashMap<String, String> colToVar;
	// private HashMap<String, List<String>> varToCol;
	private UnionFind<String> vars;

	public HypergraphBuilder() {
		h = new Hypergraph();
		nextVar = 0;
		colToVar = new HashMap<>();
		// varToCol = new HashMap<>();
		vars = new UnionFind<String>(new HashSet<String>());
	}

	// TODO views are not considered :(
	// for each defining predicate add a new edge
	public void buildEdge(Predicate table) {
		if (table instanceof ViewPredicate) {
			buildEdge((ViewPredicate) table);
			return;
		}
		String tableAlias = table.getAlias();
		Edge e = new Edge(tableAlias);
		for (String attr : table) {
			String var = "X" + nextVar++;
			String col = getFullName(tableAlias, attr);
			addMappings(col, var);
			e.addVertex(var);
			vars.addElement(var);
		}
		h.addEdge(e);
	}

	private void addMappings(String col, String var) {
		colToVar.put(col, var);
		// if (varToCol.get(var) == null) {
		// varToCol.put(var, new LinkedList<>());
		// }
		// varToCol.get(var).add(col);
	}

	public void buildEdge(ViewPredicate table) {
		String tableAlias = table.getAlias();
		for (Predicate pred : table.getDefiningPredicates()) {
			String predCompleteName = getFullName(tableAlias, pred.getAlias());
			// TODO pred could be a view and its name should be
			// prolonged even more
			Edge e = new Edge(predCompleteName);
			for (String attr : pred) {
				String var = "X" + nextVar++;
				String col = getFullName(predCompleteName, attr);
				addMappings(col, var);
				e.addVertex(var);
				vars.addElement(var);
			}
			h.addEdge(e);
		}
	}

	public void buildJoin(Equality join) {
		String col1 = join.getPredicate1().getDefiningAttribute(join.getAttribute1());
		String col2 = join.getPredicate2().getDefiningAttribute(join.getAttribute2());
		String var1 = colToVar.get(col1);
		String var2 = colToVar.get(col2);
		vars.union(var1, var2);
	}

	public void buildViewJoin(String viewAlias, Equality join) {
		String col1 = join.getPredicate1().getDefiningAttribute(join.getAttribute1());
		String col2 = join.getPredicate2().getDefiningAttribute(join.getAttribute2());
		String var1 = colToVar.get(getFullName(viewAlias, col1));
		String var2 = colToVar.get(getFullName(viewAlias, col2));
		vars.union(var1, var2);
	}
	
	public Hypergraph getHypergraph() {
		System.out.println(vars);
		for (Edge e : h.getEdges()) {
			for (String v : e.getVertices()) {
				String newName = vars.find(v);
				e.renameVertex(v, newName);
			}
		}
		return h;
	}

	public HashMap<String, List<String>> getVarToColMapping() {
		HashMap<String, List<String>> varToCol = new HashMap<>();
		for (String col : colToVar.keySet()) {
			String var = vars.find(colToVar.get(col));
			if (!varToCol.containsKey(var)) {
				varToCol.put(var, new LinkedList<>());
			}
			varToCol.get(var).add(col);
		}
		return varToCol;
	}

}
