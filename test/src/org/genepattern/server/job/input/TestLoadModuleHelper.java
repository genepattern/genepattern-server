package org.genepattern.server.job.input;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.drm.JobRunner;
import org.genepattern.junitutil.JobInfoLoaderFromMap;
import org.genepattern.junitutil.TaskLoader;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.JobInfoLoader;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit tests for initializing the values for the job input form.
 * 
 * @author pcarr
 *
 */
public class TestLoadModuleHelper {
    private static TaskLoader taskLoader;
    private static JobInfoLoader jobInfoLoader;
    private static String adminUserId;
    private static GpContext userContext;
    private static URL gpUrl;

    final private static String cmsLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9";    
    final private static String ecmrLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00046:2"; 
    final private static String testLsid="urn:lsid:broad.mit.edu:cancer.software.genepatterntest.module.analysis:00001:1";
    
    private JobInput reloadedValues;
    private String _fileParam;
    private String _formatParam;
    private Map<String,String[]> parameterMap;

    @BeforeClass
    static public void beforeClass() {
        adminUserId="admin";
        userContext=new GpContextFactory.Builder()
            .userId(adminUserId)
            .isAdmin(true)
            .build();
        taskLoader=new TaskLoader();
        taskLoader.addTask(TestLoadModuleHelper.class, "ComparativeMarkerSelection_v9.zip");
        taskLoader.addTask(TestLoadModuleHelper.class, "ExtractComparativeMarkerResults_v2.zip");

        jobInfoLoader = new JobInfoLoaderFromMap();
        
        gpUrl=GpFilePath.getGenePatternUrl();
    }
    
    @Before
    public void init() {
        reloadedValues=null; //not a reloaded job
        _fileParam=null; //not from a sendTo file menu
        _formatParam=null; //not from a sendTo file menu
        parameterMap=Collections.emptyMap();
    }    

    private LinkedHashMap<String,List<String>> initCms() {
        LinkedHashMap<String,List<String>> expectedValues=new LinkedHashMap<String,List<String>>();
        //initVal(expectedValues, "input.file", "");
        //initVal(expectedValues, "cls.file", "");
        //initVal(expectedValues, "confounding.variable.cls.file", "");
        initVal(expectedValues, "test.direction", "2");
        initVal(expectedValues, "test.statistic","0");
        initVal(expectedValues, "min.std", "");
        initVal(expectedValues, "number.of.permutations","10000");
        initVal(expectedValues, "log.transformed.data","false");
        initVal(expectedValues, "complete","false");
        initVal(expectedValues, "balanced","false");
        initVal(expectedValues, "random.seed","779948241");
        initVal(expectedValues, "smooth.p.values","true");
        initVal(expectedValues, "phenotype.test","one versus all");
        initVal(expectedValues, "output.filename","<input.file_basename>.comp.marker.odf");
        
        return expectedValues;
    }
    
    private LinkedHashMap<String,List<String>> initEcmr() {
        LinkedHashMap<String,List<String>> expectedValues=new LinkedHashMap<String,List<String>>();
        //initVal(expectedValues, "comparative.marker.selection.filename", "");
        //initVal(expectedValues, "dataset.filename", "");
        initVal(expectedValues, "field", "");
        initVal(expectedValues, "min", "");
        initVal(expectedValues, "max", "");
        initVal(expectedValues, "number.of.neighbors", "");
        initVal(expectedValues, "base.output.name", "<comparative.marker.selection.filename_basename>.filt");

        return expectedValues;
    }

    /**
     * Helper method to put a list of values into the expectedDefaultValues map.
     * To avoid Java boilerplate code.
     * @param pname
     * @param pval
     */
    private void initVal(final LinkedHashMap<String,List<String>> expectedValues, final String pname, final String pval) {
        List<String> pvalList=new ArrayList<String>();
        pvalList.add(pval);
        expectedValues.put(pname, pvalList);
    }
    
