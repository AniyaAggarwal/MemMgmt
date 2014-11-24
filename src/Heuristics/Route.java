package Heuristics;

import java.util.ArrayList;
import java.util.List;

import soot.SootMethod;

class Route {
		List<SootMethod> route;
		
		Route(){
			route  = new ArrayList<SootMethod>();	
		}

		Route(Route r){
			route  = new ArrayList<SootMethod>(r.getRoute());	
		}

		public boolean contains(SootMethod m) {
			// TODO Auto-generated method stub
			if(route.contains(m))
				return true;
			return false;
		}
		
		public List<SootMethod> getRoute(){
			return route;
		}

		public void insert(SootMethod m) {
			// TODO Auto-generated method stub
			route.add(m);
			
		}

		public void print() {
			// TODO Auto-generated method stub
			System.out.println(route);
		}

		public int size() {
			// TODO Auto-generated method stub
			return route.size();
			
		}

		public SootMethod get(int i) {
			// TODO Auto-generated method stub
			return route.get(i);
		}
	}
