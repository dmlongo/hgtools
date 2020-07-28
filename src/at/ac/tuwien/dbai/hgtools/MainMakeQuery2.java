package at.ac.tuwien.dbai.hgtools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.sql2hg.sqlshare.PredicateBuilder;
import at.ac.tuwien.dbai.hgtools.sql2hg.sqlshare.SchemaWithDuplicates;
import at.ac.tuwien.dbai.hgtools.sql2hg.sqlshare.TableNamesFinder;
import at.ac.tuwien.dbai.hgtools.util.Util;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.WithItem;

public class MainMakeQuery2 {

	private static int skipS = 0;
	private static int skipE = 0;
	private static String outDir = "output";

	private static String query;
	private static String viewDir;

	private static HashMap<String, LinkedList<String>> tab2users = new HashMap<>();
	private static HashMap<String, LinkedList<String>> user2tabs = new HashMap<>();
	private static HashMap<String, String> view2file = new HashMap<>();
	private static HashMap<String, String> file2view = new HashMap<>();

	private static HashSet<String> allUsers = new HashSet<>();
	private static HashMap<String, Integer> numTablesOf = new HashMap<>();

	private static class Record {
		public Record(Statement stmt) {
			this.stmt = stmt;
		}

		Statement stmt = null;
		PredicateDefinition mySignature = null;

		HashSet<String> users = new HashSet<>();
		HashSet<String> certainTables = new HashSet<>();
		HashSet<String> ambiguousTables = new HashSet<>();
		HashSet<String> undefinedTables = new HashSet<>();
		HashSet<String> viewTables = new HashSet<>();

		HashSet<PredicateDefinition> preds = null;
		Collection<PredicateBuilder> subPreds = null;
		HashSet<PredicateDefinition> ambiguousPreds = new HashSet<>();
		HashSet<PredicateDefinition> undefinedPreds = new HashSet<>();
		HashSet<PredicateDefinition> viewPreds = new HashSet<>();
		HashSet<String> unknownColumns = null;
	}

	private static ArrayList<Record> recs = new ArrayList<>();

	public static void main(String[] args) throws JSQLParserException, IOException {
		args = setOtherArgs(args);

		query = args[0];
		viewDir = args[1];
		String view2fileName = args[2];
		String file2viewName = args[3];
		String tab2userName = args[4];
		String user2tabName = args[5];
		String fullSchemaName = args[6];

		Util.loadListMap(tab2users, tab2userName);
		Util.loadListMap(user2tabs, user2tabName);
		Util.loadSimpleMap(view2file, view2fileName);
		Util.loadSimpleMap(file2view, file2viewName);

		SchemaWithDuplicates schema = new SchemaWithDuplicates();
		String schemaString = Util.readSQLFile(fullSchemaName);
		Util.readSQLPredicateDefinitions(schemaString, schema);

		File file = new File(query);
		File[] files;
		if (file.isDirectory()) {
			files = file.listFiles();
		} else {
			files = new File[1];
			files[0] = file;
		}
		processFiles(files, schema);
	}

	private static void processFiles(File[] files, SchemaWithDuplicates schema)
			throws JSQLParserException, IOException {
		for (File file : files) {
			if (file.isDirectory()) {
				// System.out.println("Directory: " + file.getName());
				processFiles(file.listFiles(), schema); // Calls same method again.
			} else if (Util.isSQLFile(file.getName())) {
				String sqlString = Util.readSQLFile(file.getPath(), skipS, skipE);
				Statements stmts = CCJSqlParserUtil.parseStatements(sqlString);
				Select sel = (Select) stmts.getStatements().get(0);

				TableNamesFinder tabFinder = new TableNamesFinder();
				PredicateBuilder pb = new PredicateBuilder();
				HashSet<String> visited = new HashSet<>();
				recs.add(new Record(sel));
				for (int i = 0; i < recs.size(); i++) {
					Record r = recs.get(i);
					HashSet<String> tables = tabFinder.findTables(sel);
					processRecord(r, tables);
					r.preds = pb.buildPredicates(r.stmt);
					r.subPreds = pb.getSubPredicates();
					r.unknownColumns = pb.getUnknowncolumns();
					r.mySignature = pb.getMySignature();

					for (String view : r.viewTables) {
						if (!visited.contains(view)) {
							CreateView vStmt = getView(view);
							recs.add(new Record(vStmt));
						}
					}
					visited.addAll(r.viewTables);
				}

				// TODO elabora i subPreds a un certo punto
				
				expandPartialPredicates(schema);
				resolveAmbiguities(schema);
				clean();

				int k = query.lastIndexOf(File.separator);
				if (k >= 0) {
					query = query.substring(k + 1);
				}
				Select s = (Select) recs.get(0).stmt;
				LinkedList<WithItem> items = new LinkedList<>();
				items.addAll(s.getWithItemsList());
				for (int i = 1; i < recs.size(); i++) {
					CreateView cv = (CreateView) recs.get(i).stmt;
					WithItem item = makeWithItem(cv);
					items.add(item);
				}
				s.setWithItemsList(items);
				String qOut = "complete_" + query;
				System.out.println(qOut);
				Util.writeToFile(qOut, s);

				HashSet<PredicateDefinition> allPreds = new HashSet<PredicateDefinition>();
				for (Record r : recs) {
					allPreds.addAll(r.preds);
					allPreds.addAll(r.undefinedPreds);
				}
				Schema outSchema = new Schema();
				for (PredicateDefinition p : allPreds) {
					if (p.arity() == 0) {
						String name = p.getName();
						LinkedList<String> attrs = new LinkedList<>();
						attrs.add("\"\"");
						p = new PredicateDefinition(name, attrs);
					}
					outSchema.addPredicateDefinition(p);
				}
				String schOut = "schema_" + query;
				System.out.println(schOut);
				Util.writeToFile(schOut, outSchema);
			}
		}
	}

