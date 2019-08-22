package at.ac.tuwien.dbai.hgtools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import at.ac.tuwien.dbai.hgtools.sql2hg.old.BasePredicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.old.HypergraphFromSQLFinder;
import at.ac.tuwien.dbai.hgtools.sql2hg.old.HypergraphFromSQLHelper;
import at.ac.tuwien.dbai.hgtools.sql2hg.old.SameColumn;
import at.ac.tuwien.dbai.hgtools.sql2hg.old.Schema;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Select;


public class MainSQL {

	public static void main(String[] args) throws JSQLParserException {
		Schema schema = new Schema();
		
		String schemaString = readFile(args[0]);
		
		Statements schemaStmts = CCJSqlParserUtil.parseStatements(schemaString);
		 
		for ( Statement schemaStmt : schemaStmts.getStatements() ) {
			try {
			   CreateTable tbl = (CreateTable) schemaStmt;
			
			   
			   //System.out.println("Table: "+tbl.getTable().getName());
			   BasePredicate p = new BasePredicate(tbl.getTable().getName());
		       for (ColumnDefinition cdef : tbl.getColumnDefinitions()) {
			      //System.out.println("+++ " + cdef.getColumnName());
			      p.addAttribute(cdef.getColumnName());
		       }
		       schema.addPredicate(p);
			}
		    catch (ClassCastException c) { }
		}
		
		System.out.println("filename;vertices;edges;degree;bip;b3ip;b4ip;vc");
		
		for (int i=1; i < args.length; i++) {
			File file = new File(args[i]);
			File[] files;
			if (file.isDirectory()) {
				files = file.listFiles();
			} else {
				files = new File[1];
				files[0] = file;
			}
		    processFiles(files, schema);
		}
	}
	
	private static void processFiles(File[] files, Schema schema) throws JSQLParserException {
		for (File file : files) {
	        if (file.isDirectory()) {
	            //System.out.println("Directory: " + file.getName());
	            processFiles(file.listFiles(), schema); // Calls same method again.
	        } else if (file.getName().endsWith("sql")) {
	        	
			    HypergraphFromSQLHelper sql2hg = new HypergraphFromSQLHelper();
			    String sqlString = readFile(file.getPath());
			    Statement stmt = CCJSqlParserUtil.parse(sqlString);
			
			    Select selectStmt = (Select)stmt;
			    HypergraphFromSQLFinder hgFinder = new HypergraphFromSQLFinder();
			    hgFinder.run(schema, selectStmt);
			    Map<String,String> tableList = hgFinder.getAtomList();
			
			    for (String s : tableList.keySet()) {
			    	//System.out.println(s+": "+tableList.get(s));
				    sql2hg.addAtom(schema.getPredicate(tableList.get(s)), s);
			    }
			
			    for (SameColumn sc : hgFinder.getJoinList()) {
			    	Column a = sc.getA();
			    	Column b = sc.getB();
			    	//System.out.println(a.getTable()+"."+a.getColumnName()+"="+b.getTable()+"."+b.getColumnName());
			    	sql2hg.addJoin(sc);
			    }
			
			
			    Hypergraph H = sql2hg.getHypergraph();
			
			    System.out.print(file.getPath()+";");
			    System.out.print(H.cntVertices()+";");
			    System.out.print(H.cntEdges()+";");
			    System.out.print(H.degree()+";");
			    System.out.print(H.cntBip(2)+";");
			    System.out.print(H.cntBip(3)+";");
			    System.out.print(H.cntBip(4)+";");
			    System.out.println(H.VCdimension());
	        }
		}
	}

	public static String readFile(String fName) {
		String s = "";
		
		try (BufferedReader br = new BufferedReader(new FileReader(fName)))
		{
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
}
