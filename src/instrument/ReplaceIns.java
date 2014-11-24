package instrument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import abc.NewTest;

import soot.Body;
import soot.BooleanType;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AnyNewExpr;
import soot.jimple.IdentityRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;

public class ReplaceIns {
	
	SootClass objPool, bufPool;
	SootMethod m;
	SootClass c;
	Body b;
	Local appContext, buf_l;
	Unit last_stmt;
//	Unit last_appCnxtStmt;
	
	public ReplaceIns(SootMethod method,
			Map<Type, Set<Unit>> map,
			Map<Local, Set<Unit>> saveToBuf) {
		// TODO Auto-generated method stub
	
		m= method;
		b = m.getActiveBody();
		c = m.getDeclaringClass();
		getUtilClasses();
		Unit unit = getAppCntxtInsPt();
		getBufferObj(unit);
		
	//	addAppContext(unit);
		Set<SootClass> set = new HashSet<SootClass>();
		
		if(map != null){
			for(Map.Entry<Type, Set<Unit>> e: map.entrySet()){
				if(!NewTest.newTypeSet.contains(e.getKey()))
					continue;
				String s = e.getKey().toString();
				String cls_name = s+"_cls";//s.substring(s.indexOf(".")+1)+"_cls";
				Local cls = Jimple.v().newLocal(cls_name, RefType.v(Scene.v().getSootClass("java.lang.String")));
				if(isLocalAvailable(cls)){
					cls = getAvailableLocal(cls);
				}
				else{
					b.getLocals().add(cls);
					b.getUnits().insertAfter(Jimple.v().newAssignStmt(cls, StringConstant.v(e.getKey().toString())), last_stmt);//last_appCnxtStmt);
				}
				
				for(Unit u: e.getValue())
					replaceAllocStmt(u, cls);
			}
		
		}	
		
		if(saveToBuf !=null){
			for(Map.Entry<Local, Set<Unit>> e: saveToBuf.entrySet()){
				Local l = e.getKey();
				String type = l.getType().toString();
				String cls_name = type+"_cls";//type.substring(type.indexOf(".")+1)+"_cls";
				
				
				Local cls = Jimple.v().newLocal(cls_name, RefType.v(Scene.v().getSootClass("java.lang.String")));
				
				if(isLocalAvailable(cls)){
					cls = getAvailableLocal(cls);
				}
				else{
					b.getLocals().add(cls);
			
				
					b.getUnits().insertAfter(Jimple.v().newAssignStmt(cls, StringConstant.v(l.getType().toString())), last_stmt);//last_appCnxtStmt);
				}
			
				for(Unit u: e.getValue()){
					insSavePoint(l, u, cls);
				}
			}
		}
		
	}
	
	private Local getAvailableLocal(Local cls) {
		// TODO Auto-generated method stub
		for(Local l: b.getLocals()){
			if(l.toString().equals(cls.toString()) && l.getType().equals(cls.getType()))
				return l;
		}
		
		return null;
	}

	private boolean isLocalAvailable(Local cls) {
		// TODO Auto-generated method stub
		for(Local l: b.getLocals()){
			if(l.toString().equals(cls.toString()) && l.getType().equals(cls.getType()))
					return true;
		}
		
		return false;
	}

	private void getBufferObj(Unit unit) {
		// TODO Auto-generated method stub
		buf_l = Jimple.v().newLocal("bufPool", RefType.v(bufPool));
		b.getLocals().add(buf_l);
		
		last_stmt = Jimple.v().newAssignStmt(buf_l, Jimple.v().newStaticFieldRef(BuffPoolInstrument.bufferPool.makeRef()));
		b.getUnits().insertBefore(last_stmt, unit);	
	
	}

