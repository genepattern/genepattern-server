/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import javax.servlet.http.HttpServletRequest;

import org.genepattern.webservice.TaskInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * junit tests for initializing the JSON representation for a drop-down menu for a module input parameter.
 * Use cases:
 * (1) just get the meta-data, don't expand the drop-down
 * (2) expand the drop-down, unless it's a dynamic drop-down
 * (3) expand the drop-down, including dynamic drop-down
 * 
 * @author pcarr
 *
 */
public class TestChoiceInfoToJson {
    private String choiceDir="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/";
    HttpServletRequest request=null;
    TaskInfo taskInfo=null;
    ChoiceInfo choiceInfo=Mockito.mock(ChoiceInfo.class);

    @Before
    public void setUp() {
    }
    
    @Test
    public void nullChoiceInfo()  throws JSONException {
        
        JSONObject jsonObj = ChoiceInfoHelper.initChoiceInfoJson(request, taskInfo, choiceInfo);
        Assert.assertNotNull(jsonObj);
    }
    
    @Test
    public void dynamicDropDown() throws JSONException {
        Mockito.when(choiceInfo.getChoiceDir()).thenReturn(choiceDir);
        JSONObject jsonObj = ChoiceInfoHelper.initChoiceInfoJson(request, taskInfo, choiceInfo);
        Assert.assertEquals("choiceDir", choiceDir, 
                jsonObj.getString("choiceDir"));
        
    }
}
