package org.genepattern.server.util;


import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;

public class PropertiesManager {


	public static boolean storeChange(String key, String value){
		boolean storeSuccess = false;
		System.setProperty(key, value);
	
		String dir = System.getProperty("genepattern.properties");
		File propFile = new File(dir, "genepattern.properties");
		FileInputStream fis = null;
		FileOutputStream fos = null;
		Properties props = new Properties();
		try {
			fis = new FileInputStream(propFile);
			props.load(fis);
			fis.close();
			fis = null;
			props.setProperty(key, value);
			fos = new FileOutputStream(propFile);
			props.store(fos, "#Genepattern server updated " + key);
			fos.close();			
			fos = null;
			storeSuccess = true;
		} catch (Exception e){
			storeSuccess = false;
		} finally {
			try {	if (fis != null) fis.close();} catch (Exception e){}
			try { if (fos != null) fos.close();} catch (Exception ee){}

		}
		return storeSuccess;
	}


}