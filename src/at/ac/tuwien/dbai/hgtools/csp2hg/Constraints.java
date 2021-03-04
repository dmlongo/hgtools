package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.dbai.hgtools.util.Writable;

public class Constraints implements Writable {
    private ArrayList<Constraint> constrs = new ArrayList<>(200);

    public void addConstraint(Constraint c) {
        if (c == null) {
            throw new IllegalArgumentException();
        }
        constrs.add(c);
    }

    public List<String> toFile() {
        ArrayList<String> out = new ArrayList<>(constrs.size() * 7);
        for (Constraint c : constrs) {
            out.addAll(c.toFile());
        }
        return out;
    }

}
