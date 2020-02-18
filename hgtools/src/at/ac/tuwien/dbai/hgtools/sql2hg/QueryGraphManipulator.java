package at.ac.tuwien.dbai.hgtools.sql2hg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
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

	public List<Graph<SelectBody, SubqueryEdge>> computeDependencyGraphs() {
		Graph<SelectBody, SubqueryEdge> step1 = new DefaultDirectedGraph<>(SubqueryEdge.class);
		setup(step1);
		addViews(step1);

		HashMap<SelectBody, Integer> selToIntMap = new HashMap<>();
		ArrayList<SelectBody> intToSelMap = new ArrayList<>();
		Graph<Integer, SubqueryEdge> step2 = substituteSelectBodies(step1, selToIntMap, intToSelMap);
		removeUselessEdges(step2);

		ArrayList<Integer> roots = new ArrayList<>();
		List<Graph<Integer, SubqueryEdge>> comps = computeConnComps(step2, roots);
		int i = 0;
		LinkedList<Graph<SelectBody, SubqueryEdge>> results = new LinkedList<>();
		for (Graph<Integer, SubqueryEdge> g : comps) {
			Graph<SelectBody, SubqueryEdge> simpleG = simplify(g, roots.get(i++));
			results.add(simpleG);
		}
		return results;
	}

	private Graph<SelectBody, SubqueryEdge> substituteBack(Graph<Integer, SubqueryEdge> g,
			ArrayList<SelectBody> intToSelMap) {
		Graph<SelectBody, SubqueryEdge> f = new DefaultDirectedGraph<>(SubqueryEdge.class);
		for (int v : g.vertexSet()) {
			f.addVertex(intToSelMap.get(v));
		}
		for (SubqueryEdge e : g.edgeSet()) {
			SelectBody source = intToSelMap.get(g.getEdgeSource(e));
			SelectBody dest = intToSelMap.get(g.getEdgeTarget(e));
			f.addEdge(source, dest, e);
		}
		return f;
	}

	private LinkedList<Graph<SelectBody, SubqueryEdge>> substituteBack(
			LinkedList<Graph<Integer, SubqueryEdge>> preResults, ArrayList<SelectBody> intToSelMap) {
		LinkedList<Graph<SelectBody, SubqueryEdge>> output = new LinkedList<>();
		for (Graph<Integer, SubqueryEdge> g : preResults) {
			Graph<SelectBody, SubqueryEdge> f = substituteBack(g, intToSelMap);
			output.add(f);
		}
		return output;
	}

	private Graph<Integer, SubqueryEdge> substituteSelectBodies(Graph<SelectBody, SubqueryEdge> step1,
			HashMap<SelectBody, Integer> selToIntMap, ArrayList<SelectBody> intToSelMap) {
		Graph<Integer, SubqueryEdge> g = new DefaultDirectedGraph<>(SubqueryEdge.class);
		Iterator<SelectBody> it = new DepthFirstIterator<>(step1, qExtr.getRoot());
		while (it.hasNext()) {
			SelectBody sel = it.next();
			int id = intToSelMap.size();
			intToSelMap.add(sel);
			selToIntMap.put(sel, id);
			g.addVertex(id);
		}
		for (SubqueryEdge e : step1.edgeSet()) {
			int sourceID = selToIntMap.get(step1.getEdgeSource(e));
			int destID = selToIntMap.get(step1.getEdgeTarget(e));
			g.addEdge(sourceID, destID, e);
		}
		return g;
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

	private Integer findRoot(Set<Integer> vertices) {
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
				c.addEdge(source, dest, e);
			}
		}
		return c;
	}

	private Graph<SelectBody, SubqueryEdge> simplify(Graph<Integer, SubqueryEdge> f, int selectBody) {
		// TODO Auto-generated method stub
		Graph<SelectBody, SubqueryEdge> output = new DefaultDirectedGraph<>(SubqueryEdge.class);
		
		return output;
	}

	private void setup(Graph<SelectBody, SubqueryEdge> result) {
		Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();
		Graphs.addGraph(result, query);

		/*
		 * ArrayList<SelectBody> vertices = new ArrayList<>(query.vertexSet().size());
		 * for (SelectBody v : query.vertexSet()) { vertices.add(v);
		 * result.addVertex(v); }
		 * 
		 * for (int i = 0; i < vertices.size() - 1; i++) { for (int j = i + 1; j <
		 * vertices.size(); j++) { SelectBody sourceVertex = vertices.get(i); SelectBody
		 * targetVertex = vertices.get(j); if (query.containsEdge(sourceVertex,
		 * targetVertex)) { SubqueryEdge e = query.getEdge(sourceVertex, targetVertex);
		 * result.addEdge(sourceVertex, targetVertex, e); } if
		 * (query.containsEdge(targetVertex, sourceVertex)) { SubqueryEdge e =
		 * query.getEdge(targetVertex, sourceVertex); result.addEdge(targetVertex,
		 * sourceVertex, e); } } }
		 */
	}

	private void addViews(Graph<SelectBody, SubqueryEdge> result) {
		Graph<SelectBody, SubqueryEdge> query = qExtr.getQueryStructure();
		SelectBody root = qExtr.getRoot();
		HashMap<SelectBody, LinkedList<String>> selectToViewMap = qExtr.getSelectToViewMap();
		HashMap<String, QueryExtractor> viewToGraphMap = qExtr.getViewToGraphMap();

		Iterator<SelectBody> it = new DepthFirstIterator<>(query, root);
		while (it.hasNext()) {
			SelectBody select = it.next();
			if (selectToViewMap.containsKey(select)) {
				for (String viewName : selectToViewMap.get(select)) {
					QueryExtractor vExtr = viewToGraphMap.get(viewName);
					Graph<SelectBody, SubqueryEdge> view = vExtr.getQueryStructure();
					SelectBody vRoot = vExtr.getRoot();
					HashSet<SelectBody> visited = new HashSet<>();
					visited.add(vRoot);
					LinkedList<SelectBody> toVisit = new LinkedList<>();
					toVisit.addFirst(select);
					Iterator<SelectBody> viewIt = new DepthFirstIterator<SelectBody, SubqueryEdge>(view, vRoot);
					while (viewIt.hasNext()) {
						SelectBody vCurr = viewIt.next();
						SelectBody currentRoot = toVisit.removeFirst();
						visited.add(currentRoot);
						for (SubqueryEdge e : view.outgoingEdgesOf(vCurr)) {
							SelectBody dest = view.getEdgeTarget(e);
							if (!dest.equals(vRoot) && !result.containsVertex(dest)) {
								// TODO ok, what if the view is called again in a different select and dest it's
								// legitimately another instance of the same select? I have to add the new
								// vertex
								result.addVertex(dest);
							}
							if (dest.equals(vRoot)) {
								result.addEdge(currentRoot, select, e);
							} else {
								result.addEdge(currentRoot, dest, e);
							}
							if (!visited.contains(dest)) {
								toVisit.addFirst(dest);
							}
						}
					}
				}
			}
		}
	}

	private void removeUselessEdges(Graph<Integer, SubqueryEdge> result) {
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
			if (e.isOperatorNegated() || e.getOperator().equals(SubqueryEdge.Operator.OTHER)) {
				int source = result.getEdgeSource(e);
				int dest = result.getEdgeTarget(e);
				toRemove.add(new ToRemove(source, dest));
			}
		}
		for (ToRemove edge : toRemove) {
			result.removeEdge(edge.source, edge.dest);
		}
	}

}
