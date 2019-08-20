package at.ac.tuwien.dbai.hgtools.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//import static com.google.common.base.Preconditions.checkArgument;

public class CombinationIterator<T> implements Iterator<Collection<T>> {

private int[] indices;
private List<T> elements;
private boolean hasNext = true;

public CombinationIterator(List<T> elements, int k) throws IllegalArgumentException {
    //checkArgument(k<=elements.size(), "Impossible to select %d elements from hand of size %d", k, elements.size());
    this.indices = new int[k];
    for(int i=0; i<k; i++)
        indices[i] = k-1-i;
    this.elements = elements;
}

public CombinationIterator(Set<T> elements, int k) throws IllegalArgumentException {
    //checkArgument(k<=elements.size(), "Impossible to select %d elements from hand of size %d", k, elements.size());
    this.indices = new int[k];
    for(int i=0; i<k; i++)
        indices[i] = k-1-i;
    this.elements = new ArrayList<T>();
    this.elements.addAll(elements);
}

public boolean hasNext() {
    return hasNext;
}

private int inc(int[] indices, int maxIndex, int depth) throws IllegalStateException {
    if(depth == indices.length) {
        throw new IllegalStateException("The End");
    }
    if(indices[depth] < maxIndex) {
        indices[depth] = indices[depth]+1;
    } else {
        indices[depth] = inc(indices, maxIndex-1, depth+1)+1;
    }
    return indices[depth];
}

private boolean inc() {
    try {
        inc(indices, elements.size() - 1, 0);
        return true;
    } catch (IllegalStateException e) {
        return false;
    }
}

public Collection<T> next() {
    Collection<T> result = new ArrayList<T>(indices.length);
    for(int i=indices.length-1; i>=0; i--) {
        result.add(elements.get(indices[i]));
    }
    hasNext = inc();
    return result;
}

public void remove() {
    throw new UnsupportedOperationException();
}

}