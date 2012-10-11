package org.genepattern.server.eula;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    
    /**
     * Get a File object, from the name of a file which is in the same directory as this source file.
     * @param filename
     * @return
     */
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
        //the name of a zip file, relative to this source file
        File zipfile = getSourceFile(filename);
        return initTaskInfoFromZipfile(zipfile);
    }

    private static TaskInfo initTaskInfoFromZipfile(final File zipfile) {
        TaskInfo taskInfo = null;
        try {
            taskInfo = TaskUtil.getTaskInfoFromZip(zipfile);
        }
        catch (IOException e) {
            Assert.fail("Error getting taskInfo from zipFile="+zipfile+". Error: "+e.getLocalizedMessage());
        }
        catch (Throwable t) {
            Assert.fail("Error getting taskInfo from zipFile="+zipfile+". Error: "+t.getLocalizedMessage());
        }
        return taskInfo;
    }
    
    private static List<TaskInfo> initTaskInfosFromZip(final String filename) {
        List<TaskInfo> taskInfos = new ArrayList<TaskInfo>();
        //the name of a zip file, relative to this source file
        File zipFile = getSourceFile(filename);
        
        TaskInfo taskInfo = null;
        try {
            taskInfo = TaskUtil.getTaskInfoFromZip(zipFile);
            taskInfos.add(taskInfo);
            return taskInfos;
        }
        catch (Throwable t) {
            //must be a pipeline with modules
        }

        ZipWalker zipWalker = new ZipWalker(zipFile);
        try {
            zipWalker.walk();
        }
        catch (Exception e) {
            //TODO: Need to hold onto this, and fail the test after we attempt to clean up
        }
        List<File> nestedZips = zipWalker.getNestedZips();
        for(File nestedZip : nestedZips) {
            TaskInfo nestedTask=null;
            try {
                nestedTask = TaskUtil.getTaskInfoFromZip(nestedZip);
                taskInfos.add(nestedTask);
            }
            catch (Throwable t) {
                //ignore zip entries
            }
            finally {
                //delete the tmp file
            }
        }
        try {
            zipWalker.deleteTmpFiles();
        }
        catch (Exception e) {
            Assert.fail(e.getLocalizedMessage());
        }
        return taskInfos;
    }

    /**
     * Make sure we can initialize a TaskInfo from the zip file for a module.
     * The zip file is created by exporting the module from the GP server.
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
     * Make sure we can initialize a list of TaskInfo from the zip file for a pipeline.
     * The zip file is created by exported the pipeline from the GP server, including modules.
     */
    @Test
    public void testGetPipelineTaskInfosFromZip() {
        final String filename="testPipelineWithLicensedModules_Dupe_ModuleName_v3.zip";
        List<TaskInfo> taskInfos = initTaskInfosFromZip(filename); 
        Assert.assertNotNull("taskInfos==null", taskInfos);
        Assert.assertEquals("expected number of taskInfos nested in zip file", 8, taskInfos.size());
        
        //this first taskInfo should be the exported pipeline
        TaskInfo pipeline=taskInfos.get(0);
        Assert.assertEquals("pipeline.name", "testPipelineWithLicensedModules_Dupe_ModuleName", pipeline.getName());
        Assert.assertEquals("pipeline.lsid", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:822:3", pipeline.getLsid());
        Assert.assertTrue("pipeline.isPipeline", pipeline.isPipeline());
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
        List<EulaInfo> eulas=EulaManager.instance().getAllEulaForModule(taskContext);
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

    /**
     * Theoretically possible, so make sure we know how to get, display and sort EulaInfo
     * when there are more than one license file for a single module.
     */
    @Test
    public void testMulipleEulaFromModule() {
        final String filename="testLicenseAgreement_v3.zip";
        final File gpLicense=getSourceFile("gp_server_license.txt");
        final File exampleLicense=getSourceFile("example_license.txt");
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        stub.addLicenseFile(taskInfo, gpLicense);
        stub.addLicenseFile(taskInfo, exampleLicense);
        EulaManager.instance().setGetEulaFromTask(stub);
        
        final String userId="gp_user";
        Context taskContext=Context.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        List<EulaInfo> eulas=EulaManager.instance().getAllEulaForModule(taskContext);
        Assert.assertNotNull("eulas==null", eulas);
        Assert.assertEquals("eulas.size", 2, eulas.size());
        
        String exampleLicenseContent=EulaInfo.fileToString(exampleLicense); 
        String gpLicenseContent=EulaInfo.fileToString(gpLicense); 
        assertEulaInfo(eulas, 0, "testLicenseAgreement", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3",   "3", exampleLicenseContent);
        assertEulaInfo(eulas, 1, "testLicenseAgreement", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3",   "3", gpLicenseContent);
    }

    /**
     * Make sure we can get the list of EulaInfo from a pipeline which,
     *     a) has a EULA, and
     *     b) contains modules which have a EULA, and
     *     c) contains some modules which have multiple EULA
     */
    @Test
    public void testGetEulaFromPipeline() {
        final String filename="testPipelineWithLicensedModules_Dupe_ModuleName_v3.zip";
        final List<TaskInfo> taskInfos = initTaskInfosFromZip(filename); 
        final TaskInfo pipelineTask=taskInfos.get(0);
        final File gpLicenseFile=getSourceFile("gp_server_license.txt");
        final File exampleLicenseFile=getSourceFile("example_license.txt");
        
        //hand-code license agreements, instead of extracting from the zip file
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        for(TaskInfo taskInfo : taskInfos) {
            stub.addLicenseFile(taskInfo, gpLicenseFile);
        }
        //add an extra license file to the first taskInfo
        stub.addLicenseFile(taskInfos.get(0), exampleLicenseFile);
        //add an extra license file to the last taskInfo
        //stub.addLicenseFile(taskInfos.get( taskInfos.size()-1 ), gpLicenseFile);
        EulaManager.instance().setGetEulaFromTask(stub);
        GetTaskStrategy myTaskInfoCache = new GetTaskStrategy() {
            //@Override
            public TaskInfo getTaskInfo(final String lsid) {
                //quick and dirty implementation
                if (lsid==null) {
                    return null;
                }
                for(TaskInfo taskInfo : taskInfos) {
                    if (lsid.equals(taskInfo.getLsid())) {
                        return taskInfo;
                    }
                }
                return null;
            }
        };
        EulaManager.instance().setGetTaskStrategy(myTaskInfoCache);
        
        TaskInfo lastEulaTask = myTaskInfoCache.getTaskInfo("urn:lsid:9090.gpdev.gpint01:genepatternmodules:816:1");
        stub.addLicenseFile(lastEulaTask, exampleLicenseFile);

        final String userId="gp_user";
        Context taskContext=Context.getContextForUser(userId);
        taskContext.setTaskInfo(pipelineTask);
        List<EulaInfo> eulas=EulaManager.instance().getAllEulaForModule(taskContext);
        Assert.assertNotNull("eulas==null", eulas);
        Assert.assertEquals("eulas.size", 10, eulas.size());
        
        //validate the sort order
        String exampleLicenseContent=EulaInfo.fileToString(exampleLicenseFile); 
        String gpLicenseContent=EulaInfo.fileToString(gpLicenseFile); 
        int i=0;
        assertEulaInfo(eulas, i++, "testPipelineWithLicensedModules_Dupe_ModuleName", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:822:3",   "3", exampleLicenseContent);
        assertEulaInfo(eulas, i++, "testPipelineWithLicensedModules_Dupe_ModuleName", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:822:3",   "3", gpLicenseContent);
        assertEulaInfo(eulas, i++, "testLicenseModuleA", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:815:5",   "5", gpLicenseContent);
        assertEulaInfo(eulas, i++, "testLicenseModuleA", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:815:3",   "3", gpLicenseContent);
        assertEulaInfo(eulas, i++, "testLicenseModuleA_renamed", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:815:4",   "4", gpLicenseContent);
        assertEulaInfo(eulas, i++, "testLicenseModuleB", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:816:12", "12", gpLicenseContent);
        assertEulaInfo(eulas, i++, "testLicenseModuleB", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:816:10", "10", gpLicenseContent);
        assertEulaInfo(eulas, i++, "testLicenseModuleB", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:816:9",   "9", gpLicenseContent);
        assertEulaInfo(eulas, i++, "testLicenseModuleB", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:816:1",   "1", exampleLicenseContent);
        assertEulaInfo(eulas, i++, "testLicenseModuleB", 
                "urn:lsid:9090.gpdev.gpint01:genepatternmodules:816:1",   "1", gpLicenseContent);
    }
    
    /**
     * Helper method for comparing the ith element from a list of EulaInfo, with expected values, provided as arguments to this method.
     * 
     * @param eulas, the List of EulaInfo returned by the EulaManager method call
     * @param i, the index into the list with the expected values
     * @param expectedName, the expected module name
     * @param expectedLsid, the expected lsid
     * @param expectedVersion, the expected version (e.g. "3")
     * @param expectedContent, the expected content of the license file, as a String
     */
    static private void assertEulaInfo(List<EulaInfo> eulas, final int i, final String expectedName, final String expectedLsid, final String expectedVersion, final String expectedContent) {
        final EulaInfo eula=eulas.get(i);
        final String pre="eula["+i+"].";
        assertEulaInfo(pre, eula, expectedName, expectedLsid, expectedVersion, expectedContent);
    }
    static private void assertEulaInfo(final String pre, final EulaInfo eula, final String expectedName, final String expectedLsid, final String expectedVersion, final String expectedContent) {
        Assert.assertEquals(pre+"moduleName", expectedName, eula.getModuleName());
        Assert.assertEquals(pre+"moduleLsid", expectedLsid, eula.getModuleLsid());
        Assert.assertEquals(pre+"moduleLsidVersion", expectedVersion, eula.getModuleLsidVersion());
        try {
            Assert.assertEquals(pre+"content", expectedContent, eula.getContent());
        }
        catch (EulaInfo.EulaInitException e) {
            Assert.fail(pre+"conent, error getting content: "+e.getLocalizedMessage());
        }
    }
}
