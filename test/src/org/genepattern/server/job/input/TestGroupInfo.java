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
    
    @Test
    public void testParamInfoParser() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileParam("input.file", "Demo file grouping parameter", false);
        pinfo.getAttributes().put(GroupInfo.PROP_NUM_GROUPS, "0+");
        pinfo.getAttributes().put(GroupInfo.PROP_GROUP_COLUMN_LABEL, "sample type");
        pinfo.getAttributes().put(GroupInfo.PROP_FILE_COLUMN_LABEL, "replicate");

        final GroupInfo groupInfo=new GroupInfo.Builder().fromParameterInfo(pinfo).build();
        Assert.assertNotNull("", groupInfo);
        Assert.assertEquals("minNumGroups", 0, (int) groupInfo.getMinNumGroups());
    }

}
