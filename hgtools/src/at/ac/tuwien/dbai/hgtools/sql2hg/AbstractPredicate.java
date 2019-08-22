package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Iterator;

public abstract class AbstractPredicate implements Predicate {

	protected PredicateDefinition definition;
	protected String alias;

	public AbstractPredicate(PredicateDefinition def, String alias) {
		if (def == null || alias == null) {
			throw new NullPointerException();
		}
		this.definition = def;
		this.alias = alias;
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
		this.alias = alias;
	}

	@Override
	public String getAlias() {
		return alias.equals("") ? definition.getName() : alias;
	}

	@Override
	public String getAttributeAlias(String attr) {
		Iterator<String> originalIt = definition.iterator();
		Iterator<String> aliasIt = iterator();
		while (aliasIt.hasNext()) {
			String originalAttr = originalIt.next();
			String aliasAttr = aliasIt.next();
			if (attr.equals(originalAttr)) {
				return aliasAttr;
			}
		}
		// TODO a null could be propagated if attr doesn't exist
		return null;
	}

	@Override
	public String getOriginalAttribute(String alias) {
		Iterator<String> originalIt = definition.iterator();
		Iterator<String> aliasIt = iterator();
		while (aliasIt.hasNext()) {
			String originalAttr = originalIt.next();
			String aliasAttr = aliasIt.next();
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
	public Iterator<String> iterator() {
		return new AliasedAttributesIterator();
	}

	@Override
	public void addDefiningPredicate(Predicate pred) {
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
		sb.append(getAlias());
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

}
