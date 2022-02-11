/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.taskinstall;

public interface RecordInstallInfo {
    void save(InstallInfo taskInstallInfo) throws Exception;
    int delete(String lsid) throws Exception;

}
