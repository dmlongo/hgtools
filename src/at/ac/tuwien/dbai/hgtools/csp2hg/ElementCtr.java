package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.List;

import org.xcsp.common.Condition;
import org.xcsp.common.Condition.ConditionVal;
import org.xcsp.common.Types.TypeRank;

import at.ac.tuwien.dbai.hgtools.util.Writables;

public class ElementCtr implements Constraint {

    private ArrayList<String> list;
    private int startIndex;
    private String index;
    private TypeRank rank;
    private Condition condition;

    public ElementCtr(String[] list, int startIndex, String index, TypeRank rank, Condition condition) {
        if (list == null) {
            throw new NullPointerException();
        }
        this.list = new ArrayList<>(list.length);
        for (String v : list) {
            this.list.add(v);
        }
        this.startIndex = startIndex;
        this.index = index;
        this.rank = rank;
        this.condition = condition;
    }

    @Override
    public List<String> toFile() {
        ArrayList<String> out = new ArrayList<>(6);
        out.add("ElementCtr");
        out.add(Writables.stringify(list, ' '));
        out.add(Integer.toString(startIndex));
        out.add(index);
        out.add(rank.toString());
        long k = ((ConditionVal) condition).k; // ok only for XCSP-core
        out.add(Long.toString(k));
        return out;
    }

    @Override
    public List<String> getVariables() {
        ArrayList<String> vars = new ArrayList<>(list.size() + 1);
        vars.addAll(list);
        vars.add(index);
        return vars;
    }

}
