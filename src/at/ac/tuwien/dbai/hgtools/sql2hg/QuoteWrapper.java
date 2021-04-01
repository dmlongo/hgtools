package at.ac.tuwien.dbai.hgtools.sql2hg;

import at.ac.tuwien.dbai.hgtools.util.Util;

public class QuoteWrapper extends NameTransformer {

	@Override
	protected String transform(String s) {
		return Util.addQuote(s);
	}

}
