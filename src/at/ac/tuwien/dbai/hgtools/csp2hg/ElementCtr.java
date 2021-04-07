package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.ArrayList;
import java.util.List;

import org.xcsp.common.Condition;
import org.xcsp.common.Condition.ConditionVal;
import org.xcsp.common.Types.TypeRank;

import at.ac.tuwien.dbai.hgtools.util.Writables;

public class ElementCtr implements Constraint {

    private String name;
    private ArrayList<String> list;
    private int startIndex;
    private String index;
    private TypeRank rank;
    private Condition condition;

    public ElementCtr(String name, String[] list, int startIndex, String index, TypeRank rank, Condition condition) {
        if (name == null || list == null) {
            throw new NullPointerException();
        }
        this.name = name;
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
        ArrayList<String> out = new ArrayList<>(8);
        out.add("ElementCtr");
        out.add(name);
        out.add(Writables.stringify(getVariables(), ' '));
        out.add(Writables.stringify(list, ' '));
        out.add(Integer.toString(startIndex));
        out.add(Writables.stringify(index));
        out.add(rank.toString());
        long k = ((ConditionVal) condition).k; // TODO ok only for miniTrack17
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

    @Override
    public String getType() {
        return "ElementCtr";
    }

}
