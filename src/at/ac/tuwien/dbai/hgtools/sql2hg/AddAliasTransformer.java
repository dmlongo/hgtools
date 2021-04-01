package at.ac.tuwien.dbai.hgtools.sql2hg;

import net.sf.jsqlparser.expression.Alias;
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

public class AddAliasTransformer {

	private StatVisitor vStatement = new StatVisitor();
	private SelVisitor vSelect = new SelVisitor();
	private FromItVisitor vFromItem = new FromItVisitor();
	private ExprVisitor vExpression = new ExprVisitor();

	private String prefix = "anonymous";
	private int id = 0;

	public void run(Statement stmt) {
		stmt.accept(vStatement);
	}

	private String nextAlias() {
		return prefix + id++;
	}

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
				for (SelectItem item : plainSelect.getSelectItems()) {
					item.accept(vExpression);
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
			withItem.getSelectBody().accept(vSelect);
		}

	}

	private class FromItVisitor extends FromItemVisitorAdapter {

		@Override
		public void visit(SubSelect subSelect) {
			if (subSelect.getAlias() == null) {
				String alias = nextAlias();
				subSelect.setAlias(new Alias(alias));
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
			if (subjoin.getAlias() == null) {
				String alias = nextAlias();
				subjoin.setAlias(new Alias(alias));
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
			if (aThis.getAlias() == null) {
				String alias = nextAlias();
				aThis.setAlias(new Alias(alias));
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
		public void visit(SubSelect subSelect) {
			if (subSelect.getAlias() == null) {
				String alias = nextAlias();
				subSelect.setAlias(new Alias(alias));
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
			createView.getSelect().accept(this);
		}

	}

}
