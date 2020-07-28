package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.TreeMap;

import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;

public class MainCountStmts {

	private static int skipS = 0;
	private static int skipE = 0;
	private static String outDir = "output";
	private static boolean split = false;

	private static String query;
	private static TreeMap<String, Integer> out;
	private static LinkedList<String> err;

	public static void main(String[] args) throws IOException {
		args = setOtherArgs(args);

		query = args[0];
		out = new TreeMap<>();
		err = new LinkedList<>();

		File file = new File(query);
		File[] files;
		if (file.isDirectory()) {
			files = file.listFiles();
		} else {
			files = new File[1];
			files[0] = file;
		}
		processFiles(files);

		String outFile = outDir + File.separator + "count_stmts.txt";
		Files.write(Paths.get(outFile), toFile(out), Charset.forName("UTF-8"));
		String errFile = outDir + File.separator + "count_stmts_err.txt";
		Files.write(Paths.get(errFile), err, Charset.forName("UTF-8"));
	}

	private static Iterable<String> toFile(TreeMap<String, Integer> map) {
		LinkedList<String> res = new LinkedList<>();
		for (String key : map.keySet()) {
			String line = key + ";" + map.get(key);
			res.add(line);
		}
		return res;
	}

	private static void processFiles(File[] files) throws IOException {
		for (File file : files) {
			if (file.isDirectory()) {
				// System.out.println("Directory: " + file.getName());
				processFiles(file.listFiles()); // Calls same method again.
			} else if (Util.isSQLFile(file.getName())) {
				query = file.getName();
				System.out.println("Processing " + query);

				String sqlString = Util.readSQLFile(file.getPath(), skipS, skipE);
				try {
					Statements stmts = CCJSqlParserUtil.parseStatements(sqlString);
					int n = stmts.getStatements().size();
					out.put(query, n);
					if (split && n > 1) {
						for (int i = 0; i < stmts.getStatements().size(); i++) {
							Statement s = stmts.getStatements().get(i);

							int dot = query.lastIndexOf('.');
							String name = query.substring(0, dot);
							String ext = query.substring(dot);
							String filename = outDir + File.separator + name + "_" + i + ext;
							Util.writeToFile(filename, s);
						}
					}
				} catch (JSQLParserException e) {
					err.add(query);
				}
			}
		}
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
			case "-split":
				split = true;
				args = Util.shiftLeftResize(args, 1);
				break;
			default:
				throw new RuntimeException("Unknown command: " + cmd);
			}
		}
		return args;
	}
}
