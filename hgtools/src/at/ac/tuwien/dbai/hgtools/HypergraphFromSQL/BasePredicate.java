package at.ac.tuwien.dbai.hgtools.HypergraphFromSQL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Data;

@Data
public class BasePredicate {
	private String name;
	private List<String> literals;

	public BasePredicate(String name) {
		this.name = name;
		literals = new ArrayList<String>(50);
	}

	public void addLiteral(String lit) {
		literals.add(lit);
	}

	public void removeLiteral(String lit) {
		literals.remove(lit);
	}

	public String getName() {
		return name;
	}

	public List<String> getLiterals() {
		return literals;
	}

	public void setLiterals(List<String> literals) {
		this.literals = literals;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(200);
		sb.append(name);
		sb.append('(');
		Iterator<String> it = literals.iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			if (it.hasNext()) {
				sb.append(',');
			}
		}
		sb.append(')');
		return sb.toString();
	}
}
