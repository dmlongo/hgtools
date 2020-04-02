package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.Graph;

import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor.SubqueryEdge;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;

public class QuerySimplifier extends QueryVisitorNoExpressionAdapter {

	private Graph<SelectBody, SubqueryEdge> graph;
	private QueryExtractor qExtr;
	private LinkedList<Select> queries;

	private ExprVisitor exprVisitor;
	private PlainSelect tempBody;

	public QuerySimplifier(Graph<SelectBody, SubqueryEdge> depGraph, QueryExtractor qExtr) {
		if (depGraph == null || qExtr == null) {
			throw new NullPointerException();
		}
		this.graph = depGraph;
		this.qExtr = qExtr;
		this.queries = new LinkedList<>();
		this.exprVisitor = new ExprVisitor();
		this.tempBody = null;
	}

	public List<Select> getSimpleQueries() {
		run();
		return queries;
	}

	private void run() {
		HashMap<String, WithItem> views = new HashMap<>();
		for (String viewName : qExtr.getViewToGraphMap().keySet()) {
			SelectBody viewBody = qExtr.getViewToGraphMap().get(viewName).getRoot();
			WithItem view = simplify(viewBody, viewName);
			views.put(viewName, view);
		} // TODO fix this, look into vExtr

		HashSet<SelectBody> selects = findIndependentSelects(graph);
		for (SelectBody body : selects) {
			Select stmt = simplify(body);
			PlainSelect newBody = (PlainSelect) stmt.getSelectBody();
			LinkedList<WithItem> withItemsList = new LinkedList<>(findViewsOf(newBody, views));
			stmt.setWithItemsList(withItemsList);
			queries.add(stmt);
		}
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

	private LinkedList<String> findViewsOf(SelectBody sel, QueryExtractor qExtr) {
		if (qExtr.getSelectToViewMap().containsKey(sel)) {
			LinkedList<String> res = expandViewsList(qExtr.getSelectToViewMap().get(sel), qExtr, new HashSet<>());
			return res;
		}

		// order-dependent - same select in different branches = problem
		for (QueryExtractor vExtr : qExtr.getViewToGraphMap().values()) {
			LinkedList<String> res = findViewsOf(sel, vExtr);
			if (res != null) {
				return res;
			}
		}
		return new LinkedList<String>();
	}

	private LinkedList<String> expandViewsList(LinkedList<String> toExpand, QueryExtractor qExtr,
			HashSet<String> visited) {
		HashSet<String> resSet = new HashSet<>(toExpand);

		LinkedList<String> toVisit = new LinkedList<>(resSet);
		while (!toVisit.isEmpty()) {
			String view = toVisit.removeFirst();
			if (!visited.contains(view)) {
				visited.add(view);

				QueryExtractor vExtr = qExtr.getViewToGraphMap().get(view);
				SelectBody vBody = vExtr.getRoot();
				LinkedList<String> newRes = findViewsOf(vBody, vExtr);

				resSet.addAll(newRes);
				toVisit.addAll(newRes);
			}
		}
		return new LinkedList<String>(resSet);
	}

	private HashSet<SelectBody> findIndependentSelects(Graph<SelectBody, SubqueryEdge> g) {
		HashSet<SelectBody> res = new HashSet<>();
		for (SelectBody s : g.vertexSet()) {
			if (g.outDegreeOf(s) == 0) {
				res.add(s);
			}
		}
		return res;
	}

	private WithItem simplify(SelectBody viewBody, String viewName) {
		exprVisitor.reset();
		WithItem view = new WithItem();
		view.setName(viewName);
		tempBody = new PlainSelect();
		viewBody.accept(this);
		view.setSelectBody(tempBody);
		tempBody = null;
		return view;
	}

	private Select simplify(SelectBody body) {
		exprVisitor.reset();
		Select sel = new Select();
		tempBody = new PlainSelect();
		body.accept(this);
		sel.setSelectBody(tempBody);
		tempBody = null;
		return sel;
	}

	// SelectBody : PlainSelect, SetOperationList, ValuesStatement, WithItem

	@Override
	public void visit(PlainSelect plainSelect) {
		FromItem fromItem = plainSelect.getFromItem();
		if (fromItem != null) {
			if (!(fromItem instanceof SubSelect)) {
				tempBody.setFromItem(fromItem);
			} else {
				String name = fromItem.getAlias().getName();
				tempBody.setFromItem(new Table(name));
			}
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
				if (!(item instanceof SelectExpressionItem)) {
					tempBody.addSelectItems(item);
				} else {
					item.accept(this);
				}
			}
		}

		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(exprVisitor);
			tempBody.setWhere(exprVisitor.getExpression());
		}

		// if (plainSelect.getOracleHierarchical() != null) {
		// plainSelect.getOracleHierarchical().accept(exprVisitor);
		// }
	}

	private Join dealWithJoin(Join join) {
		Join newJ = new Join();
		setJoinMod(join, newJ);
		FromItem joinItem = join.getRightItem();
		if (!(joinItem instanceof SubSelect)) {
			newJ.setRightItem(join.getRightItem());
		} else {
			String name = joinItem.getAlias().getName();
			newJ.setRightItem(new Table(name));
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

	// SelectItem : AllColumns, AllTableColumns, SelectExpressionItem

	@Override
	public void visit(SelectExpressionItem selectExpressionItem) {
		if (!(selectExpressionItem.getExpression() instanceof SubSelect)) {
			tempBody.addSelectItems(selectExpressionItem);
		} else {
			// TODO there could also be an alias
			// chissene, robe nella select sono ininfluenti
			/*
			 * SubSelect exprItem = (SubSelect) item.getExpression(); if
			 * (exprItem.getWithItemsList() != null) { for (WithItem withItem :
			 * exprItem.getWithItemsList()) { withItem.accept(this); } } String aliasName =
			 * createViewName(); WithItem table = new WithItem(); table.setName(aliasName);
			 * table.setSelectBody(exprItem.getSelectBody());
			 * 
			 * SelectBody curr = resolver.getCurrentSelect(); if (selectToViewMap.get(curr)
			 * == null) { selectToViewMap.put(curr, new LinkedList<>()); }
			 * selectToViewMap.get(curr).add(aliasName);
			 * 
			 * table.accept(this);
			 */
		}
	}

	private class ExprVisitor extends ExpressionVisitorAdapter {
		// TODO might consider to set a SelectVisitor
		// invece conviene non averlo cosi si saltano le subselect

		// TODO manca la gestone di AND, OR e altri operatori di confronto

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
				eqs.add(expr);
			}
		}
	}

}