    /**
     * Helper method to compare expected initial values (as a map) with
     * the actual initial values (as a JSON object).
     * 
     * @param expected
     * @param actual
     * @throws JSONException
     */
    private void checkResults(final Map<String,List<String>> expected, final JSONObject actual) throws JSONException {
        Assert.assertNotNull("initialValues", actual);
        //compare JSONArray with Map
        Assert.assertEquals("numValues", expected.size(), actual.length());
        for(Entry<String,List<String>> entry : expected.entrySet()) {
            final String pname=entry.getKey();
            JSONArray jsonValues=actual.getJSONArray(entry.getKey());
            Assert.assertEquals(""+pname+".numValues", entry.getValue().size(), jsonValues.length());
            int idx=0;
            for(final String expectedValueAtIdx : entry.getValue()) {
                Assert.assertEquals(""+pname+"["+idx+"]", expectedValueAtIdx, jsonValues.get(idx));
                ++idx;
            }
        }
    }
    
    @Test
    public void testFromDefaultValues() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        taskInfo.getLsid(), taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(initCms(), actualInitialValues);
    }

    @Test
    public void testFromSendToMenu() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        _fileParam=gpUrl.toExternalForm()+"/jobResults/688040/CEL_IK50.cvt.gct";
        _formatParam="gct";
        //expecting input.file to match the _fileParam
        final LinkedHashMap<String,List<String>> cmsExpectedValues=initCms();
        cmsExpectedValues.put("input.file", new ArrayList<String>(Arrays.asList( _fileParam )));
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        cmsLsid, taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(cmsExpectedValues, actualInitialValues);
    }
    
    @Test
    public void testFromSendToMenuFormatParamNotSet() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        _fileParam=gpUrl.toExternalForm()+"/jobResults/688040/CEL_IK50.cvt.gct";
        //expecting input.file to match the _fileParam
        final LinkedHashMap<String,List<String>> cmsExpectedValues=initCms();
        cmsExpectedValues.put("input.file", new ArrayList<String>(Arrays.asList( _fileParam )));
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        cmsLsid, taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(cmsExpectedValues, actualInitialValues);
    }
    
    /**
     * 
     * @throws Exception
     */
    @Test
    public void testFromRecendJobsTab() throws Exception {
        //&_file=http%3A%2F%2Fgpdev.broadinstitute.org%2Fgp%2FjobResults%2F50043%2Fall_aml_test.comp.marker.odf&_format=Comparative%20Marker%20Selection
        _fileParam=gpUrl.toExternalForm()+"/jobResults/50043/all_aml_test.comp.marker.odf";
        _formatParam="Comparative Marker Selection";
    }
    
    @Test
    public void testSendFromUploadsTab() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        _fileParam=gpUrl.toExternalForm()+"/users/test/all_aml_test.cls";
        _formatParam="cls";
        final LinkedHashMap<String,List<String>> cmsExpectedValues=initCms();
        cmsExpectedValues.put("cls.file", new ArrayList<String>(Arrays.asList( _fileParam )));
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        cmsLsid, taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(cmsExpectedValues, actualInitialValues);        
    }
    
    @Test
    public void testFromRequestParam() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        final String inputFile="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct";
        parameterMap=new HashMap<String,String[]>();
        parameterMap.put("input.file", new String[] {inputFile} );
        //expecting input.file to match the request parameter
        final LinkedHashMap<String,List<String>> cmsExpectedValues=initCms();
        cmsExpectedValues.put("input.file", new ArrayList<String>(Arrays.asList( inputFile )));
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        cmsLsid, taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(cmsExpectedValues, actualInitialValues);
    }
    
    @Test
    public void testNoParameters() throws Exception {
        final LinkedHashMap<String,List<String>> cmsExpectedValues=initCms();
        cmsExpectedValues.clear();
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        cmsLsid, new ParameterInfo[] {}, reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(cmsExpectedValues, actualInitialValues);
    }
    
    /**
     * Test sending a '.odf' file as a 'Comparative Marker Selection' file.
     * 
     * For example, send odf result file from a run of ComparativeMarkerSelection to the ExtractComparativeMarkerSelectionResults module.
     * 
     * @throws Exception
     */
    @Test
    public void testSendOdfAsComparativeMarkerSelection() throws Exception {
        _fileParam=gpUrl.toExternalForm()+"/jobResults/50043/all_aml_test.comp.marker.odf";
        _formatParam="Comparative Marker Selection";
        final LinkedHashMap<String,List<String>> expectedValues=initEcmr();
        expectedValues.put("comparative.marker.selection.filename", new ArrayList<String>(Arrays.asList( _fileParam )));
        //ExtractComparativeMarkerResults
        final TaskInfo taskInfo = taskLoader.getTaskInfo(ecmrLsid);
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        taskInfo.getLsid(), taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(expectedValues, actualInitialValues);
    }

    /**
     * Test case for sending an external url as a _fileParam in the HTTP request for loading a job.
     * 
     * @throws Exception
     */
    @Test
    public void testSendExternalUrl() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        _fileParam="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct";
        _formatParam="gct";
        final LinkedHashMap<String,List<String>> cmsExpectedValues=initCms();
        cmsExpectedValues.put("input.file", new ArrayList<String>(Arrays.asList( _fileParam )));
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        cmsLsid, taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(cmsExpectedValues, actualInitialValues);
    }
    
    /**
     * Test case for sending a file to a module from the GenomeSpace tab.
     * 
     * Example link,
     *     http://genepattern/gp/pages/index.jsf
     *        ?lsid=urn%3Alsid%3Abroad.mit.edu%3Acancer.software.genepattern.module.analysis%3A00044%3A9
     *        &_file=https%3A//dm.genomespace.org/datamanager/file/Home/pcarr/all_aml_test.gct
     *        &_format=gct
     */
    @Test
    public void testSendFromGenomeSpaceTab() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        _fileParam="https://dm.genomespace.org/datamanager/file/Home/pcarr/all_aml_test.gct";
        _formatParam="gct";
        final LinkedHashMap<String,List<String>> cmsExpectedValues=initCms();
        cmsExpectedValues.put("input.file", new ArrayList<String>(Arrays.asList( _fileParam )));
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        cmsLsid, taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(cmsExpectedValues, actualInitialValues);
    }
    
    @Test
    public void testSendFromGsTabSpecialChars() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        _fileParam="https://dm.genomespace.org/datamanager/file/Home/pcarr/all%20aml%20test.gct";
        _formatParam="gct";
        final LinkedHashMap<String,List<String>> cmsExpectedValues=initCms();
        cmsExpectedValues.put("input.file", new ArrayList<String>(Arrays.asList( _fileParam )));
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        cmsLsid, taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(cmsExpectedValues, actualInitialValues);
    }
    
