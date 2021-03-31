package at.ac.tuwien.dbai.hgtools.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class GraphUtils {

    private GraphUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static <V> Set<V> findTerminalNodes(Graph<V, ?> g) {
        HashSet<V> res = new HashSet<>();
        for (V v : g.vertexSet()) {
            if (g.outDegreeOf(v) == 0) {
                res.add(v);
            }
        }
        return res;
    }

    public static <V> List<Graph<V, DefaultEdge>> computeConnComps(Graph<V, DefaultEdge> deps) {
        List<Graph<V, DefaultEdge>> res = new ArrayList<>();
        ConnectivityInspector<V, DefaultEdge> connInsp = new ConnectivityInspector<>(deps);
        List<Set<V>> compSets = connInsp.connectedSets();
        for (Set<V> cVertices : compSets) {
            Graph<V, DefaultEdge> c = buildComponent(deps, cVertices);
            res.add(c);
        }
        return res;
    }

    private static <V> Graph<V, DefaultEdge> buildComponent(Graph<V, DefaultEdge> deps, Set<V> cVertices) {
        Graph<V, DefaultEdge> c = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (V v : cVertices) {
            c.addVertex(v);
        }
        for (DefaultEdge e : deps.edgeSet()) {
            V source = deps.getEdgeSource(e);
            V dest = deps.getEdgeTarget(e);
            if (cVertices.contains(source) && cVertices.contains(dest)) {
                c.addEdge(source, dest);
            }
        }
        return c;
    }

    public static <V extends Comparable<V>, E extends DefaultEdge> List<Graph<V, E>> sortComponents(
            List<Graph<V, E>> comps) {
        HashMap<V, Graph<V, E>> compsMap = makeMap(comps);
        ArrayList<V> roots = new ArrayList<>(compsMap.keySet());
        Collections.sort(roots);
        List<Graph<V, E>> res = new ArrayList<>();
        for (V r : roots) {
            res.add(compsMap.get(r));
        }
        return res;
    }

    private static <V extends Comparable<V>, E extends DefaultEdge> HashMap<V, Graph<V, E>> makeMap(
            List<Graph<V, E>> comps) {
        HashMap<V, Graph<V, E>> res = new HashMap<>();
        for (Graph<V, E> c : comps) {
            V r = findRoots(c).get(0);
            res.put(r, c);
        }
        return res;
    }

    public static <V extends Comparable<V>, E extends DefaultEdge> List<V> findRoots(Graph<V, E> c) {
        List<V> res = new ArrayList<>();
        for (V s : c.vertexSet()) {
            if (c.inDegreeOf(s) == 0) {
                res.add(s);
            }
        }
        Collections.sort(res); // alphabetical order
        return res;
    }

    public static <V extends Comparable<V>, E extends DefaultEdge> List<V> reverseBFSAlphabeticalSort(Graph<V, E> g) {
        LinkedList<V> res = new LinkedList<>();
        List<V> roots = findRoots(g);
        for (V r : roots) {
            LinkedList<V> toVisit = new LinkedList<>();
            HashSet<V> visited = new HashSet<>();
            toVisit.addLast(r);
            while (!toVisit.isEmpty()) {
                V v = toVisit.removeFirst();
                if (!visited.contains(v)) {
                    res.addFirst(v);
                    TreeSet<V> sortedChildren = new TreeSet<>();
                    Set<E> children = g.outgoingEdgesOf(v);
                    for (E e : children) {
                        sortedChildren.add(g.getEdgeTarget(e));
                    }
                    toVisit.addAll(sortedChildren);
                    visited.add(v);
                }
            }
        }
        return removeDuplicates(res);
    }

    private static <V> LinkedList<V> removeDuplicates(LinkedList<V> list) {
        LinkedList<V> res = new LinkedList<>();
        HashSet<V> seen = new HashSet<>();
        for (V s : list) {
            if (!seen.contains(s)) {
                res.add(s);
                seen.add(s);
            }
        }
        return res;
    }

    public static <V, E extends DefaultEdge> void printGraph(Graph<V, E> graph) {
        ArrayList<V> vertices = new ArrayList<>(graph.vertexSet().size());
        for (V v : graph.vertexSet()) {
            vertices.add(v);
        }
        System.out.println("Vertices:");
        for (int i = 0; i < vertices.size(); i++) {
            System.out.println(i + " -> " + vertices.get(i));
        }
        System.out.println("Edges:");
        for (int i = 0; i < vertices.size() - 1; i++) {
            for (int j = i + 1; j < vertices.size(); j++) {
                V sourceVertex = vertices.get(i);
                V targetVertex = vertices.get(j);
                if (graph.containsEdge(sourceVertex, targetVertex)) {
                    E e = graph.getEdge(sourceVertex, targetVertex);
                    System.out.println("(" + i + ", " + j + ") -> " + e);
                }
                if (graph.containsEdge(targetVertex, sourceVertex)) {
                    E e = graph.getEdge(targetVertex, sourceVertex);
                    System.out.println("(" + j + ", " + i + ") -> " + e);
                }
            }
        }
    }

}
