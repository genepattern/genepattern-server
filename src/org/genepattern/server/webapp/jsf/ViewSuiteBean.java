/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.webservice.server.DirectoryManager;


public class ViewSuiteBean implements java.io.Serializable {
	private static Logger log = Logger.getLogger(ViewSuiteBean.class);
	 
    private List suites;
    private Suite currentSuite;
    List<ModuleCategory> categories;
    
    /**
     * @return
     */
    public List<Suite> getSuites() {
    	List<Suite> list = (suites==null)?(new SuiteDAO()).findAll():suites;
		return list;   	
    }
    
    /**
     * Delete the selected jobs and files.
     * 
     * @return
     */
    public void delete(ActionEvent event) {
        String[] selectedSuites = UIBeanHelper.getRequest().getParameterValues("selectedSuites");
        deleteSuites(selectedSuites);

    }
    
    /**
     * @param suiteLsids
     */
    private void deleteSuites(String[] suiteLsids) {
        String user = UIBeanHelper.getUserId();
        if (suiteLsids != null) { 	
            for (String lsid : suiteLsids) {
            	Suite s = (Suite) HibernateUtil.getSession().get(org.genepattern.server.domain.Suite.class, lsid);
            	if (s.getOwner().equals(user)) {
            		(new SuiteDAO()).delete(s);
            	}
            }
        }
    }
    
    /**
     * @return
     */
    public String viewSuite() {
    	String lsid = UIBeanHelper.getRequest().getParameter("lsid");
    	currentSuite = (new SuiteDAO()).findById(lsid);
    	
    	
            
    	return (currentSuite!=null)?"view suite":"failure";
    }
    
    
    /**
     * @return
     */
    public Suite getCurrentSuite() {
    	return currentSuite;
    }
    
    
    
    /**
     * @return
     */
    public List getCategoryColumnsForSuite() {
        List<List> cols = new ArrayList<List>();
        
        if(categories == null) {
          categories = (new ModuleHelper()).getTasksByTypeForSuite(currentSuite);
        }
        
        // Find the midpoint in the category list.
        int totalCount = 0;
        for (ModuleCategory cat : categories) {
            totalCount += cat.getModuleCount();
        }
        int midpoint = totalCount / 2;

        cols.add(new ArrayList());
        cols.add(new ArrayList());
        int cumulativeCount = 0;
        for (ModuleCategory cat : categories) {
            if (cumulativeCount < midpoint) {
                cols.get(0).add(cat);
            }
            else {
                cols.get(1).add(cat);
            }
            cumulativeCount += cat.getModuleCount();
        }
        return cols;
    }
    
    /**
     * @return
     */
    public String getSupportFiles() {
    	StringBuffer supportFiles = new StringBuffer();
    	try {
    		String suiteDirPath = DirectoryManager.getSuiteLibDir(currentSuite.getName(), currentSuite.getLsid(), currentSuite.getOwner());
    		File suiteDir = new File(suiteDirPath);
    		File[] allFiles=suiteDir.listFiles();
    		for (File file:allFiles) {
    			supportFiles.append(file.getAbsolutePath()).append("\n");
    		}  		
    	}catch (Exception e) {
            HibernateUtil.rollbackTransaction(); // This shouldn't be
                                                    // neccessary, but just in
                                                    // case
            throw new RuntimeException(e); // @todo -- replace with appropriate
                                            // GP exception
        }
        return (supportFiles.length()>0)?supportFiles.substring(0, supportFiles.length()-1).toString():"";
    }

}
