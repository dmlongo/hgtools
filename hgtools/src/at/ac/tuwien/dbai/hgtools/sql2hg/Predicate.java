package at.ac.tuwien.dbai.hgtools.sql2hg;

public interface Predicate extends Iterable<String> {
	
	PredicateDefinition getPredicateDefinition();
	
	String getPredicateName();

	void setAlias(String alias);
	String getAlias();
	
	void setAttributeAlias(String attr, String alias);
	String getAttributeAlias(String attr);
	String getOriginalAttribute(String alias);

	boolean existsAttribute(String attr);
	
	// PredicateComponent
	
	void addDefiningPredicate(Predicate pred);

	void defineAttribute(String viewAttr, String defPred, String defAttr);

	String getDefiningAttribute(String viewAttr);

	default void addDefiningPredicates(Predicate... preds) {
		for (Predicate pred : preds) {
			addDefiningPredicate(pred);
		}
	}
	
}
