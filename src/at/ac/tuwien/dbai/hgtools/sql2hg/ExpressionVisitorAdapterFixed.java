package at.ac.tuwien.dbai.hgtools.sql2hg;

import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.WindowElement;
import net.sf.jsqlparser.expression.WindowOffset;
import net.sf.jsqlparser.expression.WindowRange;
import net.sf.jsqlparser.statement.select.OrderByElement;

public class ExpressionVisitorAdapterFixed extends ExpressionVisitorAdapter {

	@Override
	public void visit(Function function) {
		if (function.getParameters() != null) {
			function.getParameters().accept(this);
		}

		if (function.getNamedParameters() != null) {
			function.getNamedParameters().accept(this);
		}

		if (function.getKeep() != null) {
			function.getKeep().accept(this);
		}
	}

	@Override
	public void visit(AnalyticExpression expr) {
		if (expr.getExpression() != null) {
			expr.getExpression().accept(this);
		}
		if (expr.getDefaultValue() != null) {
			expr.getDefaultValue().accept(this);
		}
		if (expr.getOffset() != null) {
			expr.getOffset().accept(this);
		}
		if (expr.getKeep() != null) {
			expr.getKeep().accept(this);
		}
		// this is a problem in the adapter
		if (expr.getOrderByElements() != null) {
			for (OrderByElement element : expr.getOrderByElements()) {
				element.getExpression().accept(this);
			}
		}

		if (expr.getWindowElement() != null) {
			processWindowElement(expr.getWindowElement());
		}
	}

	public void processWindowElement(WindowElement we) {
		WindowRange range = we.getRange();
		if (range != null) {
			if (range.getStart() != null && range.getStart().getExpression() != null) {
				range.getStart().getExpression().accept(this);
			}
			if (range.getEnd() != null && range.getEnd().getExpression() != null) {
				range.getEnd().getExpression().accept(this);
			}
		}

		WindowOffset offset = we.getOffset();
		if (offset != null && offset.getExpression() != null) {
			offset.getExpression().accept(this);
		}
	}

}
