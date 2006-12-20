/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.process.InstallTask;
import org.genepattern.server.process.InstallTasksCollectionUtils;
import org.genepattern.server.process.ZipSuite;
import org.genepattern.server.process.ZipSuiteWithDependents;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;


public class ManageSuiteBean /*implements java.io.Serializable*/ {
	private static Logger log = Logger.getLogger(ManageSuiteBean.class);
	 
    private List suites;
    private Suite currentSuite= null;
    private List<ModuleCategory> categories;
    
    private boolean includeDependents = false;
    private Map<String, String> supportFiles= null;
    
    /**
     * @return
     */
    public List<Suite> getSuites() {
    	List<Suite> list = (suites==null)?(new SuiteDAO()).findAll():suites;
		return list;   	
    }
    
    /**
     * @return
     */
    public Suite getCurrentSuite() {
    	return currentSuite;
    }
    
    public boolean isCurrentSuiteSet() {
    	return (currentSuite!=null);
    }
    
    /**
     * @return
     */
    public List getCategoryColumnsForSuite() {
        List<List> cols = new ArrayList<List>();
        
        if (currentSuite!=null) {
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
        }
        return cols;
    }
    
    /**
     * @return
     */
    /*public String getSupportFiles() {
    	StringBuffer supportFiles = new StringBuffer();
    	try {
    		if (currentSuite!=null) {
	    		String suiteDirPath = DirectoryManager.getSuiteLibDir(currentSuite.getName(), currentSuite.getLsid(), currentSuite.getOwner());
	    		File suiteDir = new File(suiteDirPath);
	    		File[] allFiles=suiteDir.listFiles();
	    		for (File file:allFiles) {
	    			supportFiles.append(file.getAbsolutePath()).append("\n");
	    		}
    		}
    	}catch (Exception e) {
            HibernateUtil.rollbackTransaction(); // This shouldn't be
                                                    // neccessary, but just in
                                                    // case
            throw new RuntimeException(e); // @todo -- replace with appropriate
                                            // GP exception
        }
        return (supportFiles.length()>0)?supportFiles.substring(0, supportFiles.length()-1).toString():"";
    }*/
    
    public Map getSupportFiles() {
    	
    	try {
    		if (currentSuite!=null) {
	    		String suiteDirPath = DirectoryManager.getSuiteLibDir(currentSuite.getName(), currentSuite.getLsid(), currentSuite.getOwner());
	    		File suiteDir = new File(suiteDirPath);
	    		File[] allFiles=suiteDir.listFiles();
	    		int cnt=1;
	    		supportFiles = (allFiles.length>0) ? new HashMap<String, String>():null;
	    		for (File file:allFiles) {
	    			supportFiles.put("sf"+cnt, file.getAbsolutePath());
	    			cnt++;
	    		}
    		}
    	}catch (Exception e) {
            HibernateUtil.rollbackTransaction(); // This shouldn't be
                                                    // neccessary, but just in
                                                    // case
            throw new RuntimeException(e); // @todo -- replace with appropriate
                                            // GP exception
        }
        return supportFiles;
    }
    
    /**
     * @return
     */
    public String view() {
    	String lsid = UIBeanHelper.getRequest().getParameter("lsid");
    	currentSuite = (new SuiteDAO()).findById(lsid);
    	return (currentSuite!=null)?"view suite":"failure";
    }

    /**
     * @param event
     */
    public void exportExDependents(ActionEvent event) {
    	ZipSuite zs = new ZipSuite();
    	export(zs);
    }
    
    public String exportInDependents() {
    	ZipSuite zs = new ZipSuiteWithDependents();
    	//List missingLsids = checkVersion();
    	export(zs);
    	return ""/*"task catalog"*/;
    }
    
