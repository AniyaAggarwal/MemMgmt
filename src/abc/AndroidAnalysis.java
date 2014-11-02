package abc;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.callgraph.Targets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.jimple.toolkits.callgraph.CHATransformer;
import ppg.parse.Constant;

import soot.ArrayType;
import soot.Body;
import soot.BodyTransformer;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.ClassSource;
import soot.DoubleType;
import soot.FloatType;
import soot.G;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.UnitBox;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.JastAddJ.ConditionalExpr;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.DoubleConstant;
import soot.jimple.Expr;
import soot.jimple.FieldRef;
import soot.jimple.FloatConstant;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LongConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.ConditionExprBox;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.LoopNestTree;
import soot.util.Chain;
import soot.util.Switch;

public class AndroidAnalysis //extends BodyTransformer
{ 
	private static Body bb;
	
	private static SootMethod m;
	List<SootField> field_to_be_ini;
	SootMethod method;
	
	private static AndroidAnalysis instance= new AndroidAnalysis();
	public AndroidAnalysis(SootMethod method) {
		// TODO Auto-generated constructor stub
		m= method;
		field_to_be_ini = new ArrayList<SootField>();
		myTransform(m.getActiveBody());
		
	}

	public AndroidAnalysis(){}
	
	public static AndroidAnalysis v() { return instance; }
  

    //protected void internalTransform(Body b, String phaseName, Map options)
    void myTransform(Body b)
	{	
    	
    	field_to_be_ini = new ArrayList<SootField>();
    	//check if class is static inner one.
    	
    	if(NewTest.map.keySet().contains(b.getMethod())){
    		System.out.println("\n\n"+b.getMethod());
    		
    		
    		BriefUnitGraph bg = new BriefUnitGraph(b);
			
    		Map<Unit, Loop> m = NewTest.map.get(b.getMethod());
    		
    		for(Map.Entry<Unit, Loop> entry2: m.entrySet()){
    			
    			Unit original_u1 = entry2.getKey();
    			Unit original_u11 = null; 
    			
    			Unit u1 = (Unit) entry2.getKey().clone();
    			Unit  u11 = null;
    			List<Unit> u2 = bg.getSuccsOf(entry2.getKey());
    			if(u2.size() ==1){
    				u11 = (Unit) u2.get(0).clone();
    				original_u11 = u2.get(0);
    			}
    			
    			List<Unit> pred_loop = bg.getPredsOf(entry2.getValue().getHead());
    			Unit insert_after_u = null;
    			
    			for(Unit u: pred_loop){
    				if(u instanceof GotoStmt)
    					continue;
    				insert_after_u = u;
    			}
    			
    			if(insert_after_u != null && u11!=null){
    				System.out.println("\n\n"+insert_after_u);
    				System.out.println(u1);
    				System.out.println(u11);
    				if(u11 instanceof InvokeStmt){
    					
    					InvokeExpr expr = ((InvokeStmt) u11).getInvokeExpr();
    					
    					if(expr instanceof SpecialInvokeExpr){
    						SootMethod method_toCall  = addResetMethodToClass(expr);
    						
    						List<ValueBox> def = u1.getDefBoxes();
    						if(def.size()==1){
    							Value v = def.get(0).getValue();
    							InvokeExpr ienew= Jimple.v().newVirtualInvokeExpr((Local) v, method_toCall.makeRef());
    							b.getUnits().insertAfter(Jimple.v().newInvokeStmt(ienew), original_u11);
    						}
    						b.getUnits().insertAfter(u1, insert_after_u);
        					b.getUnits().insertAfter(u11, u1);
    						
    						b.getUnits().remove(original_u1);
    					 	b.getUnits().remove(original_u11);
    					 	
        					
    					}
    				}
    				
    			}	
    		}
    			
    			
    	}   	
    	
    }


