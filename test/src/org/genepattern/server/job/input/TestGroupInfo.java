package org.genepattern.server.job.input;

import org.genepattern.webservice.ParameterInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * junit tests for the GroupInfo class.
 * @author pcarr
 *
 */
public class TestGroupInfo {
    public static final ParameterInfo createGroupParam(final String numGroups) {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileParam("input.file", "Demo file grouping parameter", false);
        pinfo.getAttributes().put(NumValues.PROP_NUM_VALUES, "0+");
        pinfo.getAttributes().put(GroupInfo.PROP_NUM_GROUPS, numGroups);
        //pinfo.getAttributes().put(GroupInfo.PROP_GROUP_COLUMN_LABEL, "sample type");
        //pinfo.getAttributes().put(GroupInfo.PROP_FILE_COLUMN_LABEL, "replicate");
        return pinfo;
    }
    
    @Test
    public void testParamInfoParser() {
        final ParameterInfo pinfo=createGroupParam("0+");
        final GroupInfo groupInfo=new GroupInfo.Builder().fromParameterInfo(pinfo).build();
        Assert.assertNotNull("", groupInfo);
        Assert.assertEquals("minNumGroups", 0, (int) groupInfo.getMinNumGroups());
        Assert.assertEquals("maxNumGroups", (Integer) null, (Integer) groupInfo.getMaxNumGroups());
        Assert.assertEquals("groupColumnLabel", (String) null, groupInfo.getGroupColumnLabel());
        Assert.assertEquals("fileColumnLabel", (String) null, groupInfo.getFileColumnLabel());
    }
    
    @Test
    public void testNoGroups() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileParam("input.file", "Demo file grouping parameter", false);
        final GroupInfo groupInfo=new GroupInfo.Builder().fromParameterInfo(pinfo).build();
        Assert.assertNull("by default (including all legacy modules), the groupInfo object is null", groupInfo);
    }
    
    @Test
    public void testFixedNumGroups() {
        final ParameterInfo pinfo=createGroupParam("2");
        final GroupInfo groupInfo=new GroupInfo.Builder().fromParameterInfo(pinfo).build();
        Assert.assertNotNull("", groupInfo);
        Assert.assertEquals("minNumGroups", Integer.valueOf(2), groupInfo.getMinNumGroups());
        Assert.assertEquals("maxNumGroups", Integer.valueOf(2), groupInfo.getMaxNumGroups());        
    }
    
    @Test
    public void testGroupRange() {
        final ParameterInfo pinfo=createGroupParam("1..10");
        final GroupInfo groupInfo=new GroupInfo.Builder().fromParameterInfo(pinfo).build();
        Assert.assertNotNull("", groupInfo);
        Assert.assertEquals("minNumGroups", Integer.valueOf(1), groupInfo.getMinNumGroups());
        Assert.assertEquals("maxNumGroups", Integer.valueOf(10), groupInfo.getMaxNumGroups());        
    }

}
