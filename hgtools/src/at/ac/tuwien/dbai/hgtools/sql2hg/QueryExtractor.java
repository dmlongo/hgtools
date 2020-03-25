package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashMap;
import java.util.LinkedList;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import at.ac.tuwien.dbai.hgtools.util.NameStack;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.AnyType;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.PivotXml;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.ValuesList;
import net.sf.jsqlparser.statement.select.WithItem;

public class QueryExtractor extends QueryVisitorNoExpressionAdapter {

	private class ExprVisitor extends ExpressionVisitorAdapter {
		private SubqueryEdge currEdge;

		public ExprVisitor(SelectVisitor sv) {
			currEdge = new SubqueryEdge(SubqueryEdge.Operator.OTHER, false);
			setSelectVisitor(sv);
		}

		@Override
		public void visit(EqualsTo equalsTo) {
			ClauseType ct = ClauseType.determineClauseType(equalsTo);
			switch (ct) {
			case COLUMN_OP_COLUMN:
				SelectBody source = resolver.getCurrentSelect();

				Column left = (Column) equalsTo.getLeftExpression();
				String leftResolve = (left.getTable() != null) ? left.getTable().getName() : left.getColumnName();
				SelectBody leftTarget = resolver.resolve(leftResolve);
				if (!leftTarget.equals(source)) {
					SubqueryEdge se = new SubqueryEdge(SubqueryEdge.Operator.JOIN, false);
					query.addEdge(source, leftTarget, se);
				}

				Column right = (Column) equalsTo.getRightExpression();
				String rightResolve = (right.getTable() != null) ? right.getTable().getName() : right.getColumnName();
				SelectBody rightTarget = resolver.resolve(rightResolve);
				if (!rightTarget.equals(source)) {
					SubqueryEdge se = new SubqueryEdge(SubqueryEdge.Operator.JOIN, false);
					query.addEdge(source, rightTarget, se);
				}

				if (!leftTarget.equals(source) && !rightTarget.equals(source)) {
					throwException("WEIRD EQUALSTO\n" + equalsTo); // TODO troviamo un caso ed esaminiamolo
				}

				break;
			case COLUMN_OP_CONSTANT:
				break;
			case COLUMN_OP_SUBSELECT:
				// just go to the SubSelect
				equalsTo.getRightExpression().accept(exprVisitor);
				break;
			case OTHER:
				break;
			default:
				throw new AssertionError("Unknown clause type: " + ct);
			}
		}

		/*
		 * @Override public void visit(Column tableColumn) { SelectBody source =
		 * resolver.getCurrentSelect(); String toResolve = (tableColumn.getTable() !=
		 * null) ? tableColumn.getTable().getName() : tableColumn.getColumnName();
		 * SelectBody target = resolver.resolve(toResolve); if (!target.equals(source))
		 * { query.addEdge(source, target); } }
		 */

		@Override
		public void visit(NotExpression notExpr) {
			currEdge.setNegation(true);
			notExpr.getExpression().accept(this);
		}

		@Override
		public void visit(InExpression expr) {
			if (expr.getRightItemsList() instanceof SubSelect) {
				currEdge.setOperator(SubqueryEdge.Operator.IN);
				currEdge.setNegation(expr.isNot());
				if (expr.getLeftExpression() != null) {
					expr.getLeftExpression().accept(this);
				} else if (expr.getLeftItemsList() != null) {
					expr.getLeftItemsList().accept(this);
				}
				expr.getRightItemsList().accept(this);
			}
		}

		@Override
		public void visit(ExistsExpression expr) {
			if (expr.getRightExpression() instanceof SubSelect) {
				currEdge.setOperator(SubqueryEdge.Operator.EXISTS);
				expr.getRightExpression().accept(this);
			} else {
				throwException("EXISTS WITHOUT SUBSELECT\n" + expr); // TODO
			}
		}

		@Override
		public void visit(AnyComparisonExpression expr) {
			if (expr.getAnyType().equals(AnyType.ANY)) {
				currEdge.setOperator(SubqueryEdge.Operator.ANY);
			}
			visit(expr.getSubSelect());
		}

		@Override
		public void visit(SubSelect subSelect) {
			if (subSelect.getWithItemsList() != null) {
				for (WithItem item : subSelect.getWithItemsList()) {
					item.accept(getSelectVisitor());
				}
			}
			if (subSelect.getAlias() != null) {
				resolver.addNameToCurrentScope(subSelect.getAlias().getName());
			}
			SelectBody parent = resolver.getCurrentSelect();
			SelectBody child = subSelect.getSelectBody();
			query.addVertex(child);
			query.addEdge(parent, child, currEdge);
			currEdge = new SubqueryEdge(SubqueryEdge.Operator.OTHER, false);
			boolean inSetOpList = child instanceof SetOperationList;
			resolver.enterNewScope(child, inSetOpList);
			child.accept(getSelectVisitor());
			resolver.exitCurrentScope();

			if (subSelect.getPivot() != null) {
				subSelect.getPivot().accept(this);
			}
		}

