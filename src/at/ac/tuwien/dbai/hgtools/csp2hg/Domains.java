package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import at.ac.tuwien.dbai.hgtools.util.Writable;
import at.ac.tuwien.dbai.hgtools.util.Writables;

public class Domains implements Writable {
    private HashMap<String, Domain> doms = new HashMap<>();

    public void addVar(String v, int minValue, int maxValue) {
        if (v == null) {
            throw new IllegalArgumentException();
        }
        if (doms.containsKey(v)) {
            throw new IllegalArgumentException(v + " is already here!");
        }
        doms.put(v, new IntervalDomain(minValue, maxValue));
    }

    public void addVar(String v, int[] dom) {
        if (v == null || dom == null) {
            throw new IllegalArgumentException();
        }
        if (doms.containsKey(v)) {
            throw new IllegalArgumentException(v + " is already here!");
        }
        doms.put(v, new ExplicitDomain(dom));
    }

    public Map<String, Domain> getDoms() {
        return Collections.unmodifiableMap(doms);
    }

    public boolean contains(String var, int value) {
        return doms.get(var).contains(value);
    }

    public boolean contains(String[] vars, int[] tuple) {
        if (vars.length != tuple.length) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < vars.length; i++) {
            if (!doms.get(vars[i]).contains(tuple[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> toFile() {
        ArrayList<String> out = new ArrayList<>(100);
        for (Entry<String, Domain> entry : doms.entrySet()) {
            StringBuilder sb = new StringBuilder(200);
            sb.append(Writables.stringify(entry.getKey()));
            sb.append(';');
            sb.append(entry.getValue().toFile());
            out.add(sb.toString());
        }
        return out;
    }

}
