/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.user;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class User {
    String userId;
    byte[] password;
    String email;
    Date lastLoginDate;
    Date registrationDate;
    String lastLoginIP;
    int totalLoginCount;
    Set<UserProp> props = new HashSet<UserProp>();

    public String getUserId() {
        return userId;
    }

    public void setUserId(String username) {
        this.userId = username;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] pw) {
        password = pw;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public Date getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	public String getLastLoginIP() {
        return lastLoginIP;
    }

    public void setLastLoginIP(String lastLoginIP) {
        this.lastLoginIP = lastLoginIP;
    }

    public int getTotalLoginCount() {
        return totalLoginCount;
    }

    public void setTotalLoginCount(int totalLoginCount) {
        this.totalLoginCount = totalLoginCount;
    }

    public Set<UserProp> getProps() {
        return props;
    }

    public void setProps(Set<UserProp> props) {
        this.props = props;
    }
    
    public void incrementLoginCount() {
        totalLoginCount++;
    }
}
