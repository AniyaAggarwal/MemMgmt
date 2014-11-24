package Heuristics;

import java.util.Map;
import java.util.Set;

import abc.NewTest;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.annotation.logic.Loop;

/**
 * Heuristic 2: Given a method from which some newly allocated object does not escape its scope, detect if such
 * a method is called from within a loop from any other method.
 * 
 * Please note this hueristic will be useful only if the object to which memory is allocated is one of the
 * parameters of the method.
 * 
 * @author Ani
 *
 */
public class OptMethodCallInLoop {
	
	public void init(){
		for(Map.Entry<SootMethod, Set<Loop>> e: NewTest.methodToLoop_map.entrySet()){
			for(Map.Entry<SootMethod, Set<Unit>> e2 : NewTest.nonEscNewAlloc.entrySet()){
				if(Routes.GetAllRoutes(e.getKey(), e2.getKey()).size() > 0){
					System.out.println(e2.getKey().toString()+ " is called from within loop "+e.getKey().toString());
				}
			}
		}
			
	}
	

}
