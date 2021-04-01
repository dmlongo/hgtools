package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor.SubqueryEdge;
import at.ac.tuwien.dbai.hgtools.util.GraphUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;

public class QuerySimplifier extends QueryVisitorNoExpressionAdapter {

	private static String makeName(String prefix, String name) {
		return prefix.equals("") ? name : prefix + "_" + name;
	}

	private Schema schema;
	private Graph<SelectBody, SubqueryEdge> graph;
	private QueryExtractor qExtr;
	private LinkedList<Query> queries;

	private ExprVisitor exprVisitor;
	private PlainSelect tempBody;
	private String tempPrefix;
	private List<String> tmpSelectItems;

	private HashMap<String, String> viewNamesMap;
	private HashSet<String> ambiguousNames;

	public QuerySimplifier(Schema schema, Graph<SelectBody, SubqueryEdge> depGraph, QueryExtractor qExtr) {
		if (schema == null || depGraph == null || qExtr == null) {
			throw new NullPointerException();
		}
		this.schema = schema;
		this.graph = depGraph;
		this.qExtr = qExtr;
		this.queries = new LinkedList<>();
		this.exprVisitor = new ExprVisitor();
		this.tempBody = null;
		this.tempPrefix = "";
		this.tmpSelectItems = null;

		this.viewNamesMap = new HashMap<>();
		this.ambiguousNames = new HashSet<>();
	}

	public List<Query> getSimpleQueries() {
		run();
		return queries;
	}

	private void run() {
		collectViewNames(qExtr);
		HashMap<String, WithItem> views = collectAllViews(qExtr);
		Set<SelectBody> selects = GraphUtils.findTerminalNodes(graph); // independent selects
		for (SelectBody body : selects) {
			Select stmt = simplify(body);
			PlainSelect newBody = (PlainSelect) stmt.getSelectBody();
			HashSet<WithItem> withItems = findViewsOf(newBody, views);
			LinkedList<WithItem> withItemsList = sort(withItems);
			stmt.setWithItemsList(withItemsList);
			queries.add(new Query(stmt));
		}
	}

	private LinkedList<WithItem> sort(HashSet<WithItem> withItems) {
		HashMap<String, WithItem> views = makeMap(withItems);
		Graph<String, DefaultEdge> deps = makeDependencyGraph(views);
		List<Graph<String, DefaultEdge>> comps = GraphUtils.computeConnComps(deps);
		List<Graph<String, DefaultEdge>> sortedComps = GraphUtils.sortComponents(comps);
		LinkedList<WithItem> res = new LinkedList<>();
		for (Graph<String, DefaultEdge> g : sortedComps) {
			List<String> dfs = GraphUtils.reverseBFSAlphabeticalSort(g);
			for (String vName : dfs) {
				res.add(views.get(vName));
			}
		}
		return res;
	}

	private HashMap<String, WithItem> makeMap(HashSet<WithItem> withItems) {
		HashMap<String, WithItem> res = new HashMap<>();
		for (WithItem v : withItems) {
			res.put(v.getName(), v);
		}
		return res;
	}

	private Graph<String, DefaultEdge> makeDependencyGraph(HashMap<String, WithItem> views) {
		Graph<String, DefaultEdge> res = new DefaultDirectedGraph<>(DefaultEdge.class);
		for (WithItem v : views.values()) {
			res.addVertex(v.getName());
		}
		for (WithItem v : views.values()) {
			String src = v.getName();
			LinkedList<String> children = dependenciesOf(v, views);
			for (String dest : children) {
				res.addEdge(src, dest);
			}
		}
		return res;
	}

	private LinkedList<String> dependenciesOf(WithItem v, HashMap<String, WithItem> views) {
		// views are already simplified: there are only tables in the from
		LinkedList<String> res = new LinkedList<>();
		PlainSelect ps = (PlainSelect) v.getSelectBody();
		if (ps.getFromItem() != null) {
			String t = ((Table) ps.getFromItem()).getName();
			if (views.containsKey(t)) {
				res.add(t);
			}
		}
		if (ps.getJoins() != null) {
			for (Join j : ps.getJoins()) {
				String t = ((Table) j.getRightItem()).getName();
				if (views.containsKey(t)) {
					res.add(t);
				}
			}
		}
		return res;
	}

