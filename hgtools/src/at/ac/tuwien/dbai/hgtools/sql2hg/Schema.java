package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;
import java.util.Iterator;

public class Schema implements Iterable<PredicateDefinition> {

	private HashMap<String, PredicateDefinition> definitions;

	public Schema() {
		definitions = new HashMap<>();
	}

	public void addPredicateDefinition(PredicateDefinition predDef) {
		if (predDef == null) {
			throw new NullPointerException();
		}
		definitions.put(predDef.getName(), predDef);
	}
	
	public PredicateDefinition getPredicateDefinition(String predDef) {
		// TODO should I make sure I don't return null if pred doesn't exist?
		return definitions.get(predDef);
	}
	
	public boolean existsPredicateDefinition(String predDef) {
		return definitions.containsKey(predDef);
	}
	
	public boolean existsAttributeInPredicateDefinition(String attr, String predDef) {
		PredicateDefinition p = definitions.get(predDef);
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
