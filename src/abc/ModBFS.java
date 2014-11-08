package abc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import soot.Local;
import soot.Unit;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LiveLocals;
import soot.toolkits.scalar.SimpleLiveLocals;

class ModBFS{
	
	private UnitGraph graph;
	private LiveLocals sll;
	Queue<Unit> queue;
	Set<Unit> units_already_seen;
	Map<Local, Set<Local>> map;
	//Set<Local> lastUsefound, map_keyset;
	Map<Unit, Set<Local>> result_map;
	
	ModBFS(UnitGraph g, Map<Local, Set<Local>> intersect_map){
		this.graph = g;
		map = intersect_map;
	//	map_keyset = new HashSet<Local>();
	//	map_keyset.addAll(map.keySet());
		sll = new MyUseAnalysis(graph);
		queue = new LinkedList<Unit>();
		units_already_seen = new HashSet<Unit>();
		result_map = new HashMap<Unit, Set<Local>>();
	//	lastUsefound = new HashSet<Local>();
		
		findLastUsage_BFS();
		
	}
	
	public Map<Unit, Set<Local>> getResultMap(){
		return result_map;
	}

	private void findLastUsage_BFS() {
		// TODO Auto-generated method stub
		List<Unit> first_node = graph.getHeads();
		if(first_node.size()>1){
			return;
		}
		queue.add(first_node.get(0));
		units_already_seen.add(first_node.get(0));
		
		
		while(!queue.isEmpty()){
			Unit u = queue.remove();
			List<Local> vr = sll.getLiveLocalsAfter(u);
			Set<Local> dead_keys = getAllDeadKeys(sll.getLiveLocalsAfter(u));
			//map_keyset.removeAll(dead_keys);			
			if(!dead_keys.isEmpty()){
				result_map.put(u, dead_keys);
			}
			//units_already_seen.add(u);
			for(Unit succ: graph.getSuccsOf(u)){
				if(!units_already_seen.contains(succ)){
					units_already_seen.add(succ);
					queue.add(succ);
				}
			}
			
		}
		
	}
	
	private  Set<Local> getAllDeadKeys(List<Local> live_var){
		Set<Local> dead_keys = new HashSet<Local>();
		
		for(Map.Entry<Local, Set<Local>> e: map.entrySet()){
			if(!live_var.contains(e.getKey())){
				Set<Local> myset = e.getValue();
				boolean flag = true;
				for(Local l: myset){
					if(live_var.contains(l)){
						flag = false;
						break;
					}
				}
				if(flag){
					dead_keys.add(e.getKey());
				}
			}
		}
		return dead_keys;
	}
	
	
}