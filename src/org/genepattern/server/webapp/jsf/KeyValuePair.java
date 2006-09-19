package org.genepattern.server.webapp.jsf;

public class KeyValuePair{
	public String key;
	public String value;

	public KeyValuePair(String n, String v){
		key = n;
		value = v;
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
}
