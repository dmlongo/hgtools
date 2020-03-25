package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.util.VertexToIntegerMapping;

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

		// removeUselessEdges(pRes);
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
		Iterator<T> it = new BreadthFirstIterator<T, SubqueryEdge>(query, root);
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
		HashMap<SelectBody, LinkedList<String>> selectToViewMap = qExtr.getSelectToViewMap();
		HashMap<String, QueryExtractor> viewToExtractor = qExtr.getViewToGraphMap();

		HashMap<String, Graph<Integer, SubqueryEdge>> viewToGraphMap = new HashMap<>();
		HashMap<String, VertexToIntegerMapping<SelectBody>> viewToIntMapping = new HashMap<>();
		for (String viewName : viewToExtractor.keySet()) {
			ArrayList<SelectBody> vIntToSelList = new ArrayList<>(query.vertexSet().size());
			HashMap<SelectBody, Integer> vSelToIntMap = new HashMap<>();
			Graph<Integer, SubqueryEdge> vRes = new DefaultDirectedGraph<>(SubqueryEdge.class);
			QueryExtractor vExtr = viewToExtractor.get(viewName);
			convertToIntGraph(vRes, vExtr, vIntToSelList, vSelToIntMap);
			addViews(vRes, vExtr, vIntToSelList, vSelToIntMap);
			viewToGraphMap.put(viewName, vRes);
			viewToIntMapping.put(viewName, new VertexToIntegerMapping<SelectBody>(vIntToSelList));
		}

		Iterator<SelectBody> it = new DepthFirstIterator<>(query, root);
		while (it.hasNext()) {
			SelectBody select = it.next();
			if (selectToViewMap.containsKey(select)) {
				for (String viewName : selectToViewMap.get(select)) {
					Graph<Integer, SubqueryEdge> view = viewToGraphMap.get(viewName);
					List<SelectBody> vIntToSelList = viewToIntMapping.get(viewName).getIndexList();
					expandGraph(select, pRes, intToSelList, selToIntMap, view, vIntToSelList);
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

	/*
	 * private void removeUselessEdges(Graph<Integer, SubqueryEdge> result) { class
	 * ToRemove { public ToRemove(int source, int dest) { this.source = source;
	 * this.dest = dest; }
	 * 
	 * int source; int dest; }
	 * 
	 * LinkedList<ToRemove> toRemove = new LinkedList<>(); for (SubqueryEdge e :
	 * result.edgeSet()) { if (e.isOperatorNegated() ||
	 * e.getOperator().equals(SubqueryEdge.Operator.OTHER)) { int source =
	 * result.getEdgeSource(e); int dest = result.getEdgeTarget(e); toRemove.add(new
	 * ToRemove(source, dest)); } } for (ToRemove edge : toRemove) {
	 * result.removeEdge(edge.source, edge.dest); } }
	 */

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

	/**
	 * private void expandGraph(Graph<SelectBody, SubqueryEdge> result, SelectBody
	 * sRoot, Graph<SelectBody, SubqueryEdge> view, SelectBody vRoot) { // TODO
	 * Auto-generated method stub HashSet<SelectBody> visited = new HashSet<>();
	 * visited.add(vRoot); LinkedList<SelectBody> toVisit = new LinkedList<>();
	 * toVisit.addFirst(sRoot); Iterator<SelectBody> viewIt = new
	 * DepthFirstIterator<SelectBody, SubqueryEdge>(view, vRoot); while
	 * (viewIt.hasNext()) { SelectBody vCurr = viewIt.next(); SelectBody currentRoot
	 * = toVisit.removeFirst(); visited.add(currentRoot); for (SubqueryEdge e :
	 * view.outgoingEdgesOf(vCurr)) { SelectBody dest = view.getEdgeTarget(e); if
	 * (!dest.equals(vRoot) && !result.containsVertex(dest)) { // TODO ok, what if
	 * the view is called again in a different select and dest it's // legitimately
	 * another instance of the same select? I have to add the new // vertex // ok,
	 * this graph doesn't admit duplicate vertices result.addVertex(dest); } if
	 * (dest.equals(vRoot)) { result.addEdge(currentRoot, sRoot, e); } else {
	 * result.addEdge(currentRoot, dest, e); } if (!visited.contains(dest)) {
	 * toVisit.addFirst(dest); } } } }
	 * 
	 * private void addViews(QueryExtractor qExtr, Graph<SelectBody, SubqueryEdge>
	 * result) { Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();
	 * SelectBody root = qExtr.getRoot(); HashMap<SelectBody, LinkedList<String>>
	 * selectToViewMap = qExtr.getSelectToViewMap(); HashMap<String, QueryExtractor>
	 * viewToExtractor = qExtr.getViewToGraphMap();
	 * 
	 * HashMap<String, Graph<SelectBody, SubqueryEdge>> viewToGraphMap = new
	 * HashMap<>(); HashMap<String, SelectBody> viewToRootMap = new HashMap<>(); for
	 * (String viewName : viewToExtractor.keySet()) { Graph<SelectBody,
	 * SubqueryEdge> vRes = new DefaultDirectedGraph<>(SubqueryEdge.class);
	 * QueryExtractor vExtr = viewToExtractor.get(viewName); Graphs.addGraph(vRes,
	 * vExtr.getQueryStructure()); addViews(vExtr, vRes);
	 * viewToGraphMap.put(viewName, vRes); viewToRootMap.put(viewName,
	 * vExtr.getRoot()); }
	 * 
	 * Iterator<SelectBody> it = new DepthFirstIterator<>(query, root); while
	 * (it.hasNext()) { SelectBody select = it.next(); if
	 * (selectToViewMap.containsKey(select)) { for (String viewName :
	 * selectToViewMap.get(select)) { Graph<SelectBody, SubqueryEdge> v =
	 * viewToGraphMap.get(viewName); SelectBody vRoot = viewToRootMap.get(viewName);
	 * expandGraph(result, select, v, vRoot); } } } }
	 * 
	 * private void expandGraph(Graph<SelectBody, SubqueryEdge> result, SelectBody
	 * sRoot, Graph<SelectBody, SubqueryEdge> view, SelectBody vRoot) { // TODO
	 * Auto-generated method stub HashSet<SelectBody> visited = new HashSet<>();
	 * visited.add(vRoot); LinkedList<SelectBody> toVisit = new LinkedList<>();
	 * toVisit.addFirst(sRoot); Iterator<SelectBody> viewIt = new
	 * DepthFirstIterator<SelectBody, SubqueryEdge>(view, vRoot); while
	 * (viewIt.hasNext()) { SelectBody vCurr = viewIt.next(); SelectBody currentRoot
	 * = toVisit.removeFirst(); visited.add(currentRoot); for (SubqueryEdge e :
	 * view.outgoingEdgesOf(vCurr)) { SelectBody dest = view.getEdgeTarget(e); if
	 * (!dest.equals(vRoot) && !result.containsVertex(dest)) { // TODO ok, what if
	 * the view is called again in a different select and dest it's // legitimately
	 * another instance of the same select? I have to add the new // vertex // ok,
	 * this graph doesn't admit duplicate vertices result.addVertex(dest); } if
	 * (dest.equals(vRoot)) { result.addEdge(currentRoot, sRoot, e); } else {
	 * result.addEdge(currentRoot, dest, e); } if (!visited.contains(dest)) {
	 * toVisit.addFirst(dest); } } } }
	 * 
	 * public List<Graph<SelectBody, SubqueryEdge>> computeDependencyGraphs() {
	 * Graph<SelectBody, SubqueryEdge> step1 = new
	 * DefaultDirectedGraph<>(SubqueryEdge.class);
	 * 
	 * // setup Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();
	 * Graphs.addGraph(step1, query); addViews(qExtr, step1);
	 * 
	 * HashMap<SelectBody, Integer> selToIntMap = new HashMap<>();
	 * ArrayList<SelectBody> intToSelMap = new ArrayList<>(); Graph<Integer,
	 * SubqueryEdge> step2 = substituteSelectBodies(step1, selToIntMap,
	 * intToSelMap); removeUselessEdges(step2);
	 * 
	 * ArrayList<Integer> roots = new ArrayList<>(); List<Graph<Integer,
	 * SubqueryEdge>> comps = computeConnComps(step2, roots); int i = 0;
	 * LinkedList<Graph<SelectBody, SubqueryEdge>> results = new LinkedList<>(); for
	 * (Graph<Integer, SubqueryEdge> g : comps) { // Graph<SelectBody, SubqueryEdge>
	 * simpleG = simplify(g, roots.get(i++)); Graph<SelectBody, SubqueryEdge>
	 * simpleG = substituteBack(g, intToSelMap); results.add(simpleG); } return
	 * results; }
	 * 
	 * private LinkedList<Graph<SelectBody, SubqueryEdge>> substituteBack(
	 * LinkedList<Graph<Integer, SubqueryEdge>> preResults, ArrayList<SelectBody>
	 * intToSelMap) { LinkedList<Graph<SelectBody, SubqueryEdge>> output = new
	 * LinkedList<>(); for (Graph<Integer, SubqueryEdge> g : preResults) {
	 * Graph<SelectBody, SubqueryEdge> f = substituteBack(g, intToSelMap);
	 * output.add(f); } return output; }
	 * 
	 * private Graph<Integer, SubqueryEdge> substituteSelectBodies(Graph<SelectBody,
	 * SubqueryEdge> step1, HashMap<SelectBody, Integer> selToIntMap,
	 * ArrayList<SelectBody> intToSelMap) { Graph<Integer, SubqueryEdge> g = new
	 * DefaultDirectedGraph<>(SubqueryEdge.class); Iterator<SelectBody> it = new
	 * DepthFirstIterator<>(step1, qExtr.getRoot()); while (it.hasNext()) {
	 * SelectBody sel = it.next(); int id = intToSelMap.size();
	 * intToSelMap.add(sel); selToIntMap.put(sel, id); g.addVertex(id); } for
	 * (SubqueryEdge e : step1.edgeSet()) { int sourceID =
	 * selToIntMap.get(step1.getEdgeSource(e)); int destID =
	 * selToIntMap.get(step1.getEdgeTarget(e)); g.addEdge(sourceID, destID, e); }
	 * return g; }
	 * 
	 * private Graph<SelectBody, SubqueryEdge> simplify(Graph<Integer, SubqueryEdge>
	 * f, int root) { // TODO Auto-generated method stub Graph<SelectBody,
	 * SubqueryEdge> output = new DefaultDirectedGraph<>(SubqueryEdge.class);
	 * 
	 * ArrayList<PlainSelect> selects = new ArrayList<>(f.vertexSet().size());
	 * List<Integer> ordVertices =
	 * f.vertexSet().stream().collect(Collectors.toList());
	 * Collections.sort(ordVertices); for (int v : ordVertices) {
	 * 
	 * PlainSelect s = new PlainSelect(); s.addSelectItems(new
	 * SelectExpressionItem(new Column("" + v))); selects.add(v, s);
	 * output.addVertex(s); } for (SubqueryEdge e : f.edgeSet()) { int v =
	 * f.getEdgeSource(e); int w = f.getEdgeTarget(e);
	 * output.addEdge(selects.get(v), selects.get(w)); }
	 * 
	 * return output; }
	 * 
	 * private void setup(Graph<SelectBody, SubqueryEdge> result) {
	 * Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();
	 * Graphs.addGraph(result, query);
	 * 
	 * // // ArrayList<SelectBody> vertices = new
	 * ArrayList<>(query.vertexSet().size()); // for (SelectBody v :
	 * query.vertexSet()) { vertices.add(v); // result.addVertex(v); } // // for
	 * (int i = 0; i < vertices.size() - 1; i++) { for (int j = i + 1; j < //
	 * vertices.size(); j++) { SelectBody sourceVertex = vertices.get(i); SelectBody
	 * // targetVertex = vertices.get(j); if (query.containsEdge(sourceVertex, //
	 * targetVertex)) { SubqueryEdge e = query.getEdge(sourceVertex, targetVertex);
	 * // result.addEdge(sourceVertex, targetVertex, e); } if //
	 * (query.containsEdge(targetVertex, sourceVertex)) { SubqueryEdge e = //
	 * query.getEdge(targetVertex, sourceVertex); result.addEdge(targetVertex, //
	 * sourceVertex, e); } } } // }
	 * 
	 * private void addViews(QueryExtractor qExtr, Graph<SelectBody, SubqueryEdge>
	 * result) { Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();
	 * SelectBody root = qExtr.getRoot(); HashMap<SelectBody, LinkedList<String>>
	 * selectToViewMap = qExtr.getSelectToViewMap(); HashMap<String, QueryExtractor>
	 * viewToExtractor = qExtr.getViewToGraphMap();
	 * 
	 * HashMap<String, Graph<SelectBody, SubqueryEdge>> viewToGraphMap = new
	 * HashMap<>(); HashMap<String, SelectBody> viewToRootMap = new HashMap<>(); for
	 * (String viewName : viewToExtractor.keySet()) { Graph<SelectBody,
	 * SubqueryEdge> vRes = new DefaultDirectedGraph<>(SubqueryEdge.class);
	 * QueryExtractor vExtr = viewToExtractor.get(viewName); Graphs.addGraph(vRes,
	 * vExtr.getQueryStructure()); addViews(vExtr, vRes);
	 * viewToGraphMap.put(viewName, vRes); viewToRootMap.put(viewName,
	 * vExtr.getRoot()); }
	 * 
	 * Iterator<SelectBody> it = new DepthFirstIterator<>(query, root); while
	 * (it.hasNext()) { SelectBody select = it.next(); if
	 * (selectToViewMap.containsKey(select)) { for (String viewName :
	 * selectToViewMap.get(select)) { Graph<SelectBody, SubqueryEdge> v =
	 * viewToGraphMap.get(viewName); SelectBody vRoot = viewToRootMap.get(viewName);
	 * expandGraph(result, select, v, vRoot); } } } }
	 * 
	 * private void expandGraph(Graph<SelectBody, SubqueryEdge> result, SelectBody
	 * sRoot, Graph<SelectBody, SubqueryEdge> view, SelectBody vRoot) { // TODO
	 * Auto-generated method stub HashSet<SelectBody> visited = new HashSet<>();
	 * visited.add(vRoot); LinkedList<SelectBody> toVisit = new LinkedList<>();
	 * toVisit.addFirst(sRoot); Iterator<SelectBody> viewIt = new
	 * DepthFirstIterator<SelectBody, SubqueryEdge>(view, vRoot); while
	 * (viewIt.hasNext()) { SelectBody vCurr = viewIt.next(); SelectBody currentRoot
	 * = toVisit.removeFirst(); visited.add(currentRoot); for (SubqueryEdge e :
	 * view.outgoingEdgesOf(vCurr)) { SelectBody dest = view.getEdgeTarget(e); if
	 * (!dest.equals(vRoot) && !result.containsVertex(dest)) { // TODO ok, what if
	 * the view is called again in a different select and dest it's // legitimately
	 * another instance of the same select? I have to add the new // vertex // ok,
	 * this graph doesn't admit duplicate vertices result.addVertex(dest); } if
	 * (dest.equals(vRoot)) { result.addEdge(currentRoot, sRoot, e); } else {
	 * result.addEdge(currentRoot, dest, e); } if (!visited.contains(dest)) {
	 * toVisit.addFirst(dest); } } } }
	 * 
	 * private void addViews(Graph<SelectBody, SubqueryEdge> result) {
	 * Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure(); SelectBody
	 * root = qExtr.getRoot(); HashMap<SelectBody, LinkedList<String>>
	 * selectToViewMap = qExtr.getSelectToViewMap(); HashMap<String, QueryExtractor>
	 * viewToGraphMap = qExtr.getViewToGraphMap(); for (QueryExtractor vExtr :
	 * viewToGraphMap.values()) {
	 * 
	 * }
	 * 
	 * Iterator<SelectBody> it = new DepthFirstIterator<>(query, root); while
	 * (it.hasNext()) { SelectBody select = it.next(); if
	 * (selectToViewMap.containsKey(select)) { for (String viewName :
	 * selectToViewMap.get(select)) { QueryExtractor vExtr =
	 * viewToGraphMap.get(viewName); Graph<SelectBody, SubqueryEdge> view =
	 * vExtr.getQueryStructure(); SelectBody vRoot = vExtr.getRoot();
	 * HashSet<SelectBody> visited = new HashSet<>(); visited.add(vRoot);
	 * LinkedList<SelectBody> toVisit = new LinkedList<>();
	 * toVisit.addFirst(select); Iterator<SelectBody> viewIt = new
	 * DepthFirstIterator<SelectBody, SubqueryEdge>(view, vRoot); while
	 * (viewIt.hasNext()) { SelectBody vCurr = viewIt.next(); SelectBody currentRoot
	 * = toVisit.removeFirst(); visited.add(currentRoot); for (SubqueryEdge e :
	 * view.outgoingEdgesOf(vCurr)) { SelectBody dest = view.getEdgeTarget(e); if
	 * (!dest.equals(vRoot) && !result.containsVertex(dest)) { // TODO ok, what if
	 * the view is called again in a different select and dest it's // legitimately
	 * another instance of the same select? I have to add the new // vertex
	 * result.addVertex(dest); } if (dest.equals(vRoot)) {
	 * result.addEdge(currentRoot, select, e); } else { result.addEdge(currentRoot,
	 * dest, e); } if (!visited.contains(dest)) { toVisit.addFirst(dest); } } } } }
	 * } }
	 */
}
