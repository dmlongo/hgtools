package at.ac.tuwien.dbai.hgtools.sql2hg;

/**
 * compareTo and equals are inconsistent. compareTo is based on order, while
 * equals is based on attribute name.
 * 
 * the methods do not consider the predicate they belong to.
 * 
 * @author david
 *
 */
public final class Attribute implements Comparable<Attribute> {

	private final String name;
	private final int position;

	public Attribute(String name, int pos) {
		if (name == null || pos < 0) {
			throw new IllegalArgumentException();
		}
		this.name = name;
		this.position = pos;
	}

	public Attribute(String name) {
		this(name, Integer.MAX_VALUE);
	}

	public String getName() {
		return name;
	}

	public int getPosition() {
		return position;
	}

	@Override
	public int compareTo(Attribute other) {
		return this.position - other.position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.toLowerCase().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Attribute)) {
			return false;
		}
		Attribute other = (Attribute) obj;
		return name.equalsIgnoreCase(other.name);
	}

	@Override
	public String toString() {
		return name;
	}

}