package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.TreeSet;

import at.ac.tuwien.dbai.hgtools.util.Writable;

public class CtrTypes implements Writable {
    private TreeSet<String> types = new TreeSet<>();

    public void addConstraint(Constraint c) {
        if (c == null) {
            throw new IllegalArgumentException();
        }
        types.add(c.getType());
    }

    @Override
    public TreeSet<String> toFile() {
        return types;
    }

}
