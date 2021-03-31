package at.ac.tuwien.dbai.hgtools.sql2hg;

public class BasePredicate extends SimplePredicate {

	public BasePredicate(PredicateDefinition def, String alias) {
		super(def, alias);
	}

	public BasePredicate(PredicateDefinition def) {
		super(def);
	}

	public BasePredicate(BasePredicate pred) {
		super(pred);
	}

	@Override
	public String getDefiningAttribute(String viewAttr) {
		// TODO should I connect viewAttr (presumably aliased) to the original
		// attribute?
		if (!existsAttribute(viewAttr)) {
			throw new IllegalArgumentException(alias + SEP + viewAttr + " does not exists.");
		}
		return alias + SEP + viewAttr;
	}

}
