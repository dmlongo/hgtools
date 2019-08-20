package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashSet;
import java.util.LinkedList;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;

public class HypergraphFinder extends QueryVisitorUnsupportedAdapter {

	private static enum ClauseType {
		ColumnOpColumn, ColumnOpConstant, Other;

		public static ClauseType determineClauseType(ComparisonOperator op) {
			Expression left = op.getLeftExpression();
			Expression right = op.getRightExpression();
			if (left instanceof Column && right instanceof Column) {
				return ColumnOpColumn;
			} else if (left instanceof Column && isConstantValue(right)) {
				return ColumnOpConstant;
			} else {
				return Other;
			}
		}

		private static boolean isConstantValue(Expression expr) {
			return expr instanceof DateValue || expr instanceof DoubleValue || expr instanceof HexValue
					|| expr instanceof LongValue || expr instanceof NullValue || expr instanceof StringValue
					|| expr instanceof TimestampValue || expr instanceof TimeValue;
		}
	}

	private static String getTableAliasName(Table table) {
		String tableAliasName;
		if (table.getAlias() != null)
			tableAliasName = table.getAlias().getName();
		else
			tableAliasName = table.getName();
		return tableAliasName;
	}

	private static enum ParsingState {
		Waiting, ReadingView, InSelect, Finished
	}

	private ParsingState currentState;

	private Schema schema;
	private NameResolver nResolver;

	private ViewPredicate currentView;
	private LinkedList<SelectExpressionItem> selExprItBuffer;

	private HashSet<PredicateInQuery> tables;
	private HashSet<Equality> joins;

	public HypergraphFinder(Schema schema) {
		if (schema == null) {
			throw new NullPointerException();
		}
		this.schema = schema;
		nResolver = new NameResolver();
		nResolver.enterNewScope(); // global scope
		currentView = null;
		selExprItBuffer = new LinkedList<>();
		currentState = ParsingState.Waiting;
		tables = new HashSet<>();
		joins = new HashSet<>();
	}

	public void run(Statement statement) {
		statement.accept(this);
	}

	public HashSet<PredicateInQuery> getTables() {
		return tables;
	}

	public HashSet<Equality> getJoins() {
		return joins;
	}

	// SelectVisitor

	@Override
	public void visit(WithItem withItem) {
		currentView = new ViewPredicate(withItem.getName());
		if (withItem.getWithItemList() != null) {
			// TODO these items should just be names for the columns of the view
			throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
		}
		withItem.getSelectBody().accept(this);
		schema.addPredicate(currentView);
		// TODO the name of the view must be added to nResolver at some point
		currentView = null;
	}

	@Override
	public void visit(PlainSelect plainSelect) {
		nResolver.enterNewScope();
		if (plainSelect.getSelectItems() != null) {
			for (SelectItem item : plainSelect.getSelectItems()) {
				item.accept(this);
			}
		}

		if (plainSelect.getFromItem() != null) {
			plainSelect.getFromItem().accept(this);
		}

		if (plainSelect.getJoins() != null) {
			for (Join join : plainSelect.getJoins()) {
				join.getRightItem().accept(this);
			}
		}

		if (currentState == ParsingState.ReadingView) {
			for (SelectExpressionItem item : selExprItBuffer) {
				String viewAttr = item.getAlias() != null ? item.getAlias().getName() : null;
				Column col = extractColumn(item.getExpression());
				String defPred = nResolver.resolveColumn(col).getPredicateName();
				String defAttr = col.getColumnName();
				if (!currentView.existsAttribute(viewAttr)) {
					currentView.addAttribute(viewAttr);
				}
				currentView.defineAttribute(viewAttr, defPred, defAttr);
			}
			selExprItBuffer.clear();
		}

		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(this);
		}

