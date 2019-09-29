package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashSet;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.schema.Column;

public class ColumnFinder extends ExpressionVisitorAdapter {

	private HashSet<Column> columns;

	public HashSet<Column> getColumns(Expression expr) {
		columns = new HashSet<>();
		expr.accept(this);
		return columns;
	}

	@Override
	public void visit(Column column) {
		columns.add(column);
	}

}
