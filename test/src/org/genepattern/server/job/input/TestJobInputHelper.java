/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.genepattern.junitutil.Demo;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFilePath;
import org.genepattern.server.job.input.batch.BatchInputFileHelper;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * jUnit tests for the JobInputHelper class.
 * 
 * @author pcarr
 *
 */
public class TestJobInputHelper {
    private static HttpServletRequest request=null;
    private static final HibernateSessionManager mgr=null;
    private static GpConfig gpConfig=null;
    
    private static TaskInfo cleTaskInfo;
    private static GpContext cleContext;
    private static TaskInfo cmsTaskInfo;
    private static GpContext cmsContext;

    final static private String createServerFilePathUrl(final String relativePath) {
        return gpHref + serverFile(relativePath);
    }

    @BeforeClass
    static public void beforeClass() {
        
        cleTaskInfo=TaskUtil.getTaskInfoFromZip(TestJobInputHelper.class, "ConvertLineEndings_v1.zip");
        cleContext=new GpContext.Builder()
            .userId(adminUserId)
            .isAdmin(true)
            .taskInfo(cleTaskInfo)
        .build();
        
        cmsTaskInfo=TaskUtil.getTaskInfoFromZip(TestJobInputHelper.class, "ComparativeMarkerSelection_v9.zip");
        cmsContext=new GpContext.Builder()
            .userId(adminUserId)
            .isAdmin(true)
            .taskInfo(cmsTaskInfo)
        .build();

        request=localRequest(); // mock request to 'http://127.0.0.1:8080/gp/
    }
    
    @Before
    public void setUp() throws MalformedURLException {
        gpConfig=new GpConfig.Builder()
            .webappDir(new File("website"))
            .genePatternURL(new URL(gpUrl))
        .build();
    }
    
    //////////////////////////////////////////
    // test cases for input file values
    //
    //////////////////////////////////////////
    @Test
    public void testGpUrlValue() throws GpServerException {
        JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, request);
        jobInputHelper.addValue("input.filename", gpHref + uploadPath(adminUserId, "all_aml_test.cls"));
        
