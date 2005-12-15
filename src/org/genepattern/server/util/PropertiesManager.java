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


import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

public class PropertiesManager {


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

}