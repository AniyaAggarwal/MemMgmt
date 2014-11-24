package abc;

import instrument.InsUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.ArrayType;
import soot.Body;
import soot.G;
import soot.Local;
import soot.PointsToSet;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.GotoStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.jimple.toolkits.annotation.purity.SootMethodFilter;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.PurityOptions;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.LiveLocals;
import soot.util.Chain;

class EscapeInterProc extends AbstractInterproceduralAnalysis{
	
	Map<SootMethod, Set<String>> summaries;
	
	Map<SootMethod, Map<Unit, Loop>> loop_reset = new HashMap<SootMethod, Map<Unit,Loop>>();
	Map<SootMethod, Map<Local, Set<Unit>>> result_map = new HashMap<SootMethod, Map<Local, Set<Unit>>>();
	
	
	Map<Loop, Unit> loop_firstStmt = new HashMap<Loop, Unit>();
	
	static private class Filter implements SootMethodFilter {
		public boolean want(SootMethod method) {
		//we can use this method to skip the analysis of java library functions..
		
			//could be optimized with HashSet....
//			String c = method.getDeclaringClass().toString();
//			String m = method.getName();
//			String[][] o = PurityInterproceduralAnalysis.pureMethods;
//			for (String[] element : o)
//				if (m.equals(element[1]) && c.startsWith(element[0])) return false;
//			    o = PurityInterproceduralAnalysis.impureMethods;
//			    for (String[] element : o)
//					if (m.equals(element[1]) && c.startsWith(element[0])) return false;
//			    o = PurityInterproceduralAnalysis.alterMethods;
//			    for (String[] element : o)
//					if (m.equals(element[1]) && c.startsWith(element[0])) return false;
			return true;
		}
	}
	
	public EscapeInterProc(CallGraph cg, Iterator<SootMethod> iterator, PurityOptions opt) {
		// TODO Auto-generated constructor stub
		super(cg, new Filter(), iterator, true);  
		summaries = new HashMap<SootMethod, Set<String>>();
		if(summaries == null){
			System.out.println("Not enough space avaliable..");
			System.exit(1);
		}
		doAnalysis(true);
	}

	@Override
	protected void analyseMethod(SootMethod method, Object o) {
		// TODO Auto-generated method stub
		
		//Call intra-procedural analysis for method.
		G.v().out.println("Analyzing Method : "+ method.getSignature());
		
		Body body = method.retrieveActiveBody();
		//ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
		/*if(method.toString().equals("<java.lang.Class: java.lang.String getSimpleName()>"))
			System.out.println("here");
		*/
		
		BriefUnitGraph bg = new BriefUnitGraph(body);
		
		NullnessAnalysis na = new NullnessAnalysis(bg, this);
		Chain<Unit> units = body.getUnits();
		Chain<Local> local_chain = body.getLocals();
		
		//summary for a method is the inset obtained for the first unit in the method.
		Unit unit = units.getFirst(); 
		
		FlowSet f  = na.inSetMap.get(unit);
		
		copy(f, o);
		
		
		Map<Local, Set<Local>> points_to_map = new HashMap<Local, Set<Local>>();
		Set<Local> new_locals = new HashSet<Local>();
		
	//	if(! method.getDeclaringClass().toString().equals("dummyMainClass")){
		if(NewTest.app_classes.contains(method.getDeclaringClass())){
			
			InsUtil util = new InsUtil();
			util.setNewExprMap(na.newExprMap);
			
			
			/* get new object creation sites and check if these objects are being escaped from the method in concern */
			Map<Value, Set<Unit>> m = new HashMap<Value, Set<Unit>>();
			for(Value v: na.newExprMap.keySet()){
				if(!f.contains(v)){
					m.put(v, na.newExprMap.get(v));
				/*	if(v instanceof Local)
						new_locals.add((Local)v);
				*/
				}
			}
			
			//For OptMethodCallInLoop heuristics
			/*
			if(m.size()>0){
				Set<Unit> nonEscAlloc = new HashSet<Unit>();
				for(Map.Entry<Value, Set<Unit>> e :m.entrySet()){
					//check if non escaping variable is one of the parameters of the method
					if(getFormalParamaters(method).contains(e.getKey())){
						nonEscAlloc.addAll(e.getValue());
					}	
				}
				NewTest.nonEscNewAlloc.put(method, nonEscAlloc);
				
			}
			*/
			
			Set<Unit> nonEscAlloc = new HashSet<Unit>();
			for(Set<Unit> u : m.values()){
				nonEscAlloc.addAll(u);
				
			}
			NewTest.nonEscNewAlloc.put(method, nonEscAlloc);
			
			
			/*get new object allocations present in loop*/
			new_locals = getNewAllocInLoop(body, m);
			
			MyPointsToAnalysis my_pta = new MyPointsToAnalysis(method);
			LiveLocals sll = new MyUseAnalysis(bg);
			Map<Local, Set<Unit>> insert_before_map = detectInsPoint(sll, my_pta.getLocalIntersects(new_locals), units);
			filter(insert_before_map, bg);
			
			System.out.println("Modified map...: "+ insert_before_map+"\n\n");
			
			//if object needs to be saved before the first stmt after the loop..then make sure buffer saving occurs after the last stmt in the loop itself and not outside the loop
			for(Entry<Local, Set<Unit>> e : insert_before_map.entrySet()){
				for(Unit u : e.getValue()){
					System.out.println(bg.getPredsOf(u));
					for(Unit u1: bg.getPredsOf(u)){
						System.out.println(bg.getPredsOf(u1));
					}
					
					for(Unit u1 : loop_firstStmt.values()){
						if(bg.getPredsOf(u).contains(u1)){
							System.out.println(bg.getPredsOf(u1));
							for(Unit u2:bg.getPredsOf(u1)){
								if(u2 instanceof GotoStmt){
									System.out.println(bg.getPredsOf(u2));
									u = u2;
								}
							}
							
						}
					}
				}
			}
			
			System.out.println("Modified map...: "+ insert_before_map+"\n\n");
			
			if(insert_before_map.size()>0){
				result_map.put(method, insert_before_map);
				util.setSavetoBuf(insert_before_map);
			}
			
		//	System.out.println(na.newExprMap);
		//	System.out.println(util.getNewExprMap());
			NewTest.result_map.put(method, util);
			
			/*
			BFS bfs = new BFS(bg, my_pta.getLocalIntersects(new_locals));
			System.out.println("\n\nintersect map is:..."+ my_pta.getLocalIntersects(new_locals)+"\n\n\n");
			
			System.out.println("bfs map "+bfs.getResultMap()+"\n\n");
			*/
		/*	
			System.out.println("Use Analysis : \n\n");
			//LiveLocals sll = new MyUseAnalysis(bg);
			for(Unit u: body.getUnits()){
				System.out.println(sll.getLiveLocalsBefore(u));
				System.out.println(u);
				System.out.println(sll.getLiveLocalsAfter(u)+"\n\n");
			}
		*/	
		//	for(Value v: m.keySet()){
				//	EscapeInterProc.printLocalIntersects(local_chain, mylocal, points_to_map);	
		//	}
//			System.out.println("\n\n\n\nThe points-to map is: \n\n"+points_to_map);
		}		
		
	}

