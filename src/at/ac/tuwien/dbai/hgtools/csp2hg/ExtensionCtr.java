package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.util.Writables;

public class ExtensionCtr implements Constraint {

    private String name;
    private ArrayList<String> vars;
    private ArrayList<String> tuples;
    private boolean supports;

    public ExtensionCtr(String name, String[] vars, int[][] tuples, boolean supports) {
        if (name == null || vars == null || tuples == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.vars = new ArrayList<>(vars.length);
        for (String v : vars) {
            this.vars.add(v);
        }
        this.tuples = new ArrayList<>(tuples.length);
        for (int[] tup : tuples) {
            this.tuples.add(Writables.stringify(tup, ',', 5 * tup.length + 2, '(', ')'));
        }
        this.supports = supports;
    }

    public ExtensionCtr(String name, String var, int[] values, boolean supports) {
        if (name == null || var == null || values == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.vars = new ArrayList<>(1);
        this.vars.add(var);
        this.tuples = new ArrayList<>(values.length);
        for (int val : values) {
            this.tuples.add(Integer.toString(val));
        }
        this.supports = supports;
    }

    @Override
    public List<String> toFile() {
        ArrayList<String> out = new ArrayList<>(5);
        out.add("ExtensionCtr");
        out.add(name);
        out.add(Writables.stringify(vars, ' '));
        out.add(supports ? "supports" : "conflicts");
        out.add(Writables.stringify(tuples, ' '));
        return out;
    }

    @Override
    public List<String> getVariables() {
        return Collections.unmodifiableList(vars);
    }

    @Override
    public String getType() {
        String suff = supports ? "supports" : "conflicts";
        return "ExtensionCtr_" + suff;
    }

}
