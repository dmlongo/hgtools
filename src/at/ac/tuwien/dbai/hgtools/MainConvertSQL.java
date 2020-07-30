package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import at.ac.tuwien.dbai.hgtools.sql2hg.ConjunctiveQueryFinder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Equality;
import at.ac.tuwien.dbai.hgtools.sql2hg.HypergraphBuilder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Predicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.sql2hg.ViewPredicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.sqlshare.ToLowerCaseTransformer;
import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

public class MainConvertSQL {

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
				Statement stmt = CCJSqlParserUtil.parse(sqlString);
				Select selectStmt = (Select) stmt;

				// make name lower case
				ToLowerCaseTransformer lc = new ToLowerCaseTransformer();
				lc.run(selectStmt);

				// ViewPredicatesFinder vpf = new ViewPredicatesFinder();
				// Collection<Predicate> viewPreds = vpf.getViewPredicates(selectStmt);
				// ViewDefinitionsFinder vdf = new ViewDefinitionsFinder(schema, viewPreds,
				// selectStmt);
				// vdf.getUpdateSchema();
				// CQFinder hgFinder = new CQFinder(schema);
				// hgFinder.find(selectStmt);

				/*
				 * for (Predicate pred : viewPreds) { PredicateDefinition predDef =
				 * pred.getPredicateDefinition(); if (pred instanceof BasePredicate) {
				 * schema.addPredicateDefinition(predDef); } else if (pred instanceof
				 * ViewPredicate) { schema.addPredicateDefinition(predDef, new
				 * ViewPredicate(predDef)); } }
				 */

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
				HashMap<String, List<String>> map = hgBuilder.getVarToColMapping();
				// System.out.println();
				// System.out.println(map);
				// System.out.println();
				System.out.println(h);

				String newFile = file.getAbsolutePath();
				int startIdx = newFile.lastIndexOf(File.separator);
				int endIdx = newFile.lastIndexOf('.');
				String fileBaseName = newFile.substring(startIdx, endIdx);
				newFile = outDir + File.separator + fileBaseName + ".hg";
				String newFileMap = outDir + File.separator + fileBaseName + ".map";
				Path newFilePath = Paths.get(newFile);
				Path newFileMapPath = Paths.get(newFileMap);
				Files.createDirectories(newFilePath.getParent());
				if (!Files.exists(newFilePath))
					Files.createFile(newFilePath);
				if (!Files.exists(newFileMapPath))
					Files.createFile(newFileMapPath);
				Files.write(Paths.get(newFile), h.toFile(), Charset.forName("UTF-8"));
				Files.write(Paths.get(newFileMap), toFile(map), Charset.forName("UTF-8"));
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
