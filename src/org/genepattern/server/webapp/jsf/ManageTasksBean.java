/**
 * 
 */
package org.genepattern.server.webapp.jsf;

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
import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class ManageTasksBean /* implements java.io.Serializable */{
    private static Logger log = Logger.getLogger(ManageTasksBean.class);

    private Collection tasks;
    private Map<String, TaskGroup> indexedTasks = new HashMap<String, TaskGroup>();
    
    private boolean showEveryonesTasks = true;
    
    public ManageTasksBean() {
    	LocalAdminClient adminClient = new LocalAdminClient(UIBeanHelper.getUserId());
    	try {
    		tasks = (tasks == null) ? adminClient.getTaskCatalog() : tasks;
	    	
	    	String userId = UIBeanHelper.getUserId();
	    	this.showEveryonesTasks = Boolean.valueOf(new UserDAO().getPropertyValue(userId, "showEveryonesTasks", String
	                .valueOf(this.showEveryonesTasks)));
	        if (this.showEveryonesTasks
	                && !new AuthorizationManagerFactoryImpl().getAuthorizationManager().checkPermission(
	                        "administrateServer", userId)) {
	        	this.showEveryonesTasks = false;

	        }
	        getIndexedTasks();
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
    
    private void getIndexedTasks() {
    	TaskInfo ti = null;
    	String lsid;
    	LSID lSID = null;
    	indexedTasks = new HashMap<String, TaskGroup>();
    	try {
	    	for (Iterator<TaskInfo> itTasks = tasks.iterator(); itTasks.hasNext(); ) {
	    		ti = (TaskInfo)itTasks.next();
	    		if (!showEveryonesTasks && !ti.getUserId().equals(UIBeanHelper.getUserId())) {
	    			continue;
	    		}
		    		
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
			}
    	}catch (Exception e) {
    		
    	}
    }
    
    public boolean isShowEveryonesJobs() {
        return showEveryonesTasks;
    }
    
    public void setShowEveryonesJobs(boolean showEveryonesTasks) {
        if (showEveryonesTasks
                && !new AuthorizationManagerFactoryImpl().getAuthorizationManager().checkPermission(
                        "administrateServer", UIBeanHelper.getUserId())) {
        	showEveryonesTasks = false;

        }
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "showEveryonesTasks", String.valueOf(showEveryonesTasks));
        this.showEveryonesTasks = showEveryonesTasks;
        getIndexedTasks();
    }
    
    public void delete(ActionEvent event) {
        String[] taskLsids = UIBeanHelper.getRequest().getParameterValues("selectedVersions");
        deleteTasks(taskLsids);
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
    					if (!showEveryonesTasks && !t.getUserId().equals(UIBeanHelper.getUserId())) {
    		    			continue;
    		    		}
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
        	private TaskInfo ti;
        	
        	public VersionInfo () {
        	}
        	
        	public VersionInfo (TaskInfo ti) {
        		this.ti=ti;
        	}
        	
        	public String getLsid() {
        		return (ti!=null) ? ti.getLsid() : null;
        		
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
        		temp.append(pti.getName()+" ver. "+getLSID(pti.getLsid()).getVersion()+"\n" );
        		pipeline = temp.toString();
        		isUsedBy = true;
        	}
        	
        	public boolean isUsedBy() {
        		return isUsedBy;
        	}
        	
        	public boolean isOwnedByUser() {
        		return ti.getUserId().equals(UIBeanHelper.getUserId());
        	}
        	
        	public boolean isAllowed() {
        		return (!isUsedBy && isOwnedByUser());
        	}
        	
        }

    }
    
}
