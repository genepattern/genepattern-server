package org.genepattern.server.webapp.jsf;

import java.util.Map;

public class KeyValuePair {
	public String key;
	public String value;
	public String altKey;
	
	public String getAltKey() {
		return altKey;
	}

	public void setAltKey(String altKey) {
		this.altKey = altKey;
	}

	public KeyValuePair(String n, String v){
		key = n;
		value = v;
		altKey = n;
	}
	public KeyValuePair(String n, String a, String v){
		key = n;
		value = v;
		altKey = a;
	}
	
	public String getKey(){
		return key;
	}
	
	public void setKey(String n){
		 key = n;
	}

	public String getValue(){
		return value;
	}
	public void setValue(String n){
		 value = n;
	}
/*	public Object setValue(Object n){
		Object old = getValue();
		value = (String)n;
		return old;
	} */
}
