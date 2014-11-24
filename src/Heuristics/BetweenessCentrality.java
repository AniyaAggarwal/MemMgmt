package Heuristics;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import abc.NewTest;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.DirectedGraph;

public class BetweenessCentrality {

	Set<SootMethod> methods;
	static private Map<SootMethod, Integer> betweenness=new LinkedHashMap<SootMethod,Integer>();
	//DirectedGraph graph;
	
	public BetweenessCentrality(){
		//graph = NewTest.dg;
		methods = new HashSet<SootMethod>();
		
		for(SootClass c: NewTest.app_classes){
			methods.addAll(c.getMethods());
		}
	}
	
	public void calcMeasure(){
		for(SootMethod m1: methods){
			for(SootMethod m2: methods){
				//setMeasure(Routes.GetAllRoutes(m1, m2));
				if(m1==m2)
					continue;
				
				Set<Route> s = Routes.GetAllRoutes(m1, m2);
				System.out.print(m1 + "\n"+m2+ "\n\n");
		   
				for(Route r: s){
					r.print();
				}
			}
		}
	}
	
	
	public static String print_routes(Set<Route> routes) 
    {
    	
    	int min=1000;
    	//Set<SootMethod> min_route=null;
    	Route min_route=null;
    	for (Route route : routes)
        {
    		//System.out.println(route);
         	if(min>(route.size()-1))
         	{
         	//	System.out.println("Route size: "+route.size());
         		min=route.size()-1;
         		min_route=new Route(route);
       
//         		System.out.println("min route "+min_route);
//         		System.out.println(min_route.get(0));
         	}
    		 
        }
    //System.out.println(min);
  		for(int i=1;i<(min_route.size()-1);i++)
 		{
 			if(betweenness.containsKey(min_route.get(i)))
			{
				int j=betweenness.get(min_route.get(i)).intValue()+1	;		
				betweenness.put(min_route.get(i), new Integer(j));
			}
			else if(!betweenness.containsKey(min_route.get(i)))
			{
				betweenness.put(min_route.get(i), new Integer(1));
			}
 		}
    	return min+"";
    
    	 
    }

	
	
	public void getMeasure(SootMethod m){
		
		
	}
	
}
