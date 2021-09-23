package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import at.ac.tuwien.dbai.hgtools.sql2hg.Equality;
import at.ac.tuwien.dbai.hgtools.sql2hg.GeneralQueryFinder;
import at.ac.tuwien.dbai.hgtools.sql2hg.HypergraphBuilder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Predicate;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.sql2hg.ToLowerCaseTransformer;
import at.ac.tuwien.dbai.hgtools.sql2hg.ViewPredicate;
import at.ac.tuwien.dbai.hgtools.util.SqlUtils;
import at.ac.tuwien.dbai.hgtools.util.Writables;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

public class ConvEval {

    private static String type;

    private static int skipS = 0;
    private static int skipE = 0;
    private static boolean print = false;
    private static String outDir = "output";

    private static Schema schema;

    private ConvEval() {
        throw new IllegalStateException("Utility class");
    }

    public static void convEval(String type, String[] args, int z) throws Exception {
        ConvEval.type = type;
        z = setOtherArgs(args, z);

        schema = new Schema();
        String schemaString = SqlUtils.readFile(args[z++]);
        SqlUtils.readPredicateDefinitions(schemaString, schema);

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

    public static void processFiles(File[] files) throws Exception {
        for (File file : files) {
            if (file.isDirectory()) {
                processFiles(file.listFiles()); // Calls same method again.
            } else if (SqlUtils.isSQLFile(file.getName())) {
                convertSQL(file);
            }
        }
    }

    private static void convertSQL(File file) throws IOException, JSQLParserException {
        String sqlString = SqlUtils.readFile(file.getPath(), skipS, skipE);
        Statement stmt = CCJSqlParserUtil.parse(sqlString);
        Select selectStmt = (Select) stmt;

        // make name lower case
        ToLowerCaseTransformer lc = new ToLowerCaseTransformer();
        lc.run(selectStmt);

        // TODO view expansion

        GeneralQueryFinder hgFinder = new GeneralQueryFinder(schema);
        hgFinder.run(selectStmt);

        HypergraphBuilder hgBuilder = new HypergraphBuilder();
        for (Predicate table : hgFinder.getTables()) {
            hgBuilder.buildEdge(table);
        }
        for (Predicate table : hgFinder.getTables()) {
            if (table instanceof ViewPredicate) {
                ViewPredicate view = (ViewPredicate) table;
                for (Equality join : view.getJoins()) {
                    hgBuilder.buildViewJoin(view.getAlias(), join);
                }
            }
        }
        for (Equality join : hgFinder.getJoins()) {
            hgBuilder.buildJoin(join);
        }
        Hypergraph hg = hgBuilder.getHypergraph();
        Map<String, List<String>> map = hgBuilder.getVarToColMapping();
        System.out.println(hg);

        String hgFile = file.getAbsolutePath();
        int startIdx = hgFile.lastIndexOf(File.separator);
        int endIdx = hgFile.lastIndexOf('.');
        String fileBaseName = hgFile.substring(startIdx, endIdx);
        hgFile = outDir + File.separator + fileBaseName + ".hg";
        Writables.writeToFile(hg, hgFile);

        String mapFile = outDir + File.separator + fileBaseName + ".map";
        Writables.writeToFile(map, mapFile);

        String filtersFile = outDir + File.separator + fileBaseName + ".flt";
        List<String> mappedFilters = substituteFlt(hgFinder.getFilters(), hgBuilder.getColToVarMapping());
        Writables.writeToFile(mappedFilters, filtersFile);

        String selectsFile = outDir + File.separator + fileBaseName + ".sel";
        List<String> mappedSelects = substituteSel(hgFinder.getSelects(), hgBuilder.getColToVarMapping());
        Writables.writeToFile(mappedSelects, selectsFile);
    }

    private static List<String> substituteFlt(List<String> filters, Map<String, String> colToVar) {
        ArrayList<String> res = new ArrayList<>(filters.size());
        for (String colFilt : filters) {
            String[] tks = colFilt.split(";");
            for (int i = 0; i < tks.length - 1; i++) {
                String modCol = tks[i].replace('.', '_');
                res.add(colFilt.replace(tks[i], colToVar.get(modCol)));
            }
        }
        return res;
    }

    private static List<String> substituteSel(List<String> selects, Map<String, String> colToVar) {
        ArrayList<String> res = new ArrayList<>(selects.size());
        for (String colSel : selects) {
            String[] tks = colSel.split(";");
            if (tks.length > 1) {
                String sel = tks[tks.length - 1];
                for (int i = 0; i < tks.length - 1; i++) {
                    String modCol = tks[i].replace('.', '_');
                    res.add(sel.replace(tks[i], colToVar.get(modCol)));
                }
            } else {
                res.add(colSel);
            }
        }
        return res;
    }

    private static int setOtherArgs(String[] args, int z) {
        while (args[z].startsWith("-")) {
            String cmd = args[z++];
            switch (cmd) {
                case "-skip":
                    skipS = Integer.parseInt(args[z++]);
                    skipE = Integer.parseInt(args[z++]);
                    break;
                case "-print":
                    print = true;
                    break;
                case "-out":
                    outDir = args[z++];
                    break;
                default:
                    throw new Main.UnsupportedCommandException(cmd);
            }
        }
        return z;
    }

}
