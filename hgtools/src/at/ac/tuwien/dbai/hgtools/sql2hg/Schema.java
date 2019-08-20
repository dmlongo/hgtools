package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;
import java.util.Iterator;

public class Schema implements Iterable<Predicate> {

	private HashMap<String, Predicate> predicates;

	public Schema() {
		predicates = new HashMap<>();
	}

	public void addPredicate(Predicate pred) {
		if (pred == null) {
			throw new NullPointerException();
		}
		predicates.put(pred.getName(), pred);
	}
	
	public Predicate getPredicate(String pred) {
		// TODO should I make sure I don't return null if pred doesn't exist?
		return predicates.get(pred);
	}
	
	public boolean existsPredicate(String pred) {
		return predicates.containsKey(pred);
	}
	
	public boolean existsAttributeInPredicate(String attr, String pred) {
		Predicate p = predicates.get(pred);
		if (p != null) {
			return p.existsAttribute(attr);
		} else {
			return false;
		}
	}
	
	@Override
	public Iterator<Predicate> iterator() {
		return predicates.values().iterator();
	}

}
