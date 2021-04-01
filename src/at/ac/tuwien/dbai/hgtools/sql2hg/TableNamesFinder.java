package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashSet;
import java.util.Set;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;

public class TableNamesFinder {

	private StatVisitor vStatement = new StatVisitor();
	private SelVisitor vSelect = new SelVisitor();
	private FromItVisitor vFromItem = new FromItVisitor();
	private ExprVisitor vExpression = new ExprVisitor();

	private HashSet<String> tables = null;

	public Set<String> findTables(Statement stmt) {
		tables = new HashSet<>();
		stmt.accept(vStatement);
		return tables;
	}

	private class SelVisitor extends SelectVisitorAdapter {

		@Override
		public void visit(PlainSelect plainSelect) {
			if (plainSelect.getSelectItems() != null) {
				for (SelectItem item : plainSelect.getSelectItems()) {
					item.accept(vExpression);
				}
			}

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

			if (plainSelect.getWhere() != null) {
				plainSelect.getWhere().accept(vExpression);
			}

			if (plainSelect.getHaving() != null) {
				plainSelect.getHaving().accept(vExpression);
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
			if (withItem.getWithItemList() != null) {
				for (SelectItem item : withItem.getWithItemList()) {
					item.accept(vExpression);
				}
			}
			withItem.getSelectBody().accept(this);
		}

	}

	private class FromItVisitor extends FromItemVisitorAdapter {

		@Override
		public void visit(Table table) {
			tables.add(table.getName());
		}

		@Override
		public void visit(SubSelect subSelect) {
			if (subSelect.getWithItemsList() != null) {
				for (WithItem item : subSelect.getWithItemsList()) {
					item.accept(vSelect);
				}
			}
			subSelect.getSelectBody().accept(vSelect);
		}

		@Override
		public void visit(SubJoin subjoin) {
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
			if (aThis.getFromItem() != null) {
				aThis.getFromItem().accept(this);
			}
		}

	}

	private class ExprVisitor extends ExpressionVisitorAdapterFixed {

		public ExprVisitor() {
			setSelectVisitor(vSelect);
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
			createView.getSelect().accept(this);
		}

	}

}