	private SootMethod addResetMethodToClass(InvokeExpr expr) {
		// TODO Auto-generated method stub
	//	System.out.println(expr + "\t\t"+expr.getArgs() + "\t\t" + expr.getMethod() + "\t\t" + expr.getMethodRef() );
		method  =  expr.getMethod();
		
		
		String newMethod_name = "reset_"+ expr.getArgCount();
		SootMethod newMethod = new SootMethod(newMethod_name, method.getParameterTypes(),
		        method.getReturnType(), Modifier.PUBLIC); //since constructors are always public
		
		method.getDeclaringClass().addMethod(newMethod);
		JimpleBody new_body = Jimple.v().newBody(newMethod);
		newMethod.setActiveBody(new_body);
		
		/*
		if(method.getDeclaringClass().getName().equals("com.example.toyexample.A")){
		    
    		SootClass classm = Scene.v().getSootClass("HelloWorld");//"HelloWorld");
    		
    		SootClass classp = method.getDeclaringClass();
    		for(SootMethod m : classm.getMethods()) 
    			if(!classp.declaresMethodByName(m.getName())){
    				SootMethod newM = new SootMethod(m.getName(), m.getParameterTypes(), m.getReturnType());
    				
    				newM.setActiveBody(m.getActiveBody());
    				classp.addMethod(newM);
    			}
    	}
    
		*/
		
		
		
		new_body.getLocals().addAll(method.getActiveBody().getLocals());	//add all locals of init() to reset()
		
		//reset all the fields of the class in reset()
		Chain<SootField> fields = method.getDeclaringClass().getFields();
		
		
		field_to_be_ini.addAll(fields);
		
		Local local = null;
		
		for(Local l: method.getActiveBody().getLocals()){
			if(l.getType().toString().equals(method.getDeclaringClass().toString())){
				local = l;
				break;
			}
		}
		
		if(local == null){
			local = Jimple.v().newLocal("mylocal", RefType.v(method.getDeclaringClass()));
			method.getActiveBody().getLocals().add(local);
		}
		
	//	field_initialize(fields, new_body, local);
		Unit this_unit=null;
		
		boolean first_this_encountered = false;
		//Build the reset() method body
		Chain units = new_body.getUnits();
		
		Unit prev_unit = null;
		Value prev_val = null;
		String arr_type = null;
		
		for(Unit u : method.getActiveBody().getUnits()){
			if(u instanceof InvokeStmt &&  ((InvokeStmt) u).getInvokeExpr() instanceof SpecialInvokeExpr) // avoid adding method call to init() of java.lang.Object class
				continue;
			else if(u instanceof JAssignStmt){
				Iterator it  = u.getUseBoxes().iterator();
				while(it.hasNext()){
					ValueBox vb = (ValueBox) it.next();
					Value v = vb.getValue();
					if(v instanceof JNewArrayExpr && prev_unit == null){
						System.out.println("ns");
						prev_unit = u;
						Iterator i = u.getDefBoxes().iterator();
						while(i.hasNext()){
							ValueBox vbox = (ValueBox) i.next();
							prev_val = vbox.getValue();
						}
						if(((JNewArrayExpr) v).getBaseType() instanceof IntType){
							arr_type = "int";
						}
						else if(((JNewArrayExpr) v).getBaseType() instanceof FloatType){
							arr_type = "float";
						}
						else if(((JNewArrayExpr) v).getBaseType() instanceof DoubleType){
							arr_type = "double";
						}
						break;
					}
				}
				if(prev_unit != null){
					Iterator i = u.getDefBoxes().iterator();
					while(i.hasNext()){
						ValueBox vb = (ValueBox) i.next();
						Value val = vb.getValue();
						if(val instanceof JInstanceFieldRef){
							System.out.println("yess");
							SootField f = ((JInstanceFieldRef) val).getField();
							Iterator itr  = u.getUseBoxes().iterator();
							while(itr.hasNext()){
								ValueBox vbx = (ValueBox) itr.next();
								Value v = vbx.getValue();
								if(v.equals(prev_val)){
									initializeArrayField(new_body, f, arr_type, local, prev_val);
									field_to_be_ini.remove(val);
									prev_val = null;
									prev_unit = null;
									arr_type = null;
								}
								
							}
		
						}
					}
				}
				else{
					units.add(u);
					Iterator i = u.getDefBoxes().iterator();
					while(i.hasNext()){
						ValueBox vb = (ValueBox) i.next();
						Value val = vb.getValue();
						if(val instanceof JInstanceFieldRef){
							
							field_to_be_ini.remove(((JInstanceFieldRef) val).getField());
							
						}
					}
				}
				
			}
			else{
					if(u instanceof IdentityStmt){
						
						for(ValueBox box : u.getUseBoxes()){
							Value value = box.getValue();
							if(value instanceof ThisRef && !first_this_encountered){
								if(units.size() ==0){
									units.add(u);
									this_unit = u;
								}
								else
									units.addFirst(u);
								first_this_encountered = true;
							}
						}
					}
					else{		
						units.addLast(u);
					}
				}
			}
		
		System.out.println("to be reset.. " + field_to_be_ini);
		field_initialize(field_to_be_ini, new_body, local, this_unit);
		
		/*
		for(SootField f : field_to_be_ini){
			//if(f.getDeclaringClass().equals(m.getDeclaringClass())){
			field_initialize(field_to_be_ini, new_body, local, this_unit);
		//	}
			
		}*/
		
		return newMethod;
	}
		
