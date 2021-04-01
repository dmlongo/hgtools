package at.ac.tuwien.dbai.hgtools.sql2hg;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;

public class TableReplacer {

	private StatVisitor vStatement = new StatVisitor();
	private SelVisitor vSelect = new SelVisitor();
	private SelItVisitor vSelectItem = new SelItVisitor();
	private FromItVisitor vFromItem = new FromItVisitor();
	private ExprVisitor vExpression = new ExprVisitor();

	private String oldName = null;
	private String newName = null;

	public void replace(Statement stmt, String oldName, String newName) {
		this.oldName = oldName;
		this.newName = newName;
		stmt.accept(vStatement);
	}

	private class SelVisitor extends SelectVisitorAdapter {

		@Override
		public void visit(PlainSelect plainSelect) {
			if (plainSelect.getSelectItems() != null) {
				for (SelectItem item : plainSelect.getSelectItems()) {
					item.accept(vSelectItem);
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
			if (withItem.getSelectBody() != null) {
				withItem.getSelectBody().accept(this);
			}
		}

	}

	private class SelItVisitor extends SelectItemVisitorAdapter {

		@Override
		public void visit(AllTableColumns columns) {
			Table t = columns.getTable();
			if (t != null && t.getName().equals(oldName)) {
				t.setName(newName);
			}
		}

		@Override
		public void visit(SelectExpressionItem item) {
			item.accept(vExpression);
		}

	}

	private class FromItVisitor extends FromItemVisitorAdapter {

		@Override
		public void visit(Table table) {
			if (table.getName().equals(oldName)) {
				table.setName(newName);
			}
		}

		@Override
		public void visit(SubSelect subSelect) {
			if (subSelect.getWithItemsList() != null) {
				for (WithItem item : subSelect.getWithItemsList()) {
					item.accept(vSelect);
				}
			}
			if (subSelect.getSelectBody() != null) {
				subSelect.getSelectBody().accept(vSelect);
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

		@Override
		public void visit(Column column) {
			Table t = column.getTable();
			if (t != null && t.getName().equals(oldName)) {
				t.setName(newName);
			}
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
