package org.genepattern.data.matrix;
import java.util.HashMap;
/**
 *@author     Joshua Gould
 */
class ObjectIntMap implements Cloneable {
	//TObjectIntHashMap map;
	HashMap map;


	public ObjectIntMap() {
		//map = new TObjectIntHashMap();
		map = new HashMap();
	}


	public ObjectIntMap(int size) {
		//	map = new TObjectIntHashMap(size);
		map = new HashMap(size);
	}


	public int get(Object key) {
		//return map.get(key);
		Integer i = (Integer) map.get(key);
		return i.intValue();
	}


	public boolean containsKey(Object value) {
		return map.containsKey(value);
	}


	public void put(Object key, int value) {
		//	map.put(key, value);
		map.put(key, new Integer(value));
	}


	public Object clone() {
		ObjectIntMap result = null;
		try {
			result = (ObjectIntMap) super.clone();
		} catch(CloneNotSupportedException e) {
		}
		result.map = (HashMap) this.map.clone();
		return result;
	}
}