	private void collectViewNames(QueryExtractor qExtr) {
		viewNamesMap.clear();
		ambiguousNames.clear();

		LinkedList<QueryExtractor> toVisit = new LinkedList<>();
		LinkedList<String> prefixes = new LinkedList<>();
		HashSet<QueryExtractor> visited = new HashSet<>();
		toVisit.addLast(qExtr);
		prefixes.addLast("");
		while (!toVisit.isEmpty()) {
			QueryExtractor qe = toVisit.removeFirst();
			String prefix = prefixes.removeFirst();
			if (!visited.contains(qe)) {
				for (String viewName : qe.getViewsDefinedHere()) {
					String newViewName = makeName(prefix, viewName);
					if (viewNamesMap.containsKey(viewName)) {
						ambiguousNames.add(viewName);
					} else {
						viewNamesMap.put(viewName, newViewName);
					}

					QueryExtractor vExtr = qe.getViewToGraphMap().get(viewName);
					toVisit.addLast(vExtr);
					prefixes.addLast(newViewName);
				}
				visited.add(qe);
			}
		}
	}

	private HashMap<String, WithItem> collectAllViews(QueryExtractor qExtr) {
		HashMap<String, WithItem> views = new HashMap<>();
		LinkedList<QueryExtractor> toVisit = new LinkedList<>();
		LinkedList<String> prefixes = new LinkedList<>();
		HashSet<QueryExtractor> visited = new HashSet<>();
		toVisit.addLast(qExtr);
		prefixes.addLast("");
		while (!toVisit.isEmpty()) {
			QueryExtractor qe = toVisit.removeFirst();
			String prefix = prefixes.removeFirst();
			if (!visited.contains(qe)) {
				for (String viewName : qe.getViewsDefinedHere()) {
					QueryExtractor vExtr = qe.getViewToGraphMap().get(viewName);
					SelectBody viewBody = vExtr.getRoot();
					String newViewName = makeName(prefix, viewName);
					WithItem view = simplify(viewBody, newViewName);
					if (!vExtr.getViewSelectItems().isEmpty()) {
						view.setWithItemList(vExtr.getViewSelectItems());
					}
					views.put(newViewName, view);

					toVisit.addLast(vExtr);
					prefixes.addLast(newViewName);
				}
				visited.add(qe);
			}
		}
		return views;
	}

	private HashSet<WithItem> findViewsOf(PlainSelect sel, HashMap<String, WithItem> views) {
		HashSet<WithItem> res = new HashSet<>();
		if (sel.getFromItem() != null) {
			Table tab = (Table) sel.getFromItem();
			String name = tab.getName();
			if (views.containsKey(name)) {
				WithItem view = views.get(name);
				res.add(view);
				res.addAll(findViewsOf((PlainSelect) view.getSelectBody(), views));
			}
		}
		if (sel.getJoins() != null) {
			for (Join j : sel.getJoins()) {
				String name = ((Table) j.getRightItem()).getName();
				if (views.containsKey(name)) {
					WithItem view = views.get(name);
					res.add(view);
					res.addAll(findViewsOf((PlainSelect) view.getSelectBody(), views));
				}
			}
		}
		return res;
	}

	private WithItem simplify(SelectBody viewBody, String viewName) {
		exprVisitor.reset();
		WithItem view = new WithItem();
		view.setName(viewName);
		tempPrefix = viewName;
		tempBody = new PlainSelect();
		viewBody.accept(this);
		if (tempBody.getSelectItems() == null) {
			if (tmpSelectItems != null && !tmpSelectItems.isEmpty()) {
				for (String s : tmpSelectItems) {
					Column col = new Column(s);
					tempBody.addSelectItems(new SelectExpressionItem(col));
				}
			} else {
				StringValue sv = new StringValue("ciao");
				SelectExpressionItem item = new SelectExpressionItem(sv);
				tempBody.addSelectItems(item);
			}
		}
		view.setSelectBody(tempBody);
		tmpSelectItems = null;
		tempBody = null;
		tempPrefix = "";
		return view;
	}

