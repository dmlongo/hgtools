package at.ac.tuwien.dbai.hgtools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

public class MainExtractSQL {

	public static void main(String[] args) throws JSQLParserException, IOException {
		Schema schema = new Schema();
		String schemaString = readFile(args[0]);
		readPredicateDefinitions(schemaString, schema);

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
				/*
				 * TODO create a regex containing all possible extensions and then check if the
				 * name matches it
				 */
				String sqlString = readFile(file.getPath());
				Statement stmt = CCJSqlParserUtil.parse(sqlString);
				Select selectStmt = (Select) stmt;
				QueryExtractor qExtr = new QueryExtractor(schema);
				qExtr.run(selectStmt);
				Graph<SelectBody, DefaultEdge> query = qExtr.getQueryStructure();

				ArrayList<SelectBody> vertices = new ArrayList<>(query.vertexSet().size());
				for (SelectBody v : query.vertexSet()) {
					vertices.add(v);
				}
				System.out.println("Vertices:");
				for (int i = 0; i < vertices.size(); i++) {
					System.out.println(i + " -> " + vertices.get(i));
				}
				System.out.println("Edges:");
				for (int i = 0; i < vertices.size() - 1; i++) {
					for (int j = i + 1; j < vertices.size(); j++) {
						SelectBody sourceVertex = vertices.get(i);
						SelectBody targetVertex = vertices.get(j);
						if (query.containsEdge(sourceVertex, targetVertex)) {
							System.out.println("(" + i + ", " + j + ")");
						}
						if (query.containsEdge(targetVertex, sourceVertex)) {
							System.out.println("(" + j + ", " + i + ")");
						}
					}
				}
			}
		}
	}

	private static List<String> toFile(HashMap<String, List<String>> map) {
		ArrayList<String> lines = new ArrayList<>(map.size());
		for (String var : map.keySet()) {
			StringBuilder sb = new StringBuilder(100);
			sb.append(var);
			sb.append('=');
			Iterator<String> it = map.get(var).iterator();
			while (it.hasNext()) {
				sb.append(it.next());
				if (it.hasNext()) {
					sb.append(',');
				}
			}
			lines.add(sb.toString());
		}
		return lines;
	}

	private static void readPredicateDefinitions(String schemaString, Schema schema) throws JSQLParserException {
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
