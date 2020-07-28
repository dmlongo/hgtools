package at.ac.tuwien.dbai.hgtools.sql2hg;

import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;

public enum ClauseType {
	COLUMN_OP_COLUMN, COLUMN_OP_CONSTANT, COLUMN_OP_SUBSELECT, OTHER;

	public static ClauseType determineClauseType(ComparisonOperator op) {
		Expression left = op.getLeftExpression();
		Expression right = op.getRightExpression();
		if (left instanceof Column && right instanceof Column) {
			return COLUMN_OP_COLUMN;
		} else if (left instanceof Column && isConstantValue(right)) {
			return COLUMN_OP_CONSTANT;
		} else if (left instanceof Column && right instanceof SubSelect) {
			return COLUMN_OP_SUBSELECT;
		} else {
			return OTHER;
		}
	}

	private static boolean isConstantValue(Expression expr) {
		return expr instanceof DateValue || expr instanceof DoubleValue || expr instanceof HexValue
				|| expr instanceof LongValue || expr instanceof NullValue || expr instanceof StringValue
				|| expr instanceof TimestampValue || expr instanceof TimeValue;
	}
}