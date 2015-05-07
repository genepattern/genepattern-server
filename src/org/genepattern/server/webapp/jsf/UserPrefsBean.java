/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.util.PropertiesManager_3_2;
import org.genepattern.visualizer.RunVisualizerConstants;

public class UserPrefsBean {
    private static Logger log = Logger.getLogger(UserPrefsBean.class);
    private String userId;
    private UserProp javaFlagsProp;
    private UserProp recentJobsProp;
    private List<String> groups;


    public UserPrefsBean() {
        this.userId = UIBeanHelper.getUserId();
        UserDAO dao = new UserDAO();
        
        GpContext userContext = GpContext.getContextForUser(userId);
        String systemVisualizerJavaFlags = ServerConfigurationFactory.instance().getGPProperty(userContext, RunVisualizerConstants.JAVA_FLAGS_VALUE);
        javaFlagsProp = dao.getProperty(userId, UserPropKey.VISUALIZER_JAVA_FLAGS, systemVisualizerJavaFlags);

        String historySize = null;
        try {
            historySize = (String) PropertiesManager_3_2.getDefaultProperties().get("historySize");
        } 
        catch (IOException e) {
            log.error("Unable to retrive historySize property", e);
        }
        recentJobsProp = dao.getProperty(userId, UserPropKey.RECENT_JOBS_TO_SHOW, (historySize == null) ? "10" : historySize);
        
        Set<String> groupSet = UserAccountManager.instance().getGroupMembership().getGroups(userId);
        groups = new ArrayList<String>(groupSet);
        //TODO: sort the groups by name
    }

    public String saveJavaFlags() {
        UIBeanHelper.setInfoMessage("Visualizer memory successfully updated.");
        return "my settings";
    }

    public String saveRecentJobs() {
        UIBeanHelper.setInfoMessage("Number of recent jobs successfully updated.");
        return "my settings";
    }

    public int getNumberOfRecentJobs() {
        if (recentJobsProp == null) {
            return 0;
        }
        return Integer.parseInt(recentJobsProp.getValue());
    }

    public void setNumberOfRecentJobs(int value) {
        if (recentJobsProp == null) {
            return;
        }
        recentJobsProp.setValue(String.valueOf(value));
    }

    public String getJavaFlags() {
        if (javaFlagsProp == null) {
            return "";
        }
        return javaFlagsProp.getValue();
    }

    public void setJavaFlags(String value) {
        if (javaFlagsProp == null) {
            return;
        }
        this.javaFlagsProp.setValue(value);
    }
    
    //add support for groups
    public List<String> getGroups() {
        return groups;
    }
    
    public List<SelectItem> getGroupSelectItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        //items.add(new SelectItem("[0] all", "all"));
        //items.add(new SelectItem("[1] me", "owned by: "+userId));
        items.add(new SelectItem("", "Private"));
        for(String groupName : groups) {
            items.add(new SelectItem(groupName+"RW", groupName+" (View and edit)"));
            items.add(new SelectItem(groupName+"R", groupName+" (View)"));
        }
        return items;
    }
}
