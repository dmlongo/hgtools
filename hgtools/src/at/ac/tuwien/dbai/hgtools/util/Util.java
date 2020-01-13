package at.ac.tuwien.dbai.hgtools.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

public class Util {

	public static String readSQLFile(String fName) {
		String s = "";
		try (BufferedReader br = new BufferedReader(new FileReader(fName))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				if (!sCurrentLine.startsWith("--"))
					s += sCurrentLine + " ";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return s;
	}

	public static void readSQLPredicateDefinitions(String schemaString, Schema schema) throws JSQLParserException {
		Statements schemaStmts = CCJSqlParserUtil.parseStatements(schemaString);
		for (Statement schemaStmt : schemaStmts.getStatements()) {
			try {
				CreateTable tbl = (CreateTable) schemaStmt;

				// System.out.println("Table: "+tbl.getTable().getName());
				String predicateName = tbl.getTable().getName();
				LinkedList<String> attributes = new LinkedList<>();
				for (ColumnDefinition cdef : tbl.getColumnDefinitions()) {
					// System.out.println("+++ " + cdef.getColumnName());
					attributes.add(cdef.getColumnName());
				}
				schema.addPredicateDefinition(new PredicateDefinition(predicateName, attributes));
			} catch (ClassCastException c) {
				System.err.println("\"" + schemaStmt + "\" is not a CREATE statement.");
			}
		}
	}

	public static boolean isSQLFile(String filename) {
		/*
		 * TODO create a regex containing all possible extensions and then check if the
		 * name matches it
		 */
		return filename.endsWith("sql") || filename.endsWith("tpl");
	}

	/**
	 * Removes angular brackets from vertex names
	 * 
	 * @param s A string s to be stringified
	 * @return A stringified String
	 */
	public static String stringify(String s) {
		String newS = new String(s);

		newS = newS.replace('[', 'L');
		newS = newS.replace(']', 'J');

		return newS;
	}

}
