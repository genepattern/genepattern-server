package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.junitutil.TaskUtil;
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
public class TestParamListHelperGetInitialValues {
    private static TaskInfo taskInfo;
    private static String lsid;
    private static ParameterInfo[] paramInfos;
    
    private JobInput reloadedValues;
    private String _fileParam;
    private String _formatParam;
    private Map<String,String[]> parameterMap;
    private LinkedHashMap<String,List<String>> expectedValues;

    @BeforeClass
    static public void initClass() {
        taskInfo = TaskUtil.getTaskInfoFromZip(TestParamListHelperGetInitialValues.class, "ComparativeMarkerSelection_v9.zip");
        lsid = taskInfo.getLsid();
        paramInfos=taskInfo.getParameterInfoArray();
    }
    
    @Before
    public void init() {
        reloadedValues=null; //not a reloaded job
        _fileParam=null; //not from a sendTo file menu
        _formatParam=null; //not from a sendTo file menu
        parameterMap=Collections.emptyMap();
        
        expectedValues=new LinkedHashMap<String,List<String>>();
        initVal("input.file", "");
        initVal("cls.file", "");
        initVal("confounding.variable.cls.file", "");
        initVal("test.direction", "2");
        initVal("test.statistic","0");
        initVal("min.std", "");
        initVal("number.of.permutations","10000");
        initVal("log.transformed.data","false");
        initVal("complete","false");
        initVal("balanced","false");
        initVal("random.seed","779948241");
        initVal("smooth.p.values","true");
        initVal("phenotype.test","one versus all");
        initVal("output.filename","<input.file_basename>.comp.marker.odf");
    }

    /**
     * Helper method to put a list of values into the expectedDefaultValues map.
     * To avoid Java boilerplate code.
     * @param pname
     * @param pval
     */
    private void initVal(final String pname, final String pval) {
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
        JSONObject actualInitialValues=ParamListHelper.getInitialValuesJson(
                lsid, paramInfos, reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(expectedValues, actualInitialValues);
    }
    
    @Test
    public void testFromSendToMenu() throws Exception {
        _fileParam="http://127.0.0.1:8080/gp/jobResults/688040/CEL_IK50.cvt.gct";
        _formatParam="gct";
        //expecting input.file to match the _fileParam
        expectedValues.put("input.file", new ArrayList<String>(Arrays.asList( _fileParam )));
        JSONObject actualInitialValues=ParamListHelper.getInitialValuesJson(
                lsid, paramInfos, reloadedValues, _fileParam, _formatParam, parameterMap);        
        checkResults(expectedValues, actualInitialValues);
    }
    
    @Test
    public void testFromSendToMenuFormatParamNotSet() throws Exception {
        _fileParam="http://127.0.0.1:8080/gp/jobResults/688040/CEL_IK50.cvt.gct";
        //_formatParam="gct";
        //expecting input.file to match the _fileParam
        expectedValues.put("input.file", new ArrayList<String>(Arrays.asList( _fileParam )));
        JSONObject actualInitialValues=ParamListHelper.getInitialValuesJson(
                lsid, paramInfos, reloadedValues, _fileParam, _formatParam, parameterMap);        
        checkResults(expectedValues, actualInitialValues);
    }
    
    @Test
    public void testFromRequestParam() throws Exception {
        final String inputFile="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct";
        parameterMap=new HashMap<String,String[]>();
        parameterMap.put("input.file", new String[] {inputFile} );
        //expecting input.file to match the request parameter
        expectedValues.put("input.file", new ArrayList<String>(Arrays.asList( inputFile )));
        JSONObject actualInitialValues=ParamListHelper.getInitialValuesJson(
                lsid, paramInfos, reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(expectedValues, actualInitialValues);
    }
    
    @Test
    public void testNoParameters() throws Exception {
        expectedValues.clear();
        JSONObject actualInitialValues=ParamListHelper.getInitialValuesJson(lsid, new ParameterInfo[] {}, reloadedValues, _fileParam, _formatParam, parameterMap);
        checkResults(expectedValues, actualInitialValues);
    }
    
}