		@Override
		public void visit(PivotXml pivot) {
			throwException(pivot);
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

	private static final String NOT_SUPPORTED_YET = "Not supported yet.";

	private static void throwException(Object obj) {
		throw new UnsupportedOperationException("Visiting: " + obj + ". " + NOT_SUPPORTED_YET);
	}

	private Schema schema;
	private NameStack resolver;
	private ExprVisitor exprVisitor;
	private SelectBody root;
	private Graph<SelectBody, SubqueryEdge> query;
	private HashMap<String, String> nameToViewMap;
	private HashMap<SelectBody, LinkedList<String>> selectToViewMap;
	private HashMap<String, QueryExtractor> viewToGraphMap;
	private int nextID;

	public static class SubqueryEdge extends DefaultEdge {
		private static final long serialVersionUID = -511975338046031776L;

		static enum Operator {
			JOIN, IN, EXISTS, ANY, VIEW, FROM_SUBSELECT, OTHER
		}

		private Operator op;
		private boolean neg;

		public SubqueryEdge() {
			super();
		}

		public SubqueryEdge(Operator op, boolean isNegated) {
			super();
			this.op = op;
			neg = isNegated;
		}

		public SubqueryEdge(SubqueryEdge e) {
			super();
			this.op = e.getOperator();
			this.neg = e.neg;
		}

		public void setOperator(Operator op) {
			this.op = op;
		}

		public void setNegation(boolean neg) {
			this.neg = neg;
		}

		public Operator getOperator() {
			return op;
		}

		public boolean isOperatorNegated() {
			return neg;
		}

		public String getLabel() {
			StringBuilder sb = new StringBuilder(200);
			sb.append(neg ? "not " : "");
			sb.append(' ');
			sb.append(op);
			return sb.toString();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(200);
			sb.append(neg ? "not " : "");
			sb.append(' ');
			sb.append(op);
			String label = sb.toString();
			return "(" + getSource() + " : " + getTarget() + " : " + label + ")";
		}
	}

	public QueryExtractor(Schema schema) {
		if (schema == null) {
			throw new NullPointerException();
		}
		this.schema = schema;
		resolver = new NameStack();
		exprVisitor = new ExprVisitor(this);
		query = new DefaultDirectedGraph<>(SubqueryEdge.class);
		nameToViewMap = new HashMap<>();
		selectToViewMap = new HashMap<>();
		viewToGraphMap = new HashMap<>();
		nextID = 0;
	}

	public QueryExtractor(Schema schema, HashMap<String, String> nameToViewMap,
			HashMap<String, QueryExtractor> viewToGraphMap) {
		this(schema);
		if (nameToViewMap == null) {
			throw new NullPointerException();
		}
		this.nameToViewMap.putAll(nameToViewMap);
		this.viewToGraphMap.putAll(viewToGraphMap);
	}

	// TODO can be called only once, otherwise reset the state
	public void run(Statement statement) {
		statement.accept(this);
	}

	public Graph<SelectBody, SubqueryEdge> getQueryStructure() {
		return query;
	}

	public SelectBody getRoot() {
		return root;
	}

	public LinkedList<String> getGlobalNames() {
		return resolver.getGlobalNames();
	}

	public HashMap<SelectBody, LinkedList<String>> getSelectToViewMap() {
		return selectToViewMap;
	}

	public HashMap<String, QueryExtractor> getViewToGraphMap() {
		return viewToGraphMap;
	}

	@Override
	public void visit(WithItem withItem) {
		String viewName = withItem.getName();
		resolver.addNameToCurrentScope(viewName);
		nameToViewMap.put(viewName, viewName);
		LinkedList<String> viewAttrs = new LinkedList<String>();

		int numWithItems = 0;
		if (withItem.getWithItemList() != null) {
			for (SelectItem item : withItem.getWithItemList()) {
				// there should be only column names here
				Column col = (Column) ((SelectExpressionItem) item).getExpression();
				String colName = col.getColumnName();
				resolver.addNameToCurrentScope(colName);
				nameToViewMap.put(colName, viewName);
				viewAttrs.add(colName);
				numWithItems++;
			}
		}
		Select body = new Select();
		body.setSelectBody(withItem.getSelectBody());
		QueryExtractor qe = new QueryExtractor(schema, nameToViewMap, viewToGraphMap);
		qe.run(body);
		for (String gName : qe.getGlobalNames()) {
			if (numWithItems <= 0) {
				resolver.addNameToCurrentScope(gName);
				nameToViewMap.put(gName, viewName);
				viewAttrs.add(gName);
			}
			numWithItems--;
		}
		PredicateDefinition viewPred = new PredicateDefinition(viewName, viewAttrs);
		schema.addPredicateDefinition(viewPred);

		viewToGraphMap.put(viewName, qe);
	}

	@Override
	public void visit(PlainSelect plainSelect) {
		FromItem fromItem = plainSelect.getFromItem();
		if (fromItem != null) {
			if (fromItem instanceof SubSelect) {
				SubSelect subSelect = (SubSelect) fromItem;
				if (subSelect.getWithItemsList() != null) {
					for (WithItem withItem : subSelect.getWithItemsList()) {
						withItem.accept(this);
					}
				}
				String aliasName = (fromItem.getAlias() != null) ? fromItem.getAlias().getName() : createViewName();
				WithItem table = new WithItem();
				table.setName(aliasName);
				table.setSelectBody(subSelect.getSelectBody());

				SelectBody curr = resolver.getCurrentSelect();
				if (selectToViewMap.get(curr) == null) {
					selectToViewMap.put(curr, new LinkedList<>());
				}
				selectToViewMap.get(curr).add(aliasName);

				table.accept(this);
			} else {
				resolver.addTableToCurrentScope((Table) fromItem);
				fromItem.accept(this);
			}
		}

		if (plainSelect.getJoins() != null) {
			for (Join join : plainSelect.getJoins()) {
				FromItem joinItem = join.getRightItem();
				if (joinItem instanceof SubSelect) {
					SubSelect subSelect = (SubSelect) joinItem;
					if (subSelect.getWithItemsList() != null) {
						for (WithItem withItem : subSelect.getWithItemsList()) {
							withItem.accept(this);
						}
					}
					String aliasName = (joinItem.getAlias() != null) ? joinItem.getAlias().getName() : createViewName();
					WithItem table = new WithItem();
					table.setName(aliasName);
					table.setSelectBody(subSelect.getSelectBody());

					SelectBody curr = resolver.getCurrentSelect();
					if (selectToViewMap.get(curr) == null) {
						selectToViewMap.put(curr, new LinkedList<>());
					}
					selectToViewMap.get(curr).add(aliasName);

					table.accept(this);
				} else {
					resolver.addTableToCurrentScope((Table) joinItem);
					joinItem.accept(this);
				}
				if (join.getOnExpression() != null) {
					join.getOnExpression().accept(exprVisitor);
				}
			}
		}

		if (plainSelect.getSelectItems() != null) {
			for (SelectItem item : plainSelect.getSelectItems()) {
				item.accept(this);
			}
		}

		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(exprVisitor);
		}

		if (plainSelect.getOracleHierarchical() != null) {
			plainSelect.getOracleHierarchical().accept(exprVisitor);
		}

		// TODO maybe analyze groupBy, having, orderBy
	}

