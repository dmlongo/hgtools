package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.Graph;

import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor;
import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor.SubqueryEdge;
import at.ac.tuwien.dbai.hgtools.sql2hg.QueryGraphManipulator;
import at.ac.tuwien.dbai.hgtools.sql2hg.QuerySimplifier;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

public class MainExtractSQL {

	private static int skipS = 0;
	private static int skipE = 0;
	private static String outDir = "output";

	public static void main(String[] args) throws JSQLParserException, IOException {
		args = setOtherArgs(args);

		Schema schema = new Schema();
		String schemaString = Util.readSQLFile(args[0]);
		Util.readSQLPredicateDefinitions(schemaString, schema);

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
			} else if (Util.isSQLFile(file.getName())) {
				String sqlString = Util.readSQLFile(file.getPath(), skipS, skipE);
				Statements stmts = CCJSqlParserUtil.parseStatements(sqlString);
				int nextID = 0;
				for (Statement stmt : stmts.getStatements()) {
					QueryExtractor qExtr = processStatement(stmt, schema);
					QueryGraphManipulator manip = new QueryGraphManipulator(qExtr);

					System.out.println("\n\nDepGraphs:");
					List<Graph<SelectBody, SubqueryEdge>> graphs = manip.computeDependencyGraphsSimplified();
					for (Graph<SelectBody, SubqueryEdge> depGraph : graphs) {
						printGraph(depGraph);
						System.out.println();

						QuerySimplifier qs = new QuerySimplifier(schema, depGraph, qExtr);
						List<Select> queries = qs.getSimpleQueries();
						for (Select query : queries) {
							System.out.println(query);
							System.out.println();
							Util.writeToFile(file, query, outDir, nextID++, queries.size());
						}
					}
				}
			}
		}
	}

	private static QueryExtractor processStatement(Statement stmt, Schema schema) {
		System.out.println("STATEMENT " + stmt);

		Select selectStmt = (Select) stmt;
		QueryExtractor qExtr = new QueryExtractor(schema);
		qExtr.run(selectStmt);
		Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();

		ArrayList<SelectBody> vertices = new ArrayList<>(query.vertexSet().size());
		for (SelectBody v : query.vertexSet()) {
			vertices.add(v);
		}
		printGraph(query, vertices);
		System.out.println();

		// globalNames
		System.out.println("globalNames:");
		for (String gName : qExtr.getGlobalNames()) {
			System.out.println(gName);
		}
		System.out.println();

		// selectToViewMap
		System.out.println("selectToViewMap:");
		for (SelectBody sel : qExtr.getSelectToViewMap().keySet()) {
			int iSel = vertices.indexOf(sel);
			System.out.println("" + iSel + " -> " + qExtr.getSelectToViewMap().get(sel));
		}
		System.out.println();

		// viewToGraphMap
		System.out.println("viewToGraphMap:");
		for (String view : qExtr.getViewToGraphMap().keySet()) {
			System.out.println(view);
			printGraph(qExtr.getViewToGraphMap().get(view).getQueryStructure());
			System.out.println();
		}

		return qExtr;
	}

	private static void printGraph(Graph<SelectBody, SubqueryEdge> query) {
		ArrayList<SelectBody> vertices = new ArrayList<>(query.vertexSet().size());
		for (SelectBody v : query.vertexSet()) {
			vertices.add(v);
		}
		printGraph(query, vertices);
	}

	private static void printGraph(Graph<SelectBody, SubqueryEdge> query, ArrayList<SelectBody> vertices) {
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
					SubqueryEdge e = query.getEdge(sourceVertex, targetVertex);
					System.out.println("(" + i + ", " + j + ", " + e.getLabel() + ")");
				}
				if (query.containsEdge(targetVertex, sourceVertex)) {
					SubqueryEdge e = query.getEdge(targetVertex, sourceVertex);
					System.out.println("(" + j + ", " + i + ", " + e.getLabel() + ")");
				}
			}
		}
	}

	private static String[] setOtherArgs(String[] args) {
		while (args[0].startsWith("-")) {
			String cmd = args[0];
			switch (cmd) {
			case "-skip":
				skipS = Integer.parseInt(args[1]);
				skipE = Integer.parseInt(args[2]);
				args = Util.shiftLeftResize(args, 3);
				break;
			case "-out":
				outDir = args[1];
				args = Util.shiftLeftResize(args, 2);
				break;
			default:
				throw new RuntimeException("Unknown command: " + cmd);
			}
		}
		return args;
	}

}
