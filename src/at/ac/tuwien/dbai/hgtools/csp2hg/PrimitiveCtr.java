package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.List;

import org.xcsp.common.Types.TypeArithmeticOperator;
import org.xcsp.common.Types.TypeConditionOperatorRel;
import org.xcsp.parser.entries.XVariables.XVarInteger;

import at.ac.tuwien.dbai.hgtools.util.Writables;

public class PrimitiveCtr implements Constraint {

    // XYK = (x aop y) op k
    // XPY = (x aop p) op y
    private enum PrimitiveType {
        UNARY, BINARY_XYK, TERNARY, BINARY_XPY
    }

    private PrimitiveType myType = null;

    private String name;
    private String x = null;
    private String y = null;
    private String z = null;
    private TypeConditionOperatorRel op = null;
    private TypeArithmeticOperator aop = null;
    private int p;
    private int k;

    public PrimitiveCtr(String name, XVarInteger x, TypeConditionOperatorRel op, int k) {
        myType = PrimitiveType.UNARY;
        this.name = name;
        this.x = x.id;
        this.op = op;
        this.k = k;
    }

    public PrimitiveCtr(String name, XVarInteger x, TypeArithmeticOperator aop, XVarInteger y,
            TypeConditionOperatorRel op, int k) {
        myType = PrimitiveType.BINARY_XYK;
        this.name = name;
        this.x = x.id;
        this.aop = aop;
        this.y = y.id;
        this.op = op;
        this.k = k;
    }

    public PrimitiveCtr(String name, XVarInteger x, TypeArithmeticOperator aop, XVarInteger y,
            TypeConditionOperatorRel op, XVarInteger z) {
        myType = PrimitiveType.TERNARY;
        this.name = name;
        this.x = x.id;
        this.aop = aop;
        this.y = y.id;
        this.op = op;
        this.z = z.id;
    }

    public PrimitiveCtr(String name, XVarInteger x, TypeArithmeticOperator aop, int p, TypeConditionOperatorRel op,
            XVarInteger y) {
        myType = PrimitiveType.BINARY_XPY;
        this.name = name;
        this.x = x.id;
        this.aop = aop;
        this.p = p;
        this.op = op;
        this.y = y.id;
    }

    @Override
    public List<String> toFile() {
        ArrayList<String> out = new ArrayList<>(4);
        out.add("PrimitiveCtr");
        out.add(name);
        String sub = null;
        String c = null;
        String xx = (x == null) ? null : Writables.stringify(x);
        String yy = (y == null) ? null : Writables.stringify(y);
        String zz = (z == null) ? null : Writables.stringify(z);
        switch (myType) {
        case UNARY:
            out.add(xx);
            c = op.toString().toLowerCase() + "(" + xx + "," + k + ")";
            break;
        case BINARY_XYK:
            out.add(xx + " " + yy);
            sub = aop.toString().toLowerCase() + "(" + xx + "," + yy + ")";
            c = op.toString().toLowerCase() + "(" + sub + "," + k + ")";
            break;
        case TERNARY:
            out.add(xx + " " + yy + " " + zz);
            sub = aop.toString().toLowerCase() + "(" + xx + "," + yy + ")";
            c = op.toString().toLowerCase() + "(" + sub + "," + zz + ")";
            break;
        case BINARY_XPY:
            out.add(xx + " " + yy);
            sub = aop.toString().toLowerCase() + "(" + xx + "," + p + ")";
            c = op.toString().toLowerCase() + "(" + sub + "," + yy + ")";
            break;
        default:
            throw new RuntimeException("Unknown case: " + myType);
        }
        out.add(c);
        return out;
    }

    @Override
    public List<String> getVariables() {
        ArrayList<String> vars = new ArrayList<>(3);
        if (x != null) {
            vars.add(x);
        }
        if (y != null) {
            vars.add(y);
        }
        if (z != null) {
            vars.add(z);
        }
        return vars;
    }

    @Override
    public String getType() {
        return "PrimitiveCtr";
    }

}
