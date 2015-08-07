/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestEulaManagerImpl {
    final static public Logger log = Logger.getLogger(TestEulaManagerImpl.class);
    
    private EulaManagerImpl eulaMgr;
    private RecordEulaStub recordEulaStub;

    @Before
    public void setUp() {
        eulaMgr=new EulaManagerImpl();
        recordEulaStub=new RecordEulaStub();
    }

    /**
     * Get a File object, from the name of a file which is in the same directory as this source file.
     * 
     * @deprecated, use FileUtil#getSourceFile instead.
     * 
     * @param filename
     * @return
     */
    public static File getSourceFile(String filename) {
        return FileUtil.getSourceFile(TestEulaManagerImpl.class, filename);
    }

    /**
     * Initialize a TaskInfo from a zip file.
     * 
     * @param filename
     * @return
     * 
     * @deprecated - use {@link TaskUtil#getTaskInfoFromZip(File)} instead
     */
    private TaskInfo initTaskInfoFromZip(final String filename) {
        return TaskUtil.getTaskInfoFromZip(this.getClass(), filename);
    }

    /**
     * Helper method for initializing a EulaInfo to add to a task.
     * @param stub
     * @param taskInfo, must have a valid LSID
     * @param licenseFile, the path to this file must be valid when reading the content of the file.
     */
    private static void addLicenseFile(final GetEulaFromTaskStub stub, TaskInfo taskInfo, File licenseFile) {
        List<File> files=new ArrayList<File>();
        files.add(licenseFile);
        addLicenseFiles(stub, taskInfo, files);
    }

    /**
     * Helper method for initializing a EulaInfo to add to a task.
     * @param stub
     * @param taskInfo, must have a valid LSID
     * @param licenseFile, the path to this file must be valid when reading the content of the file.
     */
    private static void addLicenseFiles(final GetEulaFromTaskStub stub, TaskInfo taskInfo, List<File> licenseFiles) {
        List<EulaInfo> eulaInfos = new ArrayList<EulaInfo>();
        try {
            for(File licenseFile : licenseFiles) {
                EulaInfo eulaInfo=GetEulaFromTaskStub.initEulaInfo(taskInfo, licenseFile);
                eulaInfos.add(eulaInfo);
            }
        }
        catch (InitException e) {
            Assert.fail(""+e.getLocalizedMessage());
        }
        stub.setEulas(eulaInfos, taskInfo);
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
    static public void assertEulaInfo(List<EulaInfo> eulas, final int i, final String expectedName, final String expectedLsid, final String expectedVersion, final String expectedContent) {
        final EulaInfo eula=eulas.get(i);
        final String pre="eula["+i+"].";
        assertEulaInfo(pre, eula, expectedName, expectedLsid, expectedVersion, expectedContent);
    }

    static public void assertEulaInfo(final String pre, final EulaInfo eula, final String expectedName, final String expectedLsid, final String expectedVersion, final String expectedContent) {
        Assert.assertEquals(pre+"moduleName", expectedName, eula.getModuleName());
        Assert.assertEquals(pre+"moduleLsid", expectedLsid, eula.getModuleLsid());
        Assert.assertEquals(pre+"moduleLsidVersion", expectedVersion, eula.getModuleLsidVersion());
        try {
            Assert.assertEquals(pre+"content", expectedContent, eula.getContent());
        }
        catch (InitException e) {
            Assert.fail(pre+"conent, error getting content: "+e.getLocalizedMessage());
        }
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
        List<TaskInfo> taskInfos = TaskUtil.getTaskInfosFromZipPipeline(this.getClass(), filename); 
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
        //stub.addLicenseFile(taskInfo, licenseFile);
        try {
            EulaInfo eulaIn=GetEulaFromTaskStub.initEulaInfo(taskInfo, licenseFile);
            stub.setEula(eulaIn, taskInfo);
        }
        catch (InitException e) {
            Assert.fail(""+e.getLocalizedMessage());
        }


        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);

        eulaMgr.setGetEulaFromTask(stub);

        List<EulaInfo> eulas=eulaMgr.getAllEulaForModule(taskContext);
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
        catch (InitException e) {
            Assert.fail("EulaInfo.EulaInitException thrown in eula.getContent(): "+e.getLocalizedMessage());
        }
    }

    /**
     * Theoretically possible, so make sure we know how to get, display and sort EulaInfo
     * when there are more than one license file for a single module.
     */
    @Test
    public void testMultipleEulaFromModule() {
        final String filename="testLicenseAgreement_v3.zip";
        final File gpLicense=getSourceFile("gp_server_license.txt");
        final File exampleLicense=getSourceFile("example_license.txt");
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        List<File> licenses=new ArrayList<File>();
        licenses.add(gpLicense);
        licenses.add(exampleLicense);
        addLicenseFiles(stub, taskInfo, licenses);
        
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);

        eulaMgr.setGetEulaFromTask(stub);

        List<EulaInfo> eulas=eulaMgr.getAllEulaForModule(taskContext);
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
        final List<TaskInfo> taskInfos = TaskUtil.getTaskInfosFromZipPipeline(this.getClass(), filename); 
        final TaskInfo pipelineTask=taskInfos.get(0);
        final File gpLicenseFile=getSourceFile("gp_server_license.txt");
        final File exampleLicenseFile=getSourceFile("example_license.txt");
        
        //hand-code license agreements, instead of extracting from the zip file
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        for(TaskInfo taskInfo : taskInfos) {
            addLicenseFile(stub, taskInfo, gpLicenseFile);
        }
        //add an extra license file to the first taskInfo
        List<File> twoLicenses=new ArrayList<File>();
        twoLicenses.add(gpLicenseFile);
        twoLicenses.add(exampleLicenseFile);
        addLicenseFiles(stub, taskInfos.get(0), twoLicenses);
        GetTaskStrategy myTaskInfoCache = new GetTaskStrategy() {
            //@Override
            public TaskInfo getTaskInfo(final String lsid) {
                //quick and dirty implementation
                if (lsid==null) {
                    throw new IllegalArgumentException("lsid==null");
                    //return null;
                }
                for(TaskInfo taskInfo : taskInfos) {
                    if (lsid.equals(taskInfo.getLsid())) {
                        return taskInfo;
                    }
                }
                throw new TaskLSIDNotFoundException(lsid);
                //return null;
            }
        };
        
        TaskInfo lastEulaTask = myTaskInfoCache.getTaskInfo("urn:lsid:9090.gpdev.gpint01:genepatternmodules:816:1");
        addLicenseFiles(stub, lastEulaTask, twoLicenses);

        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(pipelineTask);

        eulaMgr.setGetTaskStrategy(myTaskInfoCache);
        eulaMgr.setGetEulaFromTask(stub);
        List<EulaInfo> eulas=eulaMgr.getAllEulaForModule(taskContext);
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
    
    @Test
    public void testRequiresEulaWithNoEula() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        boolean requiresEula=eulaMgr.requiresEula(taskContext);
        Assert.assertFalse("the module requires no eula", requiresEula);
    }
    
    @Test
    public void testRequiresEula() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);
        
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        boolean requiresEula=eulaMgr.requiresEula(taskContext);
        Assert.assertTrue("Expecting to requireEula for a module which has a Eula and a user which has not yet agreed", requiresEula);
    }

    @Test
    public void testRequiresEula_NullTaskContext() {
        GpContext taskContext=null;        
        try { 
            eulaMgr.requiresEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testRequiresEula_NullTaskInfo() {
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        
        try { 
            eulaMgr.requiresEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.taskInfo==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testRequiresEula_NullLsid() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        taskInfo.getTaskInfoAttributes().put("LSID", null);
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        
        try { 
            eulaMgr.requiresEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.taskInfo==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testRequiresEula_LsidNotSet() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        taskInfo.getTaskInfoAttributes().put("LSID", "");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        
        try { 
            eulaMgr.requiresEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.taskInfo==\"\" (empty string)");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testRequiresEula_NullUserId() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);
        
        final String userId=null;
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);

        try { 
            eulaMgr.requiresEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.userId==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testRequiresEula_UserIdNotSet() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);
        
        final String userId="";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);

        try { 
            eulaMgr.requiresEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.userId==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    /**
     * Basic test-case for EulaManager recordEula.
     */
    @Test
    public void testRecordEula() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);

        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        eulaMgr.recordEula(taskContext);
    }
    
    @Test
    public void testRecordEula_NullTaskContext() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);

        GpContext taskContext=null; 
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        try { 
            eulaMgr.recordEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testRecordEula_NullTaskInfo() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);

        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(null);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        try { 
            eulaMgr.recordEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.taskInfo==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testRecordEula_NullLsid() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);

        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskInfo.getAttributes().put("LSID", null);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        try { 
            eulaMgr.recordEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.taskInfo.lsid==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testRecordEula_LsidNotSet() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);

        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskInfo.getAttributes().put("LSID", "");
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        try { 
            eulaMgr.recordEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.taskInfo.lsid==\"\" (empty string)");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testRecordEula_NullUserId() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);

        final String userId=null;
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        try { 
            eulaMgr.recordEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.userId==null");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }
    
    @Test
    public void testRecordEula_UserIdNotSet() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);

        final String userId="";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        try { 
            eulaMgr.recordEula(taskContext);
            Assert.fail("Expecting IllegalArgumentException, when taskContext.userId==\"\" (empty string)");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }
    
    @Test
    public void testRequiresEulaAlreadyAgreed() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);
        
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        eulaMgr.recordEula(taskContext);
        boolean requiresEula=eulaMgr.requiresEula(taskContext);
        Assert.assertFalse("the user has already agreed", requiresEula);
    }

    /**
     * Test EulaManager#getPendingEulaForModule, for a module which has no EULA.
     */
    @Test
    public void testGetPendingNoEula() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        List<EulaInfo> eulas=eulaMgr.getPendingEulaForModule(taskContext);
        
        //should be empty, for a module which has no eula
        Assert.assertEquals("pendingEulaForModule should be empty for a module which has no eulas", 0, eulas.size());
    }
    
    /**
     * Test EulaManager#getPendingEulaForModule, for a module with one EULA,
     *     and the currentUser has not yet agreed.
     */
    @Test
    public void testGetPendingNotYetAgreed() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);
        
        final String userId="gp_user_two";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        List<EulaInfo> eulas=eulaMgr.getPendingEulaForModule(taskContext);
        
        Assert.assertEquals("pendingEulaForModule.size should be 1 for a module which has one eula", 1, eulas.size());
    }
    
    /**
     * Test EulaManager#getPendingEulaForModule, for a module with one EULA,
     *     and the current user has agreed.
     */
    @Test
    public void testGetPendingAlreadyAgreed() {
        final String filename="testLicenseAgreement_v3.zip";
        TaskInfo taskInfo = initTaskInfoFromZip(filename);
        File licenseFile=getSourceFile("gp_server_license.txt");
        GetEulaFromTaskStub stub = new GetEulaFromTaskStub();
        addLicenseFile(stub, taskInfo, licenseFile);
        
        final String userId="gp_user_three";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        eulaMgr.setGetEulaFromTask(stub);
        eulaMgr.setRecordEulaStrategy(recordEulaStub);
        eulaMgr.recordEula(taskContext);
        List<EulaInfo> eulas=eulaMgr.getPendingEulaForModule(taskContext);

        Assert.assertEquals("pendingEulaForModule.size should be 0 if the current user has already agreed", 0, eulas.size());
    }

    @Test
    public void testSetEulaForModule() {
        final TaskInfo taskInfo = initTaskInfoFromZip("testLicenseAgreement_v3.zip");
        final File licenseFile=getSourceFile("gp_server_license.txt");
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        
        EulaInfo eula=null;
        try {
            eula=EulaManager.initEulaInfo(taskInfo, licenseFile);
        }
        catch (InitException e) {
            Assert.fail(""+e.getLocalizedMessage());
        } 
        eulaMgr.setEula(eula, taskInfo);
        List<EulaInfo> eulas=eulaMgr.getEulas(taskInfo);
        Assert.assertEquals("eulas.size should be 1", 1, eulas.size());
        
        eulaMgr.setEula(null, taskInfo);
        eulas=eulaMgr.getEulas(taskInfo);
        Assert.assertEquals("eulas.size should be 0", 0, eulas.size());
    }

    /**
     * Test-case for getting eula for a pipeline, when some (or all) of the included modules 
     * are not installed on the server.
     */
    @Test
    public void testGetPendingMissingTasks() {
        final String filename="Golub.Slonim.1999.Nature.all.aml.pipeline_v2.zip";
        final List<TaskInfo> taskInfos = TaskUtil.getTaskInfosFromZipPipeline(this.getClass(), filename); 
        
        final String userId="gp_user";
        GpContext taskContext=GpContext.getContextForUser(userId);
        //assume the first entry is the pipeline
        taskContext.setTaskInfo(taskInfos.get(0));
        
        eulaMgr.setGetTaskStrategy(new GetTaskStrategy() {

            //@Override
            public TaskInfo getTaskInfo(final String lsid) {
                if (lsid==null) {
                    throw new IllegalArgumentException("lsid==null");
                }
                if (lsid.equals(taskInfos.get(0).getLsid())) {
                    return taskInfos.get(0);
                }
                //simulate taskinfocache method call, when the module is not installed on the server
                throw new TaskLSIDNotFoundException(lsid);
            }
        });
        
        List<EulaInfo> eulas=eulaMgr.getPendingEulaForModule(taskContext);
        
        Assert.assertEquals("golub pipeline v2 has no eulas", 0, eulas.size());

    }
    
}
