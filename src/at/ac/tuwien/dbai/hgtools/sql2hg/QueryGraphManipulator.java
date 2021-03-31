package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;

import at.ac.tuwien.dbai.hgtools.sql2hg.QueryExtractor.SubqueryEdge;
import net.sf.jsqlparser.statement.select.SelectBody;

public class QueryGraphManipulator {

	private QueryExtractor qExtr;

	public QueryGraphManipulator(QueryExtractor qExtr) {
		if (qExtr == null) {
			throw new NullPointerException();
		}
		this.qExtr = qExtr;
	}

	public List<Graph<SelectBody, SubqueryEdge>> computeDependencyGraphsSimplified() {
		Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();
		ArrayList<SelectBody> intToSelList = new ArrayList<>(query.vertexSet().size());
		HashMap<SelectBody, Integer> selToIntMap = new HashMap<>();

		Graph<Integer, SubqueryEdge> pRes = new DefaultDirectedGraph<>(SubqueryEdge.class);
		convertToIntGraph(pRes, qExtr, intToSelList, selToIntMap);
		addViews(pRes, qExtr, intToSelList, selToIntMap);

		keepOnlyJoinEdges(pRes);

		ArrayList<Integer> roots = new ArrayList<>();
		List<Graph<Integer, SubqueryEdge>> comps = computeConnComps(pRes, roots);
		LinkedList<Graph<SelectBody, SubqueryEdge>> results = new LinkedList<>();
		for (Graph<Integer, SubqueryEdge> g : comps) {
			Graph<SelectBody, SubqueryEdge> simpleG = substituteBack(g, intToSelList);
			results.add(simpleG);
		}
		return results;
	}

	private void convertToIntGraph(Graph<Integer, SubqueryEdge> pRes, QueryExtractor qExtr,
			ArrayList<SelectBody> intToSelList, HashMap<SelectBody, Integer> selToIntMap) {
		Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();
		List<SelectBody> bfsOrder = bfsOrder(query, qExtr.getRoot());
		updateMapping(intToSelList, selToIntMap, bfsOrder);
		for (SelectBody s : query.vertexSet()) {
			int v = selToIntMap.get(s);
			pRes.addVertex(v);
		}
		for (SubqueryEdge e : query.edgeSet()) {
			int src = selToIntMap.get(query.getEdgeSource(e));
			int dest = selToIntMap.get(query.getEdgeTarget(e));
			pRes.addEdge(src, dest, new SubqueryEdge(e));
		}
	}

	private <T> LinkedList<T> bfsOrder(Graph<T, SubqueryEdge> query, T root) {
		LinkedList<T> res = new LinkedList<>();
		Iterator<T> it = new BreadthFirstIterator<>(query, root);
		while (it.hasNext()) {
			res.add(it.next());
		}
		return res;
	}

	private void updateMapping(ArrayList<SelectBody> intToSelList, HashMap<SelectBody, Integer> selToIntMap,
			Collection<SelectBody> newVertices) {
		int base = intToSelList.size();
		int offset = 0;
		for (SelectBody s : newVertices) {
			intToSelList.add(s);
			selToIntMap.put(s, base + offset);
			offset++;
		}
	}

	private HashMap<Integer, Integer> updateMapping(ArrayList<SelectBody> intToSelList,
			HashMap<SelectBody, Integer> selToIntMap, Collection<Integer> newVertices, List<SelectBody> newIntToSelList,
			int vRoot, int sRoot) {
		HashMap<Integer, Integer> res = new HashMap<>();
		res.put(vRoot, sRoot);
		int base = intToSelList.size();
		int offset = 0;
		for (int si : newVertices) {
			int indexInView = si;
			int indexInMain = base + offset;
			res.put(indexInView, indexInMain);

			SelectBody s = newIntToSelList.get(si);
			intToSelList.add(s);
			selToIntMap.put(s, indexInMain);
			offset++;
		}
		return res;
	}

	private void addViews(Graph<Integer, SubqueryEdge> pRes, QueryExtractor qExtr, ArrayList<SelectBody> intToSelList,
			HashMap<SelectBody, Integer> selToIntMap) {
		Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();
		SelectBody root = qExtr.getRoot();
		Map<SelectBody, LinkedList<String>> selectToViewMap = qExtr.getSelectToViewMap();
		Map<String, QueryExtractor> viewToExtractor = qExtr.getViewToGraphMap();

		HashMap<String, Graph<Integer, SubqueryEdge>> viewToGraphMap = new HashMap<>();
		HashMap<String, ArrayList<SelectBody>> viewToIntMapping = new HashMap<>();
		for (Map.Entry<String, QueryExtractor> entry : viewToExtractor.entrySet()) {
			String viewName = entry.getKey();
			QueryExtractor vExtr = entry.getValue();

			ArrayList<SelectBody> vIntToSelList = new ArrayList<>(query.vertexSet().size());
			HashMap<SelectBody, Integer> vSelToIntMap = new HashMap<>();
			Graph<Integer, SubqueryEdge> vRes = new DefaultDirectedGraph<>(SubqueryEdge.class);
			convertToIntGraph(vRes, vExtr, vIntToSelList, vSelToIntMap);
			addViews(vRes, vExtr, vIntToSelList, vSelToIntMap);
			viewToGraphMap.put(viewName, vRes);
			viewToIntMapping.put(viewName, vIntToSelList);
		}

		Iterator<SelectBody> it = new DepthFirstIterator<>(query, root);
		while (it.hasNext()) {
			SelectBody select = it.next();
			if (selectToViewMap.containsKey(select)) {
				for (String viewName : selectToViewMap.get(select)) {
					Graph<Integer, SubqueryEdge> view = viewToGraphMap.get(viewName);
					if (view != null) {
						List<SelectBody> vIntToSelList = viewToIntMapping.get(viewName);
						expandGraph(select, pRes, intToSelList, selToIntMap, view, vIntToSelList);
					}
				}
			}
		}
	}