	private void initializeArrayField(JimpleBody new_body, SootField f, String type, Local local2, Value prev_val) {
		if( type!= null && type.equals("int")){
			
			Stmt assStmt = Jimple.v().newAssignStmt( prev_val,
					Jimple.v().newInstanceFieldRef(local2, f.makeRef()));
			SootMethod m = Scene.v().getMethod("<java.util.Arrays: void fill(int[],int)>");
			List l = new ArrayList();
			l.add(prev_val);
			l.add(IntConstant.v(0));
			Stmt insertStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr( m.makeRef(),l));
			new_body.getUnits().addLast(assStmt);
			new_body.getUnits().addLast(insertStmt);
		}
		if( type!= null && type.equals("float")){
			Stmt assStmt = Jimple.v().newAssignStmt( prev_val,
					Jimple.v().newInstanceFieldRef(local2, f.makeRef()));
			SootMethod m = Scene.v().getMethod("<java.util.Arrays: void fill(float[],float)>");
			List l = new ArrayList();
			l.add(prev_val);
			l.add(FloatConstant.v(0));
			Stmt insertStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr( m.makeRef(),l));
			new_body.getUnits().addLast(assStmt);
			new_body.getUnits().addLast(insertStmt);
		}
		
		field_to_be_ini.remove(f);

	}

	private void field_initialize(List<SootField> fields, JimpleBody new_body, Local local, Unit this_unit) {
		// TODO Auto-generated method stub
		
		
		for(SootField f : fields){

			if(!f.getDeclaringClass().equals(method.getDeclaringClass())){
				continue;
			}
			
			if(f.isStatic())
				continue;
			
			Type t = f.getType();
			if(t.equals(IntType.v()) || t.equals(ByteType.v()) || t.equals(ShortType.v()) ){
				//set field value to 0 
				if(local != null){
					if(new_body.getUnits().size() == 0){
						new_body.getUnits().add(Jimple.v().newAssignStmt(
								Jimple.v().newInstanceFieldRef(local, f.makeRef()),
									IntConstant.v(0)));
					}
					else{
						new_body.getUnits().insertAfter(Jimple.v().newAssignStmt(
								Jimple.v().newInstanceFieldRef(local, f.makeRef()),
									IntConstant.v(0)), this_unit);
					
					}
				}
					
			}
			else if(t.equals(LongType.v())){
				//set field value to 0L
				if(new_body.getUnits().size() == 0){
					new_body.getUnits().add(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								LongConstant.v(0L)));
				}
				else{
					new_body.getUnits().insertAfter(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								LongConstant.v(0L)), this_unit);
				
				}
			}
			else if(t.equals(FloatType.v())){
				//set field value to 0.0f
				if(new_body.getUnits().size() == 0){
					new_body.getUnits().add(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								FloatConstant.v(0.0f)));
				}
				else{
					new_body.getUnits().insertAfter(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								FloatConstant.v(0.0f)), this_unit);
				
				}
			}
			else if(t.equals(DoubleType.v())){
				//set field value to 0.0d
				if(new_body.getUnits().size() == 0){
					new_body.getUnits().add(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								DoubleConstant.v(0.0d)));
				}
				else{
					new_body.getUnits().insertAfter(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								DoubleConstant.v(0.0d)), this_unit);
				
				}
			}
			else if(t.equals(BooleanType.v())){
				//set field value to false
				if(new_body.getUnits().size() == 0){
					new_body.getUnits().add(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								IntConstant.v(0)));
				}
				else{
					new_body.getUnits().insertAfter(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								IntConstant.v(0)), this_unit);
				
				}
			}
			else if(t.equals(CharType.v())){
				//set field value to '\u0000'
			}
			else{
				//set field value to null.. covers the case of strings also
				if(new_body.getUnits().size() == 0){
					new_body.getUnits().add(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								NullConstant.v()));
				}
				else{
					new_body.getUnits().insertAfter(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(local, f.makeRef()),
								NullConstant.v()), this_unit);
				
				}
			}
		}

	}
}