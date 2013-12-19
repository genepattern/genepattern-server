package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.Param;
import org.junit.Test;

/**
 * jUnit tests for the JobInput class.
 * @author pcarr
 *
 */
public class TestJobInput {
    public static final String GP_URL="http://127.0.0.1:8080/gp/";
    public static final String DATA_URL="ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/";
    private static final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00006:0.7";


    @Test
    public void testSingleInputValue() {
        final JobInput jobInput = new JobInput();
        jobInput.setLsid(lsid);
        jobInput.addValue("requiredFile", DATA_URL+"all_aml_test.cls");
        jobInput.addValue("optionalFile", DATA_URL+"all_aml_test.gct");
        jobInput.addValue("inputList", DATA_URL+"all_aml_test.res");

        Assert.assertEquals("numParams", 3, jobInput.getParams().size());

        Assert.assertEquals("requiredFile.numValues", 1,  
                jobInput.getParam("requiredFile").getNumValues());
        Assert.assertEquals("requiredFile.numGroups", 1, 
                jobInput.getParam("requiredFile").getNumGroups());
        Assert.assertEquals("optionalFile.numValues", 1,  
                jobInput.getParam("optionalFile").getNumValues());
        Assert.assertEquals("optionalFile.numGroups", 1, 
                jobInput.getParam("optionalFile").getNumGroups());
        Assert.assertEquals("inputList.numValues", 1,  
                jobInput.getParam("inputList").getNumValues());
        Assert.assertEquals("inputList.numGroups", 1, 
                jobInput.getParam("inputList").getNumGroups());
  
        Assert.assertNull("missingParam", jobInput.getParam("missingParam"));
    }
    
    @Test
    public void testMultiInputValues() {
        final JobInput jobInput = new JobInput();
        jobInput.setLsid(lsid);
        jobInput.addValue("requiredFile", DATA_URL+"all_aml_test.cls");
        jobInput.addValue("optionalFile", DATA_URL+"all_aml_test.gct");
        jobInput.addValue("inputList", DATA_URL+"all_aml_train.res");
        jobInput.addValue("inputList", DATA_URL+"all_aml_train.cls");
        jobInput.addValue("inputList", DATA_URL+"all_aml_train.gct");
        Assert.assertEquals("numParams", 3, jobInput.getParams().size());
        Assert.assertEquals("inputList.numValues", 3,  
                jobInput.getParam("inputList").getNumValues());
        Assert.assertEquals("inputList.numGroups", 1, 
                jobInput.getParam("inputList").getNumGroups());
        
        //double check order
        Assert.assertEquals("inputList[0]", DATA_URL+"all_aml_train.res",
                jobInput.getParam("inputList").getValues().get(0).getValue());
        Assert.assertEquals("inputList[1]", DATA_URL+"all_aml_train.cls",
                jobInput.getParam("inputList").getValues().get(1).getValue());
        Assert.assertEquals("inputList[2]", DATA_URL+"all_aml_train.gct",
                jobInput.getParam("inputList").getValues().get(2).getValue());
    }
    
    @Test
    public void testSingleNamedGroup() {
        final JobInput jobInput = new JobInput();
        jobInput.setLsid(lsid);
        jobInput.addValue("requiredFile", DATA_URL+"all_aml_test.cls");
        jobInput.addValue("optionalFile", DATA_URL+"all_aml_test.gct");
        jobInput.addValue("inputList", DATA_URL+"all_aml_train.res", new GroupId("single group"));
        jobInput.addValue("inputList", DATA_URL+"all_aml_train.cls", new GroupId("single group"));
        jobInput.addValue("inputList", DATA_URL+"all_aml_train.gct", new GroupId("single group"));
        Assert.assertEquals("numParams", 3, jobInput.getParams().size());
        
        final Param param=jobInput.getParam("inputList");
        Assert.assertEquals("numGroups", 1, param.getNumGroups());
        Assert.assertEquals("numValues", 3, param.getNumValues());
        //double check order
        Assert.assertEquals("inputList[0]", DATA_URL+"all_aml_train.res",
                jobInput.getParam("inputList").getValues().get(0).getValue());
        Assert.assertEquals("inputList[1]", DATA_URL+"all_aml_train.cls",
                jobInput.getParam("inputList").getValues().get(1).getValue());
        Assert.assertEquals("inputList[2]", DATA_URL+"all_aml_train.gct",
                jobInput.getParam("inputList").getValues().get(2).getValue());
    }
    
