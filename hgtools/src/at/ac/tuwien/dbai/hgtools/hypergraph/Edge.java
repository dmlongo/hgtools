package at.ac.tuwien.dbai.hgtools.hypergraph;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.util.Util;

public class Edge {
	private String name;
	/** Vertices are ordered in every edge. */
	private ArrayList<String> vertices;

	public Edge(String name) {
		this.name = name;
		vertices = new ArrayList<String>(50);
	}

	public Edge(String name, String[] strings) {
		this.name = name;
		vertices = new ArrayList<String>(strings.length);
		for (String s : strings) {
			vertices.add(s);
		}
	}

	public String getName() {
		return name;
	}

	public boolean contains(String v) {
		return vertices.contains(v);
	}

	public void addVertex(String v) {
		vertices.add(v);
	}

	public void renameVertex(String v, String newName) {
		int pos = vertices.indexOf(v);
		vertices.set(pos, newName);
	}

	public List<String> getVertices() {
		return vertices;
	}

	public String toString() {
		String s = "";

		s += Util.stringify(name) + "(";
		for (String v : vertices) {
			s += Util.stringify(v) + ",";
		}
		s = s.substring(0, s.length() - 1);
		s += ")";

		return s;
	}

}
