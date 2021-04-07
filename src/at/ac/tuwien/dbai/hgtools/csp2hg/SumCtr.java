package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.xcsp.common.Condition;
import org.xcsp.common.Condition.ConditionVal;

import at.ac.tuwien.dbai.hgtools.util.Writables;

public class SumCtr implements Constraint {

    private String name;
    private ArrayList<String> vars;
    private int[] coeffs;
    private ConditionVal condition;

    public SumCtr(String name, String[] vars, int[] coeffs, Condition condition) {
        if (name == null || vars == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.vars = new ArrayList<>(vars.length);
        for (String v : vars) {
            this.vars.add(v);
        }
        if (coeffs == null) {
            this.coeffs = new int[vars.length];
            for (int i = 0; i < vars.length; i++) {
                this.coeffs[i] = 1;
            }
        } else {
            this.coeffs = coeffs; // TODO maybe deep copy?
        }
        this.condition = (ConditionVal) condition;
    }

    public SumCtr(String name, String[] vars, Condition condition) {
        this(name, vars, null, condition);
    }

    @Override
    public Collection<String> toFile() {
        ArrayList<String> out = new ArrayList<>(5);
        out.add("SumCtr");
        out.add(name);
        out.add(Writables.stringify(vars, ' '));
        out.add(Writables.stringify(coeffs, ' '));
        out.add("(" + condition.operator.toString().toLowerCase() + "," + condition.k + ")");
        return out;
    }

    @Override
    public List<String> getVariables() {
        return Collections.unmodifiableList(vars);
    }

    @Override
    public String getType() {
        return "SumCtr";
    }

}
