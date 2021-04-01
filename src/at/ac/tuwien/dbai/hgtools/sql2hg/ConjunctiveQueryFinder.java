package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.WithItem;

public class ConjunctiveQueryFinder extends QueryVisitorUnsupportedAdapter {

	private enum ParsingState {
		WAITING, READING_VIEW, READING_VIEW_SETOPLIST, IN_SELECT, FINISHED
	}

	private ParsingState currentState;

	private Schema schema;
	private NameResolver nResolver;

	private static class ViewInfo {
		public ViewInfo(String name) {
			this.name = name;
			attr = new LinkedList<>();
		}

		String name;
		LinkedList<String> attr;
		PredicateDefinition def;
		ViewPredicate pred;
	}

	private ViewInfo currentViewInfo;

	private ArrayList<SelectExpressionItem> selExprItBuffer;
	private ArrayList<Alias> aliasesBuffer;
	private HashMap<String, Column> viewAttrToCol;

	private HashSet<Predicate> tables;
	private HashSet<Equality> joins;

	private PlainSelect tmpPS;

	public ConjunctiveQueryFinder(Schema schema) {
		if (schema == null) {
			throw new NullPointerException();
		}
		this.schema = schema;
		nResolver = new NameResolver();

		currentViewInfo = null;
		selExprItBuffer = new ArrayList<>(17);
		aliasesBuffer = new ArrayList<>(17);
		viewAttrToCol = new HashMap<>();

		tables = new HashSet<>();
		joins = new HashSet<>();

		tmpPS = null;

		currentState = ParsingState.WAITING;
	}

	public void run(Statement statement) {
		statement.accept(this);
	}

	public Set<Predicate> getTables() {
		return tables;
	}

	public Set<Equality> getJoins() {
		return joins;
	}

	// SelectVisitor

	@Override
	public void visit(WithItem withItem) {
		currentViewInfo = new ViewInfo(withItem.getName());
		if (withItem.getWithItemList() != null) {
			for (SelectItem item : withItem.getWithItemList()) {
				aliasesBuffer.add(new Alias(item.toString()));
			}
		}
		withItem.getSelectBody().accept(this);
		if (currentViewInfo.pred != null) {
			schema.addPredicateDefinition(currentViewInfo.def, currentViewInfo.pred);
		}
		currentViewInfo = null;
	}

	@Override
	public void visit(PlainSelect plainSelect) {
		nResolver.enterNewScope();
		tmpPS = plainSelect;

		if (plainSelect.getFromItem() == null) {
			currentState = ParsingState.READING_VIEW_SETOPLIST;
		}

		if (plainSelect.getSelectItems() != null) {
			for (SelectItem item : plainSelect.getSelectItems()) {
				// TODO if view update columns
				item.accept(this);
			}
		}

		if (currentState == ParsingState.READING_VIEW) {
			for (int i = 0; i < aliasesBuffer.size(); i++) {
				SelectExpressionItem item = selExprItBuffer.get(i);
				Alias alias = aliasesBuffer.get(i);
				item.setAlias(alias);
			}
			aliasesBuffer.clear();
			for (SelectExpressionItem item : selExprItBuffer) {
				String viewAttr = item.getAlias() != null ? item.getAlias().getName() : null;
				Column col = extractColumn(item.getExpression());

				if (viewAttr == null && col != null) {
					viewAttr = col.getColumnName();
				}

				if (viewAttr != null || col != null) {
					currentViewInfo.attr.add(viewAttr);
					if (plainSelect.getFromItem() != null) {
						viewAttrToCol.put(viewAttr, col);
					}
				}
			}
			selExprItBuffer.clear();

			currentViewInfo.def = new PredicateDefinition(currentViewInfo.name, currentViewInfo.attr);
			schema.addPredicateDefinition(currentViewInfo.def);
			currentViewInfo.pred = new ViewPredicate(currentViewInfo.def);
		} else if (currentState == ParsingState.READING_VIEW_SETOPLIST) {
			for (SelectExpressionItem item : selExprItBuffer) {
				Column col = (Column) item.getExpression();
				currentViewInfo.attr.add(col.getColumnName());
			}
			selExprItBuffer.clear();

			currentViewInfo.def = new PredicateDefinition(currentViewInfo.name, currentViewInfo.attr);
			schema.addPredicateDefinition(currentViewInfo.def);
		}

		if (plainSelect.getFromItem() != null) {
			plainSelect.getFromItem().accept(this);
		}

		if (plainSelect.getJoins() != null) {
			for (Join join : plainSelect.getJoins()) {
				join.getRightItem().accept(this);
				if (join.isNatural()) {
					findEqualities(join);
				} else if (join.getOnExpression() != null) {
					join.getOnExpression().accept(this);
				}
			}
		}

		if (currentState == ParsingState.READING_VIEW) {
			for (Map.Entry<String, Column> entry : viewAttrToCol.entrySet()) {
				String viewAttr = entry.getKey();
				Column col = entry.getValue();
				if (col != null) {
					String defPred = nResolver.resolveColumn(col).getAlias();
					String defAttr = col.getColumnName();
					currentViewInfo.pred.defineAttribute(viewAttr, defPred, defAttr);
				}
			}
			viewAttrToCol.clear();
		}

		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(this);
		}

