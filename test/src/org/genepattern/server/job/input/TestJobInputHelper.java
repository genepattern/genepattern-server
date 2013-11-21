package org.genepattern.server.job.input;

import java.io.File;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskLoader;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.rest.GpServerException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit tests for the JobInputHelper class.
 * 
 * @author pcarr
 *
 */
public class TestJobInputHelper {
    private static TaskLoader taskLoader;
    private static String adminUserId;
    private static Context userContext;
    
    final String GP_URL="http://127.0.0.1:8080/gp";
    
    //ConvertLineEndings v1
    final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:1";
    //ComparativeMarkerSelection v9
    final String cmsLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9";
    //ListFiles v0.7
    final String listFilesLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00275:0.7";
    
    final static String createUserUploadRef(final String userId, final String relativePath) {
        //TODO: to make this general purpose, need to handle userId which don't map to valid URI path components (e.g. 'test@abc.com', 'test user')
        final String rval="<GenePatternURL>/users/"+userId+"/"+relativePath;
        return rval;
    }
    
    @BeforeClass
    static public void beforeClass() {
        adminUserId="admin";
        userContext=ServerConfiguration.Context.getContextForUser(adminUserId);
        userContext.setIsAdmin(true);
        taskLoader=new TaskLoader();
        taskLoader.addTask(TestJobInputHelper.class, "ConvertLineEndings_v1.zip");
        taskLoader.addTask(TestJobInputHelper.class, "ComparativeMarkerSelection_v9.zip");
        taskLoader.addTask(TestJobInputHelper.class, "ListFiles_v0.7.zip");
    }
    
    //////////////////////////////////////////
    // test cases for input file values
    //
    //////////////////////////////////////////
    @Test
    public void testGpUrlValue() {
        final String initialValue=GP_URL+"/users/"+adminUserId+"/all_aml_test.cls";

        JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
        jobInputHelper.addValue("input.filename", initialValue);
        
    }

    //////////////////////////////////////////
    // test cases for batch input handling
    //
    //////////////////////////////////////////
    
    @Test
    public void testJobSubmit() throws GpServerException {
        JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
        jobInputHelper.addValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls");

        List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num jobs", 1, inputs.size());
        JobInput jobInput=inputs.get(0);
        Param param=jobInput.getParam("input.filename");
        Assert.assertEquals("input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls",
                param.getValues().get(0).getValue());
    }
    
