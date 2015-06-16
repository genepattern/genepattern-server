/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.eula.InitException;
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
    final static private Logger log = Logger.getLogger(GetEulaAsManifestProperty.class);
    /** the name name of the property in the manifest file */
    final static public String LICENSE="license";
    /** delimiter for a list of license in the manifest */
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
            catch (InitException e) {
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
        for(String item : items) {
            //ignore empty string
            String licenseItem=item.trim();
            if (licenseItem.length()>0) {
                list.add(licenseItem);
            }
        }
        return list;
    }

    //Override
    public void setEula(EulaInfo eula, TaskInfo taskInfo) {
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        
        log.debug("eula is null, remove license property from manifest");
        List<EulaInfo> eulas=new ArrayList<EulaInfo>();
        if (eula != null) {
            eulas.add(eula);
        }
        setEulas(eulas,taskInfo);
    }

    //Override
    public void setEulas(List<EulaInfo> eulas, TaskInfo taskInfo) {
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        
        StringBuffer buf = new StringBuffer();
        int i=0;
        for(EulaInfo eula : eulas) {
            ++i;
            if (i>1) {
                buf.append(DELIM);
                //licenseVal+=DELIM;
            }
            String s=eula.getLicense();
            buf.append(s);
        }
        String licenseVal=buf.toString();
        taskInfo.giveTaskInfoAttributes().put("license", licenseVal); 
        
        //save to manifest file
        //save to DB
    }
}
