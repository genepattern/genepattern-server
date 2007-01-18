/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class ManageTasksBean /* implements java.io.Serializable */{
    private static Logger log = Logger.getLogger(ManageTasksBean.class);

    private Collection tasks;
    private Map<String, TaskGroup> indexedTasks = new HashMap<String, TaskGroup>();
    
    public ManageTasksBean() {
    	LocalAdminClient adminClient = new LocalAdminClient(UIBeanHelper.getUserId());
    	try {
    		tasks = (tasks == null) ? adminClient.getTaskCatalog() : tasks;
    	
	    	TaskInfo ti = null;
	    	String lsid;
	    	
	    	LSID lSID = null;
	    	LSID lastLSID = null;
	    	
	    	for (Iterator<TaskInfo> itTasks = tasks.iterator(); itTasks.hasNext(); ) {
	    		ti = (TaskInfo)itTasks.next();
	    		TaskInfoAttributes tia = ti.giveTaskInfoAttributes();
				lsid = tia.get(GPConstants.LSID);
				try {
					lSID = new LSID(lsid);
				} catch (MalformedURLException mue) {
					continue;
				}
				
				String lsidNoVersion = lSID.toStringNoVersion();
    			TaskGroup versionInfos = (indexedTasks.containsKey(lsidNoVersion)) ? (TaskGroup)indexedTasks.get(lsidNoVersion) : new TaskGroup();
    			versionInfos.addVersionInfo(ti);
    			indexedTasks.put(lSID.toStringNoVersion(), versionInfos);
	    		/*if (!lSID.isSimilar(lastLSID)) {
	    			
					lastLSID = lSID;	
	    		}else {
	    			
	    			
	    			versionInfos.addVersionInfo(ti);
	    		}*/
				
    		  }
	    }catch(Exception e) {
	    		
	    }
    }
    
    /**
     * @return
     */
    public Collection getTasks() {
    	List<TaskGroup> sortedTasks = new ArrayList<TaskGroup>(indexedTasks.values());
    	Collections.sort(sortedTasks, new Comparator() {
            public int compare(Object o1, Object o2) {
                String n1 = ((TaskGroup) o1).getName();
                String n2 = ((TaskGroup) o2).getName();
                return n1.compareToIgnoreCase(n2);
            }
            
        });
        return sortedTasks;
    }
    
    public void delete(ActionEvent event) {
        String[] taskLsids = UIBeanHelper.getRequest().getParameterValues("selectedVersions");
        deleteTasks(taskLsids);
        //return "delete task";
    }

    private void deleteTasks(String[] taskLsids) {
        if (taskLsids != null) {
        	LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(UIBeanHelper.getUserId());
        	TaskGroup temp = null;
            for (String lsid : taskLsids) {
            	try {
        			taskIntegratorClient.deleteTask(lsid);
        			LSID lSID = getLSID(lsid);
        			String lsidNoVersion = lSID.toStringNoVersion();
        			temp = indexedTasks.get(lsidNoVersion);
        			temp.deleteVersionInfo(lsid);
        			if (temp.getIndexedVersions().size()==0) {
        				indexedTasks.remove(lsidNoVersion);
        			}
        		} catch (WebServiceException wse) {
        			
        		}
            }
        }
    }
    /*
    public String doDescription(String description) {
    	int start = description.indexOf("http://");
    	if (start == -1) start = description.indexOf("https://");
    	if (start == -1) start = description.indexOf("ftp://");
    	if (start != -1) {
    		int end = description.indexOf(" ", start);
    		if (end == -1) end = description.indexOf(")", start);
    		if (end == -1) end = description.length();
    		description = StringUtils.htmlEncode(description.substring(0, start)) + 
    				"<a href=\"" + description.substring(start, end) + "\" target=\"_blank\">" + 
    				description.substring(start, end) + "</a>" + 
    				StringUtils.htmlEncode(description.substring(end));
    	}
    	return description;
     }*/
    
    private LSID getLSID(String lsid) {
		LSID lSID = null;
		try {
			lSID = new LSID(lsid);
		} catch (MalformedURLException mue) {
			return null;
		}
		return lSID;
	}
    
    public class TaskGroup implements Serializable {

        // The maximum size of the "short name"
    	
        private String lsidNoVersion = null;
        private String name = null;
        private String description = null;
        private Map<String, VersionInfo> indexedVersions;

        private LocalAdminClient adminClient = null;
        
        /** Creates new TaskInfo */
        public TaskGroup() {
        	adminClient = (adminClient==null) ? new LocalAdminClient(UIBeanHelper.getUserId()) :adminClient;
        	indexedVersions = (indexedVersions == null) ? new TreeMap<String, VersionInfo>() : indexedVersions;
        }
        
        public String getLsidNoVersion() {
        	return lsidNoVersion;
        }
        
        public String getName() {
        	return name;
        }
        
        public String getDescription() {
        	return description;
        }
        
        public void addVersionInfo(TaskInfo ti) throws WebServiceException {
        	lsidNoVersion = (lsidNoVersion==null) ? getLSID(ti.getLsid()).toStringNoVersion() : lsidNoVersion;
        	name = (name==null) ? ti.getName() : name;
        	description = ti.getDescription();
        	String lsid = ti.getLsid();
        	
        	VersionInfo versionInfo = (indexedVersions.containsKey(lsid)) ? indexedVersions.get(lsid) : new VersionInfo(ti);
        		
        	indexedVersions.put(lsid, versionInfo);
        	
        	if (ti.isPipeline()) {
        		TaskInfoAttributes tia  = ti.giveTaskInfoAttributes();
        		String xml = (String)tia.get(GPConstants.SERIALIZED_MODEL);
        		PipelineModel model = null;
    			try {
    				model = PipelineModel.toPipelineModel(xml);
    			} catch (Throwable t) {
    				System.err.println(t.toString() + " loading pipeline model " + ti.getName() + " - " + lsid);
    				System.err.println(xml);
    				return;
    			}
    			Map<String, String> mDependencies = model.getLsidDependencies(); // LSID/Vector of TaskInfo map
    			TaskGroup temp = null;
    			
    			for (Iterator itSubTasks = mDependencies.keySet().iterator(); itSubTasks.hasNext(); ) {
    				String keyLsid = (String)itSubTasks.next();
    				String lsidNoVersion = getLSID(keyLsid).toStringNoVersion();
    				
    				TaskInfo t = adminClient.getTask(keyLsid);
    				if (t!=null) {
    					temp = (indexedTasks.containsKey(lsidNoVersion)) ? indexedTasks.get(lsidNoVersion) : new TaskGroup();
	    				temp.addVersionInfo(t);
	    				temp.setPipelineName(keyLsid, ti);
	    				indexedTasks.put(lsidNoVersion, temp);
    				}
    			}
        	}
        	
        }
        
        public void deleteVersionInfo(String lsid) {
        	indexedVersions.remove(lsid);
        }
        
        private void setPipelineName(String lsid, TaskInfo ti) {
        	indexedVersions.get(lsid).setPipelineName(ti);
        }

        public Collection getIndexedVersions() {
        	return indexedVersions.values();
        }
        
        public boolean isAllUsedBy() {
        	boolean allUsedBy = true;
        	for (VersionInfo info : indexedVersions.values()) {
        		if (!info.isUsedBy()) {
        			allUsedBy=false;
        			break;
        		}
        	}
        	return allUsedBy;
        }
        
        public boolean isOneOwnedByUser() {
        	boolean oneOwnedByUser = false;
        	for (VersionInfo info : indexedVersions.values()) {
        		if (info.isOwnedByUser()) {
        			oneOwnedByUser=true;
        			break;
        		}
        	}
        	return oneOwnedByUser;
        }
        
        public boolean isAllowed() {
        	return !isAllUsedBy() && isOneOwnedByUser();
        }
        
        public class VersionInfo {
        	private String pipeline = null;
        	private boolean isUsedBy = false;
        	
        	private boolean isO = false;
        	private TaskInfo ti;
        	
        	private String lsid = null;
        	
        	public VersionInfo () {
        	}
        	
        	public VersionInfo (TaskInfo ti) {
        		this.ti=ti;
        	}
        	
        	public String getLsid() {
        		return lsid = (ti!=null) ? ti.getLsid() : null;
        		
        	}
        	
        	public void setLsid() {
        		this.lsid=null;
        	}
        	
        	public String getVersion() {
        		return (ti!=null) ? getLSID(ti.getLsid()).getVersion() : null;
        	}
        	
        	public String getReason() {
        		String reason = null;
        		if (ti!=null) {
    	    		String BROAD_AUTHORITY = "broad.mit.edu";
    	    		LSID lSID = getLSID(ti.getLsid());
    	    		String authority = (lSID == null ? "" : lSID.getAuthority());
    	    		
    	    		TaskInfoAttributes tia  = ti.giveTaskInfoAttributes();
    	    		reason = tia.get(GPConstants.VERSION);
    	    		if (reason.equals("1.0") && authority.equals(BROAD_AUTHORITY)) {
    					reason = "";
    				}
    				if (!reason.equals("")) {
    					reason = " (" + reason + ")";
    				}
        		}
        		return reason;
        	}
        	
        	public String getPipelineName() {
        		return pipeline;
        	}
        	
        	public void setPipelineName(TaskInfo pti) {
        		StringBuffer temp = (pipeline == null) ? new StringBuffer() : new StringBuffer(pipeline);
        		temp.append(pti.getName()+"\n");
        		pipeline = temp.toString();
        		isUsedBy = true;
        	}
        	
        	public boolean isUsedBy() {
        		return isUsedBy;
        	}
        	
        	public boolean isOwnedByUser() {
        		isO = ti.getUserId().equals(UIBeanHelper.getUserId());
        		return isO;
        	}
        	
        	public boolean isAllowed() {
        		return (!isUsedBy && isOwnedByUser());
        	}
        	
        }

    }
    
}
