package at.ac.tuwien.dbai.hgtools.hypergraph;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.Util.Util;
import lombok.Data;

@Data
public class Edge {
	private String name;
	private List<String> vertices;

	public Edge() {
		vertices = new ArrayList<String>(50);
	}

	public Edge(String name, String[] strings) {
		this.name = name;
		vertices = new ArrayList<String>(50);
		for (String s : strings) {
			vertices.add(s);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean contains(String v) {
		return vertices.contains(v);
	}

	public void addVertex(String v) {
		vertices.add(v);
	}

	public String toString() {
		String s = "";

		s += Util.stringify(name) + "(";
		for (String v : vertices)
			s += Util.stringify(v) + ",";
		s = s.substring(0, s.length() - 1);
		s += ")";

		return s;
	}

}
