package at.ac.tuwien.dbai.hgtools.HypergraphFromSQL;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

public class TableFinder {
	private Schema schema;
	private Deque<HashMap<String, String>> scopes;

	public TableFinder(Schema schema) {
		this.schema = schema;
		scopes = new LinkedList<>();
	}

	public void enterNewScope() {
		scopes.addFirst(new HashMap<>());
	}

	public void exitCurrentScope() {
		scopes.removeFirst();
	}

	public void addTableToCurrentScope(String table, String alias) {
		scopes.getFirst().put(table, alias);
	}

	public boolean findTableInCurrentScope(Column column) {
		if (column.getTable() != null) {
			return false;
		}
		Iterator<HashMap<String, String>> scopesIt = scopes.descendingIterator();
		while (scopesIt.hasNext()) {
			HashMap<String, String> sc = scopesIt.next();
			for (String tbl : sc.keySet()) {
				// TODO this distinction between normal predicates and views is not optimal
				BasePredicate p = schema.getPredicate(tbl);
				if (p == null) {
					p = schema.getPredicateFromView(tbl, column.getColumnName());
				}
				if (p != null) {
					for (String lit : p.getLiterals()) {
						if (column.getColumnName().equalsIgnoreCase(lit)) {
							column.setTable(new Table(sc.get(tbl)));
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}