	private String createViewName() {
		return "anonymousView" + nextID++;
	}

	@Override
	public void visit(SetOperationList setOpList) {
		// no matter what the operator is - I just want the subqueries
		for (SelectBody child : setOpList.getSelects()) {
			SelectBody parent = resolver.getCurrentSelect();
			query.addVertex(child);
			query.addEdge(parent, child, new SubqueryEdge(SubqueryEdge.Operator.OTHER, false));
			resolver.enterNewScope(child, true);
			child.accept(this);
			resolver.exitCurrentScope();
		}
	}

	// SelectItemVisitor

	@Override
	public void visit(AllColumns allColumns) {
		for (FromItem item : resolver.getCurrentTables()) {
			PredicateDefinition p = new PredicateFinder(schema).getPredicate(item);
			resolver.addNamesToParentScope(p.getAttributes());
			if (resolver.isTopLevel()) {
				resolver.addNamesToGlobalScope(p.getAttributes());
			}
		}
	}

	@Override
	public void visit(AllTableColumns allTableColumns) {
		String table = allTableColumns.getTable().getName();
		PredicateDefinition pred = schema.getPredicateDefinition(table);
		resolver.addNamesToParentScope(pred.getAttributes());
		if (resolver.isTopLevel()) {
			resolver.addNamesToGlobalScope(pred.getAttributes());
		}
	}

