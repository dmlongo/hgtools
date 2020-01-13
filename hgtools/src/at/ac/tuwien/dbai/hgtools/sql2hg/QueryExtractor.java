package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
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

	private static class NameStack {
		private LinkedList<HashSet<String>> scopes;
		private LinkedList<SelectBody> selects;
		private LinkedList<LinkedList<Table>> tables;

		public NameStack() {
			scopes = new LinkedList<>();
			selects = new LinkedList<>();
			tables = new LinkedList<>();
			// global scope - no select
			scopes.addFirst(new HashSet<>());
			selects.addFirst(null);
			tables.addFirst(null);
		}

		public void enterNewScope(SelectBody select) {
			scopes.addFirst(new HashSet<>());
			selects.addFirst(select);
			tables.addFirst(new LinkedList<>());
		}

		public void exitCurrentScope() {
			scopes.removeFirst();
			selects.removeFirst();
			tables.removeFirst();
		}

		public void addTableToCurrentScope(Table table) {
			tables.getFirst().add(table);
		}

		public LinkedList<Table> getCurrentTables() {
			return tables.getFirst();
		}

		public void addNameToCurrentScope(String name) {
			scopes.getFirst().add(name);
		}

		public void addNamesToCurrentScope(Collection<String> names) {
			for (String name : names) {
				addNameToCurrentScope(name);
			}
		}

		public void addNameToParentScope(String name) {
			scopes.get(1).add(name);
		}

		public void addNamesToParentScope(Collection<String> names) {
			for (String name : names) {
				addNameToParentScope(name);
			}
		}

		public SelectBody getCurrentSelect() {
			return selects.getFirst();
		}

		public SelectBody resolve(String name) {
			Iterator<SelectBody> selectIt = selects.iterator();
			for (HashSet<String> names : scopes) {
				SelectBody body = selectIt.next();
				if (names.contains(name)) {
					return body;
				}
			}
			return null;
		}
	}

	private class ExprVisitor extends ExpressionVisitorAdapter {
		public ExprVisitor(SelectVisitor sv) {
			setSelectVisitor(sv);
		}

		@Override
		public void visit(Column tableColumn) {
			SelectBody source = resolver.getCurrentSelect();
			String toResolve = (tableColumn.getTable() != null) ? tableColumn.getTable().getName()
					: tableColumn.getColumnName();
			SelectBody target = resolver.resolve(toResolve);
			if (!target.equals(source)) {
				query.addEdge(source, target);
			}
		}

		@Override
		public void visit(SubSelect subSelect) {
			// TODO take care of the views
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
			query.addEdge(parent, child);
			resolver.enterNewScope(child);
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
	private Graph<SelectBody, DefaultEdge> query;

	public QueryExtractor(Schema schema) {
		if (schema == null) {
			throw new NullPointerException();
		}
		this.schema = schema;
		resolver = new NameStack();
		exprVisitor = new ExprVisitor(this);
		query = new DefaultDirectedGraph<>(DefaultEdge.class);
	}

	// TODO can be called only once, otherwise reset the state
	public void run(Statement statement) {
		statement.accept(this);
	}

	public Graph<SelectBody, DefaultEdge> getQueryStructure() {
		return query;
	}

	@Override
	public void visit(WithItem withItem) {
		// TODO non mi convince
		resolver.addNameToCurrentScope(withItem.getName());
		SelectBody body = withItem.getSelectBody();
		resolver.enterNewScope(body);
		if (withItem.getWithItemList() != null) {
			for (SelectItem item : withItem.getWithItemList()) {
				resolver.addNameToCurrentScope(item.toString());
			}
		}
		query.addVertex(body);
		body.accept(this);
		resolver.exitCurrentScope();
	}

	@Override
	public void visit(PlainSelect plainSelect) {
		if (plainSelect.getFromItem() != null) {
			resolver.addTableToCurrentScope((Table) plainSelect.getFromItem());
			plainSelect.getFromItem().accept(this);
		}

		if (plainSelect.getJoins() != null) {
			for (Join join : plainSelect.getJoins()) {
				resolver.addTableToCurrentScope((Table) join.getRightItem());
				join.getRightItem().accept(this);
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

	@Override
	public void visit(SetOperationList setOpList) {
		// no matter what the operator is - I just want the subqueries
		for (SelectBody child : setOpList.getSelects()) {
			SelectBody parent = resolver.getCurrentSelect();
			query.addVertex(child);
			query.addEdge(parent, child);
			resolver.enterNewScope(child);
			child.accept(this);
			resolver.exitCurrentScope();
		}
	}

	// SelectItemVisitor

	@Override
	public void visit(AllColumns allColumns) {
		// TODO Auto-generated method stub
		for (FromItem item : resolver.getCurrentTables()) {
			PredicateDefinition p = new PredicateFinder(schema).getPredicate(item);
			resolver.addNamesToParentScope(p.getAttributes());
		}
	}

	@Override
	public void visit(AllTableColumns allTableColumns) {
		String table = allTableColumns.getTable().getName();
		PredicateDefinition pred = schema.getPredicateDefinition(table);
		resolver.addNamesToParentScope(pred.getAttributes());
	}

	@Override
	public void visit(SelectExpressionItem item) {
		if (item.getAlias() != null) {
			resolver.addNameToParentScope(item.getAlias().getName());
		} else if (item.getExpression() instanceof Column) {
			// TODO do I enter here if I have an alias but I'm also a Column?
			Column col = (Column) item.getExpression();
			resolver.addNameToParentScope(col.getColumnName());
		} else {
			// TODO item.accept(exprVisitor);
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
		// TODO there are views here
		// I can come here only from a FROM, right?
		// For the SELECT I go through SelectExpressionItem
		if (subSelect.getAlias() != null) {
			resolver.addNameToCurrentScope(subSelect.getAlias().getName());
		}
		SelectBody parent = resolver.getCurrentSelect();
		SelectBody child = subSelect.getSelectBody();
		query.addVertex(child);
		query.addEdge(parent, child);
		resolver.enterNewScope(child);
		child.accept(this);
		resolver.exitCurrentScope();
	}

	@Override
	public void visit(Table tableName) {
		resolver.addNameToCurrentScope(getTableAliasName(tableName));
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
		// TODO add views to the schema as predicates and then remove them at the end of
		// the parsing
		/*
		 * if (select.getWithItemsList() != null) { for (WithItem withItem :
		 * select.getWithItemsList()) { withItem.accept(this); } }
		 */
		SelectBody body = select.getSelectBody();
		query.addVertex(body);
		resolver.enterNewScope(body);
		body.accept(this);
		resolver.exitCurrentScope();
	}

}
