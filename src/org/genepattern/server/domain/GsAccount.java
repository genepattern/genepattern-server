package org.genepattern.server.domain;

public class GsAccount {
    String gpUserid = null;
    String token = null;
    
    public String getGpUserid() {
        return gpUserid;
    }
    
    public void setGpUserid(String gp_userid) {
        this.gpUserid = gp_userid;
    }
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}
