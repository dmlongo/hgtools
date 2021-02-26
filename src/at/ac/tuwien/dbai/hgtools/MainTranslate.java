package at.ac.tuwien.dbai.hgtools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import at.ac.tuwien.dbai.hgtools.hypergraph.Edge;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;

public class MainTranslate {

	public static void main(String type, String[] args, int z) {
		if (type.equals(Main.H2P)) {
			translateHgToPace(args[z]);
		} else {
			throw new Main.UnsupportedCommandException(type);
		}
	}

	private static void translateHgToPace(String p) {
		Hypergraph hg = new Hypergraph();

		Path path = Paths.get(p);
		Charset charset = Charset.defaultCharset();
		try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split("[(),.]");
				Edge e = new Edge(split[0]);
				for (int i = 1; i < split.length; i++) {
					e.addVertex(split[i]);
				}
				hg.addEdge(e);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
			System.exit(1);
		}

		String outFile = p.substring(0, p.indexOf('.')) + ".pace";
		path = Paths.get(outFile);
		try (BufferedWriter writer = Files.newBufferedWriter(path, charset)) {
			for (String s : hg.toPaceFile()) {
				writer.write(s);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
			System.exit(1);
		}
	}

}
