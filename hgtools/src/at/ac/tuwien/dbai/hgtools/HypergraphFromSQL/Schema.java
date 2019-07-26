package at.ac.tuwien.dbai.hgtools.HypergraphFromSQL;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class Schema {
	private Map<String, BasePredicate> preds;
	private Map<String, Map<String, String>> views;

	public Schema() {
		preds = new HashMap<>();
		views = new HashMap<>();
	}

	public void addViewColumn(String viewName, String viewColumn, String originalColumn) {
		Map<String, String> mapping = views.get(viewName);
		if (mapping == null) {
			mapping = new HashMap<>();
		}
		String originalPredicate = findOriginalPredicate(originalColumn);
		mapping.put(viewColumn, originalPredicate);
		views.put(viewName, mapping);
	}

	private String findOriginalPredicate(String originalColumn) {
		for (BasePredicate p : preds.values()) {
			for (String lit : p.getLiterals()) {
				if (lit.equalsIgnoreCase(originalColumn)) {
					return p.getName();
				}
			}
		}
		return null;
	}

	public void addPredicate(BasePredicate pred) {
		preds.put(pred.getName(), pred);
	}

	public BasePredicate getPredicate(String name) {
		return preds.get(name);
	}

	public BasePredicate getPredicateFromView(String tbl, String columnName) {
		Map<String, String> mapping = views.get(tbl);
		if (mapping != null) {
			String predicateName = mapping.get(columnName);
			return preds.get(predicateName);
		}
		return null;
	}
}
