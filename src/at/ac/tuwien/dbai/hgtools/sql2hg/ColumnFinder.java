package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.HashSet;
import java.util.Set;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;

public class ColumnFinder extends ExpressionVisitorAdapterFixed {

	private HashSet<Column> columns;

	public Set<Column> getColumns(Expression expr) {
		columns = new HashSet<>();
		expr.accept(this);
		return columns;
	}

	@Override
	public void visit(Column column) {
		columns.add(column);
	}

	@Override
	public String toString() {
		return columns.toString();
	}

}
