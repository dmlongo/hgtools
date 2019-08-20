package at.ac.tuwien.dbai.hgtools.sql2hg;

public class BasePredicate extends AbstractPredicate {

	public BasePredicate(String name) {
		super(name);
	}

	@Override
	public String getDefiningAttribute(String viewAttr) {
		if (!attributes.contains(new Attribute(null, viewAttr, -1))) {
			throw new IllegalArgumentException(name + "." + viewAttr + " does not exists.");
		}
		return name + "." + viewAttr;
	}

}
