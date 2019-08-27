package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractPredicate implements Predicate {

	protected PredicateDefinition definition;
	protected String alias;

	public AbstractPredicate(PredicateDefinition def, String alias) {
		if (def == null || alias == null) {
			throw new NullPointerException();
		}
		this.definition = def;
		this.alias = alias.equals("") ? definition.getName() : alias;
	}

	public AbstractPredicate(PredicateDefinition def) {
		this(def, "");
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
	public String getAttributeAlias(String attr) {
		// TODO faulty implementation
		Iterator<String> originalIt = definition.iterator();
		Iterator<String> aliasIt = iterator();
		while (originalIt.hasNext()) {
			String originalAttr = originalIt.next();
			String aliasAttr = aliasIt.next(); // TODO infinite recursion
			if (attr.equals(originalAttr)) {
				return aliasAttr;
			}
		}
		if (definition.existsAttribute(attr)) {
			return attr;
		}
		// TODO a null could be propagated if attr doesn't exist
		return null;
	}

	@Override
	public String getOriginalAttribute(String alias) {
		// TODO faulty implementation
		Iterator<String> originalIt = definition.iterator();
		Iterator<String> aliasIt = iterator();
		while (aliasIt.hasNext()) {
			String originalAttr = originalIt.next();
			String aliasAttr = aliasIt.next(); // TODO infinite recursion
			if (alias.equals(aliasAttr)) {
				return originalAttr;
			}
		}
		// TODO a null could be propagated if alias doesn't exist
		return null;
	}

	@Override
	public boolean existsAttribute(String attr) {
		for (String thisAttr : this) {
			if (attr.equals(thisAttr)) {
				return true;
			}
		}
		return false;
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
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
		for (String attr : this) {
			result = prime * result + attr.hashCode();
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AbstractPredicate)) {
			return false;
		}
		AbstractPredicate other = (AbstractPredicate) obj;
		if (alias == null) {
			if (other.alias != null) {
				return false;
			}
		} else if (!alias.equals(other.alias)) {
			return false;
		}
		if (definition == null) {
			if (other.definition != null) {
				return false;
			}
		} else if (!definition.equals(other.definition)) {
			return false;
		}

		Iterator<String> thisIt = iterator();
		Iterator<String> otherIt = other.iterator();
		while (thisIt.hasNext()) {
			if (!otherIt.hasNext()) {
				return false;
			}
			String thisAttr = thisIt.next();
			String otherAttr = otherIt.next();
			if (!thisAttr.equals(otherAttr)) {
				return false;
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
	}

}