   private List checkVersion() {
	   List<String> missingLsids = new ArrayList<String>();
	   setCurrentSuite();
       if (currentSuite != null) {
    		
    		InstallTasksCollectionUtils collection = new InstallTasksCollectionUtils(UIBeanHelper.getUserId(),
                false);
    		InstallTask[] tasks = null;
	        try {
	        	tasks = collection.getAvailableModules();
	        } catch (Exception e) {
	            log.error(e);
	        }
	        Map lsidToTaskMap = new HashMap<String, InstallTask>();
	        if (tasks != null) {
	            for (InstallTask t : tasks) {
	                lsidToTaskMap.put(t.getLsid(), t);
	            }
	        }
    		List<String> requestedLsids = currentSuite.getModules();
    		for (String requestedLsid : requestedLsids) {
    			if (lsidToTaskMap.containsKey(requestedLsid)) {
    				missingLsids.add(requestedLsid);
    			}
    		}
    	}
    	return missingLsids;
    }
    
    private void export(ZipSuite zs) {
    	String lsid = UIBeanHelper.getRequest().getParameter("lsid");
    	if (lsid == null || lsid.equals("null") || lsid.length() == 0) return;
    	
    	String userID= UIBeanHelper.getUserId(); 
		if (userID == null) return; // come back after login
	
    	FacesContext facesContext=UIBeanHelper.getFacesContext();
    	try {
    		File zipFile = zs.packageSuite(lsid, userID);
    	    
    	    
    	    HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
    	    
    	    LocalAdminClient adminClient = new LocalAdminClient(userID);
    		SuiteInfo si = adminClient.getSuite(lsid);
    	    String contentType = "application/x-zip-compressed" + "; name=\"" + si.getName()+".zip" + "\";";
    	    response.addHeader("Content-Disposition", "attachment; filename=\"" + si.getName()+".zip" + "\";");
    		response.setContentLength((int) zipFile.length());
    		response.setContentType(contentType);
       		OutputStream out = response.getOutputStream();

			//Copy the contents of the file to the output stream
			byte[] buf = new byte[1024];
			int count = 0;
			FileInputStream in = new FileInputStream(zipFile);
			while ((count = in.read(buf)) >= 0) {
				out.write(buf, 0, count);
			}
			in.close();
			out.flush();
			out.close();
			
			zipFile.delete();
			facesContext.responseComplete();
    		  
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public String edit() {
    	setCurrentSuite();
    	return "edit suite";
    }
    
    private void setCurrentSuite() {
    	if (currentSuite==null) {
	    	String lsid = UIBeanHelper.getRequest().getParameter("lsid");
	    	currentSuite = (new SuiteDAO()).findById(lsid);
    	}
    }
    
    /**
     * Delete the selected jobs and files.
     * 
     * @return
     */
    public String delete() {
        String[] selectedSuites = UIBeanHelper.getRequest().getParameterValues("selectedSuites");
        deleteSuites(selectedSuites);
        return "delete suite";
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
            		//Delete supporting files
            		
                	try {
                		String suiteDirPath = DirectoryManager.getSuiteLibDir(s.getName(), s.getLsid(), s.getOwner());
                		File suiteDir = new File(suiteDirPath);
                		File[] allFiles=suiteDir.listFiles();
                		for (File file:allFiles) {
                			file.delete();
                		}
                		suiteDir.delete();
                	}catch (Exception e) {
                        HibernateUtil.rollbackTransaction(); // This shouldn't be
                                                                // neccessary, but just in
                                                                // case
                        throw new RuntimeException(e); // @todo -- replace with appropriate
                                                        // GP exception
                    }
                	//end of deleting supporting files
            	}
            }
        }
    }
    
    public void deleteSupportFile(ActionEvent event) {
    	try {
    		String key = UIBeanHelper.getRequest().getParameter("supportFileKey");
    		supportFiles.remove(key);
    	}catch (Exception e) {
            throw new RuntimeException(e); // @todo -- replace with appropriate
                                            // GP exception
        }
    }
    
    public boolean getIncludeDependents() {
    	return includeDependents;
    }
    
    public void setIncludeDependents(boolean includeDependents) {
    	this.includeDependents = includeDependents;
    }
    
    
}
