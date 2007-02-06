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

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.util.AuthorizationManagerFactory;
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
            this.showEveryonesTasks = Boolean.valueOf(new UserDAO().getPropertyValue(userId, "showEveryonesTasks",
                    String.valueOf(this.showEveryonesTasks)));
            if (this.showEveryonesTasks
                    && !AuthorizationManagerFactory.getAuthorizationManager().checkPermission("administrateServer",
                            userId)) {
                this.showEveryonesTasks = false;

            }
            getIndexedTasks();
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
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
        indexedTasks = new HashMap<String, TaskGroup>();

        for (Iterator<TaskInfo> itTasks = tasks.iterator(); itTasks.hasNext();) {
            TaskInfo ti = (TaskInfo) itTasks.next();
            if (showEveryonesTasks || ti.getUserId().equals(UIBeanHelper.getUserId())) {

                TaskInfoAttributes tia = ti.giveTaskInfoAttributes();
                String lsid = tia.get(GPConstants.LSID);
                LSID lSID = null;
                try {
                    lSID = new LSID(lsid);
                } catch (MalformedURLException mue) {
                    log.error("Error creating LSID (Malformed URL): " + lsid, mue);
                    throw new RuntimeException("Error creating LSID (Malformed URL): " + lsid);
                }

                String lsidNoVersion = lSID.toStringNoVersion();
                TaskGroup taskGroup = (indexedTasks.containsKey(lsidNoVersion)) ? indexedTasks.get(lsidNoVersion)
                        : new TaskGroup(ti);
                taskGroup.addVersionInfo(ti);
                indexedTasks.put(lSID.toStringNoVersion(), taskGroup);
            }
        }
    }

    public boolean isShowEveryonesTasks() {
        return showEveryonesTasks;
    }

    public void setShowEveryonesTasks(boolean showEveryonesTasks) {
        if (showEveryonesTasks
                && !AuthorizationManagerFactory.getAuthorizationManager().checkPermission("administrateServer",
                        UIBeanHelper.getUserId())) {
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

        private boolean pipeline;

        /** Creates new TaskInfo */
        public TaskGroup(TaskInfo ti) {
            pipeline = ti.isPipeline();
            adminClient = (adminClient == null) ? new LocalAdminClient(UIBeanHelper.getUserId()) : adminClient;
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

        public void addVersionInfo(TaskInfo ti) {
            lsidNoVersion = (lsidNoVersion == null) ? getLSID(ti.getLsid()).toStringNoVersion() : lsidNoVersion;
            name = (name == null) ? ti.getName() : name;
            description = ti.getDescription();
            String lsid = ti.getLsid();

            VersionInfo versionInfo = (indexedVersions.containsKey(lsid)) ? indexedVersions.get(lsid)
                    : new VersionInfo(ti);

            indexedVersions.put(lsid, versionInfo);

            if (ti.isPipeline()) {
                TaskInfoAttributes tia = ti.giveTaskInfoAttributes();
                String xml = (String) tia.get(GPConstants.SERIALIZED_MODEL);
                PipelineModel model = null;
                try {
                    model = PipelineModel.toPipelineModel(xml);
                } catch (Throwable t) {
                    log.error(" loading pipeline model " + ti.getName() + " - " + lsid, t);
                    log.error(xml);
                    return;
                }
                Map<String, String> mDependencies = model.getLsidDependencies(); // LSID/Vector
                // of
                // TaskInfo
                // map
                TaskGroup temp = null;

                for (Iterator itSubTasks = mDependencies.keySet().iterator(); itSubTasks.hasNext();) {
                    String keyLsid = (String) itSubTasks.next();
                    String lsidNoVersion = getLSID(keyLsid).toStringNoVersion();

                    try {
                        TaskInfo t = adminClient.getTask(keyLsid);
                        if (t != null) {
                            if (!showEveryonesTasks && !t.getUserId().equals(UIBeanHelper.getUserId())) {
                                continue;
                            }
                            temp = (indexedTasks.containsKey(lsidNoVersion)) ? indexedTasks.get(lsidNoVersion)
                                    : new TaskGroup(ti);
                            temp.addVersionInfo(t);
                            temp.setPipelineName(keyLsid, ti);
                            indexedTasks.put(lsidNoVersion, temp);
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

        private void setPipelineName(String lsid, TaskInfo ti) {
            indexedVersions.get(lsid).addPipelineName(ti);
        }

        public Collection getIndexedVersions() {
            return indexedVersions.values();
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

        public boolean isAllowed() {
            return !isAllUsedBy() && isOneAllowed();
        }

        public class VersionInfo {

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
                deleteAuthorized = ti.getUserId().equals(userId)
                        || AuthorizationManagerFactory.getAuthorizationManager()
                                .checkPermission("adminModules", userId);
                editAuthorized = ti.getUserId().equals(userId);
            }

            public String getLsid() {
                return (ti != null) ? ti.getLsid() : null;

            }

            public String getVersion() {
                return (ti != null) ? getLSID(ti.getLsid()).getVersion() : null;
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

            public List<String> getPipelineNames() {
                return pipelineNames;
            }

            public void addPipelineName(TaskInfo pti) {
                pipelineNames.add(pti.getName() + " ver. " + getLSID(pti.getLsid()).getVersion());
                isUsedBy = true;
            }

            public boolean isUsedBy() {
                return isUsedBy;
            }

            public boolean isOwnedByUser() {
                return ti.getUserId().equals(UIBeanHelper.getUserId());
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

        }

        public boolean isPipeline() {
            return pipeline;
        }

    }

}
