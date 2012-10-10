package org.genepattern.server.eula;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.genepattern.webservice.TaskInfo;

/**
 * This helper class, for jUnit tests, implements GetEulaFromTask,
 * without making any DB queries or file system calls.
 * 
 * @author pcarr
 */
public class GetEulaFromTaskStub implements GetEulaFromTask {
    private Map<String, Set<EulaInfo>> lookupTable;
    
    public void addLicenseFile(final TaskInfo taskInfo, final File license) { 
        if (taskInfo == null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        final String moduleLsid = taskInfo.getLsid();
        if (moduleLsid == null || moduleLsid.length()==0) {
            throw new IllegalArgumentException("taskInfo.lsid not set");
        }
        //lazy-init
        if (lookupTable == null) {
            lookupTable = new HashMap<String, Set<EulaInfo>>();
        }
        Set<EulaInfo> infos = lookupTable.get(moduleLsid);
        if (infos == null) {
            infos = new TreeSet<EulaInfo>();
            lookupTable.put(moduleLsid, infos);
        }
        EulaInfo eula = initEulaInfo(taskInfo, license);
        infos.add(eula);
    }

    public void addLicenseFiles(final TaskInfo taskInfo, final Set<File> licenses) {
        for(final File license : licenses) {
            addLicenseFile(taskInfo, license);
        }
    }
    
    private EulaInfo initEulaInfo(final TaskInfo taskInfo, final File licenseFile) {
        EulaInfo eula = new EulaInfo();
        eula.setModuleLsid(taskInfo.getLsid());
        eula.setModuleName(taskInfo.getName());
        eula.setLicenseFile(licenseFile);
        return eula;
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

}
