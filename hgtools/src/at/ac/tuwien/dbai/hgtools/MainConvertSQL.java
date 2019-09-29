package at.ac.tuwien.dbai.hgtools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import at.ac.tuwien.dbai.hgtools.sql2hg.ConjunctiveQueryFinder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Equality;
import at.ac.tuwien.dbai.hgtools.sql2hg.HypergraphBuilder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Predicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.sql2hg.ViewPredicate;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Select;

public class MainConvertSQL {

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
