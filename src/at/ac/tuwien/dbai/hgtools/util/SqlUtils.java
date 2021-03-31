package at.ac.tuwien.dbai.hgtools.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

public class SqlUtils {

    private SqlUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String readFile(String fName) {
        return SqlUtils.readFile(fName, 0, 0);
    }

    public static String readFile(String fName, int skipStart, int skipEnd) {
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

    static boolean skip(String s) {
        return s.startsWith("--") || s.trim().isEmpty();
    }

    public static void readPredicateDefinitions(String schemaString, Schema schema) throws JSQLParserException {
        Statements schemaStmts = CCJSqlParserUtil.parseStatements(schemaString);
        for (Statement schemaStmt : schemaStmts.getStatements()) {
            try {
                CreateTable tbl = (CreateTable) schemaStmt;

                String predicateName = tbl.getTable().getName();
                LinkedList<String> attributes = new LinkedList<>();
                for (ColumnDefinition cdef : tbl.getColumnDefinitions()) {
                    attributes.add(cdef.getColumnName());
                }
                schema.addPredicateDefinition(new PredicateDefinition(predicateName, attributes));
            } catch (ClassCastException c) {
                System.err.println("\"" + schemaStmt + "\" is not a CREATE statement.");
            }
        }
    }

    public static boolean isSQLFile(String filename) {
    	return filename.endsWith("sql") || filename.endsWith("tpl");
    }

}
