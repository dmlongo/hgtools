package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;
import java.util.List;

public class SimplePredicate extends AbstractPredicate {

	private HashMap<String, String> attrToAlias;
	private HashMap<String, String> aliasToAttr;

	public SimplePredicate(PredicateDefinition def, String alias) {
		super(def, alias);
		attrToAlias = new HashMap<>();
		aliasToAttr = new HashMap<>();
	}

	public SimplePredicate(PredicateDefinition def) {
		super(def);
		attrToAlias = new HashMap<>();
		aliasToAttr = new HashMap<>();
	}

	public SimplePredicate(SimplePredicate pred) {
		super(pred.definition, pred.alias);
		attrToAlias = new HashMap<>();
		aliasToAttr = new HashMap<>();
		for (String attr : pred.attrToAlias.keySet()) {
			attrToAlias.put(attr, pred.attrToAlias.get(attr));
			aliasToAttr.put(pred.attrToAlias.get(attr), attr);
		}
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

	@Override
	public String getAttributeAlias(String attr) {
		String result = attrToAlias.get(attr);
		if (result == null && definition.existsAttribute(attr)) {
			result = attr;
		}
		return result;
	}

	@Override
	public String getOriginalAttribute(String alias) {
		String result = aliasToAttr.get(alias);
		if (result == null && definition.existsAttribute(alias)) {
			result = alias;
		}
		return result;
	}

	/**
	 * Exists attribute either aliased or original.
	 * 
	 * @return
	 */
	@Override
	public boolean existsAttribute(String attr) {
		return definition.existsAttribute(attr) || aliasToAttr.containsKey(attr);
	}

	public static void main(String[] args) {
		PredicateDefinition p1Def = new PredicateDefinition("p1", new String[] { "a1", "a2", "a3" });
		PredicateDefinition p2Def = new PredicateDefinition("pred2", new String[] { "a1", "a2", "a3" });
		PredicateDefinition p3Def = new PredicateDefinition("pp3", new String[] { "c2", "a1" });
		PredicateDefinition p1CopyDef = new PredicateDefinition("p1", new String[] { "a1", "a2", "a3" });

		SimplePredicate p1 = new SimplePredicate(p1Def);
		p1.setAlias("mainPred");
		p1.setAttributeAlias("a1", "mainAttr");
		System.out.println(p1.existsAttribute("mainAttr"));
		p1.getAttributeAlias("a1");
		p1.getOriginalAttribute("mainAttr");

		// System.out.println(p1);
	}

}
