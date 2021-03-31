package at.ac.tuwien.dbai.hgtools.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.SelectBody;

public class NameStack {
	private LinkedList<HashSet<String>> scopes;
	private LinkedList<Boolean> setOpLists;
	private LinkedList<SelectBody> selects;
	private LinkedList<LinkedList<Table>> tables;
	private LinkedList<String> globalNames;

	public NameStack() {
		scopes = new LinkedList<>();
		setOpLists = new LinkedList<>();
		selects = new LinkedList<>();
		tables = new LinkedList<>();
		// global scope - no select
		globalNames = new LinkedList<>();
		scopes.addFirst(new HashSet<>());
		setOpLists.addFirst(false);
		selects.addFirst(null);
		tables.addFirst(null);
	}

	public void enterNewScope(SelectBody select, boolean inSetOpList) {
		scopes.addFirst(new HashSet<>());
		setOpLists.addFirst(inSetOpList);
		selects.addFirst(select);
		tables.addFirst(new LinkedList<>());
	}

	public void enterNewScope(SelectBody select) {
		enterNewScope(select, false);
	}

	public void exitCurrentScope() {
		scopes.removeFirst();
		setOpLists.removeFirst();
		selects.removeFirst();
		tables.removeFirst();
	}

	public void addTableToCurrentScope(Table table) {
		tables.getFirst().add(table);
	}

	public List<Table> getCurrentTables() {
		return tables.getFirst();
	}

	public void addNameToCurrentScope(String name) {
		scopes.getFirst().add(name);
	}

	public void addNamesToCurrentScope(Collection<String> names) {
		for (String name : names) {
			addNameToCurrentScope(name);
		}
	}

	public void addNameToParentScope(String name) {
		scopes.get(1).add(name);
	}

	public void addNamesToParentScope(Collection<String> names) {
		for (String name : names) {
			addNameToParentScope(name);
		}
	}

	public boolean isTopLevel() {
		// TODO non lo so, check, lascia stare
		if (scopes.size() == 2) {
			return true;
		}
		for (int i = 0; i < setOpLists.size() - 1; i++) {
			if (!Boolean.TRUE.equals(setOpLists.get(i))) {
				return false;
			}
		}
		return true;
	}

	public void addGlobalName(String name) {
		globalNames.add(name);
	}

	public void addNamesToGlobalScope(Collection<String> names) {
		for (String name : names) {
			addGlobalName(name);
		}
	}

	public List<String> getGlobalNames() {
		return globalNames;
	}

	public SelectBody getCurrentSelect() {
		return selects.getFirst();
	}

	public SelectBody resolve(String name) {
		Iterator<SelectBody> selectIt = selects.iterator();
		for (HashSet<String> names : scopes) {
			SelectBody body = selectIt.next();
			if (names.contains(name)) {
				return body;
			}
		}
		return null;
	}
}