/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.genepattern.server.eula.InitException;
import org.genepattern.webservice.TaskInfo;

/**
 * This helper class, for jUnit tests, implements GetEulaFromTask,
 * without making any DB queries or file system calls.
 * 
 * @author pcarr
 */
public class GetEulaFromTaskStub implements GetEulaFromTask {
    private Map<String, Set<EulaInfo>> lookupTable;

    /**
     * Helper method for initializing a EulaInfo for the given taskInfo and licenseFile.
     * 
     * @param taskInfo, must be non-null and have a valid lsid
     * @param licenseFile, must be readable from the current working directory. This same exact path is used when reading the content.
     * @return
     * @throws EulaInitException
     */
    final static public EulaInfo initEulaInfo(final TaskInfo taskInfo, final File licenseFile) throws InitException {
        EulaInfo eula = new EulaInfo();
        eula.setModuleLsid(taskInfo.getLsid());
        eula.setModuleName(taskInfo.getName());
        eula.setLicenseFile(licenseFile);
        return eula;
    }

    //reset back to zero
    private void clearEulaInfo(final String moduleLsid) {
        if (lookupTable==null) {
            //ignore
            return;
        }
        lookupTable.remove(moduleLsid);
    }
    private void addEulaInfo(final String moduleLsid, final EulaInfo eula) {
        //lazy-init
        if (lookupTable == null) {
            lookupTable = new HashMap<String, Set<EulaInfo>>();
        }
        Set<EulaInfo> infos = lookupTable.get(moduleLsid);
        if (infos == null) {
            infos = new TreeSet<EulaInfo>();
            lookupTable.put(moduleLsid, infos);
        }
        infos.add(eula); 
    }
    
    public List<EulaInfo> getEulasFromTask(final TaskInfo taskInfo) {
        if (lookupTable==null) {
            return Collections.emptyList();
        }
        if (taskInfo==null) {
            //TODO: what to do with a null arg
            return Collections.emptyList(); 
        }
        final String moduleLsid=taskInfo.getLsid();
        if (moduleLsid==null) {
            //TODO: what to do when lsid is not set
            return Collections.emptyList(); 
        }
        Set<EulaInfo> eulas = lookupTable.get(moduleLsid);
        if (eulas==null) {
            return Collections.emptyList();
        }
        return new ArrayList<EulaInfo>(eulas);
    }

    //Override
    public void setEula(EulaInfo eula, TaskInfo taskInfo) throws IllegalArgumentException {
        List<EulaInfo> eulas=new ArrayList<EulaInfo>(); 
        if (eula!=null) {
            eulas.add(eula);
        }
        setEulas(eulas, taskInfo);
    }

    //Override
    public void setEulas(List<EulaInfo> eulas, TaskInfo taskInfo) throws IllegalArgumentException {
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        final String lsid=taskInfo.getLsid();
        if (lsid==null) {
            throw new IllegalArgumentException("taskInfo.lsid==null");
        }
        if (lsid.length()==0) {
            throw new IllegalArgumentException("taskInfo.lsid is not set");
        }
        clearEulaInfo(lsid);
        for(EulaInfo eula : eulas) {
            addEulaInfo(lsid, eula);
        } 
    }

}
