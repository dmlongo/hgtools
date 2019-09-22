package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.tukaani.xz.LZMAInputStream;
import org.w3c.dom.Document;
import org.xcsp.common.Types.TypeFlag;
import org.xcsp.parser.callbacks.XCallbacks2;
import org.xcsp.parser.entries.XVariables.XVarInteger;

import at.ac.tuwien.dbai.hgtools.hypergraph.Edge;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;

public class HypergraphFromCSPHelper implements XCallbacks2 {
	private Implem implem = new Implem(this);
	private Map<XVarInteger, String> mapVar = new LinkedHashMap<>();
	private Hypergraph H = new Hypergraph();
	private int iEdge = 0;

	public HypergraphFromCSPHelper(String filename) throws Exception {
		loadInstance(filename);
	}

	@Override
	public Implem implem() {
		return implem;
	}

	public Hypergraph getHypergraph() {
		return H;
	}

	@Override
	public Document loadDocument(String fileName) throws Exception {
		if (fileName.endsWith("xml.lzma")) {
			LZMAInputStream input = new LZMAInputStream(new BufferedInputStream(new FileInputStream(fileName)));

			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
			return document;
		} else if (fileName.endsWith(".xml")) {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new FileInputStream(new File(fileName)));
		} else
			return null;
	}

	@Override
	public void buildVarInteger(XVarInteger xx, int minValue, int maxValue) {
		String x = xx.id;
		mapVar.put(xx, x);
	}

	@Override
	public void buildVarInteger(XVarInteger xx, int[] values) {
		String x = xx.id;
		mapVar.put(xx, x);
	}

	private String trVar(Object x) {
		return mapVar.get((XVarInteger) x);
	}

	private String[] trVars(Object vars) {
		return Arrays.stream((XVarInteger[]) vars).map(x -> mapVar.get(x)).toArray(String[]::new);
	}

	private String[][] trVars2D(Object vars) {
		return Arrays.stream((XVarInteger[][]) vars).map(t -> trVars(t)).toArray(String[][]::new);
	}

	@Override
	public void buildCtrExtension(String id, XVarInteger[] list, int[][] tuples, boolean positive,
			Set<TypeFlag> flags) {
		// Arrays.stream(trVars(list)).forEach(t -> System.out.print(t));
		// System.out.println();
		H.addEdge(new Edge("E" + ++iEdge, trVars(list)));
	}

}
