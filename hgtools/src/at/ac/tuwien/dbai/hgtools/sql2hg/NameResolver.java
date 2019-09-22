package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import net.sf.jsqlparser.schema.Column;

public class NameResolver {

	private Deque<HashMap<String, Predicate>> scopes;

	public NameResolver() {
		scopes = new LinkedList<>();
	}

	public void enterNewScope() {
		scopes.addFirst(new HashMap<>());
	}

	public void exitCurrentScope() {
		scopes.removeFirst();
	}

	public void addTableToCurrentScope(Predicate table) {
		scopes.getFirst().put(table.getAlias(), table);
	}
	
	public Collection<Predicate> getPredicatesInCurrentScope() {
		return scopes.getFirst().values();
	}
	
	public Predicate resolveColumn(Column column) {
		Predicate res = null;
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

	public Predicate resolveColumn(String column) {
		Predicate res = findTableOf(column);
		if (res == null) {
			throw new RuntimeException("The table of " + column + " cannot be found.");
		}
		return res;
	}

	public Predicate resolveTableName(String table) {
		Iterator<HashMap<String, Predicate>> scopesIt = scopes.descendingIterator();
		while (scopesIt.hasNext()) {
			HashMap<String, Predicate> sc = scopesIt.next();
			Predicate res = sc.get(table);
			if (res != null) {
				return res;
			}
		}
		return null;
	}

	private Predicate findTableOf(String column) {
		Iterator<HashMap<String, Predicate>> scopesIt = scopes.descendingIterator();
		while (scopesIt.hasNext()) {
			HashMap<String, Predicate> sc = scopesIt.next();
			for (Predicate pred : sc.values()) {
				if (pred.existsAttribute(column)) {
					return pred;
				}
			}
		}
		return null;
	}

}