	private static void clean() {
		HashSet<PredicateDefinition> views = new HashSet<>();
		for (int i = 1; i < recs.size(); i++) {
			views.add(recs.get(i).mySignature);
		}
		for (Record r : recs) {
			for (String v : r.viewTables) {
				PredicateDefinition pv = Util.getPred(views, v);
				if (pv != null) {
					r.viewPreds.add(pv);
				}
			}
			updateColSet(r.unknownColumns, r.viewPreds);
			if (r.unknownColumns.size() > 0) {
				throw new RuntimeException("There are still unknown columns: " + r.unknownColumns + "\nin: " + r.stmt);
			}
			updateSet(r.undefinedPreds, r.viewTables);
		}
	}

	private static void updateSet(HashSet<PredicateDefinition> preds, HashSet<String> predNames) {
		Iterator<PredicateDefinition> it = preds.iterator();
		while (it.hasNext()) {
			if (predNames.contains(it.next().getName())) {
				it.remove();
			}
		}
	}

	private static void updateColSet(HashSet<String> unknownColumns, HashSet<PredicateDefinition> preds) {
		Iterator<String> it = unknownColumns.iterator();
		while (it.hasNext()) {
			String uc = it.next();
			if (Util.isCovered(uc, preds)) {
				it.remove();
			}
		}
	}

	private static void expandPartialPredicates(SchemaWithDuplicates schema) {
		for (Record r : recs) {
			HashSet<PredicateDefinition> newPreds = new HashSet<>();
			for (PredicateDefinition p : r.preds) {
				LinkedList<PredicateDefinition> candidates = computeCandidates(p, schema);
				if (candidates.size() == 1) {
					newPreds.add(candidates.getFirst());
				} else if (candidates.size() > 1) {
					r.ambiguousPreds.add(p);
				} else { // candidates.size() == 0
					r.undefinedPreds.add(p);
				}
			}
			r.preds = newPreds;
			updateUnknownColumns(r);
		}
	}

	private static void updateUnknownColumns(Record r) {
		Iterator<String> it = r.unknownColumns.iterator();
		while (it.hasNext()) {
			String col = it.next();
			for (PredicateDefinition p : r.preds) {
				if (p.existsAttribute(col)) {
					it.remove();
					break;
				}
			}
		}
	}

	private static void resolveAmbiguities(SchemaWithDuplicates schema) {
		HashSet<String> certainUsers = new HashSet<>();
		for (Record r : recs) {
			certainUsers.addAll(r.users);
		}

		for (Record r : recs) {
			Iterator<PredicateDefinition> it = r.ambiguousPreds.iterator();
			while (it.hasNext()) {
				PredicateDefinition uStub = it.next();
				LinkedList<PredicateDefinition> candidates = computeCandidates(uStub, schema);
				if (candidates.size() <= 1) {
					throw new RuntimeException("Unexpected number of candidates for " + uStub + ": " + candidates);
				}
				PredicateDefinition winner = choosePredicate(candidates, r);
				if (!r.preds.contains(winner)) {
					r.preds.add(winner);
				} else {
					throw new RuntimeException("r.preds already contains " + winner);
				}
				it.remove();
			}
			updateUnknownColumns(r);
			updateAmbTables(r);
		}
	}

	private static void updateAmbTables(Record r) {
		Iterator<String> it = r.ambiguousTables.iterator();
		while (it.hasNext()) {
			String t = it.next();
			if (containsPred(r.preds, t)) {
				it.remove();
			}
		}
	}

	private static boolean containsPred(HashSet<PredicateDefinition> preds, String t) {
		for (PredicateDefinition p : preds) {
			if (p.getName().equals(t)) {
				return true;
			}
		}
		return false;
	}

