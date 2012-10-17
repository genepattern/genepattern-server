package org.genepattern.server.eula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.eula.EulaInfo.EulaInitException;
import org.genepattern.webservice.TaskInfo;

/**
 * Rule for getting EulaInfo from a TaskInfo, if there is a 'license=' entry in the manifest then the module requires an EULA.
 *     E.g.
 *     license=license.txt
 *     
 * Just in case, allow for multiple licenses, separated by a ',' delimiter
 *     E.g.
 *     license=gp_license.txt,example_license.txt
 *     
 * @author pcarr
 */
public class GetEulaAsManifestProperty implements GetEulaFromTask {
    public static Logger log = Logger.getLogger(GetEulaAsManifestProperty.class);
    final static public String DELIM=",";

    //Override
    public List<EulaInfo> getEulasFromTask(TaskInfo taskInfo) {
        Object licenseObj = taskInfo.getAttributes().get("license");
        if (licenseObj == null) {
            return Collections.emptyList();
        }
        List<EulaInfo> eulas = new ArrayList<EulaInfo>();
        String licenseStr;
        if (licenseObj instanceof String) {
            licenseStr = (String) licenseObj;
        }
        else {
            licenseStr = licenseObj.toString();
        }

        List<String> licenseFilenames=getLicenseFilenames(licenseStr);
        for(String licenseFilename : licenseFilenames) {
            EulaInfo eula = new EulaInfo();
            try {
                eula.setModuleLsid(taskInfo.getLsid());
            }
            catch (EulaInitException e) {
                log.error("Invalid taskInfo.lsid="+taskInfo.getLsid(),e);
            }
            eula.setModuleName(taskInfo.getName());
            eula.setLicense(licenseFilename);
            eulas.add(eula);
        }


        return eulas;
    }
    
    private static List<String> getLicenseFilenames(String licenseVal) {
        List<String> list=new ArrayList<String>();
        String[] items=licenseVal.split(DELIM);
        for(String s : items) {
            list.add(s.trim());
        }
        return list;
    }

    //Override
    public void setEula(EulaInfo eula, TaskInfo taskInfo) {
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        
        //null EulaInfo means, this task has no attached EULA
        log.debug("eula is null, remove license property from manifest");
        taskInfo.giveTaskInfoAttributes().remove("license");
        
        List<EulaInfo> eulas=new ArrayList<EulaInfo>();
        eulas.add(eula);
        setEulas(eulas,taskInfo);
    }

    //Override
    public void setEulas(List<EulaInfo> eulas, TaskInfo taskInfo) {
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        
        String licenseVal="";
        int i=0;
        for(EulaInfo eula : eulas) {
            ++i;
            if (i>1) {
                licenseVal+=DELIM;
            }
            String s=eula.getLicenseFile().getName();
            licenseVal+=s;
        }
        taskInfo.giveTaskInfoAttributes().put("license", licenseVal); 
    }

}
