package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.dbai.hgtools.util.SqlUtils;
import at.ac.tuwien.dbai.hgtools.util.Util;
import at.ac.tuwien.dbai.hgtools.util.Writables;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitor;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.ValuesList;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.values.ValuesStatement;

public class GeneralQueryFinder {

    protected static final String NOT_SUPPORTED_YET = "Not supported yet.";

    private static void throwException(Object obj) {
        throw new UnsupportedOperationException("Visiting: " + obj + ". " + NOT_SUPPORTED_YET);
    }

    private StatVisitor vStatement = new StatVisitor();
    private SelVisitor vSelect = new SelVisitor();
    private SelItVisitor vSelectItem = new SelItVisitor();
    private FromItVisitor vFromItem = new FromItVisitor();
    private ExprVisitor vExpression = new ExprVisitor();
    private OrderByVisitor vOrderBy = new OrdByVisitor();
    private GroupByVisitor vGroupBy = new GrpByVisitor();

    private Schema schema;
    private HashMap<String, Predicate> tables;
    private HashSet<Equality> joins;
    private ArrayList<String> filters;
    private ArrayList<String> selects;

    public GeneralQueryFinder(Schema schema) {
        if (schema == null) {
            throw new NullPointerException("The schema cannot be null.");
        }
        this.schema = schema;
        this.tables = new HashMap<>();
        this.joins = new HashSet<>();
        this.filters = new ArrayList<>();
        this.selects = new ArrayList<>();
    }

    public void run(Statement stmt) {
        stmt.accept(vStatement);
    }

    public Collection<Predicate> getTables() {
        return tables.values();
    }

    public Set<Equality> getJoins() {
        return joins;
    }

    public List<String> getFilters() {
        return filters;
    }

