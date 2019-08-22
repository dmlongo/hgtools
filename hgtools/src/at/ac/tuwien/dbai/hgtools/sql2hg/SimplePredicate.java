package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;

public class SimplePredicate extends AbstractPredicate {

	private HashMap<String, String> attrToAlias;
	private HashMap<String, String> aliasToAttr;

	public SimplePredicate(PredicateDefinition def, String alias) {
		super(def, alias);
	}

	public SimplePredicate(PredicateDefinition def) {
		super(def);
	}

	@Override
	public void setAttributeAlias(String attr, String alias) {
		if (attr == null || alias == null) {
			throw new NullPointerException();
		}
		if (!definition.existsAttribute(attr)) {
			throw new IllegalArgumentException(definition.getName() + "." + attr + " does not exist");
		}
		// TODO there could be a situation in which the alias is equal to the name of a
		// different attribute. Is it even allowed? I think I should disallow it.
		attrToAlias.put(attr, alias);
		aliasToAttr.put(alias, attr);
	}

	// TODO we can reimplement the other methods for efficiency

}
