package at.ac.tuwien.dbai.hgtools;

import java.io.File;

import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import at.ac.tuwien.dbai.hgtools.sql2hg.ConjunctiveQueryFinder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Equality;
import at.ac.tuwien.dbai.hgtools.sql2hg.HypergraphBuilder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Predicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.sql2hg.ViewPredicate;
import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

public class MainSQL {

	public static void main(String[] args) throws JSQLParserException {
		Schema schema = new Schema();
		String schemaString = Util.readSQLFile(args[0]);
		Util.readSQLPredicateDefinitions(schemaString, schema);

		System.out.println("filename;vertices;edges;degree;bip;b3ip;b4ip;vc");

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

	private static void processFiles(File[] files, Schema schema) throws JSQLParserException {
		for (File file : files) {
			if (file.isDirectory()) {
				// System.out.println("Directory: " + file.getName());
				processFiles(file.listFiles(), schema); // Calls same method again.
			} else if (Util.isSQLFile(file.getName())) {
				String sqlString = Util.readSQLFile(file.getPath());
				Statement stmt = CCJSqlParserUtil.parse(sqlString);
				Select selectStmt = (Select) stmt;
				ConjunctiveQueryFinder hgFinder = new ConjunctiveQueryFinder(schema);
				hgFinder.run(selectStmt);

				HypergraphBuilder hgBuilder = new HypergraphBuilder();
				for (Predicate table : hgFinder.getTables()) {
					// System.out.println(table);
					hgBuilder.buildEdge(table);
				}
				for (Predicate table : hgFinder.getTables()) {
					if (table instanceof ViewPredicate) {
						ViewPredicate view = (ViewPredicate) table;
						// System.out.println(view);
						for (Equality join : view.getJoins()) {
							// System.out.println(join);
							hgBuilder.buildViewJoin(view.getAlias(), join);
						}
					}
				}
				for (Equality join : hgFinder.getJoins()) {
					// System.out.println(join);
					hgBuilder.buildJoin(join);
				}
				Hypergraph h = hgBuilder.getHypergraph();

				System.out.print(file.getPath() + ";");
				System.out.print(h.cntVertices() + ";");
				System.out.print(h.cntEdges() + ";");
				System.out.print(h.degree() + ";");
				System.out.print(h.cntBip(2) + ";");
				System.out.print(h.cntBip(3) + ";");
				System.out.print(h.cntBip(4) + ";");
				System.out.println(h.VCdimension());
			}
		}
	}

}