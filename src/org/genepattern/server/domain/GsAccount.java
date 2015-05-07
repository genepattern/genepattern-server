/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.domain;

import java.util.Date;

public class GsAccount {
    String gpUserId = null;
    String gsUserId = null;
    String token = null;
    Date tokenTimestamp = null;
    String email = null;
    
    public String getGpUserId() {
        return gpUserId;
    }
    
    public void setGpUserId(String gp_userid) {
        this.gpUserId = gp_userid;
    }
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }

    public String getGsUserId() {
        return gsUserId;
    }

    public void setGsUserId(String gsUserId) {
        this.gsUserId = gsUserId;
    }

    public Date getTokenTimestamp() {
        return tokenTimestamp;
    }

    public void setTokenTimestamp(Date tokenTimestamp) {
        this.tokenTimestamp = tokenTimestamp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
