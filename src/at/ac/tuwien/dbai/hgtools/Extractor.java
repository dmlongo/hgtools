package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jgrapht.Graph;

import at.ac.tuwien.dbai.hgtools.sql2hg.Query;
import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor;
import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor.SubqueryEdge;
import at.ac.tuwien.dbai.hgtools.sql2hg.QueryGraphManipulator;
import at.ac.tuwien.dbai.hgtools.sql2hg.QuerySimplifier;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.sql2hg.ToLowerCaseTransformer;
import at.ac.tuwien.dbai.hgtools.util.SqlUtils;
import at.ac.tuwien.dbai.hgtools.util.Util;
import at.ac.tuwien.dbai.hgtools.util.Writables;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

public class Extractor {

	private static String type;

	private static int skipS = 0;
	private static int skipE = 0;
	private static String outDir = "output";

	private static Schema schema;

	private Extractor() {
		throw new IllegalStateException("Utility class");
	}

	public static void extract(String type, String[] args, int z) throws JSQLParserException, IOException {
		Extractor.type = type;
		z = setOtherArgs(args, z);

		schema = new Schema();
		String schemaString = SqlUtils.readFile(args[z++]);
		SqlUtils.readPredicateDefinitions(schemaString, schema);

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
				processFiles(file.listFiles()); // Calls same method again.
			} else if (!isFileTypeOk(file)) {
				return;
			}
			String sqlString = SqlUtils.readFile(file.getPath(), skipS, skipE);
			Statements stmts = CCJSqlParserUtil.parseStatements(sqlString);
			int nextID = 0;
			for (Statement stmt : stmts.getStatements()) {
				ToLowerCaseTransformer lc = new ToLowerCaseTransformer();
				lc.run(stmt);

				QueryExtractor qExtr = processStatement(stmt, schema);
				QueryGraphManipulator manip = new QueryGraphManipulator(qExtr);

				List<Graph<SelectBody, SubqueryEdge>> graphs = manip.computeDependencyGraphsSimplified();
				for (Graph<SelectBody, SubqueryEdge> depGraph : graphs) {
					QuerySimplifier qs = new QuerySimplifier(schema, depGraph, qExtr);
					List<Query> queries = qs.getSimpleQueries();
					for (Query q : queries) {
						String filename = Util.makeNextFilename(outDir, file.getName(), nextID++, queries.size());
						Writables.writeToFile(q, filename);
					}
				}
			}
		}
	}

	private static QueryExtractor processStatement(Statement stmt, Schema schema) {
		Select selectStmt = (Select) stmt;
		QueryExtractor qExtr = new QueryExtractor(schema);
		qExtr.run(selectStmt);
		return qExtr;
	}

	private static boolean isFileTypeOk(File file) {
		if (type.equals(Main.SQL)) {
			return SqlUtils.isSQLFile(file.getName());
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
