package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;

import at.ac.tuwien.dbai.hgtools.csp2hg.CtrTypes;
import at.ac.tuwien.dbai.hgtools.csp2hg.XCSPConstraintReader;
import at.ac.tuwien.dbai.hgtools.csp2hg.XCSPLoadInstanceException;
import at.ac.tuwien.dbai.hgtools.util.Writables;

public class Classifier {

    private static String type;

    private static int skipS = 0;
    private static int skipE = 0;
    private static boolean print = false;
    private static String outDir = "output";

    private Classifier() {
        throw new IllegalStateException("Utility class");
    }

    public static void classify(String type, String[] args, int z) throws Exception {
        Classifier.type = type;
        z = setOtherArgs(args, z);

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
            } else if (!isFileTypeOk(file)) {
                return;
            }
            classifyXCSP(file);
        }
    }

    private static void classifyXCSP(File file) throws IOException, XCSPLoadInstanceException {
        if (Main.verbose) {
            System.out.println("+ Classifying: " + file.getPath());
            System.out.println("++ Read");
        }

        XCSPConstraintReader constrReader = new XCSPConstraintReader(file.getPath());
        CtrTypes types = constrReader.getTypes();

        if (Main.verbose) {
            System.out.println("++ Output");
        }

        String cspFile = file.getPath();
        String noDirCspFile = cspFile.substring(cspFile.lastIndexOf(File.separator));
        String typesFile = outDir + File.separator + noDirCspFile + ".type";
        Writables.writeToFile(types, typesFile);

        if (print) {
            for (String t : types.toFile()) {
                System.out.println(t);
            }
        }
    }

    private static boolean isFileTypeOk(File file) {
        if (type.equals(Main.XCSP)) {
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
