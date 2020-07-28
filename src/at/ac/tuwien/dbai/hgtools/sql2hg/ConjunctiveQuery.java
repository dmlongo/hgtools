package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ConjunctiveQuery {

	private LinkedList<String> columns;
	private LinkedList<Predicate> tables;
	private LinkedList<Equality> joins;

	public ConjunctiveQuery() {
		columns = new LinkedList<>();
		tables = new LinkedList<>();
		joins = new LinkedList<>();
	}

	public void addColumn(String col) {
		if (col == null) {
			throw new NullPointerException();
		}
		columns.add(col);
	}

	public void addTable(Predicate table) {
		if (table == null) {
			throw new NullPointerException();
		}
		tables.add(table);
	}

	public void addJoin(Equality join) {
		if (join == null) {
			throw new NullPointerException();
		}
		joins.add(join);
	}

	public List<String> getColumns() {
		// TODO make unmodifiable
		return columns;
	}
	
	public List<Predicate> getTables() {
		// TODO make unmodifiable
		return tables;
	}
	
	public List<Equality> getJoins() {
		// TODO make unmodifiable
		return joins;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(500);

		sb.append("SELECT ");
		Iterator<String> colIt = columns.iterator();
		while (colIt.hasNext()) {
			String col = colIt.next();
			sb.append(col);
			if (colIt.hasNext()) {
				sb.append(',');
				sb.append(' ');
			}
		}
		sb.append('\n');

		sb.append("FROM ");
		Iterator<Predicate> tabIt = tables.iterator();
		while (tabIt.hasNext()) {
			String tab = toString(tabIt.next());
			sb.append(tab);
			if (tabIt.hasNext()) {
				sb.append(',');
				sb.append(' ');
			}
		}
		sb.append('\n');

		sb.append("WHERE ");
		Iterator<Equality> joinIt = joins.iterator();
		while (joinIt.hasNext()) {
			String join = joinIt.next().toString();
			sb.append(join);
			if (tabIt.hasNext()) {
				sb.append(" AND ");
			}
		}
		sb.append(';');
		
		return sb.toString();
	}

	private String toString(Predicate pred) {
		StringBuilder sb = new StringBuilder(50);
		String predName = pred.getPredicateName();
		String alias = pred.getAlias();
		sb.append(predName);
		if (!predName.equals(alias)) {
			sb.append(' ');
			sb.append(alias);
		}
		return sb.toString();
	}

}
