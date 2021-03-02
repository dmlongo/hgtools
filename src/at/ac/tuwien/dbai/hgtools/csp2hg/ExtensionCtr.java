package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.util.Util;

public class ExtensionCtr implements Constraint {

    private ArrayList<String> vars;
    private ArrayList<String> tuples;
    private boolean supports;

    public ExtensionCtr(String[] vars, int[][] tuples, boolean supports) {
        if (vars == null || tuples == null) {
            throw new NullPointerException();
        }
        this.vars = new ArrayList<>(vars.length);
        for (String v : vars) {
            this.vars.add(v);
        }
        this.tuples = new ArrayList<>(tuples.length);
        for (int[] tup : tuples) {
            StringBuilder sb = new StringBuilder(5 * tup.length + 2);
            sb.append('(');
            for (int i = 0; i < tup.length; i++) {
                sb.append(Integer.toString(tup[i]));
                if (i < tup.length - 1) {
                    sb.append(',');
                }
            }
            sb.append(')');
            this.tuples.add(sb.toString());
        }
        this.supports = supports;
    }

    public ExtensionCtr(String var, int[] values, boolean supports) {
        if (var == null || values == null) {
            throw new NullPointerException();
        }
        this.vars = new ArrayList<>(1);
        this.vars.add(Util.stringify(var));
        this.tuples = new ArrayList<>(values.length);
        for (int val : values) {
            this.tuples.add(Integer.toString(val));
        }
        this.supports = supports;
    }

    @Override
    public List<String> getVariables() {
        return Collections.unmodifiableList(vars);
    }

    @Override
    public List<String> toFile() {
        ArrayList<String> out = new ArrayList<>(200);
        out.add("ExtensionCtr");

        StringBuilder sbVars = new StringBuilder(200);
        for (int i = 0; i < vars.size(); i++) {
            sbVars.append(Util.stringify(vars.get(i)));
            if (i < vars.size() - 1) {
                sbVars.append(' ');
            }
        }
        out.add(sbVars.toString());

        StringBuilder sbTuples = new StringBuilder(200);
        for (int i = 0; i < tuples.size(); i++) {
            sbTuples.append(tuples.get(i));
            if (i < tuples.size() - 1) {
                sbTuples.append(' ');
            }
        }
        out.add(sbTuples.toString());

        out.add(supports ? "supports" : "conflicts");
        return out;
    }

}
