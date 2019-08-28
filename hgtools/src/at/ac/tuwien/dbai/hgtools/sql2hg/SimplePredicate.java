package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class SimplePredicate implements Predicate {

	protected PredicateDefinition definition;
	protected String alias;
	protected HashSet<String> attrNames;
	protected HashMap<Attribute, Attribute> attrToAlias;
	protected HashMap<Attribute, Attribute> aliasToAttr;

	public SimplePredicate(PredicateDefinition def, String alias) {
		if (def == null || alias == null) {
			throw new NullPointerException();
		}
		this.definition = def;
		this.alias = alias.equals("") ? definition.getName() : alias;
		attrNames = new HashSet<>();
		for (String attr : definition) {
			attrNames.add(attr.toLowerCase());
		}
		attrToAlias = new HashMap<>();
		aliasToAttr = new HashMap<>();
	}

	public SimplePredicate(PredicateDefinition def) {
		this(def, "");
	}

	public SimplePredicate(SimplePredicate pred) {
		this(pred.definition, pred.alias);
		for (Attribute attr : pred.attrToAlias.keySet()) {
			Attribute attrAlias = pred.attrToAlias.get(attr);
			attrNames.add(attrAlias.getName().toLowerCase());
			attrToAlias.put(attr, attrAlias);
			aliasToAttr.put(attrAlias, attr);
		}
	}

	@Override
	public PredicateDefinition getPredicateDefinition() {
		return definition;
	}

	@Override
	public String getPredicateName() {
		return definition.getName();
	}

	@Override
	public void setAlias(String alias) {
		if (alias == null) {
			throw new NullPointerException();
		}
		this.alias = alias.equals("") ? definition.getName() : alias;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public void setAttributeAlias(String attr, String alias) {
		if (attr == null || alias == null) {
			throw new NullPointerException();
		}
		if (!definition.existsAttribute(attr)) {
			throw new IllegalArgumentException(definition.getName() + "." + attr + " does not exist");
		}
		// TODO there could be a situation in which the alias is equal to the name of a
		// different attribute. Is it even allowed? I think I should disallow it.
		attrNames.add(alias.toLowerCase());

		Attribute thisAttr = definition.getAttribute(attr);
		int pos = thisAttr.getPosition();
		Attribute aliasAttr = new Attribute(alias, pos);
		attrToAlias.put(thisAttr, aliasAttr);
		aliasToAttr.put(aliasAttr, thisAttr);
	}

	@Override
	public String getAttributeAlias(String attr) {
		if (!attrNames.contains(attr.toLowerCase())) {
			return null;
		}
		Attribute alias = attrToAlias.get(new Attribute(attr));
		if (alias != null) {
			return alias.getName();
		} else if (definition.existsAttribute(attr)) {
			return attr;
		}
		return null;
	}

	@Override
	public String getOriginalAttribute(String alias) {
		// TODO check again
		if (!attrNames.contains(alias.toLowerCase())) {
			return null;
		}

		Attribute result = aliasToAttr.get(new Attribute(alias));
		if (result != null) {
			return result.getName();
		} else if (definition.existsAttribute(alias)) {
			return alias;
		}
		return null;
	}

	@Override
	public boolean existsAttribute(String attr) {
		return attrNames.contains(attr.toLowerCase());
		// return definition.existsAttribute(attr) || aliasToAttr.containsKey(new
		// Attribute(attr));
	}

	@Override
	public void addDefiningPredicate(Predicate pred) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Predicate> getDefiningPredicates() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void defineAttribute(String viewAttr, String defPred, String defAttr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDefiningAttribute(String viewAttr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addJoin(String pred1, String attr1, String pred2, String attr2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Equality> getJoins() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<String> iterator() {
		return new AliasedAttributesIterator();
	}

	/**
	 * 
	 * Iterates over the attributes of the predicate definition, but uses attribute
	 * aliases, if defined.
	 * 
	 * @author david
	 *
	 */
	private class AliasedAttributesIterator implements Iterator<String> {

		private Iterator<String> predIt = definition.iterator();

		@Override
		public boolean hasNext() {
			return predIt.hasNext();
		}

		@Override
		public String next() {
			return getAttributeAlias(predIt.next());
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + alias.toLowerCase().hashCode();
		result = prime * result + definition.hashCode();
		for (Attribute attr : attrToAlias.keySet()) {
			result = prime * result + attr.hashCode();
			result = prime * result + attrToAlias.get(attr).hashCode();
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SimplePredicate)) {
			return false;
		}
		SimplePredicate other = (SimplePredicate) obj;
		if (!alias.equalsIgnoreCase(other.alias)) {
			return false;
		}
		if (attrToAlias.size() != other.attrToAlias.size()) {
			return false;
		}
		if (!definition.equals(other.definition)) {
			return false;
		}
		for (Attribute attr : attrToAlias.keySet()) {
			Attribute otherAttrAlias = other.attrToAlias.get(attr);
			if (otherAttrAlias == null) {
				return false;
			} else {
				Attribute attrAlias = attrToAlias.get(attr);
				if (!attrAlias.equals(otherAttrAlias)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(200);
		sb.append(alias);
		sb.append('(');
		Iterator<String> it = iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			if (it.hasNext()) {
				sb.append(',');
			}
		}
		sb.append(')');
		return sb.toString();
	}

	public static void main(String[] args) {
		PredicateDefinition leaf1Def = new PredicateDefinition("leaf1", new String[] { "attr1", "attr2", "attr3" });
		PredicateDefinition leaf2Def = new PredicateDefinition("leaf2", new String[] { "col1", "col2" });
		PredicateDefinition leaf3Def = new PredicateDefinition("leaf3", new String[] { "id", "name", "surname" });

		BasePredicate leaf1 = new BasePredicate(leaf1Def);
		System.out.println(leaf1);
		BasePredicate leaf2 = new BasePredicate(leaf2Def);
		System.out.println(leaf2);
		BasePredicate leaf3 = new BasePredicate(leaf3Def);
		System.out.println(leaf3);

		PredicateDefinition view1Def = new PredicateDefinition("view1", new String[] { "alias1", "alias2", "alias3" });
		ViewPredicate view1 = new ViewPredicate(view1Def);
		view1.addDefiningPredicates(leaf2, leaf3);
		view1.defineAttribute("alias1", "leaf2", "col2");
		view1.defineAttribute("alias2", "leaf3", "name");
		view1.defineAttribute("alias3", "leaf2", "col1");
		System.out.println(view1);

		PredicateDefinition view2Def = new PredicateDefinition("view2", new String[] { "vAttr1", "vAttr2", "vAttr3" });
		ViewPredicate view2 = new ViewPredicate(view2Def);
		view2.addDefiningPredicates(leaf1, view1);
		view2.defineAttribute("vAttr1", "leaf1", "attr2");
		view2.defineAttribute("vAttr2", "view1", "alias2");
		view2.defineAttribute("vAttr3", "view1", "alias1");
		System.out.println(view2);

		System.out.println();
		System.out.println("Def of leaf3.surname: " + leaf3.getDefiningAttribute("surname"));
		System.out.println("Def of view1.alias3: " + view1.getDefiningAttribute("alias3"));
		System.out.println("Def of view2.vAttr1: " + view2.getDefiningAttribute("vAttr1"));
		System.out.println("Def of view2.vAttr2: " + view2.getDefiningAttribute("vAttr2"));
		System.out.println("Def of view2.vAttr3: " + view2.getDefiningAttribute("vAttr3"));

		PredicateDefinition p1Def = new PredicateDefinition("p1", new String[] { "a1", "a2", "a3" });
		// PredicateDefinition p2Def = new PredicateDefinition("pred2", new String[] {
		// "a1", "a2", "a3" });
		// PredicateDefinition p3Def = new PredicateDefinition("pp3", new String[] {
		// "c2", "a1" });
		// PredicateDefinition p1CopyDef = new PredicateDefinition("p1", new String[] {
		// "a1", "a2", "a3" });

		System.out.println();
		SimplePredicate p1 = new SimplePredicate(p1Def);
		p1.setAlias("mainPred");
		p1.setAttributeAlias("a1", "mainAttr");
		System.out.println(p1.existsAttribute("mainAttr"));
		p1.getAttributeAlias("a1");
		p1.getOriginalAttribute("mainAttr");

		// System.out.println(p1);
	}

}
