package at.ac.tuwien.dbai.hgtools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statements;

public class MainTest {

	public static void main(String[] args) throws JSQLParserException {
		String query = readFile(args[0]);
		Statements stmt = CCJSqlParserUtil.parseStatements(query);
		System.out.println(stmt);
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
