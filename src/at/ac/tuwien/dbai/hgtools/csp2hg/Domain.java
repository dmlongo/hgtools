package at.ac.tuwien.dbai.hgtools.csp2hg;

public interface Domain {
    boolean contains(int val);

    String toFile();
}