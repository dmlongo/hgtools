package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

public class MainExtractSQL {

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

}
