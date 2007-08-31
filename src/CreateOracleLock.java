import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.genepattern.server.AnalysisManager;
import org.genepattern.server.AnalysisTask;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;

import java.util.Vector;


public class CreateOracleLock {
	static Logger log = Logger.getLogger(CreateOracleLock.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		loadProperties();

		//AnalysisManager.getInstance();
		//AnalysisTask.startQueue();
		 //HibernateUtil.beginTransaction();
//		 GenePatternAnalysisTask genepattern = new GenePatternAnalysisTask();
//         Vector jobQueue = genepattern.getWaitingJobs();
//         
//         log.debug("About to commit after getWaitingJobs()");
//         HibernateUtil.commitTransaction();
//         log.debug("After commit");
		
		AnalysisTask t = new AnalysisTask(20);
		t.run();
		
		System.out.println("Done");
	}

	
	
    protected static void loadProperties() throws Exception {
    	File propFile = null;
    	FileInputStream fis = null;
    	
    	try {
    	    
    	    Properties sysProps = System.getProperties();
    	    String dir = "/Users/liefeld/projects/oracle_test";
    	    
    	    propFile = new File(dir, "genepattern.properties");
    	    Properties props = new Properties();

    	    fis = new FileInputStream(propFile);
    	    props.load(fis);
    	    log.debug("loaded GP properties from " + propFile.getCanonicalPath());

    	   
    	    // copy all of the new properties to System properties
    	    for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
	    		String key = (String) iter.next();
	    		String val = (String) props.getProperty(key);
	    		if (val.startsWith(".")) {
	    		    val = new File(val).getAbsolutePath();
	    		}
	    		sysProps.setProperty(key, val);
	    	}

    	    propFile = new File(dir, "build.properties");
    	    fis = new FileInputStream(propFile);
    	    props.load(fis);
    	    fis.close();
    	    fis = null;
    	    // copy all of the new properties to System properties
    	    for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
	    		String key = (String) iter.next();
	    		String val = (String) props.getProperty(key);
	    		if (val.startsWith(".")) {
	    		    val = new File(val).getCanonicalPath();
	    		}
	    		sysProps.setProperty(key, val);
    	    }

    	  
    	} catch (IOException ioe) {
    	    ioe.printStackTrace();
    	    String path = null;
    	    try {
    		path = propFile.getCanonicalPath();
    	    } catch (IOException ioe2) {
    	    }
    	   
    	} finally {
    	    try {
    		if (fis != null)
    		    fis.close();
    	    } catch (IOException ioe) {
    	    }
    	}
        }

}
