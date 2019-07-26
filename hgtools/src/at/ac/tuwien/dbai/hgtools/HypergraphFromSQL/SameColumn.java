package at.ac.tuwien.dbai.hgtools.HypergraphFromSQL;

import lombok.Data;
import net.sf.jsqlparser.schema.Column;

@Data
public class SameColumn {
	private Column a;
	private Column b;

	public Column getA() {
		return a;
	}

	public Column getB() {
		return b;
	}

	public void setB(Column b) {
		this.b = b;
	}

	public void setA(Column a) {
		this.a = a;
	}
}
