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
    final String userId="test";
    final Context userContext = ServerConfiguration.Context.getContextForUser(userId);
    
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
        taskLoader=new TaskLoader();
        taskLoader.addTask(TestJobInputHelper.class, "ConvertLineEndings_v1.zip");
        taskLoader.addTask(TestJobInputHelper.class, "ComparativeMarkerSelection_v9.zip");
        taskLoader.addTask(TestJobInputHelper.class, "ListFiles_v0.7.zip");
    }
    
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
        
        jobInputHelper.addBatchValue("input.file", createUserUploadRef(userId, "01.gct"));
        jobInputHelper.addBatchValue("cls.file", createUserUploadRef(userId, "01.cls"));
        jobInputHelper.addBatchValue("input.file", createUserUploadRef(userId, "02.gct"));
        jobInputHelper.addBatchValue("cls.file", createUserUploadRef(userId, "02.cls"));
        final List<JobInput> inputs=jobInputHelper.prepareBatch();
        Assert.assertEquals("num jobs", 2, inputs.size());
        
        int idx=0;
        for(JobInput input : inputs) {
            ++idx;
            Param inputFile=input.getParam("input.file");
            Param clsFile=input.getParam("cls.file");
            Assert.assertEquals("num 'input.file'", 1, inputFile.getNumValues());
            Assert.assertEquals("input.file["+idx+"]", createUserUploadRef(userId,"0"+idx+".gct"), inputFile.getValues().get(0).getValue());
            Assert.assertEquals("num 'cls.file'", 1, clsFile.getNumValues());
            Assert.assertEquals("cls.file["+idx+"]", createUserUploadRef(userId,"0"+idx+".cls"), clsFile.getValues().get(0).getValue());
        }
    }
    
    /**
     * Test a batch job with multiple input parameters, but only one value for each batch parameter.
     * @throws GpServerException
     */
    @Test
    public void testAddBatchMultiParamOneValue() throws GpServerException {
        final JobInputHelper jobInputHelper=new JobInputHelper(userContext, cmsLsid, null, taskLoader);
        
        jobInputHelper.addBatchValue("input.file", createUserUploadRef(userId, "01.gct"));
        jobInputHelper.addBatchValue("cls.file", createUserUploadRef(userId, "01.cls"));
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
    
    

}
