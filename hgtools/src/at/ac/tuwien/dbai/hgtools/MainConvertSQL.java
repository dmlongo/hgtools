package at.ac.tuwien.dbai.hgtools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import at.ac.tuwien.dbai.hgtools.HypergraphFromSQL.BasePredicate;
import at.ac.tuwien.dbai.hgtools.HypergraphFromSQL.HypergraphFromSQLFinder;
import at.ac.tuwien.dbai.hgtools.HypergraphFromSQL.HypergraphFromSQLHelper;
import at.ac.tuwien.dbai.hgtools.HypergraphFromSQL.SameColumn;
import at.ac.tuwien.dbai.hgtools.HypergraphFromSQL.Schema;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Select;

public class MainConvertSQL {

	public static void main(String[] args) throws JSQLParserException, IOException {
		Schema schema = new Schema();

		String schemaString = readFile(args[0]);

		Statements schemaStmts = CCJSqlParserUtil.parseStatements(schemaString);

		for (Statement schemaStmt : schemaStmts.getStatements()) {
			try {
				CreateTable tbl = (CreateTable) schemaStmt;

				// System.out.println("Table: "+tbl.getTable().getName());
				BasePredicate p = new BasePredicate(tbl.getTable().getName());
				for (ColumnDefinition cdef : tbl.getColumnDefinitions()) {
					// System.out.println("+++ " + cdef.getColumnName());
					p.addLiteral(cdef.getColumnName());
				}
				schema.addPredicate(p);
			} catch (ClassCastException c) {
			}
		}

		for (int i = 1; i < args.length; i++) {
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

	private static void processFiles(File[] files, Schema schema) throws JSQLParserException, IOException {
		for (File file : files) {
			if (file.isDirectory()) {
				// System.out.println("Directory: " + file.getName());
				processFiles(file.listFiles(), schema); // Calls same method again.
			} else if (file.getName().endsWith("sql") || file.getName().endsWith("tpl")) {

				HypergraphFromSQLHelper sql2hg = new HypergraphFromSQLHelper();
				String sqlString = readFile(file.getPath());
				Statement stmt = CCJSqlParserUtil.parse(sqlString);

				Select selectStmt = (Select) stmt;
				HypergraphFromSQLFinder hgFinder = new HypergraphFromSQLFinder();
				hgFinder.run(schema, selectStmt);
				Map<String, String> tableList = hgFinder.getAtomList();

				for (String s : tableList.keySet()) {
					System.out.println(s + ": " + tableList.get(s));
					System.out.println(schema.getPredicate(tableList.get(s)));
					sql2hg.addAtom(schema.getPredicate(tableList.get(s)), s);
				}

				for (SameColumn sc : hgFinder.getJoinList()) {
					Column a = sc.getA();
					Column b = sc.getB();
					System.out.println(
							a.getTable() + "." + a.getColumnName() + "=" + b.getTable() + "." + b.getColumnName());
					sql2hg.addJoin(sc);
				}

				Hypergraph H = sql2hg.getHypergraph();

				String newFile = file.getPath();
				newFile = "output/" + newFile.substring(0, newFile.lastIndexOf(".")) + ".hg";

				Path newFilePath = Paths.get(newFile);
				Files.createDirectories(newFilePath.getParent());
				if (!Files.exists(newFilePath))
					Files.createFile(newFilePath);
				Files.write(Paths.get(newFile), H.toFile(), Charset.forName("UTF-8"));
			}
		}
	}

	public static String readFile(String fName) {
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

}
