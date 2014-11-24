package util.objectpool;

import java.util.HashMap;

import android.app.Application;


public class BufferPool extends Application{
	private HashMap<String, ObjectPool> pool;
	
	public BufferPool(){
		pool=new HashMap<String, ObjectPool>();
	}
	
	public boolean isObjInPool(String c) {
		// TODO Auto-generated method stub
		
		if(pool.containsKey(c)){
			if(pool.get(c).hasObject())
				return true;
		}
		return false;
	}
	
	public Object getObject(String c){
		return pool.get(c).getObject();
	}
	
	public void saveObject(String c, Object o){	
		if(!pool.containsKey(c)){
			ObjectPool mypool = new ObjectPool(400);
			pool.put(c, mypool);
		}
		pool.get(c).freeObject(o);
	}
}