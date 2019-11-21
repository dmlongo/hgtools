package at.ac.tuwien.dbai.hgtools.sql2hg;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Block;
import net.sf.jsqlparser.statement.Commit;
import net.sf.jsqlparser.statement.DeclareStatement;
import net.sf.jsqlparser.statement.DescribeStatement;
import net.sf.jsqlparser.statement.ExplainStatement;
import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.ShowColumnsStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.UseStatement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.AlterView;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.ValuesList;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import net.sf.jsqlparser.statement.values.ValuesStatement;

public class QueryVisitorNoExpressionAdapter
		implements StatementVisitor, SelectVisitor, SelectItemVisitor, FromItemVisitor {

	@Override
	public void visit(Table tableName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(SubSelect subSelect) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(SubJoin subjoin) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(LateralSubSelect lateralSubSelect) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ValuesList valuesList) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(TableFunction tableFunction) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ParenthesisFromItem aThis) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(AllColumns allColumns) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(AllTableColumns allTableColumns) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(SelectExpressionItem selectExpressionItem) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Comment comment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Commit commit) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Delete delete) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Update update) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Insert insert) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Replace replace) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Drop drop) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Truncate truncate) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(CreateIndex createIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(CreateTable createTable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(CreateView createView) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(AlterView alterView) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Alter alter) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Statements stmts) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Execute execute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(SetStatement set) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ShowColumnsStatement set) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Merge merge) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Select select) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Upsert upsert) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(UseStatement use) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Block block) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ValuesStatement values) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DescribeStatement describe) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ExplainStatement aThis) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ShowStatement aThis) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DeclareStatement aThis) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(PlainSelect plainSelect) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(SetOperationList setOpList) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(WithItem withItem) {
		// TODO Auto-generated method stub

	}

}