	private static PredicateDefinition choosePredicate(LinkedList<PredicateDefinition> candidates, Record r) {
		TreeSet<Map.Entry<PredicateDefinition, Integer>> ord = new TreeSet<>(
				new Comparator<Map.Entry<PredicateDefinition, Integer>>() {
					@Override
					public int compare(Entry<PredicateDefinition, Integer> e1, Entry<PredicateDefinition, Integer> e2) {
						int diff = e1.getValue() - e2.getValue();
						if (diff != 0) {
							return -1 * diff;
						} else {
							return e1.getKey().getName().compareTo(e2.getKey().getName());
						}
					}
				});

		for (PredicateDefinition c : candidates) {
			ord.add(new Entry<PredicateDefinition, Integer>() {
				public PredicateDefinition getKey() {
					return c;
				}

				public Integer getValue() {
					HashSet<String> attrs = new HashSet<>(c.getAttributes());
					return Util.setIntersection(attrs, r.unknownColumns).size();
				}

				public Integer setValue(Integer value) {
					return null;
				}
			});
		}

		return ord.iterator().next().getKey();
	}

	private static LinkedList<PredicateDefinition> computeCandidates(PredicateDefinition p,
			SchemaWithDuplicates schema) {
		LinkedList<PredicateDefinition> candidates = schema.getPredicateDefinitions(p.getName());
		if (candidates != null) {
			Iterator<PredicateDefinition> it = candidates.iterator();
			while (it.hasNext()) {
				if (!Util.isContained(p, it.next())) {
					it.remove();
				}
			}
			return candidates;
		} else {
			return new LinkedList<PredicateDefinition>();
		}
	}

	private static HashSet<String> collectPredNames(HashSet<PredicateDefinition> preds) {
		HashSet<String> res = new HashSet<>();
		for (PredicateDefinition p : preds) {
			res.add(p.getName());
		}
		return res;
	}

	private static String chooseUser(Set<String> candidates) {
		TreeSet<Map.Entry<String, Integer>> ord = new TreeSet<Map.Entry<String, Integer>>(
				new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
						int diff = e1.getValue() - e2.getValue();
						if (diff != 0) {
							return -1 * diff;
						} else {
							return e1.getKey().compareTo(e2.getKey());
						}
					}
				});

		for (String c : candidates) {
			ord.add(new Entry<String, Integer>() {
				public String getKey() {
					return c;
				}

				public Integer getValue() {
					return numTablesOf.get(c);
				}

				public Integer setValue(Integer value) {
					return null;
				}
			});
		}

		return ord.iterator().next().getKey();
	}

	private static void processRecord(Record r, HashSet<String> tables) {
		for (String tab : tables) {
			tab = Util.addQuote(tab);
			LinkedList<String> users = tab2users.get(tab);
			if (users != null) {
				if (users.size() == 1) {
					r.certainTables.add(tab);
					r.users.add(users.getFirst());
				} else {
					r.ambiguousTables.add(tab);
				}

				allUsers.addAll(users);
				for (String u : users) {
					if (numTablesOf.get(u) == null) {
						numTablesOf.put(u, 1);
					} else {
						int n = numTablesOf.get(u);
						numTablesOf.put(u, n + 1);
					}
				}
			} else {
				if (view2file.containsKey(tab)) {
					r.viewTables.add(tab);
				} else {
					r.undefinedTables.add(tab);
				}
			}
		}
	}

	private static CreateView getView(String view) throws JSQLParserException {
		String vFile = view2file.get(view);
		File fullPath = new File(viewDir + File.separator + vFile);
		String sqlString = Util.readSQLFile(fullPath.getPath());
		Statements stmts = CCJSqlParserUtil.parseStatements(sqlString);
		CreateView cv = (CreateView) stmts.getStatements().get(0);
		return cv;
	}

	private static WithItem makeWithItem(CreateView cv) {
		WithItem res = new WithItem();
		res.setName(cv.getView().getName());
		res.setSelectBody(cv.getSelect().getSelectBody());
		List<SelectItem> colNames = new LinkedList<>();
		for (String col : cv.getColumnNames()) {
			colNames.add(new SelectExpressionItem(new Column(col)));
		}
		res.setWithItemList(colNames);
		return res;
	}

	private static String[] setOtherArgs(String[] args) {
		while (args[0].startsWith("-")) {
			String cmd = args[0];
			switch (cmd) {
			case "-skip":
				skipS = Integer.parseInt(args[1]);
				skipE = Integer.parseInt(args[2]);
				args = Util.shiftLeftResize(args, 3);
				break;
			case "-out":
				outDir = args[1];
				args = Util.shiftLeftResize(args, 2);
				break;
			default:
				throw new RuntimeException("Unknown command: " + cmd);
			}
		}
		return args;
	}

}
