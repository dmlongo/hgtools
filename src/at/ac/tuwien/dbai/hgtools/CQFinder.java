package at.ac.tuwien.dbai.hgtools;

import java.util.HashSet;

import at.ac.tuwien.dbai.hgtools.sql2hg.Equality;
import at.ac.tuwien.dbai.hgtools.sql2hg.Predicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import net.sf.jsqlparser.statement.select.Select;

public class CQFinder {

	private HashSet<Predicate> tables;
	private HashSet<Equality> joins;

	public CQFinder(Schema schema) {
		// TODO Auto-generated constructor stub
	}

	public void find(Select selectStmt) {
		// TODO Auto-generated method stub

	}

	public HashSet<Predicate> getTables() {
		// TODO Auto-generated method stub
		return null;
	}

	public HashSet<Equality> getJoins() {
		// TODO Auto-generated method stub
		return null;
	}

}