        List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 1, inputs.size());
        assertEquals("input[0].input.filename[0]", 
                // expected
                gpHref + uploadPath(adminUserId, "all_aml_test.cls"),
                // actual
                inputs.get(0).getParam("input.filename").getValues().get(0).getValue()
                );
    }

    //////////////////////////////////////////
    // test cases for group input handling
    //
    //////////////////////////////////////////
    @Test
    public void testAddGroupValue() throws GpServerException {
        JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, request);
        jobInputHelper.addValue("input.filename", 
                dataFtpDir+"all_aml_train.cls",
                new GroupId("train"));
        jobInputHelper.addValue("input.filename", 
                dataFtpDir+"all_aml_train.gct",
                new GroupId("train"));
        jobInputHelper.addValue("input.filename", 
                dataFtpDir+"all_aml_train.res",
                new GroupId("train"));
        jobInputHelper.addValue("input.filename", 
                dataFtpDir+"all_aml_test.cls",
                new GroupId("test"));
        jobInputHelper.addValue("input.filename", 
                dataFtpDir+"all_aml_test.gct",
                new GroupId("test"));
        jobInputHelper.addValue("input.filename", 
                dataFtpDir+"all_aml_test.res",
                new GroupId("test"));
        
        List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 1, inputs.size());
        JobInput jobInput=inputs.get(0);
        Param param=jobInput.getParam("input.filename");
        assertEquals("numGroups", 2, param.getNumGroups());
        assertEquals("numValues", 6, param.getNumValues());
        
        //verify the groupings
        final GroupId train=new GroupId("train");
        final GroupId test=new GroupId("test");
        assertEquals("group[0]", new GroupId("train"), param.getGroups().get(0));
        assertEquals("group[1]", new GroupId("test"), param.getGroups().get(1));

        assertEquals("group[0][0]", dataFtpDir+"all_aml_train.cls", param.getValue(train, 0).getValue());
        assertEquals("group[0][1]", dataFtpDir+"all_aml_train.gct", param.getValue(train, 1).getValue());
        assertEquals("group[0][2]", dataFtpDir+"all_aml_train.res", param.getValue(train, 2).getValue());
        assertEquals("group[1][0]", dataFtpDir+"all_aml_test.cls", param.getValue(test, 0).getValue());
        assertEquals("group[1][1]", dataFtpDir+"all_aml_test.gct", param.getValue(test, 1).getValue());
        assertEquals("group[1][2]", dataFtpDir+"all_aml_test.res", param.getValue(test, 2).getValue());
    }
    
    //////////////////////////////////////////
    // test cases for batch input handling
    //
    //////////////////////////////////////////
    
    @Test
    public void testJobSubmit() throws GpServerException {
        JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, request);
        jobInputHelper.addValue("input.filename", dataFtpDir+"all_aml_train.cls");

        List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 1, inputs.size());
        JobInput jobInput=inputs.get(0);
        Param param=jobInput.getParam("input.filename");
        assertEquals("input.filename", dataFtpDir+"all_aml_train.cls",
                param.getValues().get(0).getValue());
    }
    
    @Test
    public void testAddBatchValue() throws GpServerException {
        JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, request);
        jobInputHelper.addBatchValue("input.filename", dataFtpDir+"all_aml_train.cls");
        jobInputHelper.addBatchValue("input.filename", dataFtpDir+"all_aml_train.gct");
        jobInputHelper.addBatchValue("input.filename", dataFtpDir+"all_aml_test.cls");
        jobInputHelper.addBatchValue("input.filename", dataFtpDir+"all_aml_test.gct");

        List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 4, inputs.size());
    }

    /**
     * Test a batch job with multiple input parameters.
     * @throws GpServerException
     */
    @Test
    public void testAddBatchMultiParam() throws GpServerException {
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        jobInputHelper.addBatchValue("input.file", gpHref + serverFile("all_aml/all_aml_test.gct"));
        jobInputHelper.addBatchValue("cls.file", gpHref + serverFile("all_aml/all_aml_test.cls"));
        jobInputHelper.addBatchValue("input.file", gpHref + serverFile("all_aml/all_aml_train.gct"));
        jobInputHelper.addBatchValue("cls.file", gpHref + serverFile("all_aml/all_aml_train.cls"));
        
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 2, inputs.size());
        
        assertEquals(gpHref + serverFile("all_aml/all_aml_test.gct"), inputs.get(0).getParam("input.file").getValues().get(0).getValue());
        assertEquals(gpHref + serverFile("all_aml/all_aml_test.cls"), inputs.get(0).getParam("cls.file").getValues().get(0).getValue());
        assertEquals(gpHref + serverFile("all_aml/all_aml_train.gct"), inputs.get(1).getParam("input.file").getValues().get(0).getValue());
        assertEquals(gpHref + serverFile("all_aml/all_aml_train.cls"), inputs.get(1).getParam("cls.file").getValues().get(0).getValue());
    }
    
    @Test
    public void testAddBatchMultiParam_externalUrl() throws GpServerException {
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        jobInputHelper.addBatchValue("input.file", dataFtpDir + "all_aml_test.gct");
        jobInputHelper.addBatchValue("cls.file",   dataFtpDir + "all_aml_test.cls");
        jobInputHelper.addBatchValue("input.file", dataFtpDir + "all_aml_train.gct");
        jobInputHelper.addBatchValue("cls.file",   dataFtpDir + "all_aml_train.cls");

        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 2, inputs.size());
        
        assertEquals(dataFtpDir + "all_aml_test.gct",  inputs.get(0).getParam("input.file").getValues().get(0).getValue());
        assertEquals(dataFtpDir + "all_aml_test.cls",  inputs.get(0).getParam("cls.file").getValues().get(0).getValue());
        assertEquals(dataFtpDir + "all_aml_train.gct", inputs.get(1).getParam("input.file").getValues().get(0).getValue());
        assertEquals(dataFtpDir + "all_aml_train.cls", inputs.get(1).getParam("cls.file").getValues().get(0).getValue()); 
    }
    
    @Test public void multiBatchParam_mixedInternalAndExternal() throws GpServerException  {
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        jobInputHelper.addBatchValue("input.file", dataFtpDir + "all_aml_test.gct" );
        jobInputHelper.addBatchValue("input.file", gpHref + serverFile("all_aml/all_aml_train.gct"));
        jobInputHelper.addBatchValue("cls.file", dataFtpDir + "all_aml_test.cls");
        jobInputHelper.addBatchValue("cls.file", gpHref + serverFile("all_aml/all_aml_train.cls"));

        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 2, inputs.size());
        
        assertEquals(dataFtpDir + "all_aml_test.gct",  inputs.get(0).getParam("input.file").getValues().get(0).getValue());
        assertEquals(dataFtpDir + "all_aml_test.cls",  inputs.get(0).getParam("cls.file").getValues().get(0).getValue());
        assertEquals(gpHref + serverFile("all_aml/all_aml_train.gct"), inputs.get(1).getParam("input.file").getValues().get(0).getValue());
        assertEquals(gpHref + serverFile("all_aml/all_aml_train.cls"), inputs.get(1).getParam("cls.file").getValues().get(0).getValue()); 
        
        
    }
    
    
    /**
     * Test a batch job with multiple input parameters, but only one value for each batch parameter.
     * @throws GpServerException
     */
    @Test
    public void testAddBatchMultiParamOneValue() throws GpServerException {
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        jobInputHelper.addBatchValue("input.file", gpHref + serverFile("all_aml/all_aml_test.gct"));
        jobInputHelper.addBatchValue("cls.file", gpHref + serverFile("all_aml/all_aml_test.cls"));
        
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 1, inputs.size());
        assertEquals(gpHref + serverFile("all_aml/all_aml_test.gct"), inputs.get(0).getParam("input.file").getValues().get(0).getValue());
        assertEquals(gpHref + serverFile("all_aml/all_aml_test.cls"), inputs.get(0).getParam("cls.file").getValues().get(0).getValue()); 
    }

    @Test
    public void testaddBatchValue() throws GpServerException {
        final File batchDir=FileUtil.getDataFile("all_aml/");

        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, request);
        jobInputHelper.addBatchValue("input.filename", batchDir.getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 7, inputs.size());
    }

    /**
     * Test case for batch jobs, when the batch param declares an input file format, for instance,
     *     'fileFormat=gct', or
     *     'fileFormat=gct;res'
     */
    @Test
    public void testAddBatchDirMatchFileFormat() throws GpServerException {
        final File batchDir=FileUtil.getDataFile("all_aml/");
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        jobInputHelper.addBatchValue("input.file", batchDir.getAbsolutePath());
        //bogus value, but the module requires a cls file
        jobInputHelper.addValue("cls.file", FileUtil.getDataFile("all_aml/all_aml_test.cls").getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 4, inputs.size());
    }

    /**
     * Test case for multiple batch parameters.
     * @throws GpServerException
     */
    @Test
    public void testAddBatchDirMulti() throws GpServerException {
        final File batchDir=FileUtil.getDataFile("all_aml/");
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        jobInputHelper.addBatchValue("input.file", batchDir.getAbsolutePath());
        //bogus value, but the module requires a cls file
        jobInputHelper.addBatchValue("cls.file", batchDir.getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 2, inputs.size());
    }
    
    /**
     * Test case for a batch parameter which accepts a sub-directory as an input value,
     * fileFormat=directory.
     */
    @Test
    public void testMatchBatchOfDirectories() throws GpServerException {
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(TestJobInputHelper.class, "ListFiles_v0.7.zip");
        final GpContext gpContext=new GpContext.Builder()
            .userId(adminUserId)
            .isAdmin(true)
            .taskInfo(taskInfo)
        .build();

        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class,"batch_02/");
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, gpContext, request);
        jobInputHelper.addBatchValue("dir", batchDir.getAbsolutePath());
        jobInputHelper.addValue("outputFilename", "<dir_file>_listing.txt");
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 3, inputs.size());
    }

    /**
     * With multiple batch parameters, should only create jobs based on the union of 
     * all basenames.
     */
    @Test
    public void testAddBatchDirMultiIntersect() throws GpServerException {
        final File gctDir=FileUtil.getSourceFile(TestJobInputHelper.class,"batch_01/gct/");
        final File clsDir=FileUtil.getSourceFile(TestJobInputHelper.class,"batch_01/cls/");
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        jobInputHelper.addBatchValue("input.file", gctDir.getAbsolutePath());
        jobInputHelper.addBatchValue("cls.file", clsDir.getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 2, inputs.size());
    }
    
    /**
     * Test case for a missing batch input parameter, for instance, 
     *     addBatchValue("input.filename", directory) with CMS.
     *     CMS has an "input.file" parameter, but not an "input.filename" parameter.
     */
    @Test
    public void testMissingBatchInputParameter() {
        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/res/");
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        try {
            jobInputHelper.addBatchValue("input.filename",  batchDir.getAbsolutePath());
            final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            fail("Expecting GpServerException: No matching parameter input.filename...");
        }
        catch (GpServerException e) {
            //expected
        }
    }

    /**
     * Test case for an empty batch directory.
     */
    @Test
    public void testEmptyBatchDir() {
        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class,"empty_batch_dir/");
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, request);
        try {
            jobInputHelper.addBatchValue("input.filename", batchDir.getAbsolutePath());
            //final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            fail("Expecting GpServerException: empty batch directory");
        }
        catch (GpServerException e) {
            //expected
        }
    }

    /**
     * Test case for a non-existent batch directory.
     */
    @Test
    public void testBatchDirNotExists() {
        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class, "batch_dir_does_not_exist/");
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, request);
        try {
            jobInputHelper.addBatchValue("input.filename", batchDir.getAbsolutePath());
            //final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            fail("Expecting GpServerException: batch directory doesn't exist");
        }
        catch (GpServerException e) {
            //expected
        }
    }

    /**
     * Test case for a batch directory with no matching input files.
     * E.g. parameter fileFormat=gct and the batch input directory has no gct files.
     */
    @Test
    public void testBatchDirNoMatch() {
        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/");
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        try {
            jobInputHelper.addBatchValue("input.file", batchDir.getAbsolutePath());
            //final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            fail("Expecting GpServerException: empty batch directory");
        }
        catch (GpServerException e) {
            //expected
        }
    }
    
    /**
     * Test case for multiple batch input parameters, when the intersection of all files 
     * results in no matches.
     */
    @Test
    public void testMultiBatchDirNoMatch() {
        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class, "batch_03/");
        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        try {
            jobInputHelper.addBatchValue("input.file", batchDir.getAbsolutePath());
            jobInputHelper.addBatchValue("cls.file", batchDir.getAbsolutePath());
            //final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            fail("Expecting GpServerException: empty batch directory");
        }
        catch (GpServerException e) {
            //expected
        }
    }

    /**
     * Test case for list of files provided as input to one batch input parameter
     */
    @Test
    public void testMultipleFilesOneBatchParam() throws GpServerException
    {
        final File file1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_03/a_test.cls");
        final File file2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_03/c_test.gct");
        final File file3 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/01/b.txt");
        final File file4 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/cls/all_aml_test.cls");
        final File file5 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/03/i.txt");

        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, request);
        jobInputHelper.addBatchValue("input.filename", file1.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", file2.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", file3.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", file4.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", file5.getAbsolutePath());

        //Here we expect 5 batch jobs to be created
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 5, inputs.size());
    }

    /**
     * Test case for list of directories provided as input to one batch input parameter
     */
    @Test
    public void testMultipleDirsOneBatchParam() throws GpServerException
    {
        final File dir1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/01");
        final File dir2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/02/");
        final File dir3 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/03/");

        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, request);
        jobInputHelper.addBatchValue("input.filename", dir1.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", dir2.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", dir3.getAbsolutePath());

        //Here we expect 9 batch jobs to be created, one for each file in the directory
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 9, inputs.size());
    }


    /**
     * Test case for list of files provided as input to multiple batch input parameters
     */
    @Test
    public void testMultipleFilesForMultipleBatchParams() throws GpServerException
    {
        final File param1_file1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/gct/all_aml_test.gct");
        final File param1_file2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/res/all_aml_train.res");
        final File param1_file3 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/gct/b_test.gct");

        final File param2_file1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/cls/all_aml_train.cls");
        final File param2_file2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/cls/all_aml_test.cls");

        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        jobInputHelper.addBatchValue("input.file", param1_file1.getAbsolutePath());
        jobInputHelper.addBatchValue("input.file", param1_file2.getAbsolutePath());
        jobInputHelper.addBatchValue("input.file", param1_file3.getAbsolutePath());
        jobInputHelper.addBatchValue("cls.file", param2_file1.getAbsolutePath());
        jobInputHelper.addBatchValue("cls.file", param2_file2.getAbsolutePath());

        //Here we expect 2 batch jobs to be created since there are only two pairs of matching file base names
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 2, inputs.size());
    }

    /**
     * Test case for list of directories provided as input to multiple batch input parameters
     */
    @Test
    public void testMultipleDirsForMultipleBatchParams() throws GpServerException
    {
        final File param1_dir1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/gct/");
        final File param1_dir2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_03/");

        final File param2_dir1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_04/cls/");
        final File param2_dir2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/cls/");

        final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
        jobInputHelper.addBatchValue("input.file", param1_dir1.getAbsolutePath());
        jobInputHelper.addBatchValue("input.file", param1_dir2.getAbsolutePath());
        jobInputHelper.addBatchValue("cls.file", param2_dir1.getAbsolutePath());
        jobInputHelper.addBatchValue("cls.file", param2_dir2.getAbsolutePath());

        //Here we expect 2 batch jobs to be created since there are only two pairs of matching file base names
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 4, inputs.size());
    }

    /**
     * Test case for list of files or directories provided as input to multiple batch input parameters
     * where the basenames do not match
     */
    @Test
    public void testMultipleFilesOrDirsForMultipleBatchParamsNoMatch() throws Exception
    {
        final File param1_file1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/gct/all_aml_train.gct");
        final File param1_file2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/b.txt");
        final File param1_dir1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_04/cls/");


        final File param2_file1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_03/a_test.cls");
        final File param2_file2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/gct/all_aml_train.gct");

        try
        {
            final JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cmsContext, request);
            jobInputHelper.addBatchValue("input.file", param1_file1.getAbsolutePath());
            jobInputHelper.addBatchValue("input.file", param1_file2.getAbsolutePath());
            jobInputHelper.addBatchValue("input.file", param1_dir1.getAbsolutePath());
            jobInputHelper.addBatchValue("cls.file", param2_file1.getAbsolutePath());
            jobInputHelper.addBatchValue("cls.file", param2_file2.getAbsolutePath());

            jobInputHelper.prepareBatch();
            fail("Expecting GpServerException: No matching input files for batch parameter");
        }
        catch(GpServerException e)
        {
            String errMsg = e.getLocalizedMessage();
            if(errMsg != null)
            {
                assertTrue(errMsg.contains("No matching input files for batch parameter"));
            }
            else
            {
                fail("Expecting GpServerException: No matching input files for batch parameter");
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private ParameterInfo makeFileParam(String pname) {
        ParameterInfo pinfo = new ParameterInfo(pname, "", "");
        pinfo.setAttributes(new HashMap<String,String>());
        pinfo.getAttributes().put("MODE", "IN");
        pinfo.getAttributes().put("TYPE", "FILE");
        pinfo.getAttributes().put("fileFormat", "fq;fastq;fq.gz;fastq.gz;fq.bz2;fastq.bz2");
        return pinfo;
    }
    
    @Test
    public void listBatchDir() throws GpServerException {
        ParameterInfo pinfo = makeFileParam("input.file.1");
        File batchDir=new File(FileUtil.getDataDir(), "fastq");
        GpFilePath inputDir=new ServerFilePath(batchDir.getAbsoluteFile());
        List<String> batchFiles=BatchInputFileHelper.getBatchInputFiles("", pinfo, inputDir);
        assertEquals(6, batchFiles.size());
    }
    
    @Test
    public void pairedEndBatch_asFiles() throws GpServerException {
        // Trimmomatic 0.6
        final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00341:0.6";
        final ParameterInfo[] pinfos=new ParameterInfo[2];
        pinfos[0] = makeFileParam("input.file.1");
        pinfos[1] = makeFileParam("input.file.2");
        final TaskInfo taskInfo = Mockito.mock(TaskInfo.class);
        when(taskInfo.getLsid()).thenReturn(lsid);
        when(taskInfo.getParameterInfoArray()).thenReturn(pinfos);
        
        final GpContext taskContext=new GpContext.Builder()
            .userId(adminUserId)
            .isAdmin(true)
            .taskInfo(taskInfo)
        .build();

        final JobInputHelper jobInputHelper = new JobInputHelper(mgr, gpConfig, taskContext, request);
        jobInputHelper.addBatchValue("input.file.1", createServerFilePathUrl("fastq/a_1.fastq"));
        jobInputHelper.addBatchValue("input.file.1", createServerFilePathUrl("fastq/b_1.fastq"));
        jobInputHelper.addBatchValue("input.file.1", createServerFilePathUrl("fastq/c_1.fastq"));
        jobInputHelper.addBatchValue("input.file.2", createServerFilePathUrl("fastq/a_2.fastq"));
        jobInputHelper.addBatchValue("input.file.2", createServerFilePathUrl("fastq/b_2.fastq"));
        jobInputHelper.addBatchValue("input.file.2", createServerFilePathUrl("fastq/c_2.fastq"));
        
        List<JobInput> batchJobs=jobInputHelper.prepareBatch();
        
        assertEquals("# batch jobs", 3, batchJobs.size());
        assertEquals( "batch[0].input.file.1", 
                createServerFilePathUrl("fastq/a_1.fastq"),
                batchJobs.get(0).getParam("input.file.1").getValues().get(0).getValue());
        assertEquals( "batch[1].input.file.1", 
                createServerFilePathUrl("fastq/b_1.fastq"),
                batchJobs.get(1).getParam("input.file.1").getValues().get(0).getValue());
        assertEquals( "batch[2].input.file.1", 
                createServerFilePathUrl("fastq/c_1.fastq"),
                batchJobs.get(2).getParam("input.file.1").getValues().get(0).getValue());
        assertEquals( "batch[0].input.file.2", 
                createServerFilePathUrl("fastq/a_2.fastq"),
                batchJobs.get(0).getParam("input.file.2").getValues().get(0).getValue());
        assertEquals( "batch[1].input.file.2", 
                createServerFilePathUrl("fastq/b_2.fastq"),
                batchJobs.get(1).getParam("input.file.2").getValues().get(0).getValue());
        assertEquals( "batch[2].input.file.2", 
                createServerFilePathUrl("fastq/c_2.fastq"),
                batchJobs.get(2).getParam("input.file.2").getValues().get(0).getValue());

    }
    
    /**
     * For GP-5189, test batch job with multiple input parameters from the same directory.
     * @throws GpServerException
     */
    @Test
    public void pairedEndBatch_asDir() throws GpServerException {
        // Trimmomatic 0.6
        final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00341:0.6";
        final ParameterInfo[] pinfos=new ParameterInfo[2];
        pinfos[0] = makeFileParam("input.file.1");
        pinfos[1] = makeFileParam("input.file.2");
        final TaskInfo taskInfo = mock(TaskInfo.class);
        when(taskInfo.getLsid()).thenReturn(lsid);
        when(taskInfo.getParameterInfoArray()).thenReturn(pinfos);

        final GpContext taskContext=new GpContext.Builder()
            .userId(adminUserId)
            .isAdmin(true)
            .taskInfo(taskInfo)
        .build();

        final JobInputHelper jobInputHelper = new JobInputHelper(mgr, gpConfig, taskContext, request);
        jobInputHelper.addBatchValue("input.file.1", createServerFilePathUrl("fastq/"));
        jobInputHelper.addBatchValue("input.file.2", createServerFilePathUrl("fastq/"));

        List<JobInput> batchJobs=jobInputHelper.prepareBatch();

        assertEquals("# batch jobs", 3, batchJobs.size());
        assertEquals( "batch[0].input.file.1", 
                createServerFilePathUrl("fastq/a_1.fastq"),
                batchJobs.get(0).getParam("input.file.1").getValues().get(0).getValue());
        assertEquals( "batch[1].input.file.1", 
                createServerFilePathUrl("fastq/b_1.fastq"),
                batchJobs.get(1).getParam("input.file.1").getValues().get(0).getValue());
        assertEquals( "batch[2].input.file.1", 
                createServerFilePathUrl("fastq/c_1.fastq"),
                batchJobs.get(2).getParam("input.file.1").getValues().get(0).getValue());
        assertEquals( "batch[0].input.file.2", 
                createServerFilePathUrl("fastq/a_2.fastq"),
                batchJobs.get(0).getParam("input.file.2").getValues().get(0).getValue());
        assertEquals( "batch[1].input.file.2", 
                createServerFilePathUrl("fastq/b_2.fastq"),
                batchJobs.get(1).getParam("input.file.2").getValues().get(0).getValue());
        assertEquals( "batch[2].input.file.2", 
                createServerFilePathUrl("fastq/c_2.fastq"),
                batchJobs.get(2).getParam("input.file.2").getValues().get(0).getValue());
    }
    
    @Test
    public void baseGpHref_proxyRequest() throws GpServerException {
        //GpConfig gpConfig=mock(GpConfig.class);
        //HibernateSessionManager mgr=
        JobInputHelper jobInputHelper=new JobInputHelper(mgr, gpConfig, cleContext, clientRequest());
        List<JobInput> batch=jobInputHelper.prepareBatch();
        assertEquals("jobInput.baseGpHref", Demo.proxyHref, batch.get(0).getBaseGpHref());
    }

}
