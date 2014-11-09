package abc;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.Modifier;
import soot.Printer;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.ThisRef;
import soot.options.Options;
import soot.util.Chain;

public class Instrumenter{
	SootClass objPool, poolObj, factory;
	SootMethod method;
	Map<Local, Set<Unit>> ins_map;
	Set<String> factory_already_created;
	
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
		
		////getMethod("void <init>()>").retrieveActiveBody());
		System.out.println(Scene.v().getSootClass("java.lang.Object").getFields());
		for(SootMethod m : Scene.v().getSootClass("java.lang.Object").getMethods()){
		//	System.out.println(m.retrieveActiveBody());
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