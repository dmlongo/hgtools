package at.ac.tuwien.dbai.hgtools.sql2hg;

import at.ac.tuwien.dbai.hgtools.util.SqlUtils;
import net.sf.jsqlparser.expression.Expression;
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
		} else if (left instanceof Column && SqlUtils.isConstantValue(right)) {
			return COLUMN_OP_CONSTANT;
		} else if (left instanceof Column && right instanceof SubSelect) {
			return COLUMN_OP_SUBSELECT;
		} else {
			return OTHER;
		}
	}
}