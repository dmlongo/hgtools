package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.HashSet;

public class ExplicitDomain implements Domain {
    private final int[] values;
    private final HashSet<Integer> valSet;

    public ExplicitDomain(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }
        this.values = values;
        valSet = new HashSet<>();
        for (int v : values) {
            valSet.add(v);
        }
    }

    @Override
    public boolean contains(int val) {
        return valSet.contains(val);
    }

    @Override
    public String toFile() {
        StringBuilder sb = new StringBuilder(200);
        for (int i = 0; i < values.length; i++) {
            sb.append(Integer.toString(values[i]));
            if (i < values.length - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

}