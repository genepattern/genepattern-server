/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.user;

import java.util.Date;
import java.util.List;

public class User {

    String userId;

    byte[] password;

    String email;

    Date lastLoginDate;

    Date registrationDate;

    String lastLoginIP;

    int totalLoginCount;

    List<UserProp> props;


    public List<UserProp> getProps() {
        return props;
    }

    public void setProps(List<UserProp> props) {
        this.props = props;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] pw) {
        password = pw;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String username) {
        this.userId = username;
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

    public void incrementLoginCount() {
        totalLoginCount++;
    }

}
