package at.ac.tuwien.dbai.hgtools;

import java.util.Collection;
import java.util.HashSet;

import at.ac.tuwien.dbai.hgtools.sql2hg.Predicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateFinder;
import at.ac.tuwien.dbai.hgtools.sql2hg.QueryVisitorNoExpressionAdapter;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;

public class ViewPredicatesFinder extends QueryVisitorNoExpressionAdapter {

	private Collection<Predicate> viewPreds;

	public ViewPredicatesFinder() {
		// TODO Auto-generated constructor stub
	}

	public Collection<Predicate> getViewPredicates(Select select) {
		viewPreds = new HashSet<Predicate>();
		select.accept(this);
		return viewPreds;
	}

	@Override
	public void visit(Select select) {
		if (select.getWithItemsList() != null) {
			for (WithItem withItem : select.getWithItemsList()) {
				withItem.accept(this);
			}
		}
	}

	@Override
	public void visit(WithItem withItem) {
		PredicateDefinition predDef = new PredicateFinder(new Schema()).getPredicate(withItem);
	}

}
