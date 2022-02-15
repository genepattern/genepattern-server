/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.genepattern.junitutil.Demo;
import org.genepattern.server.webapp.rest.api.v1.task.TasksResource;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

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
    ChoiceInfo choiceInfo=mock(ChoiceInfo.class);

    @Test
    public void nullChoiceInfo()  throws JSONException {
        final String href="";
        JSONObject jsonObj = ChoiceInfoHelper.initChoiceInfoJson(href, choiceInfo);
        assertNotNull(jsonObj);
    }
    
    @Test
    public void dynamicDropDown() throws JSONException {
        final String lsid=Demo.cleLsid;
        final String pname="my.parameter";
        final String choiceDir="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/";
        final String baseHref="http://127.0.0.1:8080/gp";
        final String taskHref=baseHref + "/rest/"+TasksResource.URI_PATH+"/"+lsid;
        when(choiceInfo.getChoiceDir()).thenReturn(choiceDir);
        when(choiceInfo.getParamName()).thenReturn(pname);
        final JSONObject jsonObj = ChoiceInfoHelper.initChoiceInfoJson(taskHref, choiceInfo);
        final String expectedHref=taskHref+"/"+pname+"/choiceInfo.json";
       assertEquals("href", expectedHref, jsonObj.getString("href"));
        assertEquals("choiceDir", choiceDir, jsonObj.getString("choiceDir"));
    }
}
