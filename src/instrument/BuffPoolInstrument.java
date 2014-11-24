package instrument;

import java.lang.reflect.Modifier;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;

public class BuffPoolInstrument {
	
	public static SootClass mainAct;
	public static SootField bufferPool;
	
	public BuffPoolInstrument(SootClass mainActivity) {
		// TODO Auto-generated constructor stub
		
		mainAct = mainActivity;
		
		SootClass buf = Scene.v().getSootClass("util.objectpool.BufferPool");
		
		//add BufferPool object as field
		SootField f = new SootField("buffPool", RefType.v(buf),  Modifier.STATIC | Modifier.PUBLIC);//Modifier.STATIC |
	//	mainActivity.getFields().add(f);
		mainActivity.addField(f);
		bufferPool = f;
		
		//initialize BufferPool object in onCreate() method
		SootMethod onCreate = mainActivity.getMethodByName("onCreate");
		Body b = onCreate.getActiveBody();
		Unit insert_after = null;
		
		for(Unit u : b.getUnits()){
			if(u instanceof InvokeStmt){
				if(((InvokeStmt) u).getInvokeExpr() instanceof SpecialInvokeExpr){
					insert_after = u;
					break;
				}
			}
		}
		
		Local l = Jimple.v().newLocal("bufLocal", RefType.v(buf));
		b.getLocals().add(l);
		Stmt s1 = Jimple.v().newAssignStmt(l, Jimple.v().newNewExpr(RefType.v(buf)));
		Stmt s2 = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(l, buf.getMethodByName("<init>").makeRef()));
		Stmt s3 = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(f.makeRef()), l);
		b.getUnits().insertAfter(s1, insert_after);
		b.getUnits().insertAfter(s2, s1);
		b.getUnits().insertAfter(s3,s2);
		
		//b.getUnits().insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newsp), s1);
	}

}
