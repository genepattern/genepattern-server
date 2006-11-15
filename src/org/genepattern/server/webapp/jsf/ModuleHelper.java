package org.genepattern.server.webapp.jsf;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteHome;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import static org.genepattern.server.webapp.jsf.UIBeanHelper.getUserId;

public class ModuleHelper {
    private static Logger log = Logger.getLogger(ModuleHelper.class);

    private TaskInfo[] allTasks;

    public ModuleHelper() {
        allTasks = (new AdminDAO()).getAllTasksForUser(getUserId());
    }

    public ModuleCategory getRecentlyUsed() {
        AdminDAO dao = new AdminDAO();
        int recentJobsToShow = Integer.parseInt(UserPrefsBean.getProp(UserPropKey.RECENT_JOBS_TO_SHOW, "4").getValue());
        return new ModuleCategory("Recently Used", dao.getRecentlyRunTasksForUser(getUserId(), recentJobsToShow));
    }

    public ModuleCategory getAllTasks() {
        AdminDAO dao = new AdminDAO();
        return new ModuleCategory("All", dao.getAllTasksForUser(getUserId()));
    }

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

        List<String> categoryNames = new ArrayList(taskMap.keySet());
        Collections.sort(categoryNames);
        for (String categoryName : categoryNames) {
            TaskInfo[] modules = new TaskInfo[taskMap.get(categoryName).size()];
            modules = taskMap.get(categoryName).toArray(modules);
            categories.add(new ModuleCategory(categoryName, modules));
        }
        return categories;
    }

    public List<ModuleCategory> getTasksBySuite() {

        AdminDAO dao = new AdminDAO();
        HashMap<String, TaskInfo> taskMap = new HashMap();
        for (int i = 0; i < allTasks.length; i++) {
            try {
                LSID lsidObj = new LSID(allTasks[i].getLsid());
                taskMap.put(lsidObj.toStringNoVersion(), allTasks[i]);
            }
            catch (MalformedURLException e) {
                log.error("Error parsing lsid: ", e);
            }
        }
        ;

        List<Suite> suites = (new SuiteHome()).findAll();
        List<ModuleCategory> categories = new ArrayList(suites.size());
        for (Suite suite : suites) {
            List<String> lsids = suite.getModules();
            List<TaskInfo> suiteTasks = new ArrayList<TaskInfo>();
            for (String lsid : lsids) {
                TaskInfo ti = taskMap.get(lsid);
                if (ti != null) {
                    suiteTasks.add(ti);
                }
            }
            TaskInfo[] taskArray = new TaskInfo[suiteTasks.size()];
            suiteTasks.toArray(taskArray);
            categories.add(new ModuleCategory(suite.getName(), taskArray));

        }
        return categories;

    }

}
