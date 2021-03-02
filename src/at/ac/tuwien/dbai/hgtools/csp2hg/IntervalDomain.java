package at.ac.tuwien.dbai.hgtools.csp2hg;

public class IntervalDomain implements Domain {
    private final int min;
    private final int max;

    public IntervalDomain(int minValue, int maxValue) {
        this.min = minValue;
        this.max = maxValue;
    }

    @Override
    public boolean contains(int val) {
        return min <= val && val <= max;
    }

    @Override
    public String toFile() {
        return min + ".." + max;
    }
}