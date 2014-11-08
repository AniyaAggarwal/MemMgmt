package abc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.annotation.purity.DirectedCallGraph;
import soot.jimple.toolkits.annotation.purity.SootMethodFilter;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LiveLocals;

class BFS{
	
	private DirectedGraph graph;
	Queue<SootMethod> queue;
	Set<SootMethod> methods_already_seen;
	
	
	/*
	 * In the calling site.. add the following code
	 * BFS bfs = new BFS(new DirectedCallGraph(cg, new Filter(), Scene.v().getEntryPoints().iterator(), false));
	 * 
	 * 
	 * 	Add a private class too..
	  	static private class Filter implements SootMethodFilter {
			public boolean want(SootMethod method) {
				return true;
			}
		}
	 * 
	 * 
	 * 
	 * */
	BFS(DirectedGraph g){
		this.graph = g;
		queue = new LinkedList<SootMethod>();
		methods_already_seen = new HashSet<SootMethod>();
		traverse();
		
	}
	
	private void traverse() {
		// TODO Auto-generated method stub
		List<SootMethod> first_node = graph.getHeads();
		if(first_node.size()>1){
			return;
		}
		queue.add(first_node.get(0));
		methods_already_seen.add(first_node.get(0));
		
		while(!queue.isEmpty()){
			SootMethod m = queue.remove();
			//process m
			
			for(Object succ: graph.getSuccsOf(m)){
				SootMethod succ_m = (SootMethod)succ;
				if(!methods_already_seen.contains(succ_m)){
					methods_already_seen.add(succ_m);
					queue.add(succ_m);
				}
			}
			
		}
		
	}
	
	
}