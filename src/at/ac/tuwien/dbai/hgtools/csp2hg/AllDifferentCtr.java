package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.util.Writables;

public class AllDifferentCtr implements Constraint {

    private String name;
    private ArrayList<String> vars;

    public AllDifferentCtr(String name, String[] vars) {
        if (name == null || vars == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.vars = new ArrayList<>(vars.length);
        for (String v : vars) {
            this.vars.add(v);
        }
    }

    @Override
    public List<String> toFile() {
        ArrayList<String> out = new ArrayList<>(3);
        out.add("AllDifferentCtr");
        out.add(name);
        out.add(Writables.stringify(vars, ' ', 5 * vars.size()));
        return out;
    }

    @Override
    public List<String> getVariables() {
        return Collections.unmodifiableList(vars);
    }

    @Override
    public String getType() {
        return "AllDifferentCtr";
    }

}
