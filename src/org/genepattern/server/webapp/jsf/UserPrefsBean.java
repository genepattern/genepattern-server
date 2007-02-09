/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.util.PropertiesManager;

public class UserPrefsBean {
    private UserProp javaFlagsProp;

    private UserProp recentJobsProp;

    private static Logger log = Logger.getLogger(UserPrefsBean.class);

    public UserPrefsBean() {
        UserDAO dao = new UserDAO();
        String userId = UIBeanHelper.getUserId();
        javaFlagsProp = dao.getProperty(userId, UserPropKey.VISUALIZER_JAVA_FLAGS, System
                .getProperty("visualizer_java_flags"));

        String historySize = null;
        try {
            historySize = (String) PropertiesManager.getDefaultProperties().get("historySize");
        } catch (IOException e) {
            log.error("Unable to retrive historySize property", e);
        }
        recentJobsProp = dao.getProperty(userId, UserPropKey.RECENT_JOBS_TO_SHOW, (historySize == null) ? "10"
                : historySize);

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
        return Integer.parseInt(recentJobsProp.getValue());
    }

    public void setNumberOfRecentJobs(int value) {
        recentJobsProp.setValue(String.valueOf(value));
    }

    public String getJavaFlags() {
        return javaFlagsProp.getValue();
    }

    public void setJavaFlags(String value) {
        this.javaFlagsProp.setValue(value);
    }
}
