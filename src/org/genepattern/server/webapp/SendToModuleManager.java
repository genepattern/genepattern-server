/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.webservice.TaskInfo;

/**
 * Created by tabor on 2/25/14.
 */
public class SendToModuleManager {
    private static SendToModuleManager singleton = null;

    private Map<String, KindToTaskInfo> userToKindMap = new HashMap<String, KindToTaskInfo>();

    public static SendToModuleManager instance() {
        if (singleton == null) {
            init();
        }
        return singleton;
    }

    private static void init() {
        singleton = new SendToModuleManager();
    }

    public KindToTaskInfo getKindToTaskInfo(String user) {
        // Lazily clear the sendTo cache is needed
        if (needsUpdate(user)) {
            userToKindMap.put(user, null);
        }

        if (userToKindMap.get(user) == null) {
            KindToTaskInfo ktti = new KindToTaskInfo(user);
            userToKindMap.put(user, ktti);
        }

        return userToKindMap.get(user);
    }

    public SortedSet<TaskInfo> getSendTo(String user, String kind) {
        return getKindToTaskInfo(user).get(kind);
    }

    private boolean needsUpdate(String user) {
        KindToTaskInfo ktti = userToKindMap.get(user);

        // If it hasn't been updated then there is nothing to update
        if (ktti == null) return false;

        Date now = new Date();
        Date updated = ktti.updated();
        if (updated == null) return true;

        if (updated.before(new Date(now.getTime() - 10000 ))) return true;
        else return false;
    }

    private class KindToTaskInfo {
        private Map<String,SortedSet<TaskInfo>> kindToTaskInfo;
        private Date updated;

        public KindToTaskInfo(String user) {
            kindToTaskInfo = new HashMap<String, SortedSet<TaskInfo>>();

            AdminDAO adminDao = new AdminDAO();
            TaskInfo[] taskInfos = adminDao.getLatestTasks(user);
            for(TaskInfo taskInfo : taskInfos) {
                for(String kind : taskInfo._getInputFileTypes()) {
                    SortedSet<TaskInfo> taskInfosForMap = kindToTaskInfo.get(kind);
                    if (taskInfosForMap == null) {
                        taskInfosForMap = new TreeSet<TaskInfo>(taskInfoComparator);
                        kindToTaskInfo.put(kind, taskInfosForMap);
                    }
                    taskInfosForMap.add(taskInfo);
                }
            }

            updated = new Date();
        }

        public SortedSet<TaskInfo> get(String kind) {
            return kindToTaskInfo.get(kind);
        }

        public Date updated() {
            return updated;
        }
    }

    private static final Comparator<TaskInfo> taskInfoComparator =  new Comparator<TaskInfo>() {
        public int compare(TaskInfo o1, TaskInfo o2) {
            //1) null arg test
            if (o1 == null) {
                if (o2 == null) {
                    return 0;
                }
                return -1;
            }
            if (o2 == null) {
                return 1;
            }

            //2) null name test
            if (o1.getName() == null) {
                if (o2.getName() == null) {
                    return 0;
                }
                return -1;
            }

            return o1.getName().compareTo( o2.getName() );
        }
    };
}