	private void expandGraph(SelectBody select, Graph<Integer, SubqueryEdge> pRes, ArrayList<SelectBody> intToSelList,
			HashMap<SelectBody, Integer> selToIntMap, Graph<Integer, SubqueryEdge> view,
			List<SelectBody> vIntToSelList) {
		int vRoot = findRoot(view.vertexSet());
		int sRoot = selToIntMap.get(select);

		LinkedList<Integer> viewVertices = bfsOrder(view, vRoot);
		viewVertices.removeFirst(); // remove vRoot
		HashMap<Integer, Integer> viewToPResVerticesMap = updateMapping(intToSelList, selToIntMap, viewVertices,
				vIntToSelList, vRoot, sRoot);

		for (int v : viewToPResVerticesMap.values()) {
			pRes.addVertex(v);
		}
		for (SubqueryEdge e : view.edgeSet()) {
			int src = viewToPResVerticesMap.get(view.getEdgeSource(e));
			int dest = viewToPResVerticesMap.get(view.getEdgeTarget(e));
			pRes.addEdge(src, dest, new SubqueryEdge(e));
		}
	}

	private void keepOnlyJoinEdges(Graph<Integer, SubqueryEdge> result) {
		class ToRemove {
			public ToRemove(int source, int dest) {
				this.source = source;
				this.dest = dest;
			}

			int source;
			int dest;
		}

		LinkedList<ToRemove> toRemove = new LinkedList<>();
		for (SubqueryEdge e : result.edgeSet()) {
			if (e.isOperatorNegated() || !e.getOperator().equals(SubqueryEdge.Operator.JOIN)) {
				int source = result.getEdgeSource(e);
				int dest = result.getEdgeTarget(e);
				toRemove.add(new ToRemove(source, dest));
			}
		}
		for (ToRemove edge : toRemove) {
			result.removeEdge(edge.source, edge.dest);
		}
	}

	private List<Graph<Integer, SubqueryEdge>> computeConnComps(Graph<Integer, SubqueryEdge> result,
			ArrayList<Integer> roots) {
		List<Graph<Integer, SubqueryEdge>> comps = new LinkedList<>();
		ConnectivityInspector<Integer, SubqueryEdge> connInsp = new ConnectivityInspector<>(result);
		List<Set<Integer>> compSets = connInsp.connectedSets();
		for (Set<Integer> cVertices : compSets) {
			int root = findRoot(cVertices);
			roots.add(root);
			Graph<Integer, SubqueryEdge> c = buildComponent(result, cVertices);
			comps.add(c);
		}
		return comps;
	}

	private int findRoot(Set<Integer> vertices) {
		return Collections.min(vertices);
	}

	private Graph<Integer, SubqueryEdge> buildComponent(Graph<Integer, SubqueryEdge> result, Set<Integer> vertices) {
		Graph<Integer, SubqueryEdge> c = new DefaultDirectedGraph<>(SubqueryEdge.class);
		for (int v : vertices) {
			c.addVertex(v);
		}
		for (SubqueryEdge e : result.edgeSet()) {
			int source = result.getEdgeSource(e);
			int dest = result.getEdgeTarget(e);
			if (vertices.contains(source) && vertices.contains(dest)) {
				c.addEdge(source, dest, new SubqueryEdge(e));
			}
		}
		return c;
	}

	private Graph<SelectBody, SubqueryEdge> substituteBack(Graph<Integer, SubqueryEdge> g,
			ArrayList<SelectBody> intToSelList) {
		Graph<SelectBody, SubqueryEdge> f = new DefaultDirectedGraph<>(SubqueryEdge.class);
		for (int v : g.vertexSet()) {
			f.addVertex(intToSelList.get(v));
		}
		for (SubqueryEdge e : g.edgeSet()) {
			SelectBody source = intToSelList.get(g.getEdgeSource(e));
			SelectBody dest = intToSelList.get(g.getEdgeTarget(e));
			f.addEdge(source, dest, new SubqueryEdge(e));
		}
		return f;
	}

}