	private Select simplify(SelectBody body) {
		exprVisitor.reset();
		Select sel = new Select();
		tempPrefix = qExtr.getPrefixes().get(body);
		if (tempPrefix == null) {
			throw new NullPointerException();
		}
		tempBody = new PlainSelect();
		body.accept(this);
		if (tempBody.getSelectItems() == null) {
			if (tmpSelectItems != null && !tmpSelectItems.isEmpty()) {
				for (String s : tmpSelectItems) {
					Column col = new Column(s);
					tempBody.addSelectItems(new SelectExpressionItem(col));
				}
			} else {
				StringValue sv = new StringValue("ciao");
				SelectExpressionItem item = new SelectExpressionItem(sv);
				tempBody.addSelectItems(item);
			}
		}
		sel.setSelectBody(tempBody);
		tmpSelectItems = null;
		tempBody = null;
		tempPrefix = "";
		return sel;
	}

	// SelectBody : PlainSelect, SetOperationList, ValuesStatement, WithItem

	@Override
	public void visit(PlainSelect plainSelect) {
		if (plainSelect.getFromItem() != null) {
			plainSelect.getFromItem().accept(this);
		}

		if (plainSelect.getJoins() != null) {
			LinkedList<Join> joins = new LinkedList<>();
			for (Join join : plainSelect.getJoins()) {
				Join newJ = dealWithJoin(join);
				joins.add(newJ);
			}
			tempBody.setJoins(joins);
		}

		if (plainSelect.getSelectItems() != null) {
			for (SelectItem item : plainSelect.getSelectItems()) {
				item.accept(this);
			}
		}

		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(exprVisitor);
			tempBody.setWhere(exprVisitor.getExpression());
		}
	}

	private Join dealWithJoin(Join join) {
		Join newJ = new Join();
		setJoinMod(join, newJ);
		FromItem joinItem = join.getRightItem();
		if (joinItem instanceof Table) {
			Table newTable = updateTable((Table) joinItem);
			newJ.setRightItem(newTable);
		} else if (joinItem instanceof SubSelect) {
			String oldName = joinItem.getAlias().getName();
			String newName = makeName(tempPrefix, oldName);
			newJ.setRightItem(new Table(newName));
		} else if (joinItem instanceof ParenthesisFromItem) {
			newJ.setRightItem(makeFromItem((ParenthesisFromItem) joinItem));
		}
		if (join.getOnExpression() != null) {
			ExprVisitor ev = new ExprVisitor();
			join.getOnExpression().accept(ev);
			newJ.setOnExpression(ev.getExpression());
		}
		return newJ;
	}

	private void setJoinMod(Join join, Join newJ) {
		newJ.setCross(join.isCross());
		newJ.setFull(join.isFull());
		newJ.setInner(join.isInner());
		newJ.setLeft(join.isLeft());
		newJ.setNatural(join.isNatural());
		newJ.setOuter(join.isOuter());
		newJ.setRight(join.isRight());
		newJ.setSemi(join.isSemi());
		newJ.setSimple(join.isSimple());
		newJ.setStraight(join.isStraight());
		if (join.isWindowJoin()) {
			newJ.setJoinWindow(join.getJoinWindow());
		}
	}

	@Override
	public void visit(SetOperationList setOpList) {
		tmpSelectItems = new PredicateFinder(schema).getPredicate(setOpList).getAttributes();
	}

	// FromItem

	@Override
	public void visit(Table tableName) {
		Table newTable = updateTable(tableName);
		tempBody.setFromItem(newTable);
	}

	@Override
	public void visit(SubSelect subSelect) {
		String oldName = subSelect.getAlias().getName();
		String newName = makeName(tempPrefix, oldName);
		tempBody.setFromItem(new Table(newName));
	}

	@Override
	public void visit(ParenthesisFromItem aThis) {
		tempBody.setFromItem(makeFromItem(aThis));
	}

	private ParenthesisFromItem makeFromItem(ParenthesisFromItem par) {
		FromItem fromItem = par.getFromItem();
		if (fromItem instanceof Table) {
			Table newTable = updateTable((Table) fromItem);
			ParenthesisFromItem newPar = new ParenthesisFromItem(newTable);
			newPar.setAlias(par.getAlias());
			return newPar;
		} else if (fromItem instanceof SubSelect) {
			String oldName = ((SubSelect) fromItem).getAlias().getName();
			String newName = makeName(tempPrefix, oldName);
			ParenthesisFromItem newPar = new ParenthesisFromItem(new Table(newName));
			newPar.setAlias(par.getAlias());
			return newPar;
		} else if (fromItem instanceof ParenthesisFromItem) {
			ParenthesisFromItem insidePar = makeFromItem((ParenthesisFromItem) fromItem);
			if (insidePar != null) {
				ParenthesisFromItem newPar = new ParenthesisFromItem(insidePar);
				newPar.setAlias(par.getAlias());
				return newPar;
			} else {
				return null;
			}
		}
		return null;
	}

	private Table updateTable(Table table) {
		String oldName = table.getName();
		String newName = oldName;
		if (oldName != null && viewNamesMap.keySet().contains(oldName)) {
			if (ambiguousNames.contains(oldName)) {
				newName = makeName(tempPrefix, oldName);
			} else {
				newName = viewNamesMap.get(oldName);
			}
		}

		Table newTable = new Table(newName);
		newTable.setAlias(table.getAlias());
		return newTable;
	}

	// SelectItem : AllColumns, AllTableColumns, SelectExpressionItem

	@Override
	public void visit(AllColumns allColumns) {
		tempBody.addSelectItems(allColumns);
	}

	@Override
	public void visit(AllTableColumns allTableColumns) {
		// TODO table is alias named as view = problem
		Table newTable = updateTable(allTableColumns.getTable());
		tempBody.addSelectItems(new AllTableColumns(newTable));
	}

	@Override
	public void visit(SelectExpressionItem selectExpressionItem) {
		Expression expr = selectExpressionItem.getExpression();
		ColumnFinder cf = new ColumnFinder();
		for (Column col : cf.getColumns(expr)) {
			if (col.getTable() != null) {
				col.setTable(updateTable(col.getTable()));
			}
		}
		tempBody.addSelectItems(selectExpressionItem);
	}

	// ExprVisitor

	private class ExprVisitor extends ExpressionVisitorAdapterFixed {
		private ArrayList<EqualsTo> eqs = new ArrayList<>();

		public Expression getExpression() {
			// assuming everything is in an AND
			return andify(eqs, 0);
		}

		public void reset() {
			eqs.clear();
		}

		private Expression andify(ArrayList<EqualsTo> eqs, int i) {
			if (eqs.size() - i == 0) {
				return null;
			} else if (eqs.size() - i == 1) {
				return eqs.get(i);
			} else if (eqs.size() - i == 2) {
				return new AndExpression(eqs.get(i), eqs.get(i + 1));
			}
			return new AndExpression(eqs.get(i), andify(eqs, i + 1));
		}

		@Override
		public void visit(EqualsTo expr) {
			ClauseType ct = ClauseType.determineClauseType(expr);
			if (ct.equals(ClauseType.COLUMN_OP_COLUMN)) {
				Column left = (Column) expr.getLeftExpression();
				Column right = (Column) expr.getRightExpression();

				EqualsTo newExpr = new EqualsTo();
				newExpr.setLeftExpression(updateColumn(left));
				newExpr.setRightExpression(updateColumn(right));
				eqs.add(newExpr);
			}
		}

		private Column updateColumn(Column col) {
			Table newTable = null;
			if (col.getTable() != null) {
				newTable = updateTable(col.getTable());
			}

			return new Column(newTable, col.getColumnName());
		}

		@Override
		public void visit(OrExpression expr) {
			// ignore OR for the moment
		}
	}

}
