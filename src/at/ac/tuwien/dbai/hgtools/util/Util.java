package at.ac.tuwien.dbai.hgtools.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import at.ac.tuwien.dbai.hgtools.hypergraph.Edge;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;
import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;

public class Util {

	private Util() {
		throw new IllegalStateException("Utility class");
	}

	public static <T> T[] shiftLeftResize(T[] v, int k) {
		for (int i = 0; i < v.length - k; i++) {
			v[i] = v[i + k];
		}
		return Arrays.copyOf(v, v.length - k);
	}

	public static void writeToFile(String filename, Statement query) throws IOException {
		Path path = Paths.get(filename);
		if (!Files.exists(path))
			Files.createFile(path);
		Files.write(Paths.get(filename), toFile(query), StandardCharsets.UTF_8);
	}

	public static void writeToFile(String filename, Schema schema) throws IOException {
		Path path = Paths.get(filename);
		if (!Files.exists(path))
			Files.createFile(path);
		Files.write(Paths.get(filename), toFile(schema), StandardCharsets.UTF_8);
	}

	private static Iterable<String> toFile(Schema schema) {
		LinkedList<String> res = new LinkedList<>();
		for (PredicateDefinition p : schema) {
			StringBuilder sb = new StringBuilder(200);
			sb.append("CREATE TABLE ");
			sb.append(p.getName());
			sb.append('(');
			sb.append('\n');
			Iterator<String> it = p.getAttributes().iterator();
			while (it.hasNext()) {
				String attr = it.next();
				sb.append('\t');
				sb.append(attr);
				sb.append('\t');
				sb.append("int");
				if (it.hasNext()) {
					sb.append(',');
				}
				sb.append('\n');
			}
			sb.append(')');
			sb.append(';');
			sb.append('\n');
			sb.append('\n');
			res.add(sb.toString());
		}
		return res;
	}

	private static Iterable<String> toFile(Statement query) {
		LinkedList<String> res = new LinkedList<>();
		res.add(query.toString());
		return res;
	}

	public static String makeNextFilename(String outDir, String name, int curr, int tot) {
		String root = name.substring(0, name.lastIndexOf('.'));
		String ext = name.substring(name.lastIndexOf('.') + 1);
		String id = getID(curr, tot);
		return outDir + File.separator + root + "_" + id + "." + ext;
	}

	private static String getID(int curr, int size) {
		int id = curr;
		int dTot = 1;
		int dR = 1;
		int tot;
		int r;
		for (tot = size / 10; tot != 0; tot /= 10, dTot++)
			;
		for (r = id / 10; r != 0; r /= 10, dR++)
			;
		StringBuilder prefix = new StringBuilder(dTot - dR);
		for (int i = 0; i < (dTot - dR); i++) {
			prefix.append('0');
		}
		return prefix.toString() + id;
	}

	public static <T> List<T> deepCopy(List<T> list) {
		LinkedList<T> copy = new LinkedList<>();
		for (T e : list) {
			copy.add(e);
		}
		return copy;
	}

	public static void print(Object... objs) {
		for (Object o : objs) {
			if (o instanceof Object[]) {
				System.out.println(Arrays.toString((Object[]) o));
			} else if (o instanceof int[]) {
				System.out.println(Arrays.toString((int[]) o));
			} else {
				System.out.println(o);
			}
		}
		System.out.println();
	}

	public static String getTableAliasName(Table table) {
		String tableAliasName;
		if (table.getAlias() != null)
			tableAliasName = table.getAlias().getName();
		else
			tableAliasName = table.getName();
		return tableAliasName;
	}

