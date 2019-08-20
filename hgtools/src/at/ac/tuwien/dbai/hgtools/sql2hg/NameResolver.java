package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import net.sf.jsqlparser.schema.Column;

public class NameResolver {

	private Deque<HashMap<String, PredicateInQuery>> scopes;

	public NameResolver() {
		scopes = new LinkedList<>();
	}

	public void enterNewScope() {
		scopes.addFirst(new HashMap<>());
	}

	public void exitCurrentScope() {
		scopes.removeFirst();
	}

	public void addTableToCurrentScope(PredicateInQuery table, String alias) {
		scopes.getFirst().put(alias, table);
	}

	public PredicateInQuery resolveColumn(Column column) {
		PredicateInQuery res = null;
		if (column.getTable() != null) {
			String tableName = column.getTable().getName();
			res = resolveTableName(tableName);
			if (res == null) {
				throw new RuntimeException("The table " + tableName + " does not exist.");
			}
		} else {
			res = resolveColumn(column.getColumnName());
		}
		return res;
	}

	public PredicateInQuery resolveColumn(String column) {
		PredicateInQuery res = findTableOf(column);
		if (res == null) {
			throw new RuntimeException("The table of " + column + " cannot be found.");
		}
		return res;
	}

	private PredicateInQuery resolveTableName(String table) {
		Iterator<HashMap<String, PredicateInQuery>> scopesIt = scopes.descendingIterator();
		while (scopesIt.hasNext()) {
			HashMap<String, PredicateInQuery> sc = scopesIt.next();
			PredicateInQuery res = sc.get(table);
			if (res != null) {
				return res;
			}
		}
		return null;
	}

	private PredicateInQuery findTableOf(String column) {
		Iterator<HashMap<String, PredicateInQuery>> scopesIt = scopes.descendingIterator();
		while (scopesIt.hasNext()) {
			HashMap<String, PredicateInQuery> sc = scopesIt.next();
			for (PredicateInQuery pred : sc.values()) {
				if (pred.existsAttribute(column)) {
					return pred;
				}
			}
		}
		return null;
	}

}