		// TODO at this point I can add equalities

		if (plainSelect.getOracleHierarchical() != null) {
			plainSelect.getOracleHierarchical().accept(this);
		}

		tmpPS = null;
		nResolver.exitCurrentScope();
	}

	private void findEqualities(Join join) {
		Table joinTable = (Table) join.getRightItem();
		Predicate joinPredicate = nResolver.resolveTableName(Util.getTableAliasName(joinTable));
		for (Predicate p : nResolver.getPredicatesInCurrentScope()) {
			for (Equality eq : findCommonColumns(joinPredicate, p)) {
				joins.add(eq);
			}
		}
	}

	private ArrayList<Equality> findCommonColumns(Predicate p1, Predicate p2) {
		ArrayList<Equality> eqs = new ArrayList<>(p1.arity());
		for (String attr : p1) {
			if (p2.existsAttribute(attr)) {
				Equality eq = new Equality(p1, attr, p2, attr);
				eqs.add(eq);
			}
		}
		return eqs;
	}

	private Column extractColumn(Expression expression) {
		ColumnFinder cf = new ColumnFinder();
		Set<Column> cols = cf.getColumns(expression);
		if (cols.iterator().hasNext()) {
			return cols.iterator().next();
		} else {
			return null;
		}
	}

	// SelectItemVisitor

	@Override
	public void visit(AllColumns allColumns) {
		PredicateDefinition pd = new PredicateFinder(schema).getPredicate(tmpPS);
		for (String attr : pd.getAttributes()) {
			SelectExpressionItem item = new SelectExpressionItem();
			item.setExpression(new Column(attr));
			selExprItBuffer.add(item);
		}
	}

	@Override
	public void visit(AllTableColumns allTableColumns) {
		String table = allTableColumns.getTable().getName();
		PredicateDefinition pd = schema.getPredicateDefinition(table);
		for (String attr : pd.getAttributes()) {
			SelectExpressionItem item = new SelectExpressionItem();
			item.setExpression(new Column(attr));
			selExprItBuffer.add(item);
		}
	}

	@Override
	public void visit(SelectExpressionItem item) {
		selExprItBuffer.add(item);
	}

	// FromItemVisitor

	@Override
	public void visit(Table tableName) {
		// TODO what if I'm not coming from a FROM clause?
		String tableWholeName = tableName.getFullyQualifiedName();
		String tableAliasName = Util.getTableAliasName(tableName);
		// PredicateDefinition pred = schema.getPredicateDefinition(tableWholeName);
		Predicate table = schema.newPredicate(tableWholeName); // TODO problematic line
		table.setAlias(tableAliasName);
		// TODO column aliases must be dealt with here
		nResolver.addTableToCurrentScope(table);
		switch (currentState) {
		case READING_VIEW:
			currentViewInfo.pred.addDefiningPredicate(table);
			break;
		case IN_SELECT:
			tables.add(table);
			break;
		default:
			throw new AssertionError("The current state cannot be: " + currentState);
		}
	}

	// ExpressionVisitor - assuming only AND and equalities in here

	@Override
	public void visit(AndExpression expr) {
		visitBinaryExpression(expr);
	}

	private void visitBinaryExpression(BinaryExpression expr) {
		expr.getLeftExpression().accept(this);
		expr.getRightExpression().accept(this);
	}

	@Override
	public void visit(EqualsTo equalsTo) {
		ClauseType ct = ClauseType.determineClauseType(equalsTo);
		switch (ct) {
		case COLUMN_OP_COLUMN:
			Column left = (Column) equalsTo.getLeftExpression();
			Column right = (Column) equalsTo.getRightExpression();
			Predicate pred1 = nResolver.resolveColumn(left);
			Predicate pred2 = nResolver.resolveColumn(right);
			String leftColumn = left.getColumnName();
			String rightColumn = right.getColumnName();
			switch (currentState) {
			case READING_VIEW:
				currentViewInfo.pred.addJoin(pred1.getAlias(), leftColumn, pred2.getAlias(), rightColumn);
				break;
			case IN_SELECT:
				Equality eq = new Equality(pred1, leftColumn, pred2, rightColumn);
				joins.add(eq);
				break;
			default:
				throw new AssertionError("The current state cannot be: " + currentState);
			}
			// TODO it mustn't be done if I'm building a view
			break;
		case COLUMN_OP_CONSTANT:
			break;
		case COLUMN_OP_SUBSELECT:
			break;
		case OTHER:
			super.visit(equalsTo);
			break;
		default:
			throw new AssertionError("Unknown clause type: " + ct);
		}
	}

	// StatementVisitor

	@Override
	public void visit(Select select) {
		if (select.getWithItemsList() != null) {
			for (WithItem withItem : select.getWithItemsList()) {
				currentState = ParsingState.READING_VIEW;
				withItem.accept(this);
			}
			currentState = ParsingState.WAITING;
		}
		currentState = ParsingState.IN_SELECT;
		select.getSelectBody().accept(this);
		currentState = ParsingState.FINISHED;
	}

}
