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
public class Attribute implements Comparable<Attribute> {
	
	private String name;
	private int position;

	public Attribute(String name, int pos) {
		this.name = name;
		this.position = pos;
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
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equalsIgnoreCase(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return name;
	}
	
}