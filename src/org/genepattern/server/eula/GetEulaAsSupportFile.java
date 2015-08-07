/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.eula.InitException;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.webservice.TaskInfo;

/**
 * Rule for getting EulaInfo from a TaskInfo, if there is a support file whose name matches the pattern '*license*',
 * then the module requires a EULA.
 * 
 * @author pcarr
 */
public class GetEulaAsSupportFile implements GetEulaFromTask {
    final static private Logger log = Logger.getLogger(GetEulaAsSupportFile.class);

    //Override
    public List<EulaInfo> getEulasFromTask(final TaskInfo taskInfo) {
        String moduleLsid = taskInfo.getLsid();
        List<File> licenseFiles = getTaskLicenses(moduleLsid);
        List<EulaInfo> eulas = new ArrayList<EulaInfo>();
        for(File licenseFile : licenseFiles) {
            EulaInfo eula = initEulaInfo(taskInfo, licenseFile);
            eulas.add(eula);
        }
        return eulas;
    }

    private EulaInfo initEulaInfo(final TaskInfo taskInfo, final File licenseFile) {
        EulaInfo eula = new EulaInfo();
        try {
            eula.setModuleLsid(taskInfo.getLsid());
        }
        catch (InitException e) {
            log.error("Invalid taskInfo.lsid="+taskInfo.getLsid(),e);
        }
        eula.setModuleName(taskInfo.getName());
        //TODO: save abs path, so we don't need to do a new 'ls' to get the content
        eula.setLicense(licenseFile.getName());
        return eula;
    }

    private File getTasklibDir(final String moduleLsid) {
        //need path to tasklib
        File tasklibDir = null;
        //TODO: implement safer method, e.g. File tasklibDir = DirectoryManager.getTaskLibDirFromCache(moduleLsid);
        //    getLibDir automatically creates a directory on the file system; it's possible to cause problems if there is bogus input
        try {
            String path = DirectoryManager.getLibDir(moduleLsid);
            if (path != null) {
                tasklibDir = new File(path);
            }
        }
        catch (Throwable t) {
            log.error("Error getting libdir for moduleLsid="+moduleLsid+": "+t.getLocalizedMessage(), t);
        }
        return tasklibDir;
    }

    private List<File> getTaskLicenses(final String moduleLsid) {
        File tasklibDir=getTasklibDir(moduleLsid);
        File[] licenseFiles = tasklibDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().contains("license");
            }
        });
        if (licenseFiles==null || licenseFiles.length==0) {
            return Collections.emptyList();
        }

        List<File> rval = new ArrayList<File>();
        for(File licenseFile : licenseFiles) {
            rval.add(licenseFile);
        }
        return rval; 
    }

    //Override
    public void setEula(EulaInfo eula, TaskInfo taskInfo) {
        // ignore, it's up to the calling method to save the file into the correct location 
    }

    //Override
    public void setEulas(List<EulaInfo> eulas, TaskInfo taskInfo) {
        // ignore, it's up to the calling method to save the file into the correct location 
    }

}
