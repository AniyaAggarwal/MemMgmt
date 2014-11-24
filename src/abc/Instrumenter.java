package abc;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.Printer;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewArrayExpr;
import soot.options.Options;
import soot.util.Chain;

public class Instrumenter{
	SootClass objPool, poolObj, factory;
	SootMethod method;
	Map<Local, Set<Unit>> ins_map;
	Set<String> factory_already_created;
	
	List<SootField> field_to_be_ini;
	
	public Instrumenter(SootMethod m, Map<Local, Set<Unit>> map) {
		// TODO Auto-generated constructor stub
		method = m;
		ins_map = map;
	//	factory_already_created = new HashSet<String>();
		
		getUtilClasses();
		for(Map.Entry<Local, Set<Unit>> entry: ins_map.entrySet()){
			Local l = entry.getKey();
			SootClass c = Scene.v().getSootClass(l.getType().toString());
			if(createFactoryClass(c))
				modifyObjectClass(c);
		}
		
	}

	private void modifyObjectClass(SootClass cl) {
		// TODO Auto-generated method stub
		//add 'implements' relation
		cl.addInterface(poolObj);
		
		//add finalizePoolObject() method
		SootMethod finalize = new SootMethod("finalizePoolObject",  new ArrayList<Type>(), VoidType.v(), Modifier.PUBLIC);
		cl.addMethod(finalize);
		JimpleBody body = Jimple.v().newBody(finalize);
		finalize.setActiveBody(body);
		
		Local this_local = Jimple.v().newLocal("this_ref", RefType.v(cl));
		body.getLocals().add(this_local);
		
		body.getUnits().add(Jimple.v().newIdentityStmt(this_local, Jimple.v().newThisRef(RefType.v(cl))));
		body.getUnits().add(Jimple.v().newReturnVoidStmt());
		
		//add initializePoolObject
		
	//	Set<SootMethod> init_set = new HashSet<SootMethod>();
		//add reset method corresponding to every init()
		for(SootMethod m : cl.getMethods()){
			if(m.getName().equals("<init>"))
			{
				//init_set.add(m);

				addResetMethod(m);
				
			}
		}
		
		
		////getMethod("void <init>()>").retrieveActiveBody());
		System.out.println(Scene.v().getSootClass("java.lang.Object").getFields());
		for(SootMethod m : Scene.v().getSootClass("java.lang.Object").getMethods()){
		//	System.out.println(m.retrieveActiveBody());
		}
	}

	
	private void addResetMethod(SootMethod m) {
		// TODO Auto-generated method stub
	//	System.out.println(expr + "\t\t"+expr.getArgs() + "\t\t" + expr.getMethod() + "\t\t" + expr.getMethodRef() );
		
		field_to_be_ini = new ArrayList<SootField>();
		
		String newMethod_name = "initializePoolObject";//"reset_"+ expr.getArgCount();
		SootMethod newMethod = new SootMethod(newMethod_name, m.getParameterTypes(),
		        m.getReturnType(), Modifier.PUBLIC); //since constructors are always public
		
		m.getDeclaringClass().addMethod(newMethod);
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
		
		
		
		new_body.getLocals().addAll(m.getActiveBody().getLocals());	//add all locals of init() to reset()
		
		//reset all the fields of the class in reset()
		Chain<SootField> fields = m.getDeclaringClass().getFields();
		
		if(m.getDeclaringClass().hasSuperclass()){
			field_to_be_ini.addAll(m.getDeclaringClass().getSuperclass().getFields());
		}
		
		field_to_be_ini.addAll(fields);
		
		Local local = null;
		
		for(Local l: m.getActiveBody().getLocals()){
			if(l.getType().toString().equals(m.getDeclaringClass().toString())){
				local = l;
				break;
			}
		}
		
		if(local == null){
			local = Jimple.v().newLocal("mylocal", RefType.v(m.getDeclaringClass()));
			m.getActiveBody().getLocals().add(local);
		}
		
	//	field_initialize(fields, new_body, local);
		Unit this_unit=null;
		
		boolean first_this_encountered = false;
		//Build the reset() method body
		Chain units = new_body.getUnits();
		
		Unit prev_unit = null;
		Value prev_val = null;
		String arr_type = null;
		
		for(Unit u : m.getActiveBody().getUnits()){
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

			if(f.isStatic() || f.isFinal())
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
				
				//superclass's fields which are supposed to be set as null are left as it is.
				if(!f.getDeclaringClass().equals(new_body.getMethod().getDeclaringClass())){
					continue;
				}
				
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

	public void getUtilClasses(){
		objPool = Scene.v().getSootClass("util.objectpool.ObjectPool");
		poolObj = Scene.v().getSootClass("util.objectpool.PoolObject");
		factory = Scene.v().getSootClass("util.objectpool.PoolObjectFactory");
	
	}
	
	public boolean createFactoryClass(SootClass cl){
		String classname = cl.getName();
		String fact = classname+"Factory";
		Chain<SootClass> classes = Scene.v().getClasses(); 
		if(!classCreationAllowed(classes, fact)){
			return false;
		}
		
		SootClass c = new SootClass(fact, Modifier.PUBLIC);
		c.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
		c.addInterface(factory);
		//c.addInterface(factory);
		Scene.v().addClass(c);
		
		//add init() method
		
		SootMethod init_m = new SootMethod("<init>",  new ArrayList<Type>(), VoidType.v(), Modifier.PUBLIC);
		c.addMethod(init_m);
		JimpleBody body = Jimple.v().newBody(init_m);
		init_m.setActiveBody(body);
		
		Local this_local = Jimple.v().newLocal("this_ref", RefType.v(c));
		body.getLocals().add(this_local);
		ThisRef this_l = Jimple.v().newThisRef(RefType.v(c));
		
		body.getUnits().add(Jimple.v().newIdentityStmt(this_local, this_l));
		body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(this_local,Scene.v().makeConstructorRef(Scene.v().getSootClass("java.lang.Object"), new ArrayList<Type>()))));
		body.getUnits().add(Jimple.v().newReturnVoidStmt());
		
		//add createPoolObject() method - should be overloaded for overloaded constructor cases??
		SootMethod cpo_method = new SootMethod("createPoolObject",  new ArrayList<Type>(), RefType.v(poolObj), Modifier.PUBLIC);
		c.addMethod(cpo_method);
		JimpleBody cpo_body = Jimple.v().newBody(cpo_method);
		cpo_method.setActiveBody(cpo_body);
		
		Local fact_l = Jimple.v().newLocal("factory",RefType.v(c));
		Local poolObj_l = Jimple.v().newLocal("poolObj", RefType.v(cl));
		cpo_body.getLocals().add(fact_l);
		cpo_body.getLocals().add(poolObj_l);
		
		cpo_body.getUnits().add(Jimple.v().newIdentityStmt(fact_l, Jimple.v().newThisRef(RefType.v(c))));
		cpo_body.getUnits().add(Jimple.v().newAssignStmt(poolObj_l, Jimple.v().newNewExpr(RefType.v(Scene.v().getSootClass(classname)))));
		cpo_body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(poolObj_l, Scene.v().makeConstructorRef(cl, cpo_method.getParameterTypes()))));
		cpo_body.getUnits().add(Jimple.v().newReturnStmt(poolObj_l));		
		
		return true;
		
	}
	
	boolean classCreationAllowed(Chain<SootClass> classes, String classname){
		boolean bool  = true;
		for (SootClass cl : classes){
			if(cl.getName().equals(classname)){
				bool = false;
				break;
			}
		}
		return bool;
	}
}