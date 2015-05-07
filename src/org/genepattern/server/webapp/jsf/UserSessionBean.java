/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import org.apache.log4j.Logger;

/**
 * Managed bean for the currently logged in user.
 * 
 * Warning: this only works because this session bean is only referenced from pages which require login.
 *     Otherwise, the userId would always be null and isAdmin would always be false.
 * 
 * @author pcarr
 */
public class UserSessionBean {
    private static Logger log = Logger.getLogger(UserSessionBean.class);

    private String userId = null;
    private Boolean isAdmin = null;
    
    public UserSessionBean() {
        log.info("intializing userSessionBean");
        log.info("for user: "+getUserId());
        log.info("isAdmin: "+isAdmin());
    }

    public String getUserId() {
        if (userId == null) {
            userId = UIBeanHelper.getUserId();
        }
        return userId;
    }

    private void initIsAdmin() {
        String uid = getUserId();
        if (uid == null) {
            this.isAdmin = false;
        }
        else {
            this.isAdmin = AuthorizationHelper.adminJobs(getUserId());
        }
    }
    public boolean isAdmin() {
        if (this.isAdmin == null) {
            initIsAdmin();
        }
        return this.isAdmin;
    }
}
