/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.server.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Iterator;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

import org.genepattern.util.IGPConstants;

public class PropertiesManager implements IGPConstants {
	private static PropertiesManager inst = null;
	
	private PropertiesManager(){
		
	}
	
	public static PropertiesManager getInstance(){
		if (inst == null){
			inst = new PropertiesManager();
			
		}
		return inst;
	}

	public static boolean storeChange(String key, String value){
		boolean storeSuccess = false;
		System.setProperty(key, value);
		try {
			Properties props = getGenePatternProperties();
			props.setProperty(key, value);
			storeGenePatternProperties(props, "#Genepattern server updated key: " + key);
			storeSuccess = true;
		} catch (Exception e){
			storeSuccess = false;
		} 

		return storeSuccess;
	}

	public static boolean storeChanges(Properties newProps){
		boolean storeSuccess = false;
		for (Iterator iter = newProps.keySet().iterator(); iter.hasNext(); ){
			String key = (String)iter.next();
			String val = newProps.getProperty(key);
			System.setProperty(key, val);
		}

		try {
			int i=0;
			Properties props = getGenePatternProperties();
			StringBuffer commentBuff = new StringBuffer("#Genepattern server updated keys: ");	
			for (Iterator iter = newProps.keySet().iterator(); iter.hasNext(); i++){
				String key = (String)iter.next();
				String val = newProps.getProperty(key);
				props.setProperty(key, val);
				if (i > 0) 	commentBuff.append(", ");
				commentBuff.append(key);
			}
			storeGenePatternProperties(props, commentBuff.toString());
			storeSuccess = true;
		} catch (Exception e){
			storeSuccess = false;
		} 

		return storeSuccess;
	}


	public static boolean removeProperties(Vector keys){
		Properties sysProps = System.getProperties();
		for (int i=0; i < keys.size(); i++){
			String key = (String)keys.get(i);
			sysProps.remove(key);	
		}
		System.setProperties(sysProps);

		try {
			Properties props = getGenePatternProperties();
			StringBuffer commentBuff = new StringBuffer("#Genepattern server removed keys: ");	
			for (int i=0; i < keys.size(); i++){
				String key = (String)keys.get(i);
				props.remove(key);	
				if (i > 0) 	commentBuff.append(", ");
				commentBuff.append(key);
			}
			storeGenePatternProperties(props, commentBuff.toString());

			return true;
			
		} catch (IOException ioe){
			return false;
		}
	}

	protected static Properties getGenePatternProperties() throws IOException {
		Properties props = new Properties();
		FileInputStream fis = null;

		try {
			String dir = System.getProperty("genepattern.properties");
			File propFile = new File(dir, "genepattern.properties");
			fis = new FileInputStream(propFile);
			props.load(fis);
			fis.close();
			fis = null;
		} finally {
			if (fis != null) fis.close();
		}
		return props;

	} 
	protected static void storeGenePatternProperties(Properties props, String comment) throws IOException {
		FileOutputStream fos = null;
		try {
			String dir = System.getProperty("genepattern.properties");
			File propFile = new File(dir, "genepattern.properties");
			fos = new FileOutputStream(propFile);
			props.store(fos, comment);
			fos.close();			
			fos = null;
		} finally {
 			if (fos != null) fos.close();
		}
	}

	public static ArrayList getArrayProperty(String key, String defaultValue, String delimiter){
		
		ArrayList<String> props = new ArrayList<String>();
		String propString = System.getProperty(key, defaultValue);
		StringTokenizer strtok = new StringTokenizer(propString, delimiter);
		while (strtok.hasMoreTokens()){
			String p = strtok.nextToken();
			props.add(p);
		}
		return props;
	}
	public static boolean appendArrayPropertyAndStore(String key, String val, String delimiter, boolean unique, boolean caseSensitive){
		String propString = System.getProperty(key);
		
		if (unique){
			ArrayList currentVals = getArrayProperty(key, "", delimiter);
			Iterator iter = currentVals.iterator();
			while(iter.hasNext()){
				String aVal = (String)iter.next();
				if (caseSensitive){
					if (aVal.equals(val)) return true;
				} else {
					if (aVal.equalsIgnoreCase(val)) return true;
				}
			}
		}
		propString = propString + delimiter + val;
		return storeChange(key,propString);
	}

	public static boolean removeArrayPropertyAndStore(String key, String val, String delimiter, boolean caseSensitive){
		String propString = "";
		ArrayList currentVals = getArrayProperty(key, "", delimiter);
		Iterator iter = currentVals.iterator();
		boolean first = true;
		while(iter.hasNext()){
			String aVal = (String)iter.next();
			if (caseSensitive){
				if (aVal.equals(val)) continue;	
			} else {
				if (aVal.equalsIgnoreCase(val)) continue;
			}
			if (!first) {
				propString += delimiter;
			}
			propString += aVal;
			first = false;
		}
		return storeChange(key,propString);
		
	}
	
		
	public static String getPropsDir(){
		return System.getProperty("genepattern.properties"); // props dir
	}
	
	private Map<String, Properties> propertiesMap = new HashMap<String, Properties>();

	
	public boolean saveProperties(String name, Properties props){
		boolean storeSuccess = false;
			
		//getRequest().getSession().setAttribute(name, props);	
		propertiesMap.put(name,props);
		try {
			File propFile = new File(getPropsDir(), name + ".properties");
			FileOutputStream fos = new FileOutputStream(propFile);
			props.store(fos, " ");
			fos.close();			
			fos = null;
			storeSuccess = true;
		} catch (Exception e){
			storeSuccess = false;
		} 
		
		return storeSuccess;
	}
	
	public void reloadCommandPrefixesFromDisk() throws IOException {
		  Properties commandPrefixes = new Properties();
		  Properties taskPrefixMapping = new Properties();
		  File cpFile = new File(getPropsDir(), COMMAND_PREFIX +".properties");
		  File tpmFile = new File(getPropsDir(), TASK_PREFIX_MAPPING +".properties");
		  
		  if (cpFile.exists()) commandPrefixes.load(new FileInputStream(cpFile));
		  if (tpmFile.exists())taskPrefixMapping.load(new FileInputStream(tpmFile));
		  
		  propertiesMap.put(COMMAND_PREFIX, commandPrefixes);
		  propertiesMap.put(TASK_PREFIX_MAPPING, taskPrefixMapping);
	}

	public Properties getProperties(String name){
		return propertiesMap.get(name);
	}
	
	public Properties getCommandPrefixes(){
		return propertiesMap.get(COMMAND_PREFIX);
	}
	public void  setCommandPrefixes(Properties p){
		propertiesMap.put(COMMAND_PREFIX, p);
	}
	
	public Properties getTaskPrefixMapping(){
		return propertiesMap.get(TASK_PREFIX_MAPPING);
	}
	public void  setTaskPrefixMapping(Properties p){
		propertiesMap.put(TASK_PREFIX_MAPPING, p);
	}
	
}