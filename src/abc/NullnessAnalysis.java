package abc;

import soot.*;
import soot.jimple.*;
import abc.AbstractInterproceduralAnalysis;
import soot.toolkits.scalar.*;
import soot.toolkits.graph.*;
import soot.util.*;
import java.util.*;

import polyglot.types.FieldInstance;

class NullnessAnalysis extends BackwardFlowAnalysis
{
	 private UnitGraph g;
	 public Map<Unit, FlowSet> inSetMap;
	 public Map<Unit,FlowSet> outSetMap;
	 boolean repeat_flag = true;
	 
	 public Map<Value, Set<Unit>> newExprMap;
	 
	 AbstractInterproceduralAnalysis inter;
	 
	 protected void copy(Object src, Object dest){
        FlowSet sourceSet = (FlowSet)src,
        destSet = (FlowSet) dest;
        sourceSet.copy(destSet);
	 }

	 protected void merge(Object src1, Object src2, Object dest){
        FlowSet srcSet1 = (FlowSet) src1;
        FlowSet srcSet2 = (FlowSet) src2;
        FlowSet destSet = (FlowSet) dest;
        srcSet1.union(srcSet2, destSet);
	 }

	 FlowSet fullSet, emptySet;
	 FlowUniverse allRefLocals;
	 Map unitToGenerateSet;

	 protected Object newInitialFlow(){
        return emptySet.clone();
	 }

	 protected Object entryInitialFlow(){
        // everything could be null
        return emptySet.clone();
	 }

	 private void addGen(Unit u, Value v){
        ArraySparseSet l = (ArraySparseSet)unitToGenerateSet.get(u);
        l.add(v);
	 }

	 private void addGensFor(DefinitionStmt u){
        Value lo = u.getLeftOp();
        Value ro = u.getRightOp();

        if ( lo instanceof FieldRef && !(ro instanceof Constant)){   
        	if(!ro.toString().equals("null")){
		        //	addGen(u, lo);
        		addGen(u, ro);
	        }
        }
	 }

	 public NullnessAnalysis(UnitGraph g,  AbstractInterproceduralAnalysis inter){
    	super(g);
        this.g = g;
        this.inter = inter;
        unitToGenerateSet = new HashMap();
        inSetMap = new HashMap<Unit, FlowSet>();
        outSetMap = new HashMap<Unit, FlowSet>();
        
        newExprMap = new HashMap<Value, Set<Unit>>();
        
    //    G.v().out.println("Graph: \n"+ g);
        Body b = g.getBody();
        // set up universe, empty, full sets.

        emptySet = new ArraySparseSet();
        
        // Create gen sets.
        Iterator unitIt = b.getUnits().iterator();
        while (unitIt.hasNext()){
            Unit u = (Unit)unitIt.next();
            unitToGenerateSet.put(u, new ArraySparseSet());

            if (u instanceof DefinitionStmt){
                Value lo = ((DefinitionStmt)u).getLeftOp();
                if (lo instanceof FieldRef )
                    addGensFor((DefinitionStmt)u);     
            }
            
            Stmt st = (Stmt) u;
            if(st instanceof InvokeStmt)
            {
            	InvokeExpr e = st.getInvokeExpr();
            	SootMethod m;
	            List<Value> args;
	            if (!(e instanceof StaticInvokeExpr)) 
	            {
	            	m = e.getMethod();
	            	args = e.getArgs();
	            	if(m.getName().equals("add"))
	            	{
	            		addGen(u, args.get(0));
	            	}
	            }
            }
            
            //newly added to find object creation sites in a method
            Iterator<ValueBox> iterator = u.getUseBoxes().iterator();
            
            while(iterator.hasNext()){
            	Value val = iterator.next().getValue();
            	
            	if(val instanceof AnyNewExpr){
            		List<ValueBox> l = u.getDefBoxes();
            		Value def = null;
            		if(l.size()==1){					//I don't know what happens in the case of a[i]=10; here logically defbox should have size = 2 i guess
            			def  = l.get(0).getValue();
            		}
            		
            		if(def!=null && newExprMap.containsKey(def)){
            			Set<Unit> s = newExprMap.get(def);
            			s.add(u);
            			
            			newExprMap.put(def, s);
            		}
            		else{
            			Set<Unit> s = new HashSet<Unit>();
            			s.add(u);
            			newExprMap.put(def, s);
            		}
            	}
            }
        }
        
        while(repeat_flag){
        	repeat_flag = false;
        	 // Call superclass method to do work.
        	doAnalysis();
        }
	 }

