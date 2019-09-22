package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This class represents a database schema. The name of tables and attributes
 * are treated in a case-insensitive way.
 * 
 * @author david
 *
 */
public class Schema implements Iterable<PredicateDefinition> {

	private HashMap<String, PredicateDefinition> definitions;
	private HashMap<PredicateDefinition, Predicate> stubs;

	public Schema() {
		definitions = new HashMap<>();
		stubs = new HashMap<>();
	}

	public void addPredicateDefinition(PredicateDefinition predDef) {
		if (predDef == null) {
			throw new NullPointerException();
		}
		definitions.put(predDef.getName(), predDef);
		stubs.put(predDef, new BasePredicate(predDef));
	}

	public void addPredicateDefinition(PredicateDefinition predDef, ViewPredicate view) {
		if (predDef == null) {
			throw new NullPointerException();
		}
		definitions.put(predDef.getName(), predDef);
		stubs.put(predDef, new ViewPredicate(view));
	}

	// TODO this method (actually the whole class) has a static behavior
	public Predicate newPredicate(String predName) {
		// TODO exceptions could be raised
		Predicate pred = stubs.get(definitions.get(predName.toLowerCase()));
		if (pred instanceof BasePredicate) {
			return new BasePredicate((BasePredicate) pred);
		} else if (pred instanceof ViewPredicate) {
			return new ViewPredicate((ViewPredicate) pred);
		} else {
			throw new RuntimeException("pred= " + pred);
		}
	}

	public PredicateDefinition getPredicateDefinition(String predDef) {
		// TODO should I make sure I don't return null if pred doesn't exist?
		return definitions.get(predDef.toLowerCase());
	}

	public boolean existsPredicateDefinition(String predDef) {
		return definitions.containsKey(predDef.toLowerCase());
	}

	public boolean existsAttributeInPredicateDefinition(String attr, String predDef) {
		PredicateDefinition p = definitions.get(predDef.toLowerCase());
		if (p != null) {
			return p.existsAttribute(attr);
		} else {
			return false;
		}
	}

	@Override
	public Iterator<PredicateDefinition> iterator() {
		return definitions.values().iterator();
	}

}
