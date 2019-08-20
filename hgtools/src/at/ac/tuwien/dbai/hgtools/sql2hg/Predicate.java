package at.ac.tuwien.dbai.hgtools.sql2hg;

public interface Predicate extends Iterable<String> {
	String getName();

	default void addAttributes(String... attrs) {
		for (String attr : attrs) {
			addAttribute(attr);
		}
	}

	void addAttribute(String attr);

	boolean existsAttribute(String attr);

	default void addDefiningPredicates(Predicate... preds) {
		for (Predicate pred : preds) {
			addDefiningPredicate(pred);
		}
	}

	void addDefiningPredicate(Predicate pred);

	void defineAttribute(String viewAttr, String defPred, String defAttr);

	String getDefiningAttribute(String viewAttr);
}
