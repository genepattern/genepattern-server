package org.genepattern.server.eula;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.TaskUtil;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Test;

public class TestEulaManager {
    final static public Logger log = Logger.getLogger(TestEulaManager.class);

    /**
     * Helper class which returns the parent File of this source file.
     * @return
     */
    private static File getSourceDir() {
        String cname = TestEulaManager.class.getCanonicalName();
        int idx = cname.lastIndexOf('.');
        String dname = cname.substring(0, idx);
        dname = dname.replace('.', '/');
        File sourceDir = new File("test/src/" + dname);
        return sourceDir;
    }
    
    private static File getSourceFile(String filename) {
        File p = getSourceDir();
        return new File(p, filename);
    }

    /**
     * Initialize a TaskInfo from a zip file.
     * 
     * TODO: Implement this for pipelines which include modules.
     * 
     * @param filename
     * @return
     */
    private static TaskInfo initTaskInfoFromZip(final String filename) {
        TaskInfo taskInfo = null;
        //the name of a zip file, relative to this source file
        File zipFile = getSourceFile(filename);
        try {
            taskInfo = TaskUtil.getTaskInfoFromZip(zipFile);
        }
        catch (IOException e) {
            Assert.fail("Error getting taskInfo from zipFile="+zipFile+". Error: "+e.getLocalizedMessage());
        }
        catch (Throwable t) {
            Assert.fail("Error getting taskInfo from zipFile="+zipFile+". Error: "+t.getLocalizedMessage());
        }
        return taskInfo;
        
//        try {
//            String zipFilename = zipFile.getPath();
//            Properties props = GenePatternAnalysisTask.getPropsFromZipFile(zipFilename);
//            for(Entry<?,?> entry : props.entrySet()) {
//                String key = (String) entry.getKey();
//                String val = (String) entry.getValue();
//                String m = key;
//                String n = val;
//            }
//            
//        }
//        catch (IOException e) {
//            Assert.fail("Error loading zip file for task"+e.getLocalizedMessage());
//        }
    }

    /**
     * Test case: make sure we can initialize a TaskInfo from the zip file for a module.
     * The zip file is created by exported the module from the GP server.
     */
    @Test
    public void testGetTaskInfoFromZip() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        Assert.assertNotNull("taskInfo==null", taskInfo);
        Assert.assertEquals("taskName", "testLicenseAgreement", taskInfo.getName());
        Assert.assertEquals("taskLsid", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3", taskInfo.getLsid());
    }
    
    /**
     * Test case: make sure we can get the list of EulaInfo from a single module,
     * which requires an EULA.
     */
    @Test
    public void testGetEulaFromModule() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        Assert.assertNotNull("taskInfo==null", taskInfo);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        stub.addLicenseFile(taskInfo, licenseFile);
        EulaManager.instance().setGetEulaFromTask(stub);

        final String userId="gp_user";
        Context taskContext=Context.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        List<EulaInfo> eulas=EulaManager.instance().getAllEULAForModule(taskContext);
        Assert.assertNotNull("eulas==null", eulas);
        Assert.assertEquals("Expecting one EulaInfo", 1, eulas.size());
        EulaInfo eula = eulas.get(0);
        Assert.assertEquals("eula.moduleName", "testLicenseAgreement", eula.getModuleName());
        Assert.assertEquals("eula.moduleLsid", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3", eula.getModuleLsid());
        Assert.assertEquals("eula.moduleLsidVersion", "3", eula.getModuleLsidVersion());

        final String expectedContent=EulaInfo.fileToString(licenseFile);
        try {
            Assert.assertEquals("eula.content", expectedContent, eula.getContent());
        }
        catch (EulaInfo.EulaInitException e) {
            Assert.fail("EulaInfo.EulaInitException thrown in eula.getContent(): "+e.getLocalizedMessage());
        }
    }
}