    public List<String> getSelects() {
        return selects;
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
                    if (j.isNatural()) {
                        throwException(j);
                        findEqualities(j);
                    } else if (j.getOnExpression() != null) {
                        j.getOnExpression().accept(vExpression);
                    }
                }
            }

            if (plainSelect.getSelectItems() != null) {
                plainSelect.getSelectItems().forEach(item -> item.accept(vSelectItem));
            }

            if (plainSelect.getWhere() != null) {
                plainSelect.getWhere().accept(vExpression);
            }

            if (plainSelect.getHaving() != null) {
                throwException("having");
                plainSelect.getHaving().accept(vExpression);
            }

            if (plainSelect.getOrderByElements() != null) {
                plainSelect.getOrderByElements().forEach(obe -> obe.accept(vOrderBy));
            }

            if (plainSelect.getGroupBy() != null) {
                plainSelect.getGroupBy().accept(vGroupBy);
            }
        }

        private void findEqualities(Join join) {
            Table joinTable = (Table) join.getRightItem();
            String jtName = Util.getTableAliasName(joinTable);
            Predicate joinPredicate = tables.get(jtName);
            for (Predicate p : tables.values()) {
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

        @Override
        public void visit(SetOperationList setOpList) {
            throwException(setOpList);
        }

        @Override
        public void visit(WithItem withItem) {
            throwException(withItem);
        }

        @Override
        public void visit(ValuesStatement aThis) {
            throwException(aThis);
        }

    }

    private class SelItVisitor extends SelectItemVisitorAdapter {

        @Override
        public void visit(AllColumns columns) {
            selects.add(columns.toString());
        }

        @Override
        public void visit(AllTableColumns columns) {
            selects.add(columns.toString());
        }

        @Override
        public void visit(SelectExpressionItem item) {
            Set<String> cols = new ColumnFinderUnique().getColumns(item.getExpression());
            selects.add(Writables.stringify(cols, ';') + ";" + item.toString());
        }

    }

    private class FromItVisitor extends FromItemVisitorAdapter {

        @Override
        public void visit(Table table) {
            String tableWholeName = table.getFullyQualifiedName();
            String tableAliasName = Util.getTableAliasName(table);
            Predicate pred = schema.newPredicate(tableWholeName); // TODO problematic line
            pred.setAlias(tableAliasName);
            tables.put(tableAliasName, pred);
        }

        @Override
        public void visit(SubSelect subSelect) {
            throwException(subSelect);
        }

        @Override
        public void visit(SubJoin subjoin) {
            throwException(subjoin);
        }

        @Override
        public void visit(LateralSubSelect lateralSubSelect) {
            throwException(lateralSubSelect);
        }

        @Override
        public void visit(ValuesList valuesList) {
            throwException(valuesList);
        }

        @Override
        public void visit(TableFunction valuesList) {
            throwException(valuesList);
        }

        @Override
        public void visit(ParenthesisFromItem aThis) {
            throwException(aThis);
        }

    }

    private class ExprVisitor extends ExprVisitorUnsupportedAdapter {

        protected void visitBinaryExpression(BinaryExpression expr) {
            expr.getLeftExpression().accept(this);
            expr.getRightExpression().accept(this);
        }

        @Override
        public void visit(AndExpression expr) {
            visitBinaryExpression(expr);
        }

        @Override
        public void visit(EqualsTo equalsTo) {
            visitJoin(equalsTo);
        }

        @Override
        public void visit(GreaterThan greaterThan) {
            visitJoin(greaterThan);
        }

        @Override
        public void visit(GreaterThanEquals greaterThanEquals) {
            visitJoin(greaterThanEquals);
        }

        @Override
        public void visit(MinorThan expr) {
            visitJoin(expr);
        }

        @Override
        public void visit(MinorThanEquals expr) {
            visitJoin(expr);
        }

        @Override
        public void visit(NotEqualsTo expr) {
            visitJoin(expr);
        }

        public void visitJoin(ComparisonOperator expr) {
            ClauseType ct = ClauseType.determineClauseType(expr);
            switch (ct) {
                case COLUMN_OP_COLUMN:
                    if (!(expr instanceof EqualsTo)) {
                        throwException(expr);
                    }
                    Column left = (Column) expr.getLeftExpression();
                    Column right = (Column) expr.getRightExpression();
                    Predicate pred1 = predOf(left);
                    Predicate pred2 = predOf(right);
                    String leftColumn = left.getColumnName();
                    String rightColumn = right.getColumnName();
                    Equality eq = new Equality(pred1, leftColumn, pred2, rightColumn);
                    joins.add(eq);
                    break;
                case COLUMN_OP_CONSTANT:
                    Column col = (Column) expr.getLeftExpression();
                    String colName = col.getFullyQualifiedName();
                    filters.add(colName + ";" + expr);
                    break;
                case COLUMN_OP_SUBSELECT:
                    throwException(expr);
                    break;
                case OTHER:
                default:
                    throw new AssertionError("Unknown clause type: " + ct);
            }
        }

        private Predicate predOf(Column col) {
            Predicate res = null;
            if (col.getTable() != null) {
                String tableName = col.getTable().getName();
                res = tables.get(tableName);
                if (res == null) {
                    throw new RuntimeException("The table " + tableName + " does not exist.");
                }
            } else {
                for (Predicate pred : tables.values()) {
                    if (pred.existsAttribute(col.getColumnName())) {
                        return pred;
                    }
                }
                throw new RuntimeException("The table of " + col + " cannot be found.");
            }
            return res;
        }

        @Override
        public void visit(InExpression expr) {
            if (expr.getLeftExpression() != null) {
                expr.getRightItemsList().accept(this);
                String left = ((Column) expr.getLeftExpression()).getFullyQualifiedName();
                filters.add(left + ";" + expr);
            }
        }

        @Override
        public void visit(ExpressionList expressionList) {
            for (Expression expr : expressionList.getExpressions()) {
                if (expr instanceof SubSelect) {
                    throwException(expressionList);
                }
            }
        }

        @Override
        public void visit(LikeExpression expr) {
            String left = ((Column) expr.getLeftExpression()).getFullyQualifiedName();
            filters.add(left + ";" + expr);
        }

        @Override
        public void visit(IsNullExpression expr) {
            String left = ((Column) expr.getLeftExpression()).getFullyQualifiedName();
            filters.add(left + ";" + expr);
        }

        @Override
        public void visit(Between expr) {
            String left = ((Column) expr.getLeftExpression()).getFullyQualifiedName();
            filters.add(left + ";" + expr);
        }

        @Override
        public void visit(Parenthesis parenthesis) {
            ParVisitor vPar = new ParVisitor();
            parenthesis.getExpression().accept(vPar);
            filters.add(Writables.stringify(vPar.cols, ';') + ";" + parenthesis);
        }

        private class ParVisitor extends ExprVisitorUnsupportedAdapter {
            private HashSet<String> cols = new HashSet<>();
            private HashSet<Predicate> preds = new HashSet<>();

            protected void visitBinaryExpression(BinaryExpression expr) {
                expr.getLeftExpression().accept(this);
                expr.getRightExpression().accept(this);
            }

            @Override
            public void visit(AndExpression expr) {
                visitBinaryExpression(expr);
            }

            @Override
            public void visit(OrExpression expr) {
                visitBinaryExpression(expr);
            }

            @Override
            public void visit(EqualsTo expr) {
                checkFilter(expr);
            }

            @Override
            public void visit(LikeExpression expr) {
                checkFilter(expr);
            }

            private void checkFilter(BinaryExpression expr) {
                Column left = (Column) expr.getLeftExpression();
                cols.add(left.getFullyQualifiedName());
                preds.add(predOf(left));
                if (preds.size() != 1) {
                    throwException(expr);
                }
                if (!SqlUtils.isConstantValue(expr.getRightExpression())) {
                    throwException(expr);
                }
            }

            @Override
            public void visit(Parenthesis parenthesis) {
                parenthesis.getExpression().accept(this);
            }

            @Override
            public void visit(SubSelect subSelect) {
                throwException(subSelect);
            }
        }

        @Override
        public void visit(SubSelect subSelect) {
            throwException(subSelect);
        }

    }

    private class StatVisitor extends StatementVisitorAdapter {

        @Override
        public void visit(Select select) {
            if (select.getWithItemsList() != null) {
                throwException(select);
                for (WithItem item : select.getWithItemsList()) {
                    item.accept(vSelect);
                }
            }
            select.getSelectBody().accept(vSelect);
        }

        @Override
        public void visit(CreateView createView) {
            throwException(createView);
        }

    }

    private class OrdByVisitor implements OrderByVisitor {

        @Override
        public void visit(OrderByElement orderBy) {
            throwException(orderBy);
        }

    }

    private class GrpByVisitor implements GroupByVisitor {

        @Override
        public void visit(GroupByElement groupBy) {
            throwException(groupBy);
        }

    }

}