	 @Override
	 protected void flowThrough(Object srcValue, Object unit, Object destValue){
		// TODO Auto-generated method stub
	//	G.v().out.println("hello");
		FlowSet in = new ArraySparseSet();
      //  in = ((FlowSet)destValue).clone();
       
        FlowSet out = new ArraySparseSet();
      //  out = ((FlowSet)srcValue).clone();
       
        // Create working set.
      
  /*      Iterator boxIt;
        Stmt    s   = (Stmt)    unit;
        if(s instanceof DefinitionStmt ){
        	boxIt = s.getDefBoxes().iterator();
            while( boxIt.hasNext() ) {
            	final ValueBox box = (ValueBox) boxIt.next();
                Value value = box.getValue();
                if( value instanceof Local ){
                	if(out.contains(value))
                	{
                		DefinitionStmt as =(DefinitionStmt) s;
                		 Value ro = as.getRightOp();
                		 if (ro instanceof CastExpr)
         	                {ro = ((CastExpr) ro).getOp(); }
      
         	                in.add(ro);
                	}
                	}
                }
            }
  */      
        Stmt    s   = (Stmt)    unit;
        
        FlowSet dest = new ArraySparseSet();
       
        if (s.containsInvokeExpr()){
    	    inter.analyseCall(srcValue, s, dest);
    	    
    	    //newly added
    	    if(dest.size() > 0){
    	    	Iterator i = dest.iterator();
    	    	while(i.hasNext()){
    	    		Value v = (Value)i.next();
    	    		addGen((Unit)unit, v);
    	    	}
    	    }
    	}
          
        if(g.getSuccsOf(s).size() == 1){
        	Unit succ = g.getSuccsOf(s).get(0);
        	if(inSetMap.containsKey(succ))
        		out = inSetMap.get(succ);
        }
      
        else if(g.getSuccsOf(s).size() == 2 ){	
        	List<Unit> succ_list = g.getSuccsOf(s);
        	FlowSet succ1_in = inSetMap.get(succ_list.get(0));
        	FlowSet succ2_in = inSetMap.get(succ_list.get(1));
        	if(succ1_in==null && succ2_in!=null)
        		succ1_in= succ2_in;
        	if(succ2_in==null && succ1_in!=null)
        		succ2_in= succ1_in;
        	
        	succ1_in.union(succ2_in, out);
        }
        
        in = (FlowSet)out.clone();
        
        @SuppressWarnings("rawtypes")
		Iterator boxIt;
       
        if(s instanceof DefinitionStmt ){
        	boxIt = s.getDefBoxes().iterator();
            while( boxIt.hasNext()){
            	final ValueBox box = (ValueBox) boxIt.next();
                Value value = box.getValue();
                if(value instanceof Local){
                	if(out.contains(value)){
                		DefinitionStmt as =(DefinitionStmt) s;
                		 Value ro = as.getRightOp();
                		 if (ro instanceof CastExpr){
                			 ro = ((CastExpr) ro).getOp(); 
                		 }
                		 if(ro instanceof Local && !(ro instanceof Constant) && !(ro.toString().equals("null"))){
          	                in.add(ro);
          	        
          	                //newly added
          	                addGen((Unit)unit, ro);
                 		 }
                	}
                }
            }
        }
        
		// FlowSet dest; is now in
	     //   FlowSet src  = (FlowSet) srcValue;
        Unit    s1    = (Unit)    unit;

	    // Perform gen.
	    in.union((FlowSet)unitToGenerateSet.get(unit), in);

	    // Handle copy statements: 
	    //    x = y && 'y' in src => add 'x' to dest
	    if (s1 instanceof DefinitionStmt){
	    	DefinitionStmt as = (DefinitionStmt) s1;

	        Value ro = as.getRightOp();

	        // extract cast argument
	        if (ro instanceof CastExpr)
	        	ro = ((CastExpr) ro).getOp();
	        
	        if (as.getLeftOp() instanceof FieldRef && !(ro instanceof Constant)){
	        	in.add(ro);
	            
	            //newly added
	            addGen((Unit)unit, ro);
	        }
	    }
	        
	    if(dest.size()>0){
	    	in.union(dest, in);
	    }
	        
	    inSetMap.put((Unit) unit, in);
	    outSetMap.put((Unit) unit, out);
	        
	    if(!outSetMap.get(s).equals(out))
	    	repeat_flag = true;
    }
}