	private Set<Local> getNewAllocInLoop(Body body, Map<Value, Set<Unit>> m) {
		// TODO Auto-generated method stub
		
		Set<Local> new_alloc_loop = new HashSet<Local>();
		LoopFinder loopFinder = new LoopFinder();
		loopFinder.transform(body);
		Collection<Loop> loops = loopFinder.loops();
		
		if(NewTest.app_classes.contains(body.getMethod().getDeclaringClass()) && loops.size()>0)
		{
			Set<Loop> set = new HashSet<Loop>();
			set.addAll(loops);
			NewTest.methodToLoop_map.put(body.getMethod(), set);
		}
			
		if(loops.size() > 0){
			for(Loop l : loops){
				loop_firstStmt.put(l, l.getHead());
				List<Stmt> loop_stmt = l.getLoopStatements();
				for(Map.Entry<Value, Set<Unit>> e: m.entrySet()){
					Value val = e.getKey();
					Set<Unit> set = e.getValue();
					for(Unit u : set){
						if(loop_stmt.contains((Stmt)u)){
							if(val instanceof Local)
								new_alloc_loop.add((Local)val);
						}
					}
				}
				
			}
		}
		
		return new_alloc_loop;
		
	}

	private void filter(Map<Local, Set<Unit>> instrument_map, UnitGraph g) {
		// TODO Auto-generated method stub
		Map<Local, Set<Unit>> map_copy = new HashMap<Local, Set<Unit>>();
		Set<Unit> succs = new HashSet<Unit>();
		
		for(Map.Entry<Local, Set<Unit>>e: instrument_map.entrySet()){
			Set<Unit>s = new HashSet<Unit>();
			s.addAll(e.getValue());
			map_copy.put(e.getKey(), s);
			for(Unit u : e.getValue()){
				succs.addAll(g.getSuccsOf(u));
			}
		}
		for(Map.Entry<Local, Set<Unit>>e: map_copy.entrySet()){
			Set<Unit> val = map_copy.get(e.getKey());
			for(Unit u : val){
				if(succs.contains(u)){
					instrument_map.get(e.getKey()).remove(u);
				}
			}
		}
	}
	
	
	
//before and after both doesnot contain local and the objs pointing to it.
	private Map<Local, Set<Unit>> detectInsPoint(LiveLocals sll,
			Map<Local, Set<Local>> localIntersects, Chain<Unit> units) {
		// TODO Auto-generated method stub
		
		Map<Local, Set<Unit>> map = new HashMap<Local, Set<Unit>>();
		
		for(Unit u: units){
			List<Local> before = sll.getLiveLocalsBefore(u);
			List<Local> after = sll.getLiveLocalsAfter(u);
			System.out.println("\n"+before);
			System.out.println(u);
			System.out.println("\n"+after);
			
			for(Map.Entry<Local, Set<Local>> e: localIntersects.entrySet()){
				Set<Local> myset = e.getValue();
				Local key = e.getKey();
		//		boolean aa_flag = true;
				if(!after.contains(key)){    //if(!after.contains(key)){
			//		aa_flag = false;
					boolean a_flag = true;
					for(Local l: myset){
						if(after.contains(l)){
							a_flag = false;
							break;
						}
					}
				
					if(a_flag){
						if(!before.contains(key)){
							boolean b_flag = true;
							for(Local l: myset){
								if(before.contains(l)){
									b_flag = false;
									break;
								}
							}
							if(b_flag)
								addToMap(key, u, map);	
						}/*
						if(before.contains(key)){
							addToMap(key, u, map);
						}*/
					}
				}
			}
		
		}
	//	System.out.println("\n\nThe intersection map is... \n"+ map);
		return map;
		
	}
	
	

	private void addToMap(Local key, Unit u, Map<Local, Set<Unit>> map) {
		// TODO Auto-generated method stub
		Set<Unit> set;
		if(!map.containsKey(key)){
			set = new HashSet<Unit>();
		}
		else{
			set = map.get(key);
		}
		set.add(u);
		map.put(key, set);
	}

	@Override
	protected void applySummary(Object src,
			 					Stmt   callStmt,
			 					Object summary,
			 					Object dst) {
		//HashSet<String> source  = (HashSet<String>)src;
		FlowSet source  = (FlowSet)src;
		FlowSet dest  = (FlowSet)dst;
		FlowSet summ = (FlowSet)summary;
		
		if(summ.size() > 0){
			//Set<String> new_src = new HashSet<String>();
			//new_src.addAll(source);
			source.union(summ, dest);
		}
		
		/*
		Map<Value, Class> actual_args_map = new HashMap<Value, Class>();
		
		if (callStmt.containsInvokeExpr()) {
        	G.v().out.println("\n\ninvoke stmt is: \n"+ callStmt);
        	G.v().out.println("method call use box  " + callStmt.getUseBoxes().toString() + "\n\n");
        	Iterator it = callStmt.getUseBoxes().iterator();
        	
        	//Get actual arguments
        	while(it.hasNext()){
        		ValueBox v = (ValueBox) it.next();
        		//Value val = v.getValue();
        		if(v instanceof ImmediateBox){
        			G.v().out.println("immediate box: "+ v.getValue().toString() + v.getValue().getClass());
        		//	actual_args.add((JimpleLocal) v.getValue());
        			actual_args_map.put(v.getValue(), v.getValue().getClass());
        		}	
        	}
        	
        }
		*/
		
		
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void copy(Object src, Object dest) {
		// TODO Auto-generated method stub
		
		FlowSet sourceSet = (FlowSet)src,
		destSet = (FlowSet) dest;
		sourceSet.copy(destSet);
	}

	@Override
	protected void merge(Object src1, Object src2, Object dest) {
		// TODO Auto-generated method stub
		
		FlowSet srcSet1 = (FlowSet) src1;
        FlowSet srcSet2 = (FlowSet) src2;
        FlowSet destSet = (FlowSet) dest;
        srcSet1.union(srcSet2, destSet);

	}

	@Override
	protected Object newInitialSummary() {
		// TODO Auto-generated method stub
		
		
		return new ArraySparseSet();
		
		//return new HashSet<String>(); //null;
	}

	@Override
	protected Object summaryOfUnanalysedMethod(SootMethod method) {
		// TODO Auto-generated method stub
		
	//	SootClass c = method.getDeclaringClass();
		
		if(summaries.containsKey(method)){
				return summaries.get(method);
		}
		
		return null;		
	}
	
	private static void printLocalIntersects(Chain<Local>/*<Integer,Local>*/ ls, Local mylocal, Map<Local, Set<Local>> points_to_map) {
		
		soot.PointsToAnalysis pta = NewTest.pa;
		
		Set<PointsToSet> set = new HashSet<PointsToSet>();
	
		Set<Local> points_to_vars = new HashSet<Local>();
		
		PointsToSet r1 = pta.reachingObjects(mylocal);
		
		set.add(r1);
		//	System.out.println(mylocal.getType().toString());
		if(mylocal.getType() instanceof ArrayType){
			PointsToSet rx = pta.reachingObjectsOfArrayElement(r1);
			set.add(rx);
		}
		
		for(Local l2: ls){
			PointsToSet r2 = pta.reachingObjects(l2);	
			if (mylocal != l2){
				if(l2.getType() instanceof ArrayType){
					
				}
				for(PointsToSet s:set){
					if(s.hasNonEmptyIntersection(r2))
						points_to_vars.add(l2);
						//System.out.println(mylocal+ "intersect "+ l2 + " ? "+ s.hasNonEmptyIntersection(r2));
				}
			}
		}
		points_to_map.put(mylocal, points_to_vars);
	}

	public Map<SootMethod, Map<Local, Set<Unit>>> getResultMap() {
		// TODO Auto-generated method stub
		return result_map;
	}
	
}