package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.LinkedList;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitor;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;

public abstract class NameTransformer {

	private StatVisitor vStatement = new StatVisitor();
	private SelVisitor vSelect = new SelVisitor();
	private SelItVisitor vSelectItem = new SelItVisitor();
	private FromItVisitor vFromItem = new FromItVisitor();
	private ExprVisitor vExpression = new ExprVisitor();
	private OrderByVisitor vOrderBy = new OrdByVisitor();

	public void run(Statement stmt) {
		stmt.accept(vStatement);
	}

	protected abstract String transform(String s);

	private class SelVisitor extends SelectVisitorAdapter {

		@Override
		public void visit(PlainSelect plainSelect) {
			if (plainSelect.getFromItem() != null) {
				plainSelect.getFromItem().accept(vFromItem);
			}
			if (plainSelect.getJoins() != null) {
				for (Join j : plainSelect.getJoins()) {
					if (j.getRightItem() != null) {
						j.getRightItem().accept(vFromItem);
					}
					if (j.getOnExpression() != null) {
						j.getOnExpression().accept(vExpression);
					}
				}
			}

			if (plainSelect.getSelectItems() != null) {
				plainSelect.getSelectItems().forEach(item -> item.accept(vSelectItem));
			}

			if (plainSelect.getWhere() != null) {
				plainSelect.getWhere().accept(vExpression);
			}

			if (plainSelect.getHaving() != null) {
				plainSelect.getHaving().accept(vExpression);
			}

			if (plainSelect.getOrderByElements() != null) {
				plainSelect.getOrderByElements().forEach(obe -> obe.accept(vOrderBy));
			}
		}

		@Override
		public void visit(SetOperationList setOpList) {
			if (setOpList.getSelects() != null) {
				for (SelectBody set : setOpList.getSelects()) {
					set.accept(this);
				}
			}
		}

		@Override
		public void visit(WithItem withItem) {
			String name = withItem.getName();
			withItem.setName(transform(name));
			if (withItem.getWithItemList() != null) {
				for (SelectItem item : withItem.getWithItemList()) {
					item.accept(vSelectItem);
				}
			}
			withItem.getSelectBody().accept(vSelect);
		}

	}

	private class SelItVisitor extends SelectItemVisitorAdapter {

		@Override
		public void visit(AllTableColumns columns) {
			columns.getTable().accept(vFromItem);
		}

		@Override
		public void visit(SelectExpressionItem item) {
			if (item.getAlias() != null) {
				String alias = item.getAlias().getName();
				item.setAlias(new Alias(transform(alias)));
			}
			item.accept(vExpression);
		}

	}

	private class FromItVisitor extends FromItemVisitorAdapter {

		@Override
		public void visit(Table table) {
			String name = table.getName();
			table.setName(transform(name));
			if (table.getAlias() != null) {
				String alias = table.getAlias().getName();
				table.setAlias(new Alias(transform(alias)));
			}
		}

		@Override
		public void visit(SubSelect subSelect) {
			if (subSelect.getAlias() != null) {
				String alias = subSelect.getAlias().getName();
				subSelect.setAlias(new Alias(transform(alias)));
			}
			if (subSelect.getWithItemsList() != null) {
				for (WithItem item : subSelect.getWithItemsList()) {
					item.accept(vSelect);
				}
			}
			subSelect.getSelectBody().accept(vSelect);
		}

		@Override
		public void visit(SubJoin subjoin) {
			if (subjoin.getAlias() != null) {
				String alias = subjoin.getAlias().getName();
				subjoin.setAlias(new Alias(transform(alias)));
			}
			if (subjoin.getLeft() != null) {
				subjoin.getLeft().accept(this);
			}
			if (subjoin.getJoinList() != null) {
				for (Join j : subjoin.getJoinList()) {
					if (j.getRightItem() != null) {
						j.getRightItem().accept(this);
					}
					if (j.getOnExpression() != null) {
						j.getOnExpression().accept(vExpression);
					}
				}
			}
		}

		@Override
		public void visit(ParenthesisFromItem aThis) {
			if (aThis.getAlias() != null) {
				String alias = aThis.getAlias().getName();
				aThis.setAlias(new Alias(transform(alias)));
			}
			if (aThis.getFromItem() != null) {
				aThis.getFromItem().accept(this);
			}
		}

	}

	private class ExprVisitor extends ExpressionVisitorAdapterFixed {

		public ExprVisitor() {
			setSelectVisitor(vSelect);
		}

		@Override
		public void visit(Column column) {
			if (column.getTable() != null) {
				column.getTable().accept(vFromItem);
			}
			String name = column.getColumnName();
			column.setColumnName(transform(name));
		}

		@Override
		public void visit(SubSelect subSelect) {
			if (subSelect.getAlias() != null) {
				String alias = subSelect.getAlias().getName();
				subSelect.setAlias(new Alias(transform(alias)));
			}
			if (subSelect.getWithItemsList() != null) {
				for (WithItem item : subSelect.getWithItemsList()) {
					item.accept(vSelect);
				}
			}
			subSelect.getSelectBody().accept(vSelect);
		}

	}

	private class StatVisitor extends StatementVisitorAdapter {

		@Override
		public void visit(Select select) {
			if (select.getWithItemsList() != null) {
				for (WithItem item : select.getWithItemsList()) {
					item.accept(vSelect);
				}
			}
			select.getSelectBody().accept(vSelect);
		}

		@Override
		public void visit(CreateView createView) {
			String name = createView.getView().getName();
			createView.getView().setName(transform(name));
			if (createView.getColumnNames() != null) {
				LinkedList<String> colNames = new LinkedList<>();
				for (String col : createView.getColumnNames()) {
					colNames.add(transform(col));
				}
				createView.setColumnNames(colNames);
			}
			createView.getSelect().accept(this);
		}

	}

	private class OrdByVisitor implements OrderByVisitor {

		@Override
		public void visit(OrderByElement orderBy) {
			if (orderBy.getExpression() != null) {
				orderBy.getExpression().accept(vExpression);
			}
		}

	}

}
