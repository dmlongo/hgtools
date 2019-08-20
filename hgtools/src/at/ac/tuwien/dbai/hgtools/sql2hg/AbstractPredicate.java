package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class AbstractPredicate implements Predicate, Iterable<String> {

	protected String name;
	protected HashSet<Attribute> attributes;
	protected int numAttr;

	public AbstractPredicate(String name) {
		this.name = name;
		attributes = new HashSet<>();
		numAttr = 0;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void addAttribute(String attr) {
		if (attr == null) {
			throw new NullPointerException();
		}
		attributes.add(new Attribute(this, attr, numAttr++));
	}

	@Override
	public boolean existsAttribute(String attr) {
		return attributes.contains(new Attribute(null, attr, -1));
	}

	@Override
	public Iterator<String> iterator() {
		List<String> ordered = orderAttributes();
		return ordered.iterator();
	}

	protected List<String> orderAttributes() {
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractPredicate other = (AbstractPredicate) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
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
		BasePredicate leaf1 = new BasePredicate("leaf1");
		leaf1.addAttributes("attr1", "attr2", "attr3");
		System.out.println(leaf1);
		BasePredicate leaf2 = new BasePredicate("leaf2");
		leaf2.addAttributes("col1", "col2");
		System.out.println(leaf2);
		BasePredicate leaf3 = new BasePredicate("leaf3");
		leaf3.addAttributes("id", "name", "surname");
		System.out.println(leaf3);

		ViewPredicate view1 = new ViewPredicate("view1");
		view1.addAttributes("alias1", "alias2", "alias3");
		view1.addDefiningPredicates(leaf2, leaf3);
		view1.defineAttribute("alias1", "leaf2", "col2");
		view1.defineAttribute("alias2", "leaf3", "name");
		view1.defineAttribute("alias3", "leaf2", "col1");
		System.out.println(view1);

		ViewPredicate view2 = new ViewPredicate("view2");
		view2.addAttributes("vAttr1", "vAttr2", "vAttr3");
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
