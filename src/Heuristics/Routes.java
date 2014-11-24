package Heuristics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import abc.NewTest;

import soot.SootMethod;
import soot.toolkits.graph.DirectedGraph;
public class Routes {
	
	static Set<Route> routes;// = new ArrayList<Route>();
	static DirectedGraph graph;
			
	
	public static Set<Route> GetAllRoutes(SootMethod start, SootMethod end) 
    {
        //List<List<SootMethod>> routes = new ArrayList<List<SootMethod>>();
		
		graph = NewTest.dg;
		routes = new HashSet<Route>();
		
		Route r = new Route();
		r.insert(start);
		r.insert(end);
		
		Chain(routes, r , start, end);
		
		return routes;
		/*
		
        List<SootMethod> rights = graph.getSuccsOf(a);//left_map_rights.get(a);
        if (rights != null)
        {
            for (SootMethod right : rights) 
            {
                List<SootMethod> route = new ArrayList<SootMethod>();
                route.add(a);
                route.add(right);
              //  System.out.println(route);
                Chain(routes, null , right, aend);
            }
        }
        return routes;*/
    }

    public static void Chain(Set<Route> routes, Route route, SootMethod start, SootMethod last) 
    {
        //if no path found..ie exit statement encountered!
    	if(start==null)
    		return;
    	if(graph.getTails().contains(start)){
        	return;
        }
    	
    	//List l = graph.getTails();
    	
    	if (start.equals(last)) 
        {
            if(route != null){
            //	route.insert(last);
            	routes.add(route);
            }
           // System.out.println("Reached");
           // System.out.println("Routes "+routes);
            return ;
        }
        List<SootMethod> rights = graph.getSuccsOf(start);//left_map_rights.get(right2);
        if (rights != null) 
        {
            if(route == null)
            	route = new Route();
        	for (SootMethod right : rights) 
            {
        		//Route new_route = new Route();
                if (!route.contains(right)) 
                {
                    Route new_route = new Route(route);
                	
                //	new_route.insert(start);
                    new_route.insert(right);
                //    System.out.println(new_route);
                    Chain(routes, new_route, right, last);
                }
            }
        }
    }
}