//    /**
//     * Test case for sending a GenomeSpace file from the landing page to the job input form.
//     */
//    @Test
//    public void testSendFromGsLandingPage() {
//        //TODO: implement this test: Assert.fail("test not implemented");
//    }

    /**
     * Test case for sending an invalid _file= parameter in the HTTP request.
     */
    @Test
    public void testBogusFileParam() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        _fileParam="/xchip/gpdev/servers/shared_data/all_aml_test.gct";
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        JSONObject actualInitialValues=
                loadModuleHelper.getInitialValuesJson(
                        cmsLsid, taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        final LinkedHashMap<String,List<String>> cmsExpectedValues=initCms();
        checkResults(cmsExpectedValues, actualInitialValues);
    }

    /**
     * Test case for new (circa 3.8.0) file group feature.
     */
    @Test
    public void testAsJsonV2_singleValue() throws Exception {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(testLsid);
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls");
        JSONObject actual=LoadModuleHelper.asJsonV2(jobInput);
        
        Assert.assertNotNull(actual);
        
        JSONArray groupArray=actual.getJSONArray("input.file");
        Assert.assertEquals("num groups", 1, groupArray.length());
        JSONObject groupObj=groupArray.getJSONObject(0);
        Assert.assertEquals("group[0].groupid", "", groupObj.getString("groupid"));
        Assert.assertEquals("group[0].values.length", 1, groupObj.getJSONArray("values").length());
        Assert.assertEquals("group[0].value[0]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls", 
                groupObj.getJSONArray("values").get(0));
    }
    
    @Test
    public void testAsJsonV2_singleAnonymousGroup() throws Exception {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(testLsid);
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls");
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.gct");
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.cls");
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.gct");
        JSONObject actual=LoadModuleHelper.asJsonV2(jobInput);
        
        Assert.assertNotNull(actual);

        JSONArray groupArray=actual.getJSONArray("input.file");
        Assert.assertEquals("num groups", 1, groupArray.length());
        JSONObject groupObj=groupArray.getJSONObject(0);
        Assert.assertEquals("group[0].groupid", "", groupObj.getString("groupid"));
        Assert.assertEquals("group[0].length", 4, groupObj.getJSONArray("values").length());
        Assert.assertEquals("group[0].value[0]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls", 
                groupObj.getJSONArray("values").get(0));
        Assert.assertEquals("group[0].value[1]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.gct", 
                groupObj.getJSONArray("values").get(1));
        Assert.assertEquals("group[0].value[2]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.cls", 
                groupObj.getJSONArray("values").get(2));
        Assert.assertEquals("group[0].value[3]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.gct", 
                groupObj.getJSONArray("values").get(3));
    }

    @Test
    public void testAsJsonV2_singleNamedGroup() throws Exception {
        final JobInput jobInput=new JobInput();
        jobInput.setLsid(testLsid);
        final GroupId groupId=new GroupId("normal");
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls", groupId);
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.gct", groupId);
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.cls", groupId);
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.gct", groupId);
        JSONObject actual=LoadModuleHelper.asJsonV2(jobInput);
        
        Assert.assertNotNull(actual);

        JSONArray groupArray=actual.getJSONArray("input.file");
        Assert.assertEquals("num groups", 1, groupArray.length());
        JSONObject groupObj=groupArray.getJSONObject(0);
        Assert.assertEquals("group[0].groupid", groupId.getGroupId(), groupObj.getString("groupid"));
        Assert.assertEquals("group[0].length", 4, groupObj.getJSONArray("values").length());
        Assert.assertEquals("group[0].value[0]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls", 
                groupObj.getJSONArray("values").get(0));
        Assert.assertEquals("group[0].value[1]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.gct", 
                groupObj.getJSONArray("values").get(1));
        Assert.assertEquals("group[0].value[2]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.cls", 
                groupObj.getJSONArray("values").get(2));
        Assert.assertEquals("group[0].value[3]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.gct", 
                groupObj.getJSONArray("values").get(3));
    }

    @Test
    public void testAsJsonV2_groups() throws Exception {
        final JobInput jobInput=new JobInput();
        jobInput.setLsid(testLsid);
        final GroupId testGroup=new GroupId("test");
        final GroupId trainGroup=new GroupId("train");
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls", testGroup);
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.gct", testGroup);
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.cls", trainGroup);
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.gct", trainGroup);
        JSONObject actual=LoadModuleHelper.asJsonV2(jobInput);
        
        Assert.assertNotNull(actual);

        JSONArray groupArray=actual.getJSONArray("input.file");
        Assert.assertEquals("num groups", 2, groupArray.length());
        Assert.assertEquals("group[0].groupid", testGroup.getGroupId(), groupArray.getJSONObject(0).getString("groupid"));
        Assert.assertEquals("group[0].length", 2, groupArray.getJSONObject(0).getJSONArray("values").length());
        Assert.assertEquals("group[0].value[0]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls", 
                groupArray.getJSONObject(0).getJSONArray("values").get(0));
        Assert.assertEquals("group[0].value[1]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.gct", 
                groupArray.getJSONObject(0).getJSONArray("values").get(1));
        
        Assert.assertEquals("group[1].groupid", trainGroup.getGroupId(), groupArray.getJSONObject(1).getString("groupid"));
        Assert.assertEquals("group[1].length", 2, groupArray.getJSONObject(1).getJSONArray("values").length());
        Assert.assertEquals("group[1].value[0]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.cls", 
                groupArray.getJSONObject(1).getJSONArray("values").get(0));
        Assert.assertEquals("group[1].value[1]", "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_train.gct", 
                groupArray.getJSONObject(1).getJSONArray("values").get(1));
    }

    
    /**
     * For GP-4872, optionally include additional job configuration parameters on the job input form.
     */
    //TODO: convert to a @Test
    public void testPromptForJobConfigParam() throws Exception {
        final TaskInfo taskInfo=taskLoader.getTaskInfo(cmsLsid);
        LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext, taskLoader, jobInfoLoader);
        final JobInput initialValues=loadModuleHelper.getInitialValues(
                        taskInfo.getLsid(), taskInfo.getParameterInfoArray(), reloadedValues, _fileParam, _formatParam, parameterMap);
        
        Param jobQueue=initialValues.getParam(JobRunner.PROP_QUEUE);
        Assert.assertNotNull("jobQueue", jobQueue);

    }

}
