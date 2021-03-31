package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ViewPredicate extends SimplePredicate {

	private HashMap<String, Predicate> definingPredicates;
	private HashMap<String, String> definingAttributes;
	private LinkedList<Equality> joins;

	public ViewPredicate(PredicateDefinition def, String alias) {
		super(def, alias);
		definingPredicates = new HashMap<>();
		definingAttributes = new HashMap<>();
		joins = new LinkedList<>();
	}

	public ViewPredicate(PredicateDefinition def) {
		super(def);
		definingPredicates = new HashMap<>();
		definingAttributes = new HashMap<>();
		joins = new LinkedList<>();
	}

	public ViewPredicate(ViewPredicate view) {
		super(view);
		definingPredicates = new HashMap<>();
		definingAttributes = new HashMap<>();
		joins = new LinkedList<>();

		for (String defPred : view.definingPredicates.keySet()) {
			// TODO aliasing problems
			definingPredicates.put(defPred, view.definingPredicates.get(defPred));
		}
		for (String defAttr : view.definingAttributes.keySet()) {
			definingAttributes.put(defAttr, view.definingAttributes.get(defAttr));
		}
		for (Equality join : view.joins) {
			// TODO aliasing problems
			joins.add(join);
		}
	}

	@Override
	public void addDefiningPredicate(Predicate pred) {
		if (pred == null) {
			throw new NullPointerException();
		}
		definingPredicates.put(pred.getAlias(), pred);
	}

	@Override
	public Collection<Predicate> getDefiningPredicates() {
		// TODO aliasing
		return definingPredicates.values();
	}

	@Override
	public void defineAttribute(String viewAttr, String defPred, String defAttr) {
		if (!existsAttribute(viewAttr)) {
			throw new IllegalArgumentException(alias + SEP + viewAttr + " does not exists.");
		}
		if (!definingPredicates.containsKey(defPred)) {
			throw new IllegalArgumentException(defPred + " is not a defining predicate.");
		}
		if (!definingPredicates.get(defPred).existsAttribute(defAttr)) {
			throw new IllegalArgumentException(defPred + SEP + defAttr + " does not exists.");
		}
		String attrMap = defPred + SEP + defAttr;
		definingAttributes.put(viewAttr, attrMap);
	}

	@Override
	public String getDefiningAttribute(String viewAttr) {
		if (definingAttributes.get(viewAttr) == null) {
			throw new IllegalArgumentException(alias + SEP + viewAttr + " does not exist.");
		}
		String result = definingAttributes.get(viewAttr);
		int sep = result.indexOf(SEP);
		String defPredName = result.substring(0, sep);
		String defAttrName = result.substring(sep + 1);
		Predicate defPredicate = definingPredicates.get(defPredName);
		return alias + SEP + defPredicate.getDefiningAttribute(defAttrName);
	}

	@Override
	public void addJoin(String pred1, String attr1, String pred2, String attr2) {
		if (!definingPredicates.containsKey(pred1) || !definingPredicates.containsKey(pred2)) {
			throw new IllegalArgumentException("The predicates do not exist.");
		}
		if (!definingPredicates.get(pred1).existsAttribute(attr1)) {
			throw new IllegalArgumentException(pred1 + SEP + attr1 + " do not exist.");
		}
		if (!definingPredicates.get(pred2).existsAttribute(attr2)) {
			throw new IllegalArgumentException(pred2 + SEP + attr2 + " do not exist.");
		}
		Predicate p1 = definingPredicates.get(pred1);
		Predicate p2 = definingPredicates.get(pred2);
		joins.add(new Equality(p1, attr1, p2, attr2));
	}

	@Override
	public List<Equality> getJoins() {
		// TODO aliasing
		return joins;
	}

}
