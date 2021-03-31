package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.LinkedList;

import at.ac.tuwien.dbai.hgtools.util.Writable;
import net.sf.jsqlparser.statement.Statement;

public class Query implements Writable {

    private Statement stmt;

    public Query(Statement query) {
        if (query == null) {
            throw new NullPointerException();
        }
        this.stmt = query;
    }

    @Override
    public LinkedList<String> toFile() {
        LinkedList<String> res = new LinkedList<>();
        res.add(stmt.toString());
        return res;
    }

}
