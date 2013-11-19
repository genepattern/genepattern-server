package org.genepattern.server.domain;

public class PinModule {
    int id;
    String user;
    String lsid;
    double position;
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public String getLsid() {
        return lsid;
    }
    public void setLsid(String lsid) {
        this.lsid = lsid;
    }
    public double getPosition() {
        return position;
    }
    public void setPosition(double position) {
        this.position = position;
    }
    
}
