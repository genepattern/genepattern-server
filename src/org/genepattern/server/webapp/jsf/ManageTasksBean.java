/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.util.LSIDVersionComparator;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class ManageTasksBean {
    public class TaskGroup implements Serializable {

	private String lsidNoVersion = null;

	private String description = null;

	private TreeMap<String, VersionInfo> indexedVersions;

	private IAdminClient adminClient = null;

	private boolean pipeline;

	public TaskGroup(TaskInfo ti) {
	    pipeline = ti.isPipeline();
	    adminClient = new LocalAdminClient(UIBeanHelper.getUserId());
	    indexedVersions = new TreeMap<String, VersionInfo>(new Comparator() {
		public int compare(Object o1, Object o2) {
		    try {
			LSID lsid1 = new LSID(o1.toString());
			LSID lsid2 = new LSID(o2.toString());
			return LSIDVersionComparator.INSTANCE.compare(lsid2.getVersion(), lsid1.getVersion());
		    } catch (MalformedURLException e) {
			log.error(e);
			return 0;
		    }
		}
	    });
	    lsidNoVersion = getLSID(ti.getLsid()).toStringNoVersion();
	    description = ti.getDescription();

	}

	/**
	 * Add a specific specific versioned task.
	 * 
	 * @param taskInfo
	 */
	public void addVersionInfo(TaskInfo taskInfo) {

	    String lsid = taskInfo.getLsid();
	    VersionInfo versionInfo = new VersionInfo(taskInfo);
	    indexedVersions.put(lsid, versionInfo);

	    if (taskInfo.isPipeline()) {
		TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
		String xml = (String) tia.get(GPConstants.SERIALIZED_MODEL);
		PipelineModel model = null;
		try {
		    model = PipelineModel.toPipelineModel(xml);
		} catch (Throwable t) {
		    log.error(" loading pipeline model " + taskInfo.getName() + " - " + lsid, t);
		    log.error(xml);
		    return;
		}
		Map<String, String> mDependencies = model.getLsidDependencies(); // LSID/Vector

		for (Iterator itSubTasks = mDependencies.keySet().iterator(); itSubTasks.hasNext();) {
		    String keyLsid = (String) itSubTasks.next();
		    String lsidNoVersion = getLSID(keyLsid).toStringNoVersion();

		    try {
			TaskInfo subTask = adminClient.getTask(keyLsid);
			if (subTask != null) {
			    if (includeAllPublicModules || subTask.getUserId().equals(UIBeanHelper.getUserId())) {
				TaskGroup taskGroup = (indexedTasks.containsKey(lsidNoVersion)) ? indexedTasks
					.get(lsidNoVersion) : new TaskGroup(subTask);
				taskGroup.addVersionInfo(subTask);
				taskGroup.setPipelineName(keyLsid, taskInfo);
				indexedTasks.put(lsidNoVersion, taskGroup);
			    }
			}
		    } catch (WebServiceException e) {
			log.error(e);
			throw new RuntimeException(e);
		    }
		}
	    }
	}

	public void deleteVersionInfo(String lsid) {
	    indexedVersions.remove(lsid);
	}

	public String getDescription() {
	    return description;
	}

	public Collection getIndexedVersions() {
	    return indexedVersions.values();
	}

	public String getLsidNoVersion() {
	    return lsidNoVersion;
	}

	public String getName() {
	    return indexedVersions.isEmpty() ? "" : indexedVersions.get(indexedVersions.firstKey()).getName();
	}

	public boolean isAllowed() {
	    return !isAllUsedBy() && isOneAllowed();
	}

	public boolean isAllUsedBy() {
	    boolean allUsedBy = true;
	    for (VersionInfo info : indexedVersions.values()) {
		if (!info.isUsedBy()) {
		    allUsedBy = false;
		    break;
		}
	    }
	    return allUsedBy;
	}

	public boolean isOneAllowed() {
	    boolean oneAllowed = false;
	    for (VersionInfo info : indexedVersions.values()) {
		if (info.isAllowed()) {
		    oneAllowed = true;
		    break;
		}
	    }
	    return oneAllowed;
	}

	public boolean isPipeline() {
	    return pipeline;
	}

	private void setPipelineName(String lsid, TaskInfo ti) {
	    indexedVersions.get(lsid).addPipelineName(ti);
	}

    }

    public static class VersionInfo {

	private List<String> pipelineNames = new ArrayList<String>();

	private boolean isUsedBy = false;

	private TaskInfo ti;

	private boolean deleteAuthorized = false;

	private boolean editAuthorized = false;

	public VersionInfo() {
	}

	public VersionInfo(TaskInfo ti) {
	    this.ti = ti;
	    String userId = UIBeanHelper.getUserId();
	    deleteAuthorized = ti.getUserId().equals(userId) || AuthorizationHelper.adminModules();
	    editAuthorized = ti.getUserId().equals(userId) && LSIDUtil.getInstance().isAuthorityMine(ti.getLsid());
	}

	public void addPipelineName(TaskInfo pti) {
	    pipelineNames.add(pti.getName() + " ver. " + getLSID(pti.getLsid()).getVersion());
	    isUsedBy = true;
	}

	public String getLsid() {
	    return (ti != null) ? ti.getLsid() : null;

	}

	public String getName() {
	    return ti.getName();
	}

	public String getOwner() {
	    return ti != null ? ti.getUserId() : null;
	}

	public List<String> getPipelineNames() {
	    return pipelineNames;
	}

	public String getReason() {
	    String reason = null;
	    if (ti != null) {
		String BROAD_AUTHORITY = "broad.mit.edu";
		LSID lSID = getLSID(ti.getLsid());
		String authority = (lSID == null ? "" : lSID.getAuthority());

		TaskInfoAttributes tia = ti.giveTaskInfoAttributes();
		reason = tia.get(GPConstants.VERSION);
		if (reason.equals("1.0") && authority.equals(BROAD_AUTHORITY)) {
		    reason = "";
		}
	    }
	    return reason;
	}

	public String getVersion() {
	    return (ti != null) ? getLSID(ti.getLsid()).getVersion() : null;
	}

	public boolean isAllowed() {
	    return (!isUsedBy && deleteAuthorized);
	}

	public boolean isDeleteAuthorized() {
	    return deleteAuthorized;
	}

	public boolean isEditAuthorized() {
	    return editAuthorized;
	}

	public boolean isUsedBy() {
	    return isUsedBy;
	}

    }

    private static Logger log = Logger.getLogger(ManageTasksBean.class);

    private static LSID getLSID(String lsid) {
	LSID lSID = null;
	try {
	    lSID = new LSID(lsid);
	} catch (MalformedURLException mue) {
	    return null;
	}
	return lSID;
    }

    private Collection<TaskInfo> tasks;

    private Map<String, TaskGroup> indexedTasks = null;

    private boolean includeAllPublicModules = true;

    private boolean includeAllPrivateModules;

    private boolean adminModules;

    public ManageTasksBean() {
	adminModules = AuthorizationHelper.adminModules();
	IAdminClient adminClient = new LocalAdminClient(UIBeanHelper.getUserId());
	try {
	    String userId = UIBeanHelper.getUserId();
	    this.includeAllPublicModules = Boolean.valueOf(new UserDAO().getPropertyValue(userId,
		    "includeAllPublicModules", String.valueOf(this.includeAllPublicModules)));

	    this.includeAllPrivateModules = Boolean.valueOf(new UserDAO().getPropertyValue(userId,
		    "includeAllPrivateModules", String.valueOf(this.includeAllPrivateModules)))
		    && AuthorizationHelper.adminModules();

	    if (tasks == null) {
		if (includeAllPrivateModules) {
		    tasks = Arrays.asList(adminClient.getAllTasksForModuleAdmin());
		} else if (includeAllPublicModules) {
		    tasks = adminClient.getTaskCatalog();
		} else {
		    tasks = Arrays.asList(adminClient.getTasksOwnedBy());
		}
	    }
	} catch (WebServiceException e) {
	    log.error(e);
	    throw new RuntimeException(e);
	}
    }

    public void delete(ActionEvent event) {
	String[] taskLsids = UIBeanHelper.getRequest().getParameterValues("selectedVersions");
	deleteTasks(taskLsids);
    }

    /**
     * @return
     */
    public Collection getTasks() {
	if (indexedTasks == null) {
	    resetIndexedTasks();
	}
	List<TaskGroup> sortedTasks = new ArrayList<TaskGroup>(indexedTasks.values());
	Collections.sort(sortedTasks, new Comparator<TaskGroup>() {
	    public int compare(TaskGroup o1, TaskGroup o2) {
		String n1 = o1.getName();
		String n2 = o2.getName();
		return n1.compareToIgnoreCase(n2);
	    }

	});
	return sortedTasks;
    }

    public boolean isAdminModules() {
	return adminModules;
    }

    public boolean isIncludeAllPrivateModules() {
	return includeAllPrivateModules;
    }

    public boolean isIncludeAllPublicModules() {
	return includeAllPublicModules;
    }

    public void setIncludeAllPrivateModules(boolean includeAllPrivateModules) {
	includeAllPrivateModules = includeAllPrivateModules && AuthorizationHelper.adminModules();
	new UserDAO().setProperty(UIBeanHelper.getUserId(), "includeAllPrivateModules", String
		.valueOf(includeAllPrivateModules));
	this.includeAllPrivateModules = includeAllPrivateModules;
	resetIndexedTasks();
    }

    public void setIncludeAllPublicModules(boolean includeAllPublicModules) {
	new UserDAO().setProperty(UIBeanHelper.getUserId(), "includeAllPublicModules", String
		.valueOf(includeAllPublicModules));
	this.includeAllPublicModules = includeAllPublicModules;
	resetIndexedTasks();
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
		    if (temp.getIndexedVersions().size() == 0) {
			indexedTasks.remove(lsidNoVersion);
		    }
		} catch (Exception e) {
		    log.error(e);
		    throw new RuntimeException(e);
		}
	    }
	}
    }

    private void resetIndexedTasks() {
	indexedTasks = new HashMap<String, TaskGroup>();

	for (Iterator<TaskInfo> itTasks = tasks.iterator(); itTasks.hasNext();) {
	    TaskInfo ti = (TaskInfo) itTasks.next();
	    if (includeAllPublicModules || ti.getUserId().equals(UIBeanHelper.getUserId())) {
		String lsid = ti.getLsid();
		LSID lSID = null;
		try {
		    lSID = new LSID(lsid);
		} catch (MalformedURLException mue) {
		    log.error("Error creating LSID (Malformed URL): " + lsid, mue);
		    throw new RuntimeException("Error creating LSID (Malformed URL): " + lsid);
		}

		String lsidNoVersion = lSID.toStringNoVersion();
		TaskGroup taskGroup = indexedTasks.get(lsidNoVersion);
		if (taskGroup == null) {
		    taskGroup = new TaskGroup(ti);
		    indexedTasks.put(lsidNoVersion, taskGroup);
		}
		taskGroup.addVersionInfo(ti);

	    }
	}

    }

}
