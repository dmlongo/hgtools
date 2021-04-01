package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.xcsp.common.Condition;
import org.xcsp.common.Types.TypeArithmeticOperator;
import org.xcsp.common.Types.TypeConditionOperatorRel;
import org.xcsp.common.Types.TypeFlag;
import org.xcsp.common.Types.TypeRank;
import org.xcsp.parser.callbacks.XCallbacks2;
import org.xcsp.parser.entries.XVariables.XVar;
import org.xcsp.parser.entries.XVariables.XVarInteger;

import at.ac.tuwien.dbai.hgtools.hypergraph.Edge;
import at.ac.tuwien.dbai.hgtools.hypergraph.Hypergraph;

public class HypergraphFromXCSPHelper implements XCallbacks2 {
	private Implem implem = new Implem(this);
	private Map<XVarInteger, String> mapVar = new LinkedHashMap<>();
	private Domains domains = new Domains();
	private Constraints constrs = new Constraints();
	private Hypergraph hg = new Hypergraph();
	private int iEdge = 0;

	public HypergraphFromXCSPHelper(String filename) throws XCSPLoadInstanceException {
		try {
			loadInstance(filename);
		} catch (Exception e) {
			throw new XCSPLoadInstanceException(e);
		}
	}

	@Override
	public Implem implem() {
		return implem;
	}

	public Hypergraph getHypergraph() {
		return hg;
	}

	public Domains getDomains() {
		return domains;
	}

	public Constraints getConstraints() {
		return constrs;
	}

	@Override
	public void buildVarInteger(XVarInteger xx, int minValue, int maxValue) {
		String x = xx.id;
		mapVar.put(xx, x);
		domains.addVar(x, minValue, maxValue);
	}

	@Override
	public void buildVarInteger(XVarInteger xx, int[] values) {
		String x = xx.id;
		mapVar.put(xx, x);
		domains.addVar(x, values);
	}

	private String trVar(Object x) {
		return mapVar.get(x);
	}

	private String[] trVars(Object vars) {
		return Arrays.stream((XVarInteger[]) vars).map(x -> mapVar.get(x)).toArray(String[]::new);
	}

	@Override
	public void buildCtrExtension(String id, XVarInteger[] list, int[][] tuples, boolean positive,
			Set<TypeFlag> flags) {
		if (flags.contains(TypeFlag.STARRED_TUPLES)) {
			throw new ShortTableException();
		}
		if (flags.contains(TypeFlag.UNCLEAN_TUPLES)) {
			tuples = cleanTuples(list, tuples);
		}
		String eName = "E" + ++iEdge;
		String[] eVars = trVars(list);
		hg.addEdge(new Edge(eName, eVars));
		constrs.addConstraint(new ExtensionCtr(eName, eVars, tuples, positive));
	}

	private int[][] cleanTuples(XVarInteger[] list, int[][] tuples) {
		// TODO
		/*
		 * int[][] cleanTuples = new int[tuples.length][]; String[] vars = trVars(list);
		 * int i = 0; for (int[] tup : tuples) { if (domains.contains(vars, tup)) {
		 * cleanTuples[i++] = tup; } } return cleanTuples;
		 */
		return tuples;
	}

	@Override
	public void buildCtrExtension(String id, XVarInteger x, int[] values, boolean positive, Set<TypeFlag> flags) {
		if (flags.contains(TypeFlag.STARRED_TUPLES)) {
			throw new ShortTableException();
		}
		if (flags.contains(TypeFlag.UNCLEAN_TUPLES)) {
			values = cleanTuples(x, values);
		}
		String eName = "E" + ++iEdge;
		String eVar = trVar(x);
		hg.addEdge(new Edge(eName, eVar));
		constrs.addConstraint(new ExtensionCtr(eName, eVar, values, positive));
	}

	private int[] cleanTuples(XVarInteger x, int[] values) {
		// TODO
		return values;
	}

