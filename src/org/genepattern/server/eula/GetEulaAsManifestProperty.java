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
 * @author pcarr
 */
public class GetEulaAsManifestProperty implements GetEulaFromTask {
    public static Logger log = Logger.getLogger(GetEulaAsManifestProperty.class);

    //Override
    public List<EulaInfo> getEulasFromTask(TaskInfo taskInfo) {
        Object licenseObj = taskInfo.getAttributes().get("license");
        if (licenseObj != null) {
            String licenseStr;
            if (licenseObj instanceof String) {
                licenseStr = (String) licenseObj;
            }
            else {
                licenseStr = licenseObj.toString();
            }
            EulaInfo eula = new EulaInfo();
            try {
                eula.setModuleLsid(taskInfo.getLsid());
            }
            catch (EulaInitException e) {
                log.error("Invalid taskInfo.lsid="+taskInfo.getLsid(),e);
            }
            eula.setModuleName(taskInfo.getName());
            eula.setLicense(licenseStr);
            
            List<EulaInfo> eulas = new ArrayList<EulaInfo>();
            eulas.add(eula);
            return eulas;
        }
        return Collections.emptyList();
    }

}
