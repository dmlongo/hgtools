package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;
import java.util.Iterator;

public class PredicateInQuery implements Iterable<String> {

	private Predicate pred;
	private String alias;
	private HashMap<String, String> attrToAlias;
	private HashMap<String, String> aliasToAttr;

	public PredicateInQuery(Predicate pred) {
		if (pred == null) {
			throw new NullPointerException();
		}
		this.pred = pred;
		alias = pred.getName();
		attrToAlias = new HashMap<>();
		aliasToAttr = new HashMap<>();
	}

	public String getPredicateName() {
		return pred.getName();
	}
	
	public void setAlias(String alias) {
		if (alias == null) {
			throw new NullPointerException();
		}
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	public void setAttributeAlias(String attr, String alias) {
		if (attr == null || alias == null) {
			throw new NullPointerException();
		}
		if (!pred.existsAttribute(attr)) {
			throw new IllegalArgumentException(pred.getName() + "." + attr + " does not exist");
		}
		// TODO there could be a situation in which the alias is equal to the name of a
		// different attribute. Is it even allowed? I think I should disallow it.
		attrToAlias.put(attr, alias);
		aliasToAttr.put(alias, attr);
	}

	public String getAttributeAlias(String attr) {
		String result = attrToAlias.get(attr);
		if (result == null && pred.existsAttribute(attr)) {
			result = attr;
		}
		return result;
	}

	public String getOriginalAttribute(String alias) {
		String result = aliasToAttr.get(alias);
		if (result == null && pred.existsAttribute(alias)) {
			result = alias;
		}
		return result;
	}

	/**
	 * Exists attribute either aliased or original.
	 * 
	 * @return
	 */
	public boolean existsAttribute(String attr) {
		return pred.existsAttribute(attr) || aliasToAttr.containsKey(attr);
	}

	@Override
	public Iterator<String> iterator() {
		return new MixedIterator();
	}

	/**
	 * 
	 * Iterates over the attributes of the original predicate, but uses column
	 * aliases, if defined.
	 * 
	 * @author david
	 *
	 */
	private class MixedIterator implements Iterator<String> {

		private Iterator<String> predIt = pred.iterator();

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
		result = prime * result + ((aliasToAttr == null) ? 0 : aliasToAttr.hashCode());
		result = prime * result + ((pred == null) ? 0 : pred.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PredicateInQuery)) {
			return false;
		}
		PredicateInQuery other = (PredicateInQuery) obj;
		if (alias == null) {
			if (other.alias != null) {
				return false;
			}
		} else if (!alias.equals(other.alias)) {
			return false;
		}
		if (aliasToAttr == null) {
			if (other.aliasToAttr != null) {
				return false;
			}
		} else if (!aliasToAttr.equals(other.aliasToAttr)) {
			return false;
		}
		if (pred == null) {
			if (other.pred != null) {
				return false;
			}
		} else if (!pred.equals(other.pred)) {
			return false;
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

}
