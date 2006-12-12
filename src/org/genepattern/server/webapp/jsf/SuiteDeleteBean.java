/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;


public class SuiteDeleteBean implements java.io.Serializable {
	private static Logger log = Logger.getLogger(SuiteDeleteBean.class);
	
    private List suites;
    
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
    

}
