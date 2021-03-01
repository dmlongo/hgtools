package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.Graph;

import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor;
import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor.SubqueryEdge;
import at.ac.tuwien.dbai.hgtools.sql2hg.sqlshare.ToLowerCaseTransformer;
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

public class MainExtract {

	private static String type;

	private static int skipS = 0;
	private static int skipE = 0;
	private static String outDir = "output";

	private static Schema schema;

	public static void main(String type, String[] args, int z) throws JSQLParserException, IOException {
		MainExtract.type = type;
		z = setOtherArgs(args, z);

		schema = new Schema();
		String schemaString = Util.readSQLFile(args[z++]);
		Util.readSQLPredicateDefinitions(schemaString, schema);

		for (int i = z; i < args.length; i++) {
			File file = new File(args[i]);
			File[] files;
			if (file.isDirectory()) {
				files = file.listFiles();
			} else {
				files = new File[1];
				files[0] = file;
			}
			processFiles(files);
		}
	}

	private static void processFiles(File[] files) throws JSQLParserException, IOException {
		for (File file : files) {
			if (file.isDirectory()) {
				// System.out.println("Directory: " + file.getName());
				processFiles(file.listFiles()); // Calls same method again.
			} else if (isFileTypeOk(file)) {
				String sqlString = Util.readSQLFile(file.getPath(), skipS, skipE);
				Statements stmts = CCJSqlParserUtil.parseStatements(sqlString);
				int nextID = 0;
				for (Statement stmt : stmts.getStatements()) {
					// make name lower case
					ToLowerCaseTransformer lc = new ToLowerCaseTransformer();
					lc.run(stmt);

					QueryExtractor qExtr = processStatement(stmt, schema);
					QueryGraphManipulator manip = new QueryGraphManipulator(qExtr);

					// System.out.println("\n\nDepGraphs:");
					List<Graph<SelectBody, SubqueryEdge>> graphs = manip.computeDependencyGraphsSimplified();
					for (Graph<SelectBody, SubqueryEdge> depGraph : graphs) {
						// printGraph(depGraph);
						// System.out.println();

						QuerySimplifier qs = new QuerySimplifier(schema, depGraph, qExtr);
						List<Select> queries = qs.getSimpleQueries();
						Util.writeQueriesToFile(file, queries, outDir, nextID);
					}
				}
			}
		}
	}

	private static QueryExtractor processStatement(Statement stmt, Schema schema) {
		// System.out.println("STATEMENT " + stmt);

		Select selectStmt = (Select) stmt;
		QueryExtractor qExtr = new QueryExtractor(schema);
		qExtr.run(selectStmt);
		Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();

		ArrayList<SelectBody> vertices = new ArrayList<>(query.vertexSet().size());
		for (SelectBody v : query.vertexSet()) {
			vertices.add(v);
		}
		// printGraph(query, vertices);
		// System.out.println();

		/*
		 * // globalNames System.out.println("globalNames:"); for (String gName :
		 * qExtr.getGlobalNames()) { System.out.println(gName); } System.out.println();
		 * 
		 * // selectToViewMap System.out.println("selectToViewMap:"); for (SelectBody
		 * sel : qExtr.getSelectToViewMap().keySet()) { int iSel =
		 * vertices.indexOf(sel); System.out.println("" + iSel + " -> " +
		 * qExtr.getSelectToViewMap().get(sel)); } System.out.println();
		 * 
		 * // viewToGraphMap System.out.println("viewToGraphMap:"); for (String view :
		 * qExtr.getViewToGraphMap().keySet()) { System.out.println(view);
		 * printGraph(qExtr.getViewToGraphMap().get(view).getQueryStructure());
		 * System.out.println(); }
		 */

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

	private static boolean isFileTypeOk(File file) {
		if (type.equals(Main.SQL)) {
			return Util.isSQLFile(file.getName());
		} else {
			return false;
		}
	}

	private static int setOtherArgs(String[] args, int z) {
		while (args[z].startsWith("-")) {
			String cmd = args[z++];
			switch (cmd) {
				case "-skip":
					skipS = Integer.parseInt(args[z++]);
					skipE = Integer.parseInt(args[z++]);
					break;
				case "-out":
					outDir = args[z++];
					break;
				default:
					throw new Main.UnsupportedCommandException(cmd);
			}
		}
		return z;
	}

}