		if (plainSelect.getOracleHierarchical() != null) {
			plainSelect.getOracleHierarchical().accept(this);
		}
		nResolver.exitCurrentScope();
	}

	private Column extractColumn(Expression expression) {
		Expression expr = expression;
		while (!(expr instanceof Column)) {
			expr = ((Function) expr).getAttribute();
		}
		return ((Column) expr);
	}

	// SelectItemVisitor

	@Override
	public void visit(AllColumns allColumns) {
	}

	@Override
	public void visit(AllTableColumns allTableColumns) {
	}

	@Override
	public void visit(SelectExpressionItem item) {
		selExprItBuffer.addLast(item);
		/*
		 * item.getExpression().accept(this); if (currentView != null &&
		 * viewColumnMap[0] != null) { if (item.getAlias() != null) { viewColumnMap[1] =
		 * item.getAlias().getName(); } else { viewColumnMap[1] = viewColumnMap[0]; }
		 * schema.addViewColumn(currentView, viewColumnMap[1], viewColumnMap[0]);
		 * viewColumnMap[0] = null; viewColumnMap[1] = null; }
		 */
	}

	// FromItemVisitor

	@Override
	public void visit(Table tableName) {
		// TODO what if I'm not coming from a FROM clause?
		String tableWholeName = tableName.getFullyQualifiedName();
		String tableAliasName = getTableAliasName(tableName);
		Predicate pred = schema.getPredicate(tableWholeName);
		PredicateInQuery table = new PredicateInQuery(pred);
		table.setAlias(tableAliasName);
		// TODO column aliases must be dealt with here
		nResolver.addTableToCurrentScope(table, tableAliasName);
		switch (currentState) {
		case ReadingView:
			currentView.addDefiningPredicate(pred);
			break;
		case InSelect:
			tables.add(table);
			break;
		default:
			throw new AssertionError("The current state cannot be: " + currentState);
		}

		/*
		 * String tableWholeName = tableName.getFullyQualifiedName(); String
		 * tableAliasName = getTableAliasName(tableName); if
		 * (!otherItemNames.contains(tableWholeName.toLowerCase()) &&
		 * !tables.containsKey(tableAliasName)) { tables.put(tableAliasName,
		 * tableWholeName); } /* TODO When tableName is the name of a view, I should add
		 * the "expanded" views (the original tables) instead of the name of the view to
		 * the current scope
		 */
		// tf.addTableToCurrentScope(tableWholeName, tableAliasName);
	}

	@Override
	public void visit(SubSelect subSelect) {
		// TODO Auto-generated method stub
		super.visit(subSelect);
		/*
		 * if (subSelect.getWithItemsList() != null) { for (WithItem withItem :
		 * subSelect.getWithItemsList()) { withItem.accept(this); } }
		 * subSelect.getSelectBody().accept(this);
		 */
	}

	@Override
	public void visit(SubJoin subjoin) {
		// TODO Auto-generated method stub
		super.visit(subjoin);
		/*
		 * subjoin.getLeft().accept(this); for (Join j : subjoin.getJoinList()) {
		 * j.getRightItem().accept(this); }
		 */
	}

	@Override
	public void visit(LateralSubSelect lateralSubSelect) {
		// TODO Auto-generated method stub
		super.visit(lateralSubSelect);
		// lateralSubSelect.getSubSelect().getSelectBody().accept(this);
	}

	// ExpressionVisitor

	@Override
	public void visit(EqualsTo equalsTo) {
		ClauseType ct = ClauseType.determineClauseType(equalsTo);
		switch (ct) {
		case ColumnOpColumn:
			Column left = (Column) equalsTo.getLeftExpression();
			Column right = (Column) equalsTo.getRightExpression();
			PredicateInQuery pred1 = nResolver.resolveColumn(left);
			PredicateInQuery pred2 = nResolver.resolveColumn(right);
			String leftColumn = left.getColumnName();
			String rightColumn = right.getColumnName();
			Equality eq = new Equality(pred1, leftColumn, pred2, rightColumn);
			joins.add(eq);
			// TODO it mustn't be done if I'm building a view
			break;
		case ColumnOpConstant:
			break;
		case Other:
			super.visit(equalsTo);
			break;
		default:
			throw new AssertionError("Unknown clause type: " + ct);
		}
	}

	@Override
	public void visit(GreaterThan greaterThan) {
	}

	@Override
	public void visit(GreaterThanEquals greaterThanEquals) {
	}

	@Override
	public void visit(MinorThan minorThan) {
	}

	@Override
	public void visit(MinorThanEquals minorThanEquals) {
	}

	@Override
	public void visit(NotEqualsTo notEqualsTo) {
	}

	@Override
	public void visit(AndExpression andExpression) {
		visitBinaryExpression(andExpression);
	}

	public void visitBinaryExpression(BinaryExpression binaryExpression) {
		binaryExpression.getLeftExpression().accept(this);
		binaryExpression.getRightExpression().accept(this);
	}

	// StatementVisitor

	@Override
	public void visit(Select select) {
		if (select.getWithItemsList() != null) {
			currentState = ParsingState.ReadingView;
			for (WithItem withItem : select.getWithItemsList()) {
				withItem.accept(this);
			}
			currentState = ParsingState.Waiting;
		}
		currentState = ParsingState.InSelect;
		select.getSelectBody().accept(this);
		currentState = ParsingState.Finished;
	}

	@Override
	public void visit(Insert insert) {
		// TODO Auto-generated method stub
		super.visit(insert);
		/*
		 * tables.put(getTableAliasName(insert.getTable()),
		 * insert.getTable().getName()); if (insert.getItemsList() != null) {
		 * insert.getItemsList().accept(this); } if (insert.getSelect() != null) {
		 * visit(insert.getSelect()); }
		 */
	}

	@Override
	public void visit(Update update) {
		// TODO Auto-generated method stub
		super.visit(update);
		/*
		 * for (Table table : update.getTables()) { tables.put(getTableAliasName(table),
		 * table.getName()); } if (update.getExpressions() != null) { for (Expression
		 * expression : update.getExpressions()) { expression.accept(this); } }
		 * 
		 * if (update.getFromItem() != null) { update.getFromItem().accept(this); }
		 * 
		 * if (update.getJoins() != null) { for (Join join : update.getJoins()) {
		 * join.getRightItem().accept(this); } }
		 * 
		 * if (update.getWhere() != null) { update.getWhere().accept(this); }
		 */
	}

	@Override
	public void visit(Delete delete) {
		// TODO Auto-generated method stub
		super.visit(delete);
		/*
		 * tables.put(getTableAliasName(delete.getTable()),
		 * delete.getTable().getName()); if (delete.getWhere() != null) {
		 * delete.getWhere().accept(this); }
		 */
	}

	@Override
	public void visit(Replace replace) {
		// TODO Auto-generated method stub
		super.visit(replace);
		/*
		 * tables.put(getTableAliasName(replace.getTable()),
		 * replace.getTable().getName()); if (replace.getExpressions() != null) { for
		 * (Expression expression : replace.getExpressions()) { expression.accept(this);
		 * } } if (replace.getItemsList() != null) {
		 * replace.getItemsList().accept(this); }
		 */
	}

	@Override
	public void visit(CreateTable createTable) {
		// TODO Auto-generated method stub
		super.visit(createTable);
		/*
		 * tables.put(getTableAliasName(create.getTable()),
		 * create.getTable().getName()); if (create.getSelect() != null) {
		 * create.getSelect().accept(this); }
		 */
	}

	@Override
	public void visit(Merge merge) {
		// TODO Auto-generated method stub
		super.visit(merge);
		/*
		 * tables.put(getTableAliasName(merge.getTable()), merge.getTable().getName());
		 * if (merge.getUsingTable() != null) { merge.getUsingTable().accept(this); }
		 * else if (merge.getUsingSelect() != null) {
		 * merge.getUsingSelect().accept((FromItemVisitor) this); }
		 */
	}

}