    @Test
    public void testAddBatchValue() throws GpServerException {
        JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
        jobInputHelper.addBatchValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls");
        jobInputHelper.addBatchValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct");
        jobInputHelper.addBatchValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls");
        jobInputHelper.addBatchValue("input.filename", 
                "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct");

        List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 4, inputs.size());
    }

    /**
     * Test a batch job with multiple input parameters.
     * @throws GpServerException
     */
    @Test
    public void testAddBatchMultiParam() throws GpServerException {
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        
        jobInputHelper.addBatchValue("input.file", createUserUploadRef(adminUserId, "01.gct"));
        jobInputHelper.addBatchValue("cls.file", createUserUploadRef(adminUserId, "01.cls"));
        jobInputHelper.addBatchValue("input.file", createUserUploadRef(adminUserId, "02.gct"));
        jobInputHelper.addBatchValue("cls.file", createUserUploadRef(adminUserId, "02.cls"));
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num jobs", 2, inputs.size());
        
        int idx=0;
        for(JobInput input : inputs) {
            ++idx;
            Param inputFile=input.getParam("input.file");
            Param clsFile=input.getParam("cls.file");
            Assert.assertEquals("num 'input.file'", 1, inputFile.getNumValues());
            Assert.assertEquals("input.file["+idx+"]", createUserUploadRef(adminUserId,"0"+idx+".gct"), inputFile.getValues().get(0).getValue());
            Assert.assertEquals("num 'cls.file'", 1, clsFile.getNumValues());
            Assert.assertEquals("cls.file["+idx+"]", createUserUploadRef(adminUserId,"0"+idx+".cls"), clsFile.getValues().get(0).getValue());
        }
    }
    
    /**
     * Test a batch job with multiple input parameters, but only one value for each batch parameter.
     * @throws GpServerException
     */
    @Test
    public void testAddBatchMultiParamOneValue() throws GpServerException {
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        
        jobInputHelper.addBatchValue("input.file", createUserUploadRef(adminUserId, "01.gct"));
        jobInputHelper.addBatchValue("cls.file", createUserUploadRef(adminUserId, "01.cls"));
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num jobs", 1, inputs.size());
    }

    @Test
    public void testAddBatchDirectory() throws GpServerException {
        final File batchDir=FileUtil.getDataFile("all_aml/");

        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.filename", batchDir.getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 7, inputs.size());
    }

    /**
     * Test case for batch jobs, when the batch param declares an input file format, for instance,
     *     'fileFormat=gct', or
     *     'fileFormat=gct;res'
     */
    @Test
    public void testAddBatchDirMatchFileFormat() throws GpServerException {
        final File batchDir=FileUtil.getDataFile("all_aml/");
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", batchDir.getAbsolutePath());
        //bogus value, but the module requires a cls file
        jobInputHelper.addValue("cls.file", FileUtil.getDataFile("all_aml/all_aml_test.cls").getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 4, inputs.size());
    }

    /**
     * Test case for multiple batch parameters.
     * @throws GpServerException
     */
    @Test
    public void testAddBatchDirMulti() throws GpServerException {
        final File batchDir=FileUtil.getDataFile("all_aml/");
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", batchDir.getAbsolutePath());
        //bogus value, but the module requires a cls file
        jobInputHelper.addBatchDirectory("cls.file", batchDir.getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 2, inputs.size());
    }
    
    /**
     * Test case for a batch parameter which accepts a sub-directory as an input value,
     * fileFormat=directory.
     */
    @Test
    public void testMatchBatchOfDirectories() throws GpServerException {
        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class,"batch_02/");
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, listFilesLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("dir", batchDir.getAbsolutePath());
        jobInputHelper.addValue("outputFilename", "<dir_file>_listing.txt");
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 3, inputs.size());
    }

    /**
     * With multiple batch parameters, should only create jobs based on the union of 
     * all basenames.
     */
    @Test
    public void testAddBatchDirMultiIntersect() throws GpServerException {
        final File gctDir=FileUtil.getSourceFile(TestJobInputHelper.class,"batch_01/gct/");
        final File clsDir=FileUtil.getSourceFile(TestJobInputHelper.class,"batch_01/cls/");
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", gctDir.getAbsolutePath());
        jobInputHelper.addBatchDirectory("cls.file", clsDir.getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 2, inputs.size());
    }
    
    /**
     * Test case for initializing the batch inputs automatically, 
     * based on user-supplied input values.
     */
    @Test
    public void testDeduceBatchParams() throws GpServerException {
        File clsDir=FileUtil.getSourceFile(TestJobInputHelper.class,"batch_01/cls/");
        
        JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
        jobInputHelper.setDeduceBatchValues(true);
        jobInputHelper.addValue("input.filename", 
                clsDir.getAbsolutePath());

        List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num jobs", 4, inputs.size());
    }

    /**
     * Test case for initializing batch inputs automatically, from more than one parameter.
     */
    @Test
    public void testDeduceBatchParamsMulti() throws GpServerException {
        final File batchDir=FileUtil.getDataFile("all_aml/");
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        jobInputHelper.setDeduceBatchValues(true);
        jobInputHelper.addValue("input.file", batchDir.getAbsolutePath());
        jobInputHelper.addValue("cls.file", batchDir.getAbsolutePath());
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 2, inputs.size());
    }
    
    /**
     * Test case for a missing batch input parameter, for instance, 
     *     addBatchDirectory("input.filename", directory) with CMS.
     *     CMS has an "input.file" parameter, but not an "input.filename" parameter.
     */
    @Test
    public void testMissingBatchInputParameter() {
        final File batchDir=FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/res/");
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.filename", 
                batchDir.getAbsolutePath());
        try {
            //final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            Assert.fail("Expecting GpServerException: empty batch directory");
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
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.filename", 
                batchDir.getAbsolutePath());
        try {
            //final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            Assert.fail("Expecting GpServerException: empty batch directory");
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
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.filename", 
                batchDir.getAbsolutePath());
        try {
            //final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            Assert.fail("Expecting GpServerException: batch directory doesn't exist");
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
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", batchDir.getAbsolutePath());
        try {
            //final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            Assert.fail("Expecting GpServerException: empty batch directory");
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
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", batchDir.getAbsolutePath());
        jobInputHelper.addBatchDirectory("cls.file", batchDir.getAbsolutePath());
        try {
            //final List<JobInput> inputs=
                    jobInputHelper.prepareBatch();
            Assert.fail("Expecting GpServerException: empty batch directory");
        }
        catch (GpServerException e) {
            //expected
        }
    }

    /**
     * Test case for list of files provided as input to one batch input parameter
     */
    @Test
    public void testMultipleFilePerBatchParam() throws GpServerException
    {
        final File file1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_03/a_test.cls");
        final File file2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_03/c_test.gct");
        final File file3 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/01/b.txt");
        final File file4 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/cls/all_aml_test.cls");
        final File file5 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/03/i.txt");

        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
        jobInputHelper.addBatchValue("input.filename", file1.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", file2.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", file3.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", file4.getAbsolutePath());
        jobInputHelper.addBatchValue("input.filename", file5.getAbsolutePath());

        //Here we expect 5 batch jobs to be created
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 5, inputs.size());
    }

    /**
     * Test case for list of directories provided as input to one batch input parameter
     */
    @Test
    public void testMultipleDirsPerBatchParam() throws GpServerException
    {
        final File dir1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/01");
        final File dir2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/02/");
        final File dir3 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_02/03/");

        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.filename", dir1.getAbsolutePath());
        jobInputHelper.addBatchDirectory("input.filename", dir2.getAbsolutePath());
        jobInputHelper.addBatchDirectory("input.filename", dir3.getAbsolutePath());

        //Here we expect 9 batch jobs to be created, one for each file in the directory
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 9, inputs.size());
    }


    /**
     * Test case for list of files provided as input to multiple batch input parameters
     */
   /* @Test
    public void testMultipleFilesForMultipleBatchParam() throws GpServerException
    {
        final File param1_file1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/gct/all_aml_test.gct");
        final File param1_file2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/res/all_aml_train.res");
        final File param1_file3 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/gct/b_test.gct");

        final File param2_file1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/cls/all_aml_train.cls");
        final File param2_file2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/cls/all_aml_test.cls");


        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchValue("input.file", param1_file1.getAbsolutePath());
        jobInputHelper.addBatchValue("input.file", param1_file2.getAbsolutePath());
        jobInputHelper.addBatchValue("input.file", param1_file3.getAbsolutePath());
        jobInputHelper.addBatchValue("cls.file", param2_file1.getAbsolutePath());
        jobInputHelper.addBatchValue("cls.file", param2_file2.getAbsolutePath());

        //Here we expect 2 batch jobs to be created since there are only two pairs of matching file base names
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 2, inputs.size());
    }*/

    /**
     * Test case for list of directories provided as input to multiple batch input parameters
     */
    /*@Test
    public void testMultipleDirsForMultipleBatchParam() throws GpServerException
    {
        final File param1_dir1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/gct/");
        final File param1_dir2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_03/");

        final File param2_dir1 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_04/cls/");
        final File param2_dir2 = FileUtil.getSourceFile(TestJobInputHelper.class, "batch_01/cls/");


        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        jobInputHelper.addBatchDirectory("input.file", param1_dir1.getAbsolutePath());
        jobInputHelper.addBatchDirectory("input.file", param1_dir2.getAbsolutePath());
        jobInputHelper.addBatchValue("cls.file", param2_dir1.getAbsolutePath());
        jobInputHelper.addBatchValue("cls.file", param2_dir2.getAbsolutePath());

        //Here we expect 2 batch jobs to be created since there are only two pairs of matching file base names
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num batch jobs", 4, inputs.size());
    }*/

//    /**
//     * Test case for a batch input directory, make sure the current user can ready the input directory.
//     * 
//     *     TODO: non-admin user using a folder in a different user's upload tab
//     *     TODO: non-admin user looking at a server file path, which is not in their allowed list of available server file paths
//     *     TODO: non-admin user who doesn't have permission to look at server file paths
//     */
//    @Test
//    public void testBatchDirPermissionsCheck() {
//        throw new IllegalArgumentException("Test not implemented!");
//    }
//
//    /**
//     * Test case for an input directory for a non-batch parameter which doesn't accept a directory. 
//     * For instance, drag a directory from the uploads tab to the 'input.file' param of the CLE module.
//     * 
//     * TODO: implement fix for this failing test-case
//     */
//    @Test
//    public void testNonBatchInputDirectory() {
//        final File batchDir=FileUtil.getDataFile("all_aml/");
//        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cleLsid, null, taskLoader);
//        jobInputHelper.addValue("input.filename", 
//                batchDir.getAbsolutePath());
//        try {
//            //final List<JobInput> inputs=
//                    jobInputHelper.prepareBatch();
//            Assert.fail("Expecting GpServerException: directory value for a file input parameter");
//        }
//        catch (GpServerException e) {
//            //expected
//        }
//    }

}
