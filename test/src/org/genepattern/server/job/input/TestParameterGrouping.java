/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.junit.Test;

/**
 * Created by nazaire on 2/11/14.
 */
public class TestParameterGrouping
{
    /**
     * Mock a TaskInfo as if it were loaded from the TestBasicAdvancedParameters_v4.zip file.
     */
    protected static TaskInfo initMockTaskInfo() {
        return initMockTaskInfo(new String[] {
                "basic.required.parameter.1", 
                "basic.required.parameter.2",
                "basic.parameter.1",
                "basic.parameter.2",
                "advanced.parameter.1",
                "advanced.parameter.2"
        });
    }
    
    /**
     * General utility method for created a mock TaskInfo
     * when the method under test extracts a list of parameter names 
     * from the TaskInfo#parameterInfoArray.
     * 
     * @param pnames parameter names declared in the manifest file
     * @return a new mock TaskInfo
     */
    protected static TaskInfo initMockTaskInfo(final String[] pnames) {
        final List<ParameterInfo> pinfos = new ArrayList<ParameterInfo>();
        for(final String pname : pnames) {
            ParameterInfo mockPinfo = mock(ParameterInfo.class);
            when(mockPinfo.getName()).thenReturn(pname);
            pinfos.add(mockPinfo);
        }
        final ParameterInfo[] mockPinfos=pinfos.toArray(new ParameterInfo[pnames.length]);
        // ... extracted from TaskInfo object
        final TaskInfo taskInfo = mock(TaskInfo.class);
        when(taskInfo.getParameterInfoArray()).thenReturn(mockPinfos);
        
        return taskInfo;
    }

    protected TaskInfo initTaskInfoFromZip() {
        return TaskUtil.getTaskInfoFromZip(this.getClass(), "TestBasicAdvancedParameters_v4.zip");
    }
    
    @Test
    public void testNumParamGroups() throws Exception
    {
        final TaskInfo taskInfo = initMockTaskInfo();
        //final TaskInfo taskInfo = initTaskInfoFromZip();
        final File paramGroupsFile = FileUtil.getSourceFile(this.getClass(), "TestBasicAdvancedParameters_v4_paramgroups.json");

        //check that there were three parameter groups defined
        final JSONArray paramGroupsJson = LoadModuleHelper.getParameterGroupsJson(taskInfo, paramGroupsFile);
        assertEquals("num param groups", paramGroupsJson.length(), 3);
    }
}
