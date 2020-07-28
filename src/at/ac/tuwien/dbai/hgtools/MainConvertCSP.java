package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import at.ac.tuwien.dbai.hgtools.csp2hg.HypergraphFromCSPHelper;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;

public class MainConvertCSP {

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
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
				// System.out.println("Directory: " + file.getName());
				processFiles(file.listFiles()); // Calls same method again.
			} else if (file.getName().contains("xml")) {
				System.out.println("+ Converting:" + file.getPath());
				System.out.println("++ Read");
				HypergraphFromCSPHelper csp2hg = new HypergraphFromCSPHelper(file.getPath());

				Hypergraph H = csp2hg.getHypergraph();

				System.out.println("++ Output");
				String newFile = file.getPath();
				newFile = "output/" + newFile.substring(0, newFile.lastIndexOf(".")) + ".hg";

				Path newFilePath = Paths.get(newFile);
				Files.createDirectories(newFilePath.getParent());
				if (!Files.exists(newFilePath))
					Files.createFile(newFilePath);
				Files.write(Paths.get(newFile), H.toFile(), Charset.forName("UTF-8"));
			}
		}
	}

}
