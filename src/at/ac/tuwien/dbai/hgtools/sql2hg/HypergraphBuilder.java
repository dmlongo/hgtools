package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.alg.util.UnionFind;

import at.ac.tuwien.dbai.hgtools.hypergraph.Edge;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;

public class HypergraphBuilder {

	public final static String SEP = "_";

	private static String getFullName(String table, String col) {
		return table.equals("") ? col : table + SEP + col;
	}

	private Hypergraph h;
	private int nextVar;
	private HashMap<String, String> colToVar;
	private UnionFind<String> vars;

	public HypergraphBuilder() {
		h = new Hypergraph();
		nextVar = 0;
		colToVar = new HashMap<>();
		vars = new UnionFind<String>(new HashSet<String>());
	}

	public void buildEdge(Predicate table) {
		buildEdge(table, "");
	}

	private void buildEdge(Predicate table, String prefix) {
		if (table instanceof BasePredicate) {
			String tableFullName = getFullName(prefix, table.getAlias());
			Edge e = new Edge(tableFullName);
			for (String attr : table) {
				String var = "X" + nextVar++;
				String col = getFullName(tableFullName, attr);
				colToVar.put(col, var);
				e.addVertex(var);
				vars.addElement(var);
			}
			h.addEdge(e);
		} else {
			ViewPredicate vPred = (ViewPredicate) table;
			String newPrefix = getFullName(prefix, vPred.getAlias());
			for (Predicate pred : vPred.getDefiningPredicates()) {
				buildEdge(pred, newPrefix);
			}
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
		// System.out.println(vars);
		for (Edge e : h.getEdges()) {
			for (String v : e.getVertices()) {
				String newName = vars.find(v);
				if (!e.renameVertex(v, newName)) {
					throw new RuntimeException("Vertex " + newName + " already exists.");
				}
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
