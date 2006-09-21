package org.genepattern.server.domain;

public class Lsid {

    private String lsid;
    private String lsidNoVersion;
    private String version;

    public String getLsid() {
        return lsid;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public String getLsidNoVersion() {
        return lsidNoVersion;
    }

    public void setLsidNoVersion(String lsidNoVersion) {
        this.lsidNoVersion = lsidNoVersion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String lversion) {
        this.version = lversion;
    }

}
