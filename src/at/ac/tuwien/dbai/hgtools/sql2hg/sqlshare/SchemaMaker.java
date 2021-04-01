package at.ac.tuwien.dbai.hgtools.sql2hg.sqlshare;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.dbai.hgtools.sql2hg.PredicateDefinition;
import at.ac.tuwien.dbai.hgtools.sql2hg.Schema;
import at.ac.tuwien.dbai.hgtools.util.Util;

public class SchemaMaker {

	private SchemaWithDuplicates schema;
	private Map<String, LinkedList<String>> tab2users;
	private Map<String, LinkedList<String>> user2tabs;
	private Map<String, String> view2file;
	private Map<String, String> file2view;

	private static class Record {
		Record parent;
		PredicateBuilder pb;

		HashSet<PredicateDefinition> certain;
		HashSet<PredicateDefinition> ambiguous;
		HashSet<PredicateDefinition> undefined;
		Set<String> unknownCols;

		public Record(PredicateBuilder pb, Record r) {
			this.pb = pb;
			this.parent = r;

			certain = new HashSet<>();
			ambiguous = new HashSet<>();
			undefined = new HashSet<>();
			unknownCols = pb.getUnknowncolumns();
		}

		@Override
		public String toString() {
			return pb.toString();
		}
	}

	public SchemaMaker(SchemaWithDuplicates schema, Map<String, LinkedList<String>> tab2users,
			Map<String, LinkedList<String>> user2tabs, Map<String, String> view2file, Map<String, String> file2view) {
		this.schema = schema;
		this.tab2users = tab2users;
		this.user2tabs = user2tabs;
		this.view2file = view2file;
		this.file2view = file2view;
	}

	// resolve ambiguities:
	// 1. for each predicate with exactly 1 definition -> expand
	// 2. eliminate discovered unknown columns
	// 3. collect ambiguous predicates (at least 2 definitions)
	// 4. disambiguate
	// 5. another sweep for unknown columns
	// 6. build undefined predicates
	// 7. if there are unknown columns it's a problem

	public Schema getSchema(PredicateBuilder pb) {
		LinkedList<Record> recs = makeRecords(pb);
		classifyPredicates(recs);
		cleanUnknownColumns(recs);
		cleanUnknownColumnsParents(recs);
		disambiguate(recs);
		cleanUnknownColumns(recs);
		cleanUnknownColumnsParents(recs);
		define(recs);
		cleanUnknownColumns(recs);
		cleanUnknownColumnsParents(recs);

		return makeSchema(recs);
	}

	private HashSet<String> getUnknownColumns(LinkedList<Record> recs) {
		HashSet<String> res = new HashSet<>();
		for (Record r : recs) {
			res.addAll(r.unknownCols);
		}
		return res;
	}

	private void define(LinkedList<Record> recs) {
		for (Record r : recs) {
			if (r.undefined.size() == 1) {
				PredicateDefinition p = r.undefined.iterator().next();
				List<String> pAttrs = p.getAttributes();
				pAttrs.addAll(r.unknownCols);
				r.certain.add(new PredicateDefinition(p.getName(), pAttrs));
				r.undefined.clear();
			} else {
				r.certain.addAll(r.undefined);
				r.undefined.clear();
			}
		}
	}

	private void disambiguate(LinkedList<Record> recs) {
		// TODO Auto-generated method stub
		for (Record r : recs) {
			if (!r.ambiguous.isEmpty()) {
				throw new RuntimeException("Need to disambiguate " + r.ambiguous);
			}
		}
	}

