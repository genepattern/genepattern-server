package org.genepattern.server.util;

import java.io.*;
import java.util.*;
import java.net.URL;

/**
 * Class which loads properties files and returns values
 * found in the Property file of interest
 * This is a singleton
 *@author Brian Gilman
 *@version $Revision $
 */

public class PropertyFactory implements FilenameFilter{


    /** 
     * the path to the property file will be located in the 
     * environment variable omnigene.conf
     */
    
    
    /**
     * The properties object that we get props out of
     */
    private static Properties props = null;

    /**
     * The internal instance of this class
     */
    private static  PropertyFactory instance = null;

    /**
     * The stream we use to load the props file
     */
    private static InputStream in = null;
    
    /**
     * Hashtable of Propery files and objects which are loaded;
     */
    private static Hashtable propsHash = null;
    
    /**
     * This is the way we keep this a singleton
     */
    protected PropertyFactory(){}


    /**
     *@return PropertiesFactory objects
     */
    public static synchronized PropertyFactory getInstance(){

	if(instance == null){
	    instance = new PropertyFactory();
	}
	
	return instance;
    }

    /**
     * loads the props file
     * they must either have a .properties extention or .props extention
     *@exception if the PropertyFile cannot be found
     */
    private Properties loadPropertiesFile(String propsFile) throws PropertyNotFoundException{
	
	try{
	    in = new FileInputStream(propsFile);
	    props = new Properties();
	    props.load(in);
	    in.close();
	}catch(IOException e){
	    throw new PropertyNotFoundException(e.toString());
	}
	
	return props;
    }

    
    /**
     *@exception if Properyfile cannot be found
     *@exception if an IO error occurs
     */
    private void getPropertiesFiles() throws IOException, PropertyNotFoundException{
	
	
	propsHash = new Hashtable();
	String[] list  = new File(System.getProperty("omnigene.conf")).list(this);
	for(int i = 0; i<list.length;i++){
	    String filePath = System.getProperty("omnigene.conf")+ File.separator + list[i];
	    
	    Properties props = loadPropertiesFile(filePath);
	    
	    propsHash.put(list[i], props);
	}
    }


    /**
     * get properties from classpath
     */
    
    public Properties getPropertiesOnClasspath(String propsFileName) throws PropertyNotFoundException{
	
	Properties p = null;
	/**
	 * First try and find the properties files on the classpath
	 * This is dependant on jdk 1.3.1 which all major platform support
	 * We'll check for this first and pass over it if we're not >=jdk1.3.1
	 * If not >= 1.3 then look for omnigene.conf property
	 */
                  
	//if((Float.parseFloat(System.getProperty("java.specification.version"))) < 1.3){
	if((Float.parseFloat(System.getProperty("java.specification.version"))) >= 1.3){
	    
	    try{
		//get reference to resource on classpath
		//URL path = ClassLoader.getSystemClassLoader().getResource(propsFileName);
		URL path = this.getClass().getClassLoader().getResource(propsFileName);
		if(path == null){
		    throw new PropertyNotFoundException("Can't find property file: " + System.getProperty("java.class.path"));
		}
		//Get this resource and load it
	        p = new Properties();
		p.load(path.openStream());
	    }catch(IOException ioe){
		System.out.println("Pros file cannot be found: " + ioe.getMessage());
	    }catch(SecurityException se){
		System.out.println("Security Exception: " + se.getMessage());
	    }
	}else{
	    throw new UnsupportedOperationException("Cannot get classpath with Java version less than 1.3");
	}
	
	return p;
    }
	    

    /**
     * used to filter the resources dir for files ending 
     * with .props or .properties
     */
    public boolean accept(File f, String name){
	if(name.endsWith(".properties") || name.endsWith(".props")){
	    return true;
	}
	
	return false;
    }
    
    /**
     * Gets Properties object given a filename 
     *@return the Properties object associated with the filename
     *@exception if can't find property file or property object
     */
     
    public Properties getProperties(String propertyFileName) throws IOException, PropertyNotFoundException{
	
	if(propsHash == null){
	    getPropertiesFiles();
	}
	
	Properties props = (Properties)propsHash.get(propertyFileName);
	    
	if(props == null){
	    throw new NullPointerException("Property File can't be found or does not exist");
	}

	return props;
    }
	
}
	
	




