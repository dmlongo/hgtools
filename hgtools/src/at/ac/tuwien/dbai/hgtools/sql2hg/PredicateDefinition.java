package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class PredicateDefinition implements Iterable<String> {

	private String name;
	private HashSet<Attribute> attributes;

	public PredicateDefinition(String name, Collection<String> attributes) {
		if (name == null) {
			throw new NullPointerException();
		}
		this.name = name;
		this.attributes = new HashSet<>();
		int pos = 0;
		for (String attrName : attributes) {
			Attribute attr = new Attribute(attrName, pos++);
			this.attributes.add(attr);
		}
	}
	
	public PredicateDefinition(String name, String[] attributes) {
		if (name == null) {
			throw new NullPointerException();
		}
		this.name = name;
		this.attributes = new HashSet<>();
		int pos = 0;
		for (String attrName : attributes) {
			Attribute attr = new Attribute(attrName, pos++);
			this.attributes.add(attr);
		}
	}

	public int arity() {
		return attributes.size();
	}

	public String getName() {
		return name;
	}

	public boolean existsAttribute(String attr) {
		return attributes.contains(new Attribute(attr, -1));
	}

	@Override
	public Iterator<String> iterator() {
		List<String> ordered = orderAttributes();
		return ordered.iterator();
	}

	private List<String> orderAttributes() {
		ArrayList<Attribute> entries = new ArrayList<>(attributes);
		Collections.sort(entries);
		// TODO rewrite it using lambda functions
		ArrayList<String> ordered = new ArrayList<String>(entries.size());
		for (Attribute attr : entries) {
			ordered.add(attr.getName());
		}
		return ordered;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PredicateDefinition)) {
			return false;
		}
		PredicateDefinition other = (PredicateDefinition) obj;
		if (attributes == null) {
			if (other.attributes != null) {
				return false;
			}
		} else if (!attributes.equals(other.attributes)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(200);
		sb.append(name);
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
		PredicateDefinition p1 = new PredicateDefinition("p1", new String[] { "a1", "a2", "a3" });
		PredicateDefinition p2 = new PredicateDefinition("pred2", new String[] { "a1", "a2", "a3" });
		PredicateDefinition p3 = new PredicateDefinition("pp3", new String[] { "c2", "a1" });
		PredicateDefinition p1Copy = new PredicateDefinition("p1", new String[] { "a1", "a2", "a3" });
		PredicateDefinition[] preds = new PredicateDefinition[] { p1, p2, p3, p1Copy };

		for (PredicateDefinition p : preds) {
			System.out.println(p + " name: " + p.getName());
			System.out.println(p + " arity: " + p.arity());
			System.out.print(p + " attributes: ");
			for (String attr : p) {
				System.out.print(attr + " ");
			}
			System.out.println();
		}
		System.out.println();

		for (PredicateDefinition p : preds) {
			System.out.println(p + " contains a1: " + p.existsAttribute("a1"));
			System.out.println(p + " contains c2: " + p.existsAttribute("c2"));
			System.out.println(p + " contains hg: " + p.existsAttribute("hg"));
		}
		System.out.println();

		System.out.println(p1 + "=" + p2 + ": " + p1.equals(p2));
		System.out.println(p3 + "=" + p1Copy + ": " + p3.equals(p1Copy));
		System.out.println(p1 + "=" + p1Copy + ": " + p1.equals(p1Copy));
	}

}