	private Schema makeSchema(LinkedList<Record> recs) {
		HashSet<PredicateDefinition> allPreds = collectPredicates(recs);

		Schema out = new Schema();
		for (PredicateDefinition pred : allPreds) {
			if (pred.arity() == 0) {
				String name = pred.getName();
				LinkedList<String> attrs = new LinkedList<>();
				attrs.add("\"\"");
				pred = new PredicateDefinition(name, attrs);
			}
			out.addPredicateDefinition(pred);
		}

		HashSet<String> uc = getUnknownColumns(recs);
		if (!uc.isEmpty()) {
			// throw new RuntimeException("There are still unknown columns: " + uc);
			PredicateDefinition p = new PredicateDefinitionQuote("UnknownColumns", uc);
			out.addPredicateDefinition(p);
		}
		if (out.numPredicates() == 0) {
			LinkedList<String> dummyAttrs = new LinkedList<>();
			dummyAttrs.add("\"\"");
			PredicateDefinition dummy = new PredicateDefinition("\"\"", dummyAttrs);
			out.addPredicateDefinition(dummy);
		}

		return out;
	}

	private HashSet<PredicateDefinition> collectPredicates(LinkedList<Record> recs) {
		HashMap<String, HashSet<String>> preds = new HashMap<>();
		for (Record r : recs) {
			for (PredicateDefinition p : r.certain) {
				if (!preds.containsKey(p.getName())) {
					preds.put(p.getName(), new HashSet<>());
				}
				preds.get(p.getName()).addAll(p.getAttributes());
			}
		}

		HashSet<PredicateDefinition> res = new HashSet<>();
		for (String name : preds.keySet()) {
			res.add(new PredicateDefinition(name, preds.get(name)));
		}
		return res;
	}

	private void cleanUnknownColumns(LinkedList<Record> recs) {
		for (Record r : recs) {
			Iterator<String> it = r.unknownCols.iterator();
			while (it.hasNext()) {
				String col = it.next();
				if (Util.isCovered(col, r.certain) || Util.isCovered(col, r.pb.getViewPredicates())) {
					it.remove();
				}
			}
		}
	}

	private void cleanUnknownColumnsParents(LinkedList<Record> recs) {
		for (Record r : recs) {
			Record parent = r.parent;
			while (parent != null) {
				Iterator<String> it = r.unknownCols.iterator();
				while (it.hasNext()) {
					String col = it.next();
					if (Util.isCovered(col, parent.certain) || Util.isCovered(col, parent.pb.getViewPredicates())) {
						it.remove();
					}
				}
				if (!r.unknownCols.isEmpty()) {
					parent = parent.parent;
				} else {
					parent = null;
				}
			}
		}
	}

	private void classifyPredicates(LinkedList<Record> recs) {
		for (Record r : recs) {
			for (PredicateDefinition pred : r.pb.getPredicates()) {
				List<PredicateDefinition> initialCandidates = schema.getPredicateDefinitions(pred.getName());
				if (initialCandidates != null) {
					HashSet<PredicateDefinition> candidates = new HashSet<>(initialCandidates);
					Iterator<PredicateDefinition> it = candidates.iterator();
					while (it.hasNext()) {
						if (!Util.isContained(pred, it.next())) {
							it.remove();
						}
					}

					if (candidates.isEmpty()) {
						r.undefined.add(pred);
					} else if (candidates.size() == 1) {
						r.certain.add(candidates.iterator().next());
					} else {
						r.ambiguous.add(pred);
					}
				} else {
					r.undefined.add(pred);
				}
			}
		}
	}

	private LinkedList<Record> makeRecords(PredicateBuilder root) {
		LinkedList<Record> recs = new LinkedList<>();

		LinkedList<PredicateBuilder> toVisit = new LinkedList<>();
		LinkedList<Record> parents = new LinkedList<>();
		toVisit.addLast(root);
		parents.addLast(null);
		while (!toVisit.isEmpty()) {
			PredicateBuilder curr = toVisit.removeFirst();
			Record p = parents.removeFirst();
			Record r = new Record(curr, p);
			recs.add(r);
			for (PredicateBuilder child : curr.getSubPredicates()) {
				toVisit.addLast(child);
				parents.addLast(r);
			}
		}

		return recs;
	}

}