	@Override
	public void visit(SelectExpressionItem item) {
		if (item.getAlias() != null) {
			resolver.addNameToParentScope(item.getAlias().getName());
			if (resolver.isTopLevel()) {
				resolver.addGlobalName(item.getAlias().getName());
			}
		} else if (item.getExpression() instanceof Column) {
			// TODO do I enter here if I have an alias but I'm also a Column?
			Column col = (Column) item.getExpression();
			resolver.addNameToParentScope(col.getColumnName());
			if (resolver.isTopLevel()) {
				resolver.addGlobalName(col.getColumnName());
			}
		} else if (item.getExpression() instanceof SubSelect) {
			SubSelect exprItem = (SubSelect) item.getExpression();
			if (exprItem.getWithItemsList() != null) {
				for (WithItem withItem : exprItem.getWithItemsList()) {
					withItem.accept(this);
				}
			}
			String aliasName = createViewName();
			WithItem table = new WithItem();
			table.setName(aliasName);
			table.setSelectBody(exprItem.getSelectBody());

			SelectBody curr = resolver.getCurrentSelect();
			if (selectToViewMap.get(curr) == null) {
				selectToViewMap.put(curr, new LinkedList<>());
			}
			selectToViewMap.get(curr).add(aliasName);

			table.accept(this);
		} else {
			// item.getExpression().accept(exprVisitor);
		}
		// things defined in the select cannot be used in the where, right?
		// item.getExpression().accept(exprVisitor);
		// The assumption is that I only need aliases, because the other attributes
		// will be added in the FROM
		/*
		 * else { // can really attributes in functions be referenced in the body?
		 * Set<String> names = extractNames(item.getExpression());
		 * resolver.addNamesToCurrentScope(names); }
		 */
	}

	/*
	 * private Set<String> extractNames(Expression expression) { ColumnFinder cf =
	 * new ColumnFinder(); Set<String> names = new HashSet<>(); for (Column col :
	 * cf.getColumns(expression)) { names.add(col.getColumnName()); } return names;
	 * }
	 */

	// FromItemVisitor
	// every FromItem can have an Alias

	@Override
	public void visit(LateralSubSelect lateralSubSelect) {
		throwException(lateralSubSelect);
	}

	@Override
	public void visit(ParenthesisFromItem aThis) {
		if (aThis.getAlias() != null) {
			resolver.addNameToCurrentScope(aThis.getAlias().getName());
		}
		aThis.getFromItem().accept(this);
	}

	@Override
	public void visit(SubJoin subjoin) {
		if (subjoin.getAlias() != null) {
			resolver.addNameToCurrentScope(subjoin.getAlias().getName());
		}
		subjoin.getLeft().accept(this);
		for (Join join : subjoin.getJoinList()) {
			join.getRightItem().accept(this);
			if (join.getOnExpression() != null) {
				join.getOnExpression().accept(exprVisitor);
			}
		}
	}

	@Override
	public void visit(SubSelect subSelect) {
		// I can come here only from a FROM, right?
		// For the SELECT I go through SelectExpressionItem
		throwException("SUBSELECT STRANA\n" + subSelect); // TODO

		if (subSelect.getWithItemsList() != null) {
			for (WithItem withItem : subSelect.getWithItemsList()) {
				withItem.accept(this);
			}
		}
		if (subSelect.getAlias() != null) {
			resolver.addNameToCurrentScope(subSelect.getAlias().getName());
		}
		SelectBody parent = resolver.getCurrentSelect();
		SelectBody child = subSelect.getSelectBody();
		query.addVertex(child);
		query.addEdge(parent, child); // TODO qui non creo l'arco. Why?
		boolean inSetOpList = child instanceof SetOperationList;
		resolver.enterNewScope(child, inSetOpList);
		child.accept(this);
		resolver.exitCurrentScope();
	}

	@Override
	public void visit(Table tableName) {
		String tableAlias = getTableAliasName(tableName);
		if (nameToViewMap.containsKey(tableAlias)) {
			SelectBody curr = resolver.getCurrentSelect();
			if (selectToViewMap.get(curr) == null) {
				selectToViewMap.put(curr, new LinkedList<>());
			}
			selectToViewMap.get(curr).add(tableAlias);
		}
		resolver.addNameToCurrentScope(tableAlias);
		PredicateDefinition pred = schema.getPredicateDefinition(tableName.getName());
		resolver.addNamesToCurrentScope(pred.getAttributes());
	}

	@Override
	public void visit(TableFunction tableFunction) {
		throwException(tableFunction);
	}

	@Override
	public void visit(ValuesList valuesList) {
		throwException(valuesList);
	}

	// StatementVisitor

	@Override
	public void visit(Select select) {
		if (select.getWithItemsList() != null) {
			for (WithItem withItem : select.getWithItemsList()) {
				withItem.accept(this);
			}
		}

		SelectBody body = select.getSelectBody();
		root = body;
		query.addVertex(body);
		boolean inSetOpList = body instanceof SetOperationList;
		resolver.enterNewScope(body, inSetOpList);
		body.accept(this);
		resolver.exitCurrentScope();
	}

}
