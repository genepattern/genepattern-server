package org.genepattern.server.auth;

import java.io.File;

public class GroupManagerFactory {
    private static IGroupManagerPlugin groupManager = null;
    public static IGroupManagerPlugin getGroupManager() {
        if (groupManager == null) {
            File userGroupMapFile = new File(System.getProperty("genepattern.properties"), "userGroups.xml");
            groupManager = new XmlGroupManager(userGroupMapFile);
        }
        return groupManager;
    }
}


