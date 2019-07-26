package at.ac.tuwien.dbai.hgtools.HypergraphFromSQL;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import at.ac.tuwien.dbai.hgtools.hypergraph.Edge;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import lombok.Data;

@Data
public class HypergraphFromSQLHelper {
	private Map<String, BasePredicate> atoms;
	private Map<String, String> varAlias;
	private int iVar = 1;

	public HypergraphFromSQLHelper() {
		atoms = new HashMap<String, BasePredicate>();
		varAlias = new HashMap<String, String>();
	}

	public void addAtom(BasePredicate b, String alias) {
		atoms.put(alias, b);
		for (String lit : b.getLiterals()) {
			varAlias.put(alias + "." + lit, "X" + iVar);
			iVar++;
		}
	}

	public void addJoin(SameColumn sc) {
		Map<String, String> tmpSet = new HashMap<String, String>();
		String var = "J" + iVar;
		iVar++;
		String varA = varAlias.get(sc.getA().getTable() + "." + sc.getA().getColumnName());
		String varB = varAlias.get(sc.getB().getTable() + "." + sc.getB().getColumnName());
		for (String key : varAlias.keySet()) {
			if (varAlias.get(key).equals(varA))
				tmpSet.put(key, var);
			if (varAlias.get(key).equals(varB))
				tmpSet.put(key, var);
		}
		varAlias.putAll(tmpSet);
	}

	public Hypergraph getHypergraph() {
		Hypergraph H = new Hypergraph();

		for (String atom : atoms.keySet()) {
			Edge e = new Edge();
			e.setName(atom);
			List<Entry<String, String>> orderedVarAlias = new LinkedList<>(varAlias.entrySet());
			Collections.sort(orderedVarAlias, new Comparator<Entry<String, String>>() {
				@Override
				public int compare(Entry<String, String> e1, Entry<String, String> e2) {
					int v1 = Integer.parseInt(e1.getValue().substring(1));
					int v2 = Integer.parseInt(e2.getValue().substring(1));
					return v1 - v2;
				}
			});

			for (Entry<String, String> entry : orderedVarAlias) {
				String var = entry.getKey();
				if (var.substring(0, var.indexOf('.')).equalsIgnoreCase(atom)) {
					e.addVertex(varAlias.get(var));
					// e.addVertex(entry.getValue());
				}
			}
			H.addEdge(e);
		}

		return H;
	}
}
