package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Collection;
import java.util.List;

public interface Predicate extends Iterable<String> {

	PredicateDefinition getPredicateDefinition();

	String getPredicateName();

	int arity();

	void setAlias(String alias);

	String getAlias();

	void setAttributeAlias(String attr, String alias);

	String getAttributeAlias(String attr);

	String getOriginalAttribute(String alias);

	boolean existsAttribute(String attr);

	// PredicateComponent

	void addDefiningPredicate(Predicate pred);

	Collection<Predicate> getDefiningPredicates();

	void defineAttribute(String viewAttr, String defPred, String defAttr);

	String getDefiningAttribute(String viewAttr);

	void addJoin(String pred1, String attr1, String pred2, String attr2);

	List<Equality> getJoins();

	default void addDefiningPredicates(Predicate... preds) {
		for (Predicate pred : preds) {
			addDefiningPredicate(pred);
		}
	}

}
