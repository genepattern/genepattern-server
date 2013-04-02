package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.junitutil.TaskUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * jUnit tests for initializing the values for the job input form.
 * 
 * @author pcarr
 *
 */
public class TestParamListHelperGetInitialValues {
    private TaskInfo taskInfo;
    private LinkedHashMap<String,List<String>> expectedDefaultValues;
    //ParameterInfo pinfo;
    
    @Before
    public void init() {
        taskInfo = TaskUtil.getTaskInfoFromZip(this.getClass(), "ComparativeMarkerSelection_v9.zip");
        expectedDefaultValues=new LinkedHashMap<String,List<String>>();
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
        expectedDefaultValues.put(pname, pvalList);
    }
    
    @Test
    public void testFromDefaultValues() {
        final ParameterInfo[] paramInfos=taskInfo.getParameterInfoArray();
        final JobInput reloadedValues=null; //not a reloaded job
        final String _fileParam=null; //not from a sendTo file menu
        final String _formatParam=null; //not from a sendTo file menu
        final Map<String,String[]> parameterMap=new HashMap<String,String[]>();
        JSONObject initialValues=null;
        try {
            initialValues=ParamListHelper.getInitialValues(
                paramInfos, reloadedValues, _fileParam, _formatParam, parameterMap);
        }
        catch (JSONException e) {
            Assert.fail(e.getLocalizedMessage());
        }
        catch (Exception e) {
            Assert.fail(e.getLocalizedMessage());
        }
        
        Assert.assertNotNull("initialValues is null", initialValues);
    }
    
}
