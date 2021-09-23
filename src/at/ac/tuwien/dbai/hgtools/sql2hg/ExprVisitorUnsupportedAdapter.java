package at.ac.tuwien.dbai.hgtools.sql2hg;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.ArrayExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.CollateExpression;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.IntervalExpression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.JsonExpression;
import net.sf.jsqlparser.expression.KeepExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.MySQLGroupConcat;
import net.sf.jsqlparser.expression.NextValExpression;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.NumericBind;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeKeyExpression;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.ValueListExpression;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseLeftShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseRightShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.IntegerDivision;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.FullTextSearch;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsBooleanExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.JsonOperator;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NamedExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.expression.operators.relational.RegExpMySQLOperator;
import net.sf.jsqlparser.expression.operators.relational.SimilarToExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.Pivot;
import net.sf.jsqlparser.statement.select.PivotVisitor;
import net.sf.jsqlparser.statement.select.PivotXml;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;
import net.sf.jsqlparser.statement.select.SubSelect;

public class ExprVisitorUnsupportedAdapter
        implements ExpressionVisitor, ItemsListVisitor, PivotVisitor, SelectItemVisitor {

    protected static final String NOT_SUPPORTED_YET = "Not supported yet.";

    private static void throwException(Object obj) {
        throw new UnsupportedOperationException("Visiting: " + obj + ". " + NOT_SUPPORTED_YET);
    }

    @Override
    public void visit(AllColumns allColumns) {
        throwException(allColumns);
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        throwException(allTableColumns);
    }

    @Override
    public void visit(SelectExpressionItem selectExpressionItem) {
        throwException(selectExpressionItem);
    }

    @Override
    public void visit(Pivot pivot) {
        throwException(pivot);
    }

    @Override
    public void visit(PivotXml pivot) {
        throwException(pivot);
    }

    @Override
    public void visit(ExpressionList expressionList) {
        throwException(expressionList);
    }

    @Override
    public void visit(NamedExpressionList namedExpressionList) {
        throwException(namedExpressionList);
    }

    @Override
    public void visit(MultiExpressionList multiExprList) {
        throwException(multiExprList);
    }

    @Override
    public void visit(BitwiseRightShift aThis) {
        throwException(aThis);
    }

    @Override
    public void visit(BitwiseLeftShift aThis) {
        throwException(aThis);
    }

    @Override
    public void visit(NullValue nullValue) {
        throwException(nullValue);
    }

    @Override
    public void visit(Function function) {
        throwException(function);
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        throwException(signedExpression);
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {
        throwException(jdbcParameter);
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {
        throwException(jdbcNamedParameter);
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        throwException(doubleValue);
    }

    @Override
    public void visit(LongValue longValue) {
        throwException(longValue);
    }

    @Override
    public void visit(HexValue hexValue) {
        throwException(hexValue);
    }

    @Override
    public void visit(DateValue dateValue) {
        throwException(dateValue);
    }

    @Override
    public void visit(TimeValue timeValue) {
        throwException(timeValue);
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        throwException(timestampValue);
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        throwException(parenthesis);
    }

    @Override
    public void visit(StringValue stringValue) {
        throwException(stringValue);
    }

    @Override
    public void visit(Addition addition) {
        throwException(addition);
    }

    @Override
    public void visit(Division division) {
        throwException(division);
    }

    @Override
    public void visit(IntegerDivision division) {
        throwException(division);
    }

    @Override
    public void visit(Multiplication multiplication) {
        throwException(multiplication);
    }

    @Override
    public void visit(Subtraction subtraction) {
        throwException(subtraction);
    }

    @Override
    public void visit(AndExpression andExpression) {
        throwException(andExpression);
    }

    @Override
    public void visit(OrExpression orExpression) {
        throwException(orExpression);
    }

    @Override
    public void visit(Between between) {
        throwException(between);
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        throwException(equalsTo);
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        throwException(greaterThan);
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        throwException(greaterThanEquals);
    }

    @Override
    public void visit(InExpression inExpression) {
        throwException(inExpression);
    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {
        throwException(fullTextSearch);
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        throwException(isNullExpression);
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        throwException(isBooleanExpression);
    }

    @Override
    public void visit(LikeExpression likeExpression) {
        throwException(likeExpression);
    }

    @Override
    public void visit(MinorThan minorThan) {
        throwException(minorThan);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        throwException(minorThanEquals);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        throwException(notEqualsTo);
    }

    @Override
    public void visit(Column tableColumn) {
        throwException(tableColumn);
    }

    @Override
    public void visit(SubSelect subSelect) {
        throwException(subSelect);
    }

    @Override
    public void visit(CaseExpression caseExpression) {
        throwException(caseExpression);
    }

    @Override
    public void visit(WhenClause whenClause) {
        throwException(whenClause);
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        throwException(existsExpression);
    }

    @Override
    public void visit(AllComparisonExpression allComparisonExpression) {
        throwException(allComparisonExpression);
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        throwException(anyComparisonExpression);
    }

    @Override
    public void visit(Concat concat) {
        throwException(concat);
    }

    @Override
    public void visit(Matches matches) {
        throwException(matches);
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        throwException(bitwiseAnd);
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        throwException(bitwiseOr);
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        throwException(bitwiseXor);
    }

    @Override
    public void visit(CastExpression cast) {
        throwException(cast);
    }

    @Override
    public void visit(Modulo modulo) {
        throwException(modulo);
    }

    @Override
    public void visit(AnalyticExpression aexpr) {
        throwException(aexpr);
    }

    @Override
    public void visit(ExtractExpression eexpr) {
        throwException(eexpr);
    }

    @Override
    public void visit(IntervalExpression iexpr) {
        throwException(iexpr);
    }

    @Override
    public void visit(OracleHierarchicalExpression oexpr) {
        throwException(oexpr);
    }

    @Override
    public void visit(RegExpMatchOperator rexpr) {
        throwException(rexpr);
    }

    @Override
    public void visit(JsonExpression jsonExpr) {
        throwException(jsonExpr);
    }

    @Override
    public void visit(JsonOperator jsonExpr) {
        throwException(jsonExpr);
    }

    @Override
    public void visit(RegExpMySQLOperator regExpMySQLOperator) {
        throwException(regExpMySQLOperator);
    }

    @Override
    public void visit(UserVariable var) {
        throwException(var);
    }

    @Override
    public void visit(NumericBind bind) {
        throwException(bind);
    }

    @Override
    public void visit(KeepExpression aexpr) {
        throwException(aexpr);
    }

    @Override
    public void visit(MySQLGroupConcat groupConcat) {
        throwException(groupConcat);
    }

    @Override
    public void visit(ValueListExpression valueList) {
        throwException(valueList);
    }

    @Override
    public void visit(RowConstructor rowConstructor) {
        throwException(rowConstructor);
    }

    @Override
    public void visit(OracleHint hint) {
        throwException(hint);
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {
        throwException(timeKeyExpression);
    }

    @Override
    public void visit(DateTimeLiteralExpression literal) {
        throwException(literal);
    }

    @Override
    public void visit(NotExpression aThis) {
        throwException(aThis);
    }

    @Override
    public void visit(NextValExpression aThis) {
        throwException(aThis);
    }

    @Override
    public void visit(CollateExpression aThis) {
        throwException(aThis);
    }

    @Override
    public void visit(SimilarToExpression aThis) {
        throwException(aThis);
    }

    @Override
    public void visit(ArrayExpression aThis) {
        throwException(aThis);
    }

}
