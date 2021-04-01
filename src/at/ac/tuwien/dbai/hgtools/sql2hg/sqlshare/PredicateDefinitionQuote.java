package at.ac.tuwien.dbai.hgtools.sql2hg.sqlshare;

import java.util.Collection;
import java.util.LinkedList;

import at.ac.tuwien.dbai.hgtools.sql2hg.Attribute;
import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.util.Util;

public class PredicateDefinitionQuote extends PredicateDefinition {

	public PredicateDefinitionQuote(String name, Collection<String> attributes) {
		super(Util.addQuote(name), new LinkedList<>());
		int pos = 0;
		for (String attrName : attributes) {
			Attribute attr = new Attribute(Util.addQuote(attrName), pos++);
			super.attributes.put(attr, attr);
		}
	}

	@Override
	public boolean existsAttribute(String attr) {
		return super.existsAttribute(Util.addQuote(attr));
	}

	@Override
	public Attribute getAttribute(String attr) {
		return super.getAttribute(Util.addQuote(attr));
	}

	@Override
	public int getPosition(String attr) {
		return super.getPosition(Util.addQuote(attr));
	}

}
