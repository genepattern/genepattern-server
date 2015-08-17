/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskLoader;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFilePath;
import org.genepattern.server.job.input.Param;
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
    private static TaskLoader taskLoader;
    private static String adminUserId;
    private static GpContext userContext;
    private static GpConfig gpConfig=null;
    
    final static String GP_URL="http://127.0.0.1:8080/gp";
    
    //ConvertLineEndings v1
    final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:1";
    //ComparativeMarkerSelection v9
    final String cmsLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9";
    //ListFiles v0.7
    final String listFilesLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00275:0.7";
    
    final static String createServerFilePathUrl(final String relativePath) {
        File file=FileUtil.getDataFile(relativePath);
        final String rval=GP_URL+"/data/"+file.getAbsolutePath();
        return rval;
    }
    
    final static String createUserUploadRef(final String userId, final String relativePath) {
        //TODO: to make this general purpose, need to handle userId which don't map to valid URI path components (e.g. 'test@abc.com', 'test user')
        final String rval="<GenePatternURL>/users/"+userId+"/"+relativePath;
        return rval;
    }

    final static String createUserUploadRefUrl(final String userId, final String relativePath) {
        final String rval=GP_URL+"/users/"+userId+"/"+relativePath;
        return rval;
    }

    @BeforeClass
    static public void beforeClass() {
        adminUserId="admin";
        userContext=new GpContext.Builder()
            .userId(adminUserId)
            .isAdmin(true)
            .build();
        taskLoader=new TaskLoader();
        taskLoader.addTask(TestJobInputHelper.class, "ConvertLineEndings_v1.zip");
        taskLoader.addTask(TestJobInputHelper.class, "ComparativeMarkerSelection_v9.zip");
        taskLoader.addTask(TestJobInputHelper.class, "ListFiles_v0.7.zip");
    }
    
    @Before
    public void setUp() throws MalformedURLException {
        gpConfig=new GpConfig.Builder()
            .genePatternURL(new URL("http://127.0.0.1:8080/gp/"))
        .build();
    }
    
    //////////////////////////////////////////
    // test cases for input file values
    //
    //////////////////////////////////////////
    @Test
    public void testGpUrlValue() {
        final String initialValue=GP_URL+"/users/"+adminUserId+"/all_aml_test.cls";

        JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cleLsid, null, taskLoader);
        jobInputHelper.addValue("input.filename", initialValue);
    }

    //////////////////////////////////////////
    // test cases for group input handling
    //
    //////////////////////////////////////////
    @Test
    public void testAddGroupValue() throws GpServerException {
        JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cleLsid, null, taskLoader);
        jobInputHelper.addValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls",
                new GroupId("train"));
        jobInputHelper.addValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct",
                new GroupId("train"));
        jobInputHelper.addValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.res",
                new GroupId("train"));
        jobInputHelper.addValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls",
                new GroupId("test"));
        jobInputHelper.addValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct",
                new GroupId("test"));
        jobInputHelper.addValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.res",
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
        
        assertEquals("group[0][0]", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls", param.getValue(train, 0).getValue());
        assertEquals("group[0][1]", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct", param.getValue(train, 1).getValue());
        assertEquals("group[0][2]", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.res", param.getValue(train, 2).getValue());
        assertEquals("group[1][0]", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls", param.getValue(test, 0).getValue());
        assertEquals("group[1][1]", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct", param.getValue(test, 1).getValue());
        assertEquals("group[1][2]", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.res", param.getValue(test, 2).getValue());
    }
    
    //////////////////////////////////////////
    // test cases for batch input handling
    //
    //////////////////////////////////////////
    
    @Test
    public void testJobSubmit() throws GpServerException {
        JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cleLsid, null, taskLoader);
        jobInputHelper.addValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls");

        List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 1, inputs.size());
        JobInput jobInput=inputs.get(0);
        Param param=jobInput.getParam("input.filename");
        assertEquals("input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls",
                param.getValues().get(0).getValue());
    }
    
    @Test
    public void testAddBatchValue() throws GpServerException {
        JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cleLsid, null, taskLoader);
        jobInputHelper.addBatchValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls");
        jobInputHelper.addBatchValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct");
        jobInputHelper.addBatchValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls");
        jobInputHelper.addBatchValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct");

        List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 4, inputs.size());
    }

    /**
     * Test a batch job with multiple input parameters.
     * @throws GpServerException
     */
    @Test
    public void testAddBatchMultiParam() throws GpServerException {
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchValue("input.file", createServerFilePathUrl("all_aml/all_aml_test.gct"));
        jobInputHelper.addBatchValue("cls.file", createServerFilePathUrl("all_aml/all_aml_test.cls"));
        jobInputHelper.addBatchValue("input.file", createServerFilePathUrl("all_aml/all_aml_train.gct"));
        jobInputHelper.addBatchValue("cls.file", createServerFilePathUrl("all_aml/all_aml_train.cls"));
        
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 2, inputs.size());
        
        //first batch
        assertEquals(createServerFilePathUrl("all_aml/all_aml_test.gct"), inputs.get(0).getParam("input.file").getValues().get(0).getValue());
        assertEquals(createServerFilePathUrl("all_aml/all_aml_test.cls"), inputs.get(0).getParam("cls.file").getValues().get(0).getValue());
        assertEquals(createServerFilePathUrl("all_aml/all_aml_train.gct"), inputs.get(1).getParam("input.file").getValues().get(0).getValue());
        assertEquals(createServerFilePathUrl("all_aml/all_aml_train.cls"), inputs.get(1).getParam("cls.file").getValues().get(0).getValue());
    }
    
    /**
     * Test a batch job with multiple input parameters, but only one value for each batch parameter.
     * @throws GpServerException
     */
    @Test
    public void testAddBatchMultiParamOneValue() throws GpServerException {
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchValue("input.file", createServerFilePathUrl("all_aml/all_aml_test.gct"));
        jobInputHelper.addBatchValue("cls.file", createServerFilePathUrl("all_aml/all_aml_test.cls"));
        
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num jobs", 1, inputs.size());
        assertEquals(createServerFilePathUrl("all_aml/all_aml_test.gct"), inputs.get(0).getParam("input.file").getValues().get(0).getValue());
        assertEquals(createServerFilePathUrl("all_aml/all_aml_test.cls"), inputs.get(0).getParam("cls.file").getValues().get(0).getValue()); 
    }

    @Test
    public void testAddBatchDirectory() throws GpServerException {
        final File batchDir=FileUtil.getDataFile("all_aml/");

        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cleLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.filename", batchDir.getAbsolutePath());
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
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", batchDir.getAbsolutePath());
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
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", batchDir.getAbsolutePath());
        //bogus value, but the module requires a cls file
        jobInputHelper.addBatchDirectory("cls.file", batchDir.getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 2, inputs.size());
    }
    
    /**
     * Test case for a batch parameter which accepts a sub-directory as an input value,
     * fileFormat=directory.
     */
    @Test
    public void testMatchBatchOfDirectories() throws GpServerException {
        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class,"batch_02/");
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, listFilesLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("dir", batchDir.getAbsolutePath());
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
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", gctDir.getAbsolutePath());
        jobInputHelper.addBatchDirectory("cls.file", clsDir.getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        assertEquals("num batch jobs", 2, inputs.size());
    }
    
    /**
     * Test case for a missing batch input parameter, for instance, 
     *     addBatchDirectory("input.filename", directory) with CMS.
     *     CMS has an "input.file" parameter, but not an "input.filename" parameter.
     */
    @Test
    public void testMissingBatchInputParameter() {
        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/res/");
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
        try {
            jobInputHelper.addBatchDirectory("input.filename",  batchDir.getAbsolutePath());
            final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            fail("Expecting GpServerException: empty batch directory");
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
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cleLsid, null, taskLoader);
        try {
            jobInputHelper.addBatchDirectory("input.filename", batchDir.getAbsolutePath());
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
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cleLsid, null, taskLoader);
        try {
            jobInputHelper.addBatchDirectory("input.filename", batchDir.getAbsolutePath());
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
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
        try {
            jobInputHelper.addBatchDirectory("input.file", batchDir.getAbsolutePath());
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
        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
        try {
            jobInputHelper.addBatchDirectory("input.file", batchDir.getAbsolutePath());
            jobInputHelper.addBatchDirectory("cls.file", batchDir.getAbsolutePath());
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

        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cleLsid, null, taskLoader);
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

        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cleLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.filename", dir1.getAbsolutePath());
        jobInputHelper.addBatchDirectory("input.filename", dir2.getAbsolutePath());
        jobInputHelper.addBatchDirectory("input.filename", dir3.getAbsolutePath());

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


        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
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


        final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", param1_dir1.getAbsolutePath());
        jobInputHelper.addBatchDirectory("input.file", param1_dir2.getAbsolutePath());
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
            final JobInputHelper jobInputHelper=new JobInputHelper(gpConfig, userContext, cmsLsid, null, taskLoader);
            jobInputHelper.addBatchDirectory("input.file", param1_file1.getAbsolutePath());
            jobInputHelper.addBatchDirectory("input.file", param1_file2.getAbsolutePath());
            jobInputHelper.addBatchDirectory("input.file", param1_dir1.getAbsolutePath());
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
        
        List<GpFilePath> batchFiles=BatchInputFileHelper.getBatchInputFiles(pinfo, inputDir);
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
        Mockito.when(taskInfo.getParameterInfoArray()).thenReturn(pinfos);

        JobInputHelper jobInputHelper = new JobInputHelper(gpConfig, userContext, lsid, null, taskInfo);
        
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
        final TaskInfo taskInfo = Mockito.mock(TaskInfo.class);
        Mockito.when(taskInfo.getParameterInfoArray()).thenReturn(pinfos);

        JobInputHelper jobInputHelper = new JobInputHelper(gpConfig, userContext, lsid, null, taskInfo);
        
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

}
