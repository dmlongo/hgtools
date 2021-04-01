package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.dbai.hgtools.csp2hg.Constraints;
import at.ac.tuwien.dbai.hgtools.csp2hg.Domains;
import at.ac.tuwien.dbai.hgtools.csp2hg.HypergraphFromXCSPHelper;
import at.ac.tuwien.dbai.hgtools.csp2hg.XCSPLoadInstanceException;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import at.ac.tuwien.dbai.hgtools.sql2hg.ConjunctiveQueryFinder;
import at.ac.tuwien.dbai.hgtools.sql2hg.Equality;
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

public class Converter {

    private static String type;

    private static int skipS = 0;
    private static int skipE = 0;
    private static boolean print = false;
    private static String outDir = "output";

    private static Schema schema;

    private Converter() {
        throw new IllegalStateException("Utility class");
    }

    public static void convert(String type, String[] args, int z) throws Exception {
        Converter.type = type;
        z = setOtherArgs(args, z);

        if (type.equals(Main.SQL)) {
            schema = new Schema();
            String schemaString = SqlUtils.readFile(args[z++]);
            SqlUtils.readPredicateDefinitions(schemaString, schema);
        }

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
            } else if (isFileTypeOk(file)) {
                if (type.equals(Main.SQL)) {
                    convertSQL(file);
                } else if (type.equals(Main.XCSP)) {
                    convertXCSP(file);
                }
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

        ConjunctiveQueryFinder hgFinder = new ConjunctiveQueryFinder(schema);
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
    }

    private static void convertXCSP(File file) throws XCSPLoadInstanceException, IOException {
        if (Main.verbose) {
            System.out.println("+ Converting: " + file.getPath());
            System.out.println("++ Read");
        }

        HypergraphFromXCSPHelper csp2hg = new HypergraphFromXCSPHelper(file.getPath());
        Hypergraph hg = csp2hg.getHypergraph();
        Domains doms = csp2hg.getDomains();
        Constraints constrs = csp2hg.getConstraints();

        if (Main.verbose) {
            System.out.println("++ Output");
        }

        String hgFile = file.getPath();
        String noDirCspFile = hgFile.substring(hgFile.lastIndexOf(File.separator));
        hgFile = outDir + File.separator + noDirCspFile + ".hg";
        Writables.writeToFile(hg, hgFile);

        String domsFile = outDir + File.separator + noDirCspFile + ".dom";
        Writables.writeToFile(doms, domsFile);

        String constrsFile = outDir + File.separator + noDirCspFile + ".ctr";
        Writables.writeToFile(constrs, constrsFile);

        if (print) {
            for (String e : hg.toFile()) {
                System.out.println(e);
            }
        }
    }

    private static boolean isFileTypeOk(File file) {
        if (type.equals(Main.SQL)) {
            return SqlUtils.isSQLFile(file.getName());
        } else if (type.equals(Main.XCSP)) {
            return file.getName().contains("xml");
        } else {
            return false;
        }
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
