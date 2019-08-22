package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;

public class ViewPredicate extends SimplePredicate implements Predicate {

	private HashMap<String, Predicate> definingPredicates;
	private HashMap<String, String> definingAttributes;
	
	public ViewPredicate(PredicateDefinition def, String alias) {
		super(def, alias);
		definingPredicates = new HashMap<>();
		definingAttributes = new HashMap<>();
	}

	public ViewPredicate(PredicateDefinition def) {
		super(def);
		definingPredicates = new HashMap<>();
		definingAttributes = new HashMap<>();
	}

	@Override
	public void addDefiningPredicate(Predicate pred) {
		if (pred == null) {
			throw new NullPointerException();
		}
		definingPredicates.put(pred.getAlias(), pred);
	}

	@Override
	public void defineAttribute(String viewAttr, String defPred, String defAttr) {
		if (!existsAttribute(viewAttr)) {
			throw new IllegalArgumentException(alias + "." + viewAttr + " does not exists.");
		}
		if (!definingPredicates.containsKey(defPred)) {
			throw new IllegalArgumentException(defPred + " is not a defining predicate.");
		}
		if (!definingPredicates.get(defPred).existsAttribute(defAttr)) {
			throw new IllegalArgumentException(defPred + "." + defAttr + " does not exists.");
		}
		String attrMap = defPred + "." + defAttr;
		definingAttributes.put(viewAttr, attrMap);
	}

	@Override
	public String getDefiningAttribute(String viewAttr) {
		// TODO Auto-generated method stub
		if (definingAttributes.get(viewAttr) == null) {
			throw new IllegalArgumentException(alias + "." + viewAttr + " does not exists.");
		}
		String result = definingAttributes.get(viewAttr);
		int dot = result.indexOf('.');
		String defPredName = result.substring(0, dot);
		String defAttrName = result.substring(dot + 1);
		Predicate defPredicate = definingPredicates.get(defPredName);
		// TODO maybe use a StringBuilder to avoid wasting memory
		return alias + "." + defPredicate.getDefiningAttribute(defAttrName);
	}

}
