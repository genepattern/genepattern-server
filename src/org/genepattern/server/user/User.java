package org.genepattern.server.user;

import java.util.Date;
import java.util.List;

public class User {

    String userId;

    byte[] password;

    String email;

    Date lastLoginDate;

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
