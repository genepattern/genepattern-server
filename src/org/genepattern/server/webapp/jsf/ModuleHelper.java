package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getUserId;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

public class ModuleHelper {
    private static Logger log = Logger.getLogger(ModuleHelper.class);

    private TaskInfo[] allTasks;

    /**
     * 
     */
    public ModuleHelper() {
        allTasks = (new AdminDAO()).getAllTasksForUser(getUserId());
    }

    /**
     * @return
     */
    public ModuleCategory getRecentlyUsed() {
        AdminDAO dao = new AdminDAO();
        String userId = UIBeanHelper.getUserId();
        int recentJobsToShow = Integer.parseInt(new UserDAO().getPropertyValue(userId, UserPropKey.RECENT_JOBS_TO_SHOW,
                "4"));
        return new ModuleCategory("Recently Used", dao.getRecentlyRunTasksForUser(getUserId(), recentJobsToShow));
    }

    /**
     * @return
     */
    public ModuleCategory getAllTasks() {
        AdminDAO dao = new AdminDAO();
        return new ModuleCategory("All", dao.getAllTasksForUser(getUserId()));
    }

    /**
     * @return
     */
    public List<ModuleCategory> getTasksByType() {

        List<ModuleCategory> categories = new ArrayList<ModuleCategory>();
        Map<String, List<TaskInfo>> taskMap = new HashMap<String, List<TaskInfo>>();

        for (int i = 0; i < allTasks.length; i++) {
            TaskInfo ti = allTasks[i];
            String taskType = ti.getTaskInfoAttributes().get("taskType");
            if (taskType == null || taskType.length() == 0) {
                taskType = "Uncategorized";
            }
            List<TaskInfo> tasks = taskMap.get(taskType);
            if (tasks == null) {
                tasks = new ArrayList<TaskInfo>();
                taskMap.put(taskType, tasks);
            }
            tasks.add(ti);
        }

        List<String> categoryNames = new ArrayList<String>(taskMap.keySet());
        Collections.sort(categoryNames);
        for (String categoryName : categoryNames) {
            TaskInfo[] modules = new TaskInfo[taskMap.get(categoryName).size()];
            modules = taskMap.get(categoryName).toArray(modules);
            categories.add(new ModuleCategory(categoryName, modules));
        }
        return categories;
    }

    public List<ModuleCategory> getSelectedTasksByType(String [] selectedLsids) {

        List<ModuleCategory> categories = new ArrayList<ModuleCategory>();
        Map<String, List<TaskInfo>> taskMap = new HashMap<String, List<TaskInfo>>();

        for (int i = 0; i < allTasks.length; i++) {
            TaskInfo ti = allTasks[i];
            String taskType = ti.getTaskInfoAttributes().get("taskType");
            if (taskType == null || taskType.length() == 0) {
                taskType = "Uncategorized";
            }
            List<TaskInfo> tasks = taskMap.get(taskType);
            if (tasks == null) {
                tasks = new ArrayList<TaskInfo>();
                taskMap.put(taskType, tasks);
            }
            tasks.add(ti);
        }

        List<String> categoryNames = new ArrayList<String>(taskMap.keySet());
        Collections.sort(categoryNames);
        ModuleCategory mc;
        for (String categoryName : categoryNames) {
            TaskInfo[] modules = new TaskInfo[taskMap.get(categoryName).size()];
            modules = taskMap.get(categoryName).toArray(modules);

            mc = new ModuleCategory(categoryName, modules);
            mc.setSelected(Arrays.asList(selectedLsids));

            categories.add(mc);
        }
        return categories;
    }

    /**
     * Return a list of tasks categorized by suite.
     * 
     * @return
     */
    public List<ModuleCategory> getTasksBySuite() {

        AdminDAO dao = new AdminDAO();

        Map<String, TaskInfo> taskMap = new HashMap<String, TaskInfo>();
        for (int i = 0; i < allTasks.length; i++) {
            try {
                LSID lsidObj = new LSID(allTasks[i].getLsid());
                taskMap.put(lsidObj.toStringNoVersion(), allTasks[i]);
            } catch (MalformedURLException e) {
                log.error("Error parsing lsid: " + allTasks[i].getLsid(), e);
            }
        }

        List<Suite> suites = (new SuiteDAO()).findAll();
        List<ModuleCategory> categories = new ArrayList<ModuleCategory>(suites.size());
        for (Suite suite : suites) {
            List<String> lsids = suite.getModules();
            List<TaskInfo> suiteTasks = new ArrayList<TaskInfo>();
            for (String lsid : lsids) {
                try {
                    LSID lsidObj = new LSID(lsid);
                    TaskInfo ti = taskMap.get(lsidObj.toStringNoVersion());
                    if (ti != null) {
                        suiteTasks.add(ti);
                    }
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    log.error("Error parsing lsid: " + lsid, e);
                }
            }
            TaskInfo[] taskArray = new TaskInfo[suiteTasks.size()];
            suiteTasks.toArray(taskArray);
            categories.add(new ModuleCategory(suite.getName(), taskArray));

        }
        return categories;

    }

    /**
     * @param suite
     * @return
     */
    public List<ModuleCategory> getTasksByTypeForSuite(Suite suite) {
        List<ModuleCategory> categories = new ArrayList<ModuleCategory>();
        List<String> lsids = suite.getModules();
        Map<String, Map<String, TaskInfo>> taskMap = new HashMap<String, Map<String, TaskInfo>>();

        Map<String, TaskInfo> queriedTasks = (new AdminDAO()).getAllTasksForUserWithLsids(getUserId(), lsids);
        for (Map.Entry entry : queriedTasks.entrySet()) {
            // TaskInfo ti = (TaskInfo)entry.getValue();
            addTaskInfoToMap(entry, taskMap);
        }

        List<String> categoryNames = new ArrayList<String>(taskMap.keySet());
        Collections.sort(categoryNames);
        ModuleCategory mc;
        Map<String, TaskInfo> lsidToTaskInfoMap;
        for (String categoryName : categoryNames) {
            TaskInfo[] modules = new TaskInfo[taskMap.get(categoryName).size()];
            lsidToTaskInfoMap = taskMap.get(categoryName);

            modules = taskMap.get(categoryName).values().toArray(modules);
            mc = new ModuleCategory(categoryName, modules);
            mc.setSelectedVersionOfModules(lsidToTaskInfoMap);

            categories.add(mc);
        }
        return categories;
    }

    /**
     * @param ti
     * @param taskMap
     */
    private void addTaskInfoToMap(Map.Entry entry, Map<String, Map<String, TaskInfo>> taskMap) {
        TaskInfo ti = (TaskInfo) entry.getValue();
        String taskType = ti.getTaskInfoAttributes().get("taskType");
        if (taskType == null || taskType.length() == 0) {
            taskType = "Uncategorized";
        }
        Map<String, TaskInfo> tasks = taskMap.get(taskType);
        if (tasks == null) {
            tasks = new HashMap<String, TaskInfo>();
            taskMap.put(taskType, tasks);
        }
        tasks.put((String) entry.getKey(), (TaskInfo) entry.getValue());
    }

}
