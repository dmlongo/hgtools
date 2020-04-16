package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.LinkedList;
import java.util.List;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.ValuesList;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.values.ValuesStatement;

public class PredicateFinder implements FromItemVisitor, SelectVisitor, SelectItemVisitor {

	private Schema schema;
	private String name;
	private List<String> attributes;
	private PredicateDefinition pred;

	private List<FromItem> fromItems;

	public PredicateFinder(Schema schema) {
		if (schema == null) {
			throw new NullPointerException();
		}
		this.schema = schema;
	}

	public PredicateDefinition getPredicate(FromItem fromItem) {
		name = "";
		attributes = new LinkedList<>();
		fromItems = new LinkedList<>();
		fromItem.accept(this);
		pred = new PredicateDefinition(name, attributes);
		return pred;
	}

	public PredicateDefinition getPredicate(PlainSelect plainSelect) {
		name = "";
		attributes = new LinkedList<>();
		fromItems = new LinkedList<>();
		plainSelect.accept(this);
		pred = new PredicateDefinition(name, attributes);
		return pred;
	}

	public PredicateDefinition getPredicate(SetOperationList setOpList) {
		PlainSelect ps = findPlainSelect(setOpList);
		return getPredicate(ps);
	}

	private PlainSelect findPlainSelect(SetOperationList setOpList) {
		for (SelectBody sb : setOpList.getSelects()) {
			if (sb instanceof PlainSelect) {
				return (PlainSelect) sb;
			} else if (sb instanceof SetOperationList) {
				return findPlainSelect((SetOperationList) sb);
			}
		}
		return null;
	}

	// FromItemVisitor

	@Override
	public void visit(Table tableName) {
		name = tableName.getName();
		attributes = schema.getPredicateDefinition(name).getAttributes();
	}

	@Override
	public void visit(SubSelect subSelect) {
		if (subSelect.getAlias() != null) {
			name = subSelect.getAlias().getName();
		}
		// assuming there are no views
		fromItems.clear();
		subSelect.getSelectBody().accept(this);
	}

	@Override
	public void visit(SubJoin subjoin) {
	}

	@Override
	public void visit(LateralSubSelect lateralSubSelect) {
	}

	@Override
	public void visit(ValuesList valuesList) {
	}

	@Override
	public void visit(TableFunction tableFunction) {
	}

	@Override
	public void visit(ParenthesisFromItem aThis) {
	}

	// SelectVisitor

	@Override
	public void visit(PlainSelect plainSelect) {
		if (plainSelect.getFromItem() != null) {
			fromItems.add(plainSelect.getFromItem());
		}

		if (plainSelect.getJoins() != null) {
			for (Join join : plainSelect.getJoins()) {
				fromItems.add(join.getRightItem());
			}
		}

		if (plainSelect.getSelectItems() != null) {
			for (SelectItem item : plainSelect.getSelectItems()) {
				item.accept(this);
			}
		}
	}

	@Override
	public void visit(SetOperationList setOpList) {
	}

	@Override
	public void visit(WithItem withItem) {
	}

	@Override
	public void visit(ValuesStatement aThis) {
	}

	// SelectItemVisitor

	@Override
	public void visit(AllColumns allColumns) {
		for (FromItem item : fromItems) {
			PredicateDefinition p = new PredicateFinder(schema).getPredicate(item);
			attributes.addAll(p.getAttributes());
		}
	}

	@Override
	public void visit(AllTableColumns allTableColumns) {
		String table = allTableColumns.getTable().getName();
		PredicateDefinition pred = schema.getPredicateDefinition(table);
		attributes.addAll(pred.getAttributes());
	}

	@Override
	public void visit(SelectExpressionItem selectExpressionItem) {
		if (selectExpressionItem.getAlias() != null) {
			attributes.add(selectExpressionItem.getAlias().getName());
		} else if (selectExpressionItem.getExpression() instanceof Column) {
			Column col = (Column) selectExpressionItem.getExpression();
			attributes.add(col.getColumnName());
		}
	}

}
