package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;

import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import at.ac.tuwien.dbai.hgtools.util.Util;

public class MainStats {

	private static String hgFile;

	public static void main(String[] args) throws IOException {
		System.out.println("filename,vertices,edges,arity,degree,bip,b3ip,b4ip,vc");

		hgFile = args[0];
		File file = new File(hgFile);
		File[] files;
		if (file.isDirectory()) {
			files = file.listFiles();
		} else {
			files = new File[1];
			files[0] = file;
		}
		processFiles(files);
	}

	private static void processFiles(File[] files) throws IOException {
		for (File file : files) {
			if (file.isDirectory()) {
				//System.out.println("Directory: " + file.getName());
				processFiles(file.listFiles()); // Calls same method again.
			} else {
				//System.out.println("Processing " + file.getName());

				String sep = ",";
				Hypergraph h = Util.hypergraphFromFile(file);
				//System.out.println(h);
				System.out.print(file.getPath() + sep);
				System.out.print(h.cntVertices() + sep);
				System.out.print(h.cntEdges() + sep);
				System.out.print(h.arity() + sep);
				System.out.print(h.degree() + sep);
				System.out.print(h.cntBip(2) + sep);
				System.out.print(h.cntBip(3) + sep);
				System.out.print(h.cntBip(4) + sep);
				System.out.println(h.VCdimension());
				//System.out.println(h.VCdimension() + sep + "1");
			}
		}
	}

}
