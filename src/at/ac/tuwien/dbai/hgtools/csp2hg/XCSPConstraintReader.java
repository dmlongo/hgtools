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

public class XCSPConstraintReader implements XCallbacks2 {
    private Implem implem = new Implem(this);
    private Map<XVarInteger, String> mapVar = new LinkedHashMap<>();
    private CtrTypes types = new CtrTypes();

    public XCSPConstraintReader(String filename) throws XCSPLoadInstanceException {
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

    public CtrTypes getTypes() {
        return types;
    }

    @Override
    public void buildVarInteger(XVarInteger xx, int minValue, int maxValue) {
        // empty implementation
    }

    @Override
    public void buildVarInteger(XVarInteger xx, int[] values) {
        // empty implementation
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
        String name = id;
        String[] vars = trVars(list);
        int[][] rel = new int[0][0];
        types.addConstraint(new ExtensionCtr(name, vars, rel, positive));
    }

    @Override
    public void buildCtrExtension(String id, XVarInteger x, int[] values, boolean positive, Set<TypeFlag> flags) {
        String name = id;
        String var = trVar(x);
        int[] rel = new int[0];
        types.addConstraint(new ExtensionCtr(name, var, rel, positive));
    }

    @Override
    public void buildCtrFalse(String id, XVar[] list) {
        // TODO Auto-generated method stub
        XCallbacks2.super.buildCtrFalse(id, list);
    }

    @Override
    public void buildCtrTrue(String id, XVar[] list) {
        // TODO Auto-generated method stub
        XCallbacks2.super.buildCtrTrue(id, list);
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, TypeConditionOperatorRel op, int k) {
        types.addConstraint(new PrimitiveCtr(id, x, op, k));
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, TypeArithmeticOperator aop, XVarInteger y,
            TypeConditionOperatorRel op, int k) {
        types.addConstraint(new PrimitiveCtr(id, x, aop, y, op, k));
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, TypeArithmeticOperator aop, XVarInteger y,
            TypeConditionOperatorRel op, XVarInteger z) {
        types.addConstraint(new PrimitiveCtr(id, x, aop, y, op, z));
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, TypeArithmeticOperator aop, int p,
            TypeConditionOperatorRel op, XVarInteger y) {
        types.addConstraint(new PrimitiveCtr(id, x, aop, p, op, y));
    }

    // TODO implement buildCtrIntension for all kinds of intensional constraints

    @Override
    public void buildCtrAllDifferent(String id, XVarInteger[] list) {
        String name = id;
        String[] vars = trVars(list);
        types.addConstraint(new AllDifferentCtr(name, vars));
    }

    @Override
    public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, TypeRank rank,
            Condition condition) {
        String name = id;
        String[] vars = trVars(list);
        String idx = trVar(index);
        types.addConstraint(new ElementCtr(name, vars, startIndex, idx, rank, condition));
    }

    @Override
    public void buildCtrSum(String id, XVarInteger[] list, Condition condition) {
        String name = id;
        String[] vars = trVars(list);
        types.addConstraint(new SumCtr(name, vars, condition));
    }

    @Override
    public void buildCtrSum(String id, XVarInteger[] list, int[] coeffs, Condition condition) {
        String name = id;
        String[] vars = trVars(list);
        types.addConstraint(new SumCtr(name, vars, coeffs, condition));
    }

}
