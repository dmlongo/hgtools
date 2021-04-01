package at.ac.tuwien.dbai.hgtools.sql2hg.sqlshare;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import at.ac.tuwien.dbai.hgtools.sql2hg.ExpressionVisitorAdapterFixed;
import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
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
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;

public class PredicateBuilder {

	private StatVisitor vStatement = new StatVisitor();
	private SelVisitor vSelect = new SelVisitor();
	private SelItVisitor vSelectItem = new SelItVisitor();
	private FromItVisitor vFromItem = new FromItVisitor();
	private ExprVisitor vExpression = new ExprVisitor();

	private HashMap<String, String> alias2def = null;
	private HashMap<String, HashSet<String>> partialDefs = null;

	private PriorityQueue<SelectItem> selItems;

	private Set<String> viewNames = null;
	private HashSet<PredicateDefinition> viewPreds = null;
	private HashMap<Select, PredicateBuilder> sel2pb = null;
	private HashSet<PredicateDefinition> preds = null;
	private HashSet<String> unknownColumns = null;

	private String myName = null;
	private List<String> myAttributes = null;
	private PredicateDefinition mySignature = null;
	private LinkedList<String> tablesInTheFrom;

	private boolean findCols;
	private boolean quotes;

	private static class CustomComp implements Comparator<SelectItem> {

		static HashMap<Class<?>, Integer> map;

		static {
			map = new HashMap<>();
			map.put(SelectExpressionItem.class, 0);
			map.put(AllTableColumns.class, 1);
			map.put(AllColumns.class, 2);
		}

		public int compare(SelectItem o1, SelectItem o2) {
			return map.get(o1.getClass()) - map.get(o2.getClass());
		}

	}

	public PredicateBuilder() {
	}

	public PredicateBuilder(String name) {
		this(name, null);
	}

	public PredicateBuilder(String name, List<String> viewAttrs) {
		this(name, viewAttrs, null);
	}

	public PredicateBuilder(String name, List<String> viewAttrs, Set<String> viewNames) {
		if (name == null) {
			throw new NullPointerException();
		}
		myName = name;
		myAttributes = viewAttrs;
		this.viewNames = viewNames;
	}

	public Set<PredicateDefinition> buildPredicates(Statement stmt) {
		return buildPredicates(stmt, false);
	}

	public Set<PredicateDefinition> buildPredicates(Statement stmt, boolean quotes) {
		alias2def = new HashMap<>();
		partialDefs = new HashMap<>();
		sel2pb = new HashMap<>();
		preds = new HashSet<>();
		unknownColumns = new HashSet<>();

		viewNames = (viewNames == null) ? new HashSet<>() : viewNames;
		viewPreds = new HashSet<>(); // TODO maybe pass also this?

		selItems = new PriorityQueue<>(new CustomComp());

		myName = (myName == null) ? "" : myName;
		if (myAttributes == null) {
			myAttributes = new LinkedList<>();
			findCols = true;
		} else {
			findCols = false;
		}
		tablesInTheFrom = new LinkedList<>();
		this.quotes = quotes;

		stmt.accept(vStatement);

		// TODO invece di far edue passate su partialDefs posso prima elaborare le
		// SelectExpression, mi fermo, caccio le colonne unknown e elaboro le allcolumns

		// TODO no partialDefs, bensi tablesinThefrom
		HashSet<String> actualTablesInTheFrom = new HashSet<>();
		for (String alias : tablesInTheFrom) {
			actualTablesInTheFrom.add(alias2def.get(alias));
		}

		if (actualTablesInTheFrom.size() == 1) {
			String key = actualTablesInTheFrom.iterator().next();
			partialDefs.get(key).addAll(unknownColumns);
			unknownColumns.clear();
		}

		for (SelectItem item : selItems) {
			item.accept(vSelectItem);
		}

		if (actualTablesInTheFrom.size() == 1) {
			String key = actualTablesInTheFrom.iterator().next();
			partialDefs.get(key).addAll(unknownColumns);
			unknownColumns.clear();
		}

		for (Map.Entry<String, HashSet<String>> entry : partialDefs.entrySet()) {
			String predName = entry.getKey();
			HashSet<String> attributes = entry.getValue();
			PredicateDefinition p = null;
			if (quotes) {
				p = new PredicateDefinitionQuote(predName, attributes);
			} else {
				p = new PredicateDefinition(predName, attributes);
			}
			if (!viewNames.contains(predName)) {
				preds.add(p);
			} else {
				viewPreds.add(p);
			}
		}

		// for (HashSet<PredicateDefinition> sub : subPreds) {
		// preds.addAll(sub);
		// }

		Iterator<String> it = unknownColumns.iterator();
		while (it.hasNext()) {
			String col = it.next();
			int n = colInPredsNum(col);
			if (n == 1) {
				it.remove();
			} else if (n >= 2) {
				throw new RuntimeException("Unkown column " + col + " appears in more than one predicate.");
			}
		}

		if (quotes) {
			mySignature = new PredicateDefinitionQuote(myName, myAttributes);
		} else {
			mySignature = new PredicateDefinition(myName, myAttributes);
		}

		return preds;
	}

	private int colInPredsNum(String col) {
		int n = 0;
		for (HashSet<String> set : partialDefs.values()) {
			if (set.contains(col)) {
				n++;
			}
		}
		return n;
	}

	public Set<String> getViewNames() {
		return viewNames;
	}

	public Set<PredicateDefinition> getViewPredicates() {
		return viewPreds;
	}

	public Set<PredicateDefinition> getPredicates() {
		return preds;
	}