    @Test
    public void testMultipleGroups() {
        final JobInput jobInput = new JobInput();
        jobInput.setLsid(lsid);
        jobInput.addValue("inputList", DATA_URL+"all_aml_train.res", new GroupId("train"));
        jobInput.addValue("inputList", DATA_URL+"all_aml_test.res", new GroupId("test"));
        jobInput.addValue("inputList", DATA_URL+"all_aml_test.cls", new GroupId("TEST"));
        jobInput.addValue("inputList", DATA_URL+"all_aml_test.gct", new GroupId(" test "));
        jobInput.addValue("inputList", DATA_URL+"all_aml_train.cls", new GroupId(" train "));
        jobInput.addValue("inputList", DATA_URL+"all_aml_train.gct", new GroupId("Train"));
        
        final Param param=jobInput.getParam("inputList");
        Assert.assertEquals("numGroups", 2, param.getNumGroups());
        Assert.assertEquals("numValues", 6, param.getNumValues());
        
        //expect groups in order
        Assert.assertEquals("groups[0]", new GroupId("train"),
                param.getGroups().get(0));
        Assert.assertEquals("groups[1]", new GroupId("test"),
                param.getGroups().get(1));

        //check values
        Assert.assertEquals("inputList[train][0]",
                DATA_URL+"all_aml_train.res",
                param.getValuesInGroup(new GroupId("train")).get(0).getValue());
        Assert.assertEquals("inputList[train][1]",
                DATA_URL+"all_aml_train.cls",
                param.getValuesInGroup(new GroupId("train")).get(1).getValue());
        Assert.assertEquals("inputList[train][2]",
                DATA_URL+"all_aml_train.gct",
                param.getValuesInGroup(new GroupId("train")).get(2).getValue());
        Assert.assertEquals("inputList[test][0]",
                DATA_URL+"all_aml_test.res",
                param.getValuesInGroup(new GroupId("test")).get(0).getValue());
        Assert.assertEquals("inputList[test][1]",
                DATA_URL+"all_aml_test.cls",
                param.getValuesInGroup(new GroupId("test")).get(1).getValue());
        Assert.assertEquals("inputList[test][2]",
                DATA_URL+"all_aml_test.gct",
                param.getValuesInGroup(new GroupId("test")).get(2).getValue());
        
        //entries are in order
        final List<ParamValue> expectedValues=new ArrayList<ParamValue>();
        expectedValues.add(new ParamValue(DATA_URL+"all_aml_train.res"));
        expectedValues.add(new ParamValue(DATA_URL+"all_aml_test.res"));
        expectedValues.add(new ParamValue(DATA_URL+"all_aml_test.cls"));
        expectedValues.add(new ParamValue(DATA_URL+"all_aml_test.gct"));
        expectedValues.add(new ParamValue(DATA_URL+"all_aml_train.cls"));
        expectedValues.add(new ParamValue(DATA_URL+"all_aml_train.gct"));
        final List<GroupId> expectedGroups=new ArrayList<GroupId>();
        expectedGroups.add(new GroupId("train"));
        expectedGroups.add(new GroupId("test"));
        expectedGroups.add(new GroupId("test"));
        expectedGroups.add(new GroupId("test"));
        expectedGroups.add(new GroupId("train"));
        expectedGroups.add(new GroupId("train"));
        
        int i=0;
        for(final Entry<GroupId,ParamValue> entry : param.getValuesAsEntries()) {
            Assert.assertEquals("entry["+i+"].group", expectedGroups.remove(0), entry.getKey());
            Assert.assertEquals("entry["+i+"]", expectedValues.remove(0), entry.getValue());
            ++i;
        }
    }

}
