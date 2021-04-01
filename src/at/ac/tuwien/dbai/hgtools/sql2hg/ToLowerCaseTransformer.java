package at.ac.tuwien.dbai.hgtools.sql2hg;

public class ToLowerCaseTransformer extends NameTransformer {

	@Override
	protected String transform(String s) {
		return s.toLowerCase();
	}

}
