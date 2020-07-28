package at.ac.tuwien.dbai.hgtools;

import java.io.File;

import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

public class MainCheckParser {

	private static String query;

	public static void main(String[] args) throws JSQLParserException {
		query = args[0];
		File file = new File(query);
		File[] files;
		if (file.isDirectory()) {
			files = file.listFiles();
		} else {
			files = new File[1];
			files[0] = file;
		}
		processFiles(files);
	}

	private static void processFiles(File[] files) throws JSQLParserException {
		for (File file : files) {
			if (file.isDirectory()) {
				// System.out.println("Directory: " + file.getName());
				processFiles(file.listFiles()); // Calls same method again.
			} else if (Util.isSQLFile(file.getName())) {
				query = file.getName();
				System.out.println("Processing " + query);

				String sqlString = Util.readSQLFile(file.getPath());
				CCJSqlParserUtil.parseStatements(sqlString);
			}
		}
	}

}
