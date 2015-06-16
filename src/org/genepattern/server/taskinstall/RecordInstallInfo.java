/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.taskinstall;

public interface RecordInstallInfo {
    void save(InstallInfo taskInstallInfo) throws Exception;
    int delete(String lsid) throws Exception;

}
