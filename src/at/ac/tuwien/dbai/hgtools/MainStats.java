package at.ac.tuwien.dbai.hgtools;

import java.io.File;

import at.ac.tuwien.dbai.hgtools.csp2hg.HypergraphFromXCSPHelper;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import at.ac.tuwien.dbai.hgtools.sql2hg.ConjunctiveQueryFinder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Equality;
import at.ac.tuwien.dbai.hgtools.sql2hg.HypergraphBuilder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Predicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.sql2hg.ViewPredicate;
import at.ac.tuwien.dbai.hgtools.util.SqlUtils;
import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

public class MainStats {

	private static final String SEPARATOR = ";";

	private static String type;

	private static Schema schema;

	public static void main(String type, String[] args, int z) throws Exception {
		MainStats.type = type;
		if (type.equals(Main.SQL)) {
			schema = new Schema();
			String schemaString = SqlUtils.readFile(args[z++]);
			SqlUtils.readPredicateDefinitions(schemaString, schema);
		}

		System.out.println("filename;vertices;edges;degree;bip;b3ip;b4ip;vc");

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

	private static void processFiles(File[] files) throws Exception {
		for (File file : files) {
			if (file.isDirectory()) {
				// System.out.println("Directory: " + file.getName());
				processFiles(file.listFiles()); // Calls same method again.
			} else if (isFileTypeOk(file)) {
				Hypergraph h = getHypergraph(file);

				System.out.print(file.getPath() + SEPARATOR);
				System.out.print(h.cntVertices() + SEPARATOR);
				System.out.print(h.cntEdges() + SEPARATOR);
				System.out.print(h.degree() + SEPARATOR);
				System.out.print(h.cntBip(2) + SEPARATOR);
				System.out.print(h.cntBip(3) + SEPARATOR);
				System.out.print(h.cntBip(4) + SEPARATOR);
				System.out.println(h.vcDimension());
			}
		}
	}

	private static boolean isFileTypeOk(File file) {
		if (type.equals(Main.SQL)) {
			return SqlUtils.isSQLFile(file.getName());
		} else if (type.equals(Main.XCSP)) {
			return file.getName().contains("xml");
		} else if (type.equals(Main.HG)) {
			return true;
		} else {
			return false;
		}
	}

	private static Hypergraph getHypergraph(File file) throws Exception {
		if (type.equals(Main.SQL)) {
			return buildHypergraphFromSQL(file);
		} else if (type.equals(Main.XCSP)) {
			HypergraphFromXCSPHelper csp2hg = new HypergraphFromXCSPHelper(file.getPath());
			return csp2hg.getHypergraph();
		} else if (type.equals(Main.HG)) {
			return Util.hypergraphFromFile(file);
		} else {
			return new Hypergraph(); // unreachable
		}
	}

	private static Hypergraph buildHypergraphFromSQL(File file) throws JSQLParserException {
		String sqlString = SqlUtils.readFile(file.getPath());
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
		return hgBuilder.getHypergraph();
	}

}
