package abc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.Local;
import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
import soot.util.Chain;

class MyPointsToAnalysis{
	SootMethod method;
	soot.PointsToAnalysis pta;
	Map<Local, Set<PointsToSet>> points_to_map;
	
	MyPointsToAnalysis(SootMethod m){
		method = m;
	}
	
	public Chain<Local> getAllLocals(){
		return method.getActiveBody().getLocals();
	}
	
	public void init(){
		pta = Scene.v().getPointsToAnalysis();
		Chain<Local> allLocals = getAllLocals();
		if(allLocals.size() <= 0)
			return;
		getPTS_AllLocals(allLocals);
	}
	
	public void getPTS_AllLocals(Chain<Local> locals){
		for(Local local: locals){
			PointsToSet r1 = pta.reachingObjects(local);
			if(points_to_map == null){
				points_to_map = new HashMap<Local, Set<PointsToSet>>();
			}
			if(points_to_map.containsKey(local)){
				points_to_map.get(local).add(r1);
			}
			else{
				Set<PointsToSet> set = new HashSet<PointsToSet>();
				set.add(r1);
				points_to_map.put(local, set);
			}
			if(local.getType() instanceof ArrayType){
				PointsToSet r2 = pta.reachingObjectsOfArrayElement(r1);
				points_to_map.get(local).add(r2);
			}
		}
	}
	
	public Map<Local, Set<Local>> getLocalIntersects(Set<Local> set) {
		
		init();
		Map<Local, Set<Local>> intersect_map = new HashMap<Local, Set<Local>>();
		
		for(Local local: set){
			Set<PointsToSet> local_set = points_to_map.get(local);
			Set<Local> myset = new HashSet<Local>();
			
			for(Local l: points_to_map.keySet()){
				if(l == local)
					continue;
				Set<PointsToSet> l_set = points_to_map.get(l);
	loop1:		for(PointsToSet p1 : local_set){
					for(PointsToSet p2 : l_set){
						if(p1.hasNonEmptyIntersection(p2)){
							myset.add(l);
							break loop1;
						}
					}
				}
			}
			if(myset.size()>0){
				intersect_map.put(local, myset);
			}
		}
		return intersect_map;
	}
		
}	
