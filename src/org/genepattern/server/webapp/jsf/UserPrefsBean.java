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

import java.util.List;

import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.user.UserPropKey;

public class UserPrefsBean {
    private UserProp javaFlagsProp;
    private UserProp recentJobsProp;

    public UserPrefsBean() {     
        javaFlagsProp = getProp(UserPropKey.VISUALIZER_JAVA_FLAGS, System.getProperty("visualizer_java_flags"));
        recentJobsProp = getProp(UserPropKey.RECENT_JOBS_TO_SHOW, "4");
    }

    public static UserProp getProp(String name, String defaultValue) {
        User user = (new UserDAO()).findById(UIBeanHelper.getUserId());
        assert user != null;
        List<UserProp> props = user.getProps();
        UserProp userProp = null;
        for (UserProp p : props) {
            if (name.equals(p.getKey())) {
                userProp = p;
                break;
            }
        }

        if (userProp == null) {
            userProp = new UserProp();
            userProp.setGpUserId(UIBeanHelper.getUserId());
            userProp.setKey(name);
            userProp.setValue(defaultValue);
            props.add(userProp);
        }
        return userProp;
    }

    public String save() {
        UIBeanHelper.setInfoMessage("Property successfully updated");
        return "my settings";
    }

    public UserProp getNumberOfRecentJobs() {
        return recentJobsProp;
    }

    public void setNumberOfRecentJobs(UserProp p) {
        recentJobsProp = p;
    }

    public UserProp getJavaFlags() {
        return javaFlagsProp;
    }

    public void setJavaFlags(UserProp javaFlagsProp) {
        this.javaFlagsProp = javaFlagsProp;
    }
}
