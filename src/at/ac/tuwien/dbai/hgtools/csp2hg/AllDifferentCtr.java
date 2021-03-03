package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.util.Util;

public class AllDifferentCtr implements Constraint {

    private ArrayList<String> vars;

    public AllDifferentCtr(String[] vars) {
        if (vars == null) {
            throw new NullPointerException();
        }
        this.vars = new ArrayList<>(vars.length);
        for (String v : vars) {
            this.vars.add(v);
        }
    }

    @Override
    public List<String> toFile() {
        ArrayList<String> out = new ArrayList<>(2);
        out.add("AllDifferentCtr");
        StringBuilder sb = new StringBuilder(5 * vars.size());
        for (int i = 0; i < vars.size(); i++) {
            sb.append(Util.stringify(vars.get(i)));
            if (i < vars.size() - 1) {
                sb.append(" ");
            }
        }
        out.add(sb.toString());
        return out;
    }

    @Override
    public List<String> getVariables() {
        return Collections.unmodifiableList(vars);
    }

}
