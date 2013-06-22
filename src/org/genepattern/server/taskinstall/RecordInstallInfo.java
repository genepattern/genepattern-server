package org.genepattern.server.taskinstall;

public interface RecordInstallInfo {
    void save(InstallInfo taskInstallInfo) throws Exception;
    int delete(String lsid) throws Exception;

}
