package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SimplePredicate implements Predicate {

	public static final String SEP = "_";

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
	public int arity() {
		return definition.arity();
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
		Attribute als = attrToAlias.get(new Attribute(attr));
		if (als != null) {
			return als.getName();
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
		for (Map.Entry<Attribute, Attribute> entry : attrToAlias.entrySet()) {
			Attribute attrK = entry.getKey();
			Attribute attrV = entry.getValue();
			result = prime * result + attrK.hashCode();
			result = prime * result + attrV.hashCode();
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
		for (Map.Entry<Attribute, Attribute> entry : attrToAlias.entrySet()) {
			Attribute attrK = entry.getKey();
			Attribute attrV = entry.getValue();
			Attribute otherAttrAlias = other.attrToAlias.get(attrK);
			if (otherAttrAlias == null) {
				return false;
			} else {
				Attribute attrAlias = attrV;
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
		final String leafName1 = "leaf1";
		final String leafName2 = "leaf2";
		final String leafName3 = "leaf3";
		PredicateDefinition leaf1Def = new PredicateDefinition(leafName1, new String[] { "attr1", "attr2", "attr3" });
		PredicateDefinition leaf2Def = new PredicateDefinition(leafName2, new String[] { "col1", "col2" });
		PredicateDefinition leaf3Def = new PredicateDefinition(leafName3, new String[] { "id", "name", "surname" });

		BasePredicate leaf1 = new BasePredicate(leaf1Def);
		System.out.println(leaf1);
		BasePredicate leaf2 = new BasePredicate(leaf2Def);
		System.out.println(leaf2);
		BasePredicate leaf3 = new BasePredicate(leaf3Def);
		System.out.println(leaf3);

		final String viewName1 = "view1";
		final String aliasName1 = "alias1";
		final String aliasName2 = "alias2";
		final String aliasName3 = "alias3";
		PredicateDefinition view1Def = new PredicateDefinition(viewName1,
				new String[] { aliasName1, aliasName2, aliasName3 });
		ViewPredicate view1 = new ViewPredicate(view1Def);
		view1.addDefiningPredicates(leaf2, leaf3);
		view1.defineAttribute(aliasName1, leafName2, "col2");
		view1.defineAttribute(aliasName2, leafName3, "name");
		view1.defineAttribute(aliasName3, leafName2, "col1");
		System.out.println(view1);

		final String vAttrName1 = "vAttr1";
		final String vAttrName2 = "vAttr2";
		final String vAttrName3 = "vAttr3";
		PredicateDefinition view2Def = new PredicateDefinition("view2",
				new String[] { vAttrName1, vAttrName2, vAttrName3 });
		ViewPredicate view2 = new ViewPredicate(view2Def);
		view2.addDefiningPredicates(leaf1, view1);
		view2.defineAttribute(vAttrName1, leafName1, "attr2");
		view2.defineAttribute(vAttrName2, viewName1, aliasName2);
		view2.defineAttribute(vAttrName3, viewName1, aliasName1);
		System.out.println(view2);

		System.out.println();
		System.out.println("Def of leaf3.surname: " + leaf3.getDefiningAttribute("surname"));
		System.out.println("Def of view1.alias3: " + view1.getDefiningAttribute(aliasName3));
		System.out.println("Def of view2.vAttr1: " + view2.getDefiningAttribute(vAttrName1));
		System.out.println("Def of view2.vAttr2: " + view2.getDefiningAttribute(vAttrName2));
		System.out.println("Def of view2.vAttr3: " + view2.getDefiningAttribute(vAttrName3));

		PredicateDefinition p1Def = new PredicateDefinition("p1", new String[] { "a1", "a2", "a3" });
		// PredicateDefinition p2Def = new PredicateDefinition("pred2", new String[] {
		// "a1", "a2", "a3" });
		// PredicateDefinition p3Def = new PredicateDefinition("pp3", new String[] {
		// "c2", "a1" });
		// PredicateDefinition p1CopyDef = new PredicateDefinition("p1", new String[] {
		// "a1", "a2", "a3" });

		System.out.println();
		final String mainAttrName = "mainAttr";
		SimplePredicate p1 = new SimplePredicate(p1Def);
		p1.setAlias("mainPred");
		p1.setAttributeAlias("a1", mainAttrName);
		System.out.println(p1.existsAttribute(mainAttrName));
		p1.getAttributeAlias("a1");
		p1.getOriginalAttribute(mainAttrName);

		// System.out.println(p1);
	}

}
