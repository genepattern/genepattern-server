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
import org.genepattern.server.user.UserHome;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.user.UserPropKey;

public class UserPrefsBean {
    private UserProp javaFlagsProp;

    public UserPrefsBean() {
        User user = (new UserHome()).findById(UIBeanHelper.getUserId());
        assert user != null;
        List<UserProp> props = user.getProps();

        for (UserProp p : props) {
            if (UserPropKey.VISUALIZER_JAVA_FLAGS.equals(p.getKey())) {
                javaFlagsProp = p;
                break;
            }
        }

        if (javaFlagsProp == null) {
            javaFlagsProp = new UserProp();
            javaFlagsProp.setKey(UserPropKey.VISUALIZER_JAVA_FLAGS);
            javaFlagsProp.setValue(System.getProperty("visualizer_java_flags"));
            props.add(javaFlagsProp);
        }
    }

    public String save() {
        UIBeanHelper.setInfoMessage("Property successfully updated");
        return "success";
    }

    public UserProp getJavaFlags() {
        return javaFlagsProp;
    }

    public void setJavaFlags(UserProp javaFlagsProp) {
        this.javaFlagsProp = javaFlagsProp;
    }
}
