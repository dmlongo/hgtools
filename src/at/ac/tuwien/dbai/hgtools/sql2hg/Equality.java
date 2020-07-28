package at.ac.tuwien.dbai.hgtools.sql2hg;

public class Equality {

	private Predicate pred1;
	private String attr1;
	private Predicate pred2;
	private String attr2;

	public Equality(Predicate pred1, String attr1, Predicate pred2, String attr2) {
		if (pred1 == null || attr1 == null || pred2 == null || attr2 == null) {
			throw new NullPointerException();
		}
		if (!pred1.existsAttribute(attr1)) {
			throw new IllegalArgumentException(attr1 + " is not an attribute of " + pred1);
		}
		if (!pred2.existsAttribute(attr2)) {
			throw new IllegalArgumentException(attr2 + " is not an attribute of " + pred2);
		}
		this.pred1 = pred1;
		this.attr1 = attr1;
		this.pred2 = pred2;
		this.attr2 = attr2;
	}

	public Predicate getPredicate1() {
		return pred1;
	}

	public String getAttribute1() {
		return attr1;
	}

	public Predicate getPredicate2() {
		return pred2;
	}

	public String getAttribute2() {
		return attr2;
	}

	@Override
	public String toString() {
		return pred1.getAlias() + "." + attr1 + "=" + pred2.getAlias() + "." + attr2;
	}

}