	public Set<String> getUnknowncolumns() {
		return unknownColumns;
	}

	public Collection<PredicateBuilder> getSubPredicates() {
		return sel2pb.values();
	}

	public PredicateDefinition getMySignature() {
		return mySignature;
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
					selItems.add(item);
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
					Select newSubSelect = new Select();
					newSubSelect.setSelectBody(set);
					PredicateBuilder pb = new PredicateBuilder("", null, viewNames);
					pb.buildPredicates(newSubSelect, quotes);
					sel2pb.put(newSubSelect, pb);
				}
			}
		}

		@Override
		public void visit(WithItem withItem) {
			String alias = withItem.getName();
			viewNames.add(alias);
			alias2def.put(alias, alias);
			if (!partialDefs.containsKey(alias)) {
				partialDefs.put(alias, new HashSet<>());
			} else {
				throw new RuntimeException(alias + " is already present: " + withItem);
			}

			LinkedList<String> viewAttrs = null;
			if (withItem.getWithItemList() != null) {
				viewAttrs = new LinkedList<>();
				for (SelectItem item : withItem.getWithItemList()) {
					Column col = (Column) ((SelectExpressionItem) item).getExpression();
					viewAttrs.add(col.getColumnName());
					partialDefs.get(alias).add(col.getColumnName());
				}
			}

			Select newSubSelect = new Select();
			newSubSelect.setSelectBody(withItem.getSelectBody());
			PredicateBuilder pb = new PredicateBuilder(alias, viewAttrs, viewNames);
			pb.buildPredicates(newSubSelect, quotes);
			sel2pb.put(newSubSelect, pb);
		}

	}

	private class SelItVisitor extends SelectItemVisitorAdapter {

		@Override
		public void visit(AllColumns columns) {
			if (findCols) {
				for (String alias : tablesInTheFrom) {
					String predName = alias2def.get(alias);
					for (String attr : partialDefs.get(predName)) {
						myAttributes.add(attr);
					}
				}
			}
		}

		@Override
		public void visit(AllTableColumns columns) {
			if (findCols) {
				String tab = alias2def.get(columns.getTable().getName());
				for (String attr : partialDefs.get(tab)) {
					myAttributes.add(attr);
				}
			}
		}

		@Override
		public void visit(SelectExpressionItem item) {
			if (findCols) {
				if (item.getAlias() != null) {
					myAttributes.add(item.getAlias().getName());
				} else if (item.getExpression() instanceof Column) {
					String col = ((Column) item.getExpression()).getColumnName();
					myAttributes.add(col);
				}
			}
			item.getExpression().accept(vExpression);
		}

	}

	private class FromItVisitor extends FromItemVisitorAdapter {

		@Override
		public void visit(Table table) {
			String alias = Util.getTableAliasName(table);
			tablesInTheFrom.add(alias);
			String tableName = table.getName();
			alias2def.put(alias, tableName);
			partialDefs.putIfAbsent(tableName, new HashSet<>());
		}

		@Override
		public void visit(SubSelect subSelect) {
			updateDefs(subSelect);

			String alias = (subSelect.getAlias() != null) ? subSelect.getAlias().getName() : "";
			tablesInTheFrom.add(alias);
			Select newSubSelect = new Select();
			newSubSelect.setWithItemsList(subSelect.getWithItemsList());
			newSubSelect.setSelectBody(subSelect.getSelectBody());
			PredicateBuilder pb = new PredicateBuilder(alias, null, viewNames);
			pb.buildPredicates(newSubSelect, quotes);
			sel2pb.put(newSubSelect, pb);
		}

		@Override
		public void visit(SubJoin subjoin) {
			updateDefs(subjoin);

			String alias = (subjoin.getAlias() != null) ? subjoin.getAlias().getName() : "";
			tablesInTheFrom.add(alias);
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
			updateDefs(aThis);

			String alias = (aThis.getAlias() != null) ? aThis.getAlias().getName() : "";
			tablesInTheFrom.add(alias);
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
				String alias = column.getTable().getName();
				String tab = alias2def.get(alias);
				if (tab != null) {
					String col = column.getColumnName();
					partialDefs.get(tab).add(col);
				} else {
					throw new RuntimeException("Cannot find table " + alias + " in column " + column);
				}
			} else {
				unknownColumns.add(column.getColumnName());
			}
		}

		@Override
		public void visit(SubSelect subSelect) {
			updateDefs(subSelect);

			String alias = (subSelect.getAlias() != null) ? subSelect.getAlias().getName() : "";
			Select newSubSelect = new Select();
			newSubSelect.setWithItemsList(subSelect.getWithItemsList());
			newSubSelect.setSelectBody(subSelect.getSelectBody());
			PredicateBuilder pb = new PredicateBuilder(alias, null, viewNames);
			pb.buildPredicates(newSubSelect, quotes);
			sel2pb.put(newSubSelect, pb);
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
			myName = createView.getView().getName();
			if (createView.getColumnNames() != null) {
				myAttributes = createView.getColumnNames();
				findCols = false;
			}
			createView.getSelect().accept(this);
		}

	}

	private void updateDefs(FromItem item) {
		if (item.getAlias() != null) {
			String alias = item.getAlias().getName();
			viewNames.add(alias);
			alias2def.put(alias, alias);
			if (!partialDefs.containsKey(alias)) {
				partialDefs.put(alias, new HashSet<>());
			} else {
				throw new RuntimeException(alias + " is already present: " + item);
			}
		}
	}

	@Override
	public String toString() {
		return mySignature.toString();
	}

}
