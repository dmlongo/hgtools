package at.ac.tuwien.dbai.hgtools.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Select;

public class Util {

	public static String readSQLFile(String fName, int skipStart, int skipEnd) {
		List<String> lines = new LinkedList<>();
		StringBuilder sb = new StringBuilder(500);
		try (BufferedReader br = new BufferedReader(new FileReader(fName))) {
			for (int i = 0; i < skipStart; i++) {
				br.readLine();
			}

			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				if (!skip(sCurrentLine)) {
					lines.add(sCurrentLine);
				}
			}
			if (lines.size() - skipEnd > 0) {
				lines = lines.subList(0, lines.size() - skipEnd);
			} else {
				lines.clear();
			}

			for (String l : lines) {
				sb.append(l);
				sb.append('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	private static boolean skip(String s) {
		return s.startsWith("--") || s.trim().isEmpty();
	}

	public static String readSQLFile(String fName) {
		return readSQLFile(fName, 0, 0);
	}

	public static void readSQLPredicateDefinitions(String schemaString, Schema schema) throws JSQLParserException {
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

	public static boolean isSQLFile(String filename) {
		/*
		 * TODO create a regex containing all possible extensions and then check if the
		 * name matches it
		 */
		return filename.endsWith("sql") || filename.endsWith("tpl");
	}

	/**
	 * Removes angular brackets from vertex names
	 * 
	 * @param s A string s to be stringified
	 * @return A stringified String
	 */
	public static String stringify(String s) {
		String newS = new String(s);

		newS = newS.replace('[', 'L');
		newS = newS.replace(']', 'J');

		return newS;
	}

	public static <T> T[] shiftLeftResize(T[] v, int k) {
		for (int i = 0; i < v.length - k; i++) {
			v[i] = v[i + k];
		}
		return Arrays.copyOf(v, v.length - k);
	}

	public static void writeToFile(File file, Select query, String outDir, int currQuery, int numQueries)
			throws IOException {
		String newFile = file.getName();
		newFile = nextOutName(outDir, newFile, currQuery, numQueries);
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

	private static String nextOutName(String outDir, String name, int curr, int tot) {
		String root = name.substring(0, name.lastIndexOf('.'));
		String ext = name.substring(name.lastIndexOf('.') + 1);
		String id = getID(curr, tot);
		return outDir + File.separator + root + "_" + id + "." + ext;
	}

	private static String getID(int curr, int size) {
		int id = curr;
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

	public static <T> LinkedList<T> deepCopy(List<T> list) {
		LinkedList<T> copy = new LinkedList<>();
		for (T e : list) {
			copy.add(e);
		}
		return copy;
	}
	
	public static String getTableAliasName(Table table) {
		String tableAliasName;
		if (table.getAlias() != null)
			tableAliasName = table.getAlias().getName();
		else
			tableAliasName = table.getName();
		return tableAliasName;
	}
	
}