	@Override
	public void buildCtrFalse(String id, XVar[] list) {
		// TODO Auto-generated method stub
		XCallbacks2.super.buildCtrFalse(id, list);
		// throw new RuntimeException();
	}

	@Override
	public void buildCtrTrue(String id, XVar[] list) {
		// TODO Auto-generated method stub
		XCallbacks2.super.buildCtrTrue(id, list);
		// throw new RuntimeException();
	}

	@Override
	public void buildCtrPrimitive(String id, XVarInteger x, TypeConditionOperatorRel op, int k) {
		String eName = "E" + ++iEdge;
		hg.addEdge(new Edge(eName, trVar(x)));
		constrs.addConstraint(new PrimitiveCtr(eName, x, op, k));
	}

	@Override
	public void buildCtrPrimitive(String id, XVarInteger x, TypeArithmeticOperator aop, XVarInteger y,
			TypeConditionOperatorRel op, int k) {
		String eName = "E" + ++iEdge;
		hg.addEdge(new Edge(eName, trVar(x), trVar(y)));
		constrs.addConstraint(new PrimitiveCtr(eName, x, aop, y, op, k));
	}

	@Override
	public void buildCtrPrimitive(String id, XVarInteger x, TypeArithmeticOperator aop, XVarInteger y,
			TypeConditionOperatorRel op, XVarInteger z) {
		String eName = "E" + ++iEdge;
		hg.addEdge(new Edge(eName, trVar(x), trVar(y), trVar(z)));
		constrs.addConstraint(new PrimitiveCtr(eName, x, aop, y, op, z));
	}

	@Override
	public void buildCtrPrimitive(String id, XVarInteger x, TypeArithmeticOperator aop, int p,
			TypeConditionOperatorRel op, XVarInteger y) {
		String eName = "E" + ++iEdge;
		hg.addEdge(new Edge(eName, trVar(x), trVar(y)));
		constrs.addConstraint(new PrimitiveCtr(eName, x, aop, p, op, y));
	}

	// TODO implement buildCtrIntension for all kinds of intensional constraints

	@Override
	public void buildCtrAllDifferent(String id, XVarInteger[] list) {
		// TODO there are many ways to represent an AllDiff constraint
		String eName = "E" + ++iEdge;
		String[] eVars = trVars(list);
		hg.addEdge(new Edge(eName, eVars));
		constrs.addConstraint(new AllDifferentCtr(eName, eVars));
	}

	@Override
	public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, TypeRank rank,
			Condition condition) {
		String eName = "E" + ++iEdge;
		String[] eVars = trVars(list);
		String idx = trVar(index);
		Edge e = new Edge(eName, eVars);
		e.addVertex(idx);
		hg.addEdge(e);
		constrs.addConstraint(new ElementCtr(eName, eVars, startIndex, idx, rank, condition));
	}

	@Override
	public void buildCtrSum(String id, XVarInteger[] list, Condition condition) {
		String eName = "E" + ++iEdge;
		String[] eVars = trVars(list);
		hg.addEdge(new Edge(eName, eVars));
		constrs.addConstraint(new SumCtr(eName, eVars, condition));
	}

	@Override
	public void buildCtrSum(String id, XVarInteger[] list, int[] coeffs, Condition condition) {
		String eName = "E" + ++iEdge;
		String[] eVars = trVars(list);
		hg.addEdge(new Edge(eName, eVars));
		constrs.addConstraint(new SumCtr(eName, eVars, coeffs, condition));
	}

	protected static class ShortTableException extends RuntimeException {
		private static final long serialVersionUID = 7699501138447795640L;

		public ShortTableException() {
			super("I cannot manage short tables");
		}
	}

	protected static class UncleanTuplesException extends RuntimeException {
		private static final long serialVersionUID = 5108781140316202705L;

		public UncleanTuplesException() {
			super("There are unclean tuples");
		}
	}

}