	public static void loadSimpleMap(Map<String, String> map, String file, boolean keyToLowerCase,
			boolean valueToLowerCase) {
		try (Stream<String> stream = Files.lines(Paths.get(file))) {
			stream.forEach(new Consumer<String>() {
				@Override
				public void accept(String s) {
					List<String> tks = splitCSVLine(s);
					if (tks.size() != 2) {
						String fError = "Error in file: " + file + "\n";
						String lError = "Wrong line: " + s + "\n";
						String tError = "tks= " + tks;
						throw new RuntimeException(fError + lError + tError);
					}
					String tab = tks.get(0);
					if (keyToLowerCase) {
						tab = tab.toLowerCase();
					}
					if (map.containsKey(tab)) {
						throw new RuntimeException(tab + " already present in map");
					}
					String val = tks.get(1);
					if (valueToLowerCase) {
						val = val.toLowerCase();
					}
					map.put(tab, val);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<String> splitCSVLine(String line) {
		ArrayList<String> tks = new ArrayList<>();
		boolean inQuotes = false;
		int start = 0;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '"') {
				inQuotes = !inQuotes;
			} else if (!inQuotes && line.charAt(i) == ';') {
				String tk = line.substring(start, i);
				tks.add(tk);
				start = i + 1;
			}
		}
		tks.add(line.substring(start));
		return tks;
	}

	public static void loadSimpleMap(Map<String, String> map, String file) {
		loadSimpleMap(map, file, false, false);
	}

	public static void loadListMap(Map<String, List<String>> map, String file, boolean keyToLowerCase,
			boolean valueToLowerCase) {
		try (Stream<String> stream = Files.lines(Paths.get(file))) {
			stream.forEach(new Consumer<String>() {
				@Override
				public void accept(String s) {
					List<String> tks = splitCSVLine(s);
					String tab = tks.get(0);
					if (keyToLowerCase) {
						tab = tab.toLowerCase();
					}
					map.putIfAbsent(tab, new LinkedList<>());
					for (String tk : tks) {
						String val = tk;
						if (valueToLowerCase) {
							val = val.toLowerCase();
						}
						map.get(tab).add(val);
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void loadListMap(Map<String, List<String>> map, String file) {
		loadListMap(map, file, false, false);
	}

	public static <T> Set<T> setDifference(Set<T> set1, Set<T> set2) {
		HashSet<T> res = new HashSet<>();
		res.addAll(set1);
		res.retainAll(set2);
		return res;
	}

	public static <T> Set<T> setIntersection(Set<T> set1, Set<T> set2) {
		HashSet<T> res = new HashSet<>();
		for (T el : set1) {
			if (set2.contains(el)) {
				res.add(el);
			}
		}
		return res;
	}

	public static boolean isContained(PredicateDefinition p1, PredicateDefinition p2) {
		boolean flag = true;
		for (String attr : p1) {
			if (!(p2.existsAttribute(attr) || p2.existsAttribute(addQuote(attr)))) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	public static PredicateDefinition getPred(Set<PredicateDefinition> preds, String name) {
		for (PredicateDefinition p : preds) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	public static boolean isCovered(String c, Set<PredicateDefinition> preds) {
		for (PredicateDefinition p : preds) {
			if (p.existsAttribute(c) || p.existsAttribute(addQuote(c))) {
				return true;
			}
		}
		return false;
	}

	public static String addQuote(String s) {
		StringBuilder sb = new StringBuilder(s.length() + 2);
		sb.append('"');
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != '"') {
				sb.append(s.charAt(i));
			}
		}
		sb.append('"');
		return sb.toString();
	}

	public static Hypergraph hypergraphFromFile(File file) throws IOException {
		Hypergraph h = new Hypergraph();
		for (String edge : getEdgeList(file)) {
			int idStart = edge.indexOf('(');
			int idEnd = edge.indexOf(')');
			String edgeName = edge.substring(0, idStart);
			String vars = edge.substring(idStart + 1, idEnd);
			Edge e = new Edge(edgeName, vars.split(","));
			h.addEdge(e);
		}
		return h;
	}

	private static List<String> getEdgeList(File file) throws IOException {
		String hg = String.join("", Files.readAllLines(file.toPath()));

		ArrayList<String> tks = new ArrayList<>();
		boolean inPar = false;
		int start = 0;
		for (int i = 0; i < hg.length(); i++) {
			if (hg.charAt(i) == '(') {
				inPar = true;
			} else if (hg.charAt(i) == ')') {
				inPar = false;
			} else if (!inPar && hg.charAt(i) == ',') {
				String tk = hg.substring(start, i);
				tks.add(tk);
				start = i + 1;
			}
		}
		tks.add(hg.substring(start));

		return tks;
	}

}
