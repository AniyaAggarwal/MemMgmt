package instrument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import abc.NewTest;

import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.FieldRef;

public class InsUtil{
	Map<Value, Set<Unit>> newExprMap;
	Map<Type, Set<Unit>> newExpr_cmplt;
	Map<Local, Set<Unit>> saveToBuf;
	

	/*
	public void setNonEscNewAlloc(Map<Value, Set<Unit>> m){
		
	}
	*/
	public void setNewExprMap(Map<Value, Set<Unit>> map){
		newExprMap = map;
		rearrange();
	}
	
	public Map<Type, Set<Unit>> getNewExprMap(){
		return newExpr_cmplt;
	}
	
	private void rearrange(){
		for(Map.Entry<Value, Set<Unit>> e: newExprMap.entrySet()){
			Value v = e.getKey();
			if(v instanceof Local){
				Type t  = ((Local)v).getType();
				if(abc.NewTest.app_classes.contains(Scene.v().getSootClass(t.toString()))){
					addToMap(t, e.getValue());
				}
			}
			if(v instanceof FieldRef){
				Type t  = ((FieldRef)v).getType();
				if(abc.NewTest.app_classes.contains(Scene.v().getSootClass(t.toString()))){
					addToMap(t, e.getValue());
				}
			}
		}
	}

	private void addToMap(Type t, Set<Unit> value) {
		// TODO Auto-generated method stub
		if(newExpr_cmplt == null){
			newExpr_cmplt = new HashMap<Type, Set<Unit>>();
		}
		
		if(!newExpr_cmplt.containsKey(t)){
			Set<Unit> set = new HashSet<Unit>();
			newExpr_cmplt.put(t, set);
		}
		newExpr_cmplt.get(t).addAll(value);
		
	}
	
	public void setSavetoBuf(Map<Local, Set<Unit>> map){
		saveToBuf = map;
	}
	
	public Map<Local, Set<Unit>> getSaveToBuf(){
		return saveToBuf;
	}
	
	
	
}