package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
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
import net.sf.jsqlparser.statement.select.WithItem;

public class MainExtractSQL {

	private static int nextID = 0;

	public static void main(String[] args) throws JSQLParserException, IOException {
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
				String sqlString = Util.readSQLFile(file.getPath());
				Statements stmts = CCJSqlParserUtil.parseStatements(sqlString);
				for (Statement stmt : stmts.getStatements()) {
					QueryExtractor qExtr = processStatement(stmt, schema);
					QueryGraphManipulator manip = new QueryGraphManipulator(qExtr);

					System.out.println("\n\nDepGraphs:");
					List<Graph<SelectBody, SubqueryEdge>> graphs = manip.computeDependencyGraphsSimplified();
					for (Graph<SelectBody, SubqueryEdge> depGraph : graphs) {
						printGraph(depGraph);
						System.out.println();

						QuerySimplifier qs = new QuerySimplifier(depGraph, qExtr);
						List<Select> queries = qs.getSimpleQueries();
						for (Select query : queries) {
							System.out.println(query);
							System.out.println();
							writeToFile(file, query, queries.size());
						}
					}
				}
			}
		}
	}

	private static void writeToFile(File file, Select query, int size) throws IOException {
		String newFile = file.getPath();
		newFile = nextOutName(newFile, size);
		System.out.println(newFile);
		Path newFilePath = Paths.get(newFile);
		Files.createDirectories(newFilePath.getParent());
		if (!Files.exists(newFilePath))
			Files.createFile(newFilePath);
		Files.write(Paths.get(newFile), toFile(query), Charset.forName("UTF-8"));
	}

	private static Iterable<String> toFile(Select query) {
		LinkedList<String> res = new LinkedList<String>();
		res.add(query.toString());
		return res;
	}

	private static Iterable<String> toFile2(Select query) {
		// TODO Auto-generated method stub
		LinkedList<String> res = new LinkedList<String>();
		for (WithItem view : query.getWithItemsList()) {
			LinkedList<String> strView = stringify(view);
			res.addAll(strView);
			res.add("\n");
		}
		LinkedList<String> strSel = stringify(query.getSelectBody());
		res.addAll(strSel);
		return res;
	}
	
	private static LinkedList<String> stringify(SelectBody body) {
		// TODO Auto-generated method stub
		return null;
	}

	private static LinkedList<String> stringify(WithItem view) {
		LinkedList<String> res = new LinkedList<>();
		
		return res;
	}
	
	private static String nextOutName(String name, int size) {
		String root = name.substring(0, name.lastIndexOf('.'));
		String ext = name.substring(name.lastIndexOf('.') + 1);
		String id = getID(size);
		return "output/" + root + "_" + id + "." + ext;
	}

	private static String getID(int size) {
		int id = nextID++;
		int dTot = 1, dR = 1;
		int tot, r;
		for (tot = size / 10; tot != 0; tot /= 10, dTot++) {
		}
		for (r = id / 10; r != 0; r /= 10, dR++) {
		}
		String prefix = "";
		for (int i = 0; i < (dTot - dR); i++) {
			prefix += "0";
		}
		return prefix + id;
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

}
