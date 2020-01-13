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
import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

public class MainConvertSQL {

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
				System.out.println(map);
				System.out.println();
				System.out.println(h);

				String newFile = file.getPath();
				String fileBaseName = newFile.substring(0, newFile.lastIndexOf("."));
				newFile = "output/" + fileBaseName + ".hg";
				String newFileMap = "output/" + fileBaseName + ".map";
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

}
