package at.ac.tuwien.dbai.hgtools.sql2hg.sqlshare;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.sql2hg.Predicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.sql2hg.ViewPredicate;

public class SchemaWithDuplicates extends Schema {

	private HashMap<String, LinkedList<PredicateDefinition>> definitions;

	public SchemaWithDuplicates() {
		super();
		definitions = new HashMap<>();
	}

	@Override
	public void addPredicateDefinition(PredicateDefinition predDef) {
		if (predDef == null) {
			throw new NullPointerException();
		}
		if (definitions.containsKey(predDef.getName())) {
			boolean dup = false;
			LinkedList<PredicateDefinition> preds = definitions.get(predDef.getName());
			for (PredicateDefinition p : preds) {
				if (p.equals(predDef)) {
					dup = true;
					break;
				}
			}
			if (!dup) {
				System.out.println(predDef.getName() + " is already in this schema!");
			} else {
				return;
			}
		} else {
			definitions.put(predDef.getName(), new LinkedList<>());
		}
		definitions.get(predDef.getName()).add(predDef);
	}

	@Override
	public void addPredicateDefinition(PredicateDefinition predDef, ViewPredicate view) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Predicate newPredicate(String predName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PredicateDefinition getPredicateDefinition(String predDef) {
		throw new UnsupportedOperationException();
	}

	public List<PredicateDefinition> getPredicateDefinitions(String predDef) {
		return definitions.get(predDef.toLowerCase());
	}

	@Override
	public boolean existsPredicateDefinition(String predDef) {
		return definitions.containsKey(predDef.toLowerCase());
	}

	@Override
	public boolean existsAttributeInPredicateDefinition(String attr, String predDef) {
		throw new UnsupportedOperationException();
	}

	public List<PredicateDefinition> existsAttributeInPredicateDefinitions(String attr, String predDef) {
		LinkedList<PredicateDefinition> res = new LinkedList<>();
		LinkedList<PredicateDefinition> defs = definitions.get(predDef.toLowerCase());
		for (PredicateDefinition pDef : defs) {
			if (pDef.existsAttribute(attr)) {
				res.add(pDef);
			}
		}
		return res;
	}

	@Override
	public boolean isViewPredicate(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<PredicateDefinition> iterator() {
		throw new UnsupportedOperationException();
	}

}