	private void insSavePoint(Local l, Unit u, Local cls){
		List list = new ArrayList();
		
		list.add(cls);
		list.add(l);
		
		b.getUnits().insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(buf_l,bufPool.getMethod("void saveObject(java.lang.String,java.lang.Object)").makeRef() ,list)), u);
		
	}
	
	/*
	private void addAppContext(Unit unit) {
		// TODO Auto-generated method stub
		buf_l = Jimple.v().newLocal("bufPool", RefType.v(bufPool));
		appContext = Jimple.v().newLocal("appContext", RefType.v(Scene.v().getSootClass("android.content.Context")));
		b.getLocals().add(buf_l);
		b.getLocals().add(appContext);
		Stmt contextStmt = Jimple.v().newAssignStmt(appContext, Jimple.v().newVirtualInvokeExpr(b.getThisLocal(), Scene.v().getSootClass("android.content.Context").getMethod("android.content.Context getApplicationContext()").makeRef()));
	//	b.getUnits().insertAfter(contextStmt, b.getUnits().getFirst());		
		b.getUnits().insertBefore(contextStmt, unit);
		
		last_appCnxtStmt = Jimple.v().newAssignStmt(buf_l, Jimple.v().newCastExpr(appContext, RefType.v(Scene.v().getSootClass("android.content.Context"))));
		b.getUnits().insertAfter(last_appCnxtStmt, contextStmt);		
		
	}
	*/
	
	private Unit getAppCntxtInsPt(){
		for(Unit u : b.getUnits()){
			Iterator it = u.getUseBoxes().iterator();
			while(it.hasNext()){
				Value v = ((ValueBox)it.next()).getValue();
				if(v instanceof IdentityRef){ //includes ThisRef and ParameterRef
					continue;
				}
				else{
					return u;
				}
			}	
		}
		return null;
	}


	void replaceAllocStmt(Unit u, Local cls){//Set<Unit> set){
		String classname = null;
	/*	Iterator it = u.getDefBoxes().iterator();
		while(it.hasNext()){
			Value v  = ((ValueBox)it.next()).getValue();
			if(v instanceof Local){
				Local l = (Local)v ;
				classname = l.getType().toString();
			}
		}
	*/	
		StringBuilder sb = new StringBuilder("void initializePoolObject");
		Value v = u.getDefBoxes().get(0).getValue();
		
		Unit spclInvk = b.getUnits().getSuccOf(u);
		if(spclInvk instanceof InvokeStmt){
			InvokeExpr invExpr = ((InvokeStmt) spclInvk).getInvokeExpr();
			if(invExpr instanceof SpecialInvokeExpr){
				String s = invExpr.getMethod().toString();  //does it yield full method name along with classname and parameters passed?
				int i = s.indexOf("(");
				sb.append(s.substring(i, s.length()-1));
				
				int j = s.indexOf(":");
				classname = s.substring(1, j);
			}
		}
		
		Unit u_succ = b.getUnits().getSuccOf(u);
		
		Unit insert_before = null;
		if(u_succ instanceof InvokeStmt){
			insert_before  = b.getUnits().getSuccOf(u_succ);
		}
		
		Local bool = Jimple.v().newLocal(v.toString()+"_flag", BooleanType.v());
		
		if(isLocalAvailable(bool)){
			bool = getAvailableLocal(bool);
		}
		else
			b.getLocals().add(bool);
		Stmt check = Jimple.v().newAssignStmt(bool, Jimple.v().newVirtualInvokeExpr(buf_l, bufPool.getMethod("boolean isObjInPool(java.lang.String)").makeRef(), cls));//StringConstant.v(classname)));
		b.getUnits().insertBefore(check, u);
		
		Local savedObj = Jimple.v().newLocal(v.toString()+"_savedObj", RefType.v(Scene.v().getSootClass("java.lang.Object")));
		
		if(isLocalAvailable(savedObj)){
			savedObj = getAvailableLocal(savedObj);
		}
		else
			b.getLocals().add(savedObj);
		
		Stmt ifStmt = Jimple.v().newIfStmt(
				Jimple.v().newEqExpr(bool, IntConstant.v(0)), u);
		b.getUnits().insertAfter(ifStmt, check);
		
		Stmt toCall = Jimple.v().newAssignStmt(savedObj, Jimple.v().newVirtualInvokeExpr(buf_l, bufPool.getMethod("java.lang.Object getObject(java.lang.String)").makeRef(), cls));//StringConstant.v(classname)));
		b.getUnits().insertAfter(toCall, ifStmt);
		
		Stmt castStmt = Jimple.v().newAssignStmt(v , Jimple.v().newCastExpr(savedObj, RefType.v(Scene.v().getSootClass(classname))));
		b.getUnits().insertAfter(castStmt, toCall);
		
		Stmt s = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr((Local) v, Scene.v().getSootClass(classname).getMethod(sb.toString()).makeRef()));
		
		b.getUnits().insertAfter(s, castStmt);	
		b.getUnits().insertAfter(Jimple.v().newGotoStmt(insert_before), s);
	}
	
	public void getUtilClasses(){
		bufPool = Scene.v().getSootClass("util.objectpool.BufferPool");
	}
	
	
}