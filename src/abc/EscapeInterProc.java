package abc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.Body;
import soot.G;
import soot.Local;
import soot.PointsToSet;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.spark.sets.DoublePointsToSet;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.jimple.toolkits.annotation.purity.SootMethodFilter;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.PurityOptions;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.util.Chain;


class EscapeInterProc extends AbstractInterproceduralAnalysis{
	
	Map<SootMethod, Set<String>> summaries;   // = new HashMap<SootClass, Map<SootMethod, Set<String>>>();
	
	Map<SootMethod, Map<Unit, Loop>> loop_reset = new HashMap<SootMethod, Map<Unit,Loop>>();
	
	
	static private class Filter implements SootMethodFilter {
		public boolean want(SootMethod method) { 
		
		//we can use this method to skip the analysis of java library functions..
		
		
			// could be optimized with HashSet....
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
		super(cg, new Filter(), iterator, true);  //opt.dump_cg());
	//	System.out.println(opt.dump_cg()+ "\t" + opt.dump_intra() + "\t"+ opt.dump_summaries()+ "\t"+opt.verbose());
	//	System.out.println(cg +"\n\n");

		summaries = new HashMap<SootMethod, Set<String>>();
		if(summaries == null){
			System.out.println("Not enough space avaliable..");
			System.exit(1);
		}
		
		doAnalysis(true);   //opts.verbose());
		
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
		//Map<Value, Type> map  = new HashMap<Value, Type>();
		Chain<Local> local_chain = body.getLocals();
		
		//summary for a method is the inset obtained for the first unit in the method.
		Unit unit = units.getFirst(); 
		FlowSet f  = na.inSetMap.get(unit);
		
		copy(f, o);
		
		Map<Local, Set<Local>> points_to_map = new HashMap<Local, Set<Local>>();
		Set<Local> new_locals = new HashSet<Local>();
		
	//	if(! method.getDeclaringClass().toString().equals("dummyMainClass")){
		if(NewTest.app_classes.contains(method.getDeclaringClass())){
			/*get new object creation sites and check if these objects are being escaped from the method in concern*/
			Map<Value, Set<Unit>> m = new HashMap<Value, Set<Unit>>();
			for(Value v: na.newExprMap.keySet()){
				if(!f.contains(v)){
					m.put(v, na.newExprMap.get(v));
					if(v instanceof Local)
						new_locals.add((Local)v);
				}
			}
			
			
			MyPointsToAnalysis my_pta = new MyPointsToAnalysis(method);
			System.out.println("\n\nintersect map is:...\n\n\n"+ my_pta.getLocalIntersects(new_locals));
			
		//	for(Value v: m.keySet()){
				
				
				
			//	EscapeInterProc.printLocalIntersects(local_chain, mylocal, points_to_map);	
		//	}
			
//			System.out.println("\n\n\n\nThe points-to map is: \n\n"+points_to_map);
			
			
		/* Find the loops present in the method */
		/*	LoopFinder loopFinder = new LoopFinder();
			loopFinder.transform(body);
			Collection<Loop> loops = loopFinder.loops();
			if(loops.size() >0){
				for(Loop l : loops){
					List<Stmt> loop_stmt = l.getLoopStatements();
			//		Body bh = new Body(loop_stmt);
			//		BriefUnitGraph loop_graph = new BriefUnitGraph(l.getLoopStatements());
					System.out.println(loop_stmt);
					for(Map.Entry<Value, Set<Unit>> e: m.entrySet()){
						Value val = e.getKey();
						Set<Unit> set = e.getValue();
						for(Unit u : set){
							if(loop_stmt.contains((Stmt)u)){
								if(loop_reset.containsKey(method)){
									loop_reset.get(method).put(u, l);
								}
								else{
									Map<Unit, Loop> map = new HashMap<Unit, Loop>();
									map.put(u, l);
											
									loop_reset.put(method, map);
								}
								
							}
						}
					}
					
				}
			}*/
		}		
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
	
}