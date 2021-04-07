package at.ac.tuwien.dbai.hgtools.csp2hg;

import java.util.List;

import at.ac.tuwien.dbai.hgtools.util.Writable;

public interface Constraint extends Writable {
    List<String> getVariables();
    String getType();
}
