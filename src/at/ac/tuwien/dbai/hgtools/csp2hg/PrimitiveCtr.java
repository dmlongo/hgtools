package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.List;

import org.xcsp.common.Types.TypeArithmeticOperator;
import org.xcsp.common.Types.TypeConditionOperatorRel;
import org.xcsp.parser.entries.XVariables.XVarInteger;

public class PrimitiveCtr implements Constraint {

    // XYK = (x aop y) op k
    // XPY = (x aop p) op y
    private enum PrimitiveType {
        UNARY, BINARY_XYK, TERNARY, BINARY_XPY
    }

    private PrimitiveType myType = null;

    private String x = null;
    private String y = null;
    private String z = null;
    private TypeConditionOperatorRel op = null;
    private TypeArithmeticOperator aop = null;
    private int p;
    private int k;

    public PrimitiveCtr(XVarInteger x, TypeConditionOperatorRel op, int k) {
        myType = PrimitiveType.UNARY;
        this.x = x.id;
        this.op = op;
        this.k = k;
    }

    public PrimitiveCtr(XVarInteger x, TypeArithmeticOperator aop, XVarInteger y, TypeConditionOperatorRel op, int k) {
        myType = PrimitiveType.BINARY_XYK;
        this.x = x.id;
        this.aop = aop;
        this.y = y.id;
        this.op = op;
        this.k = k;
    }

    public PrimitiveCtr(XVarInteger x, TypeArithmeticOperator aop, XVarInteger y, TypeConditionOperatorRel op,
            XVarInteger z) {
        myType = PrimitiveType.TERNARY;
        this.x = x.id;
        this.aop = aop;
        this.y = y.id;
        this.op = op;
        this.z = z.id;
    }

    public PrimitiveCtr(XVarInteger x, TypeArithmeticOperator aop, int p, TypeConditionOperatorRel op, XVarInteger y) {
        myType = PrimitiveType.BINARY_XPY;
        this.x = x.id;
        this.aop = aop;
        this.p = p;
        this.op = op;
        this.y = y.id;
    }

    @Override
    public List<String> toFile() {
        ArrayList<String> out = new ArrayList<>(3);
        out.add("PrimitiveCtr");
        String sub = null;
        String c = null;
        switch (myType) {
            case UNARY:
                out.add(x);
                c = op.name().toLowerCase() + "(" + x + "," + k + ")";
                break;
            case BINARY_XYK:
                out.add(x + " " + y);
                sub = aop.name().toLowerCase() + "(" + x + "," + y + ")";
                c = op.name().toLowerCase() + "(" + sub + "," + k + ")";
                break;
            case TERNARY:
                out.add(x + " " + y + " " + z);
                sub = aop.name().toLowerCase() + "(" + x + "," + y + ")";
                c = op.name().toLowerCase() + "(" + sub + "," + z + ")";
                break;
            case BINARY_XPY:
                out.add(x + " " + y);
                sub = aop.name().toLowerCase() + "(" + x + "," + p + ")";
                c = op.name().toLowerCase() + "(" + sub + "," + y + ")";
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

}