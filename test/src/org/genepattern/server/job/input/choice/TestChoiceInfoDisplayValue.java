/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.genepattern.webservice.ParameterInfo;
import org.junit.Before;
import org.junit.Test;

/**
 * junit test cases for initializing the display value for a drop-down parameter from the actual value saved to the DB.
 */
public class TestChoiceInfoDisplayValue {
    private static final String choiceSpec="=no;-ng=yes;dupeValue=item one;dupeValue=item two;value one=dupeDisplay;value two=dupeDisplay";
    private ParameterInfo formalParam;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void setUp() {
        HashMap attributes=new HashMap();
        attributes.put("default_value", "");
        attributes.put("name", "row.normalize");
        attributes.put("optional", "on");
        attributes.put("type", "java.lang.String");
        formalParam=new ParameterInfo();
        formalParam.setValue(choiceSpec);
        formalParam.setAttributes(attributes);
    }
    
    @Test
    public void getDisplayValue() {
        assertEquals("yes", ChoiceInfo.getDisplayValueForActualValue("-ng", formalParam));
    }

    @Test
    public void getDisplayValue_emptyValue() {
        assertEquals("no", ChoiceInfo.getDisplayValueForActualValue("", formalParam));
    }

    @Test
    public void getDisplayValue_missingValue() {
        assertEquals(null, ChoiceInfo.getDisplayValueForActualValue("missingValue", formalParam));
    }

    @Test
    public void getDisplayValue_duplicateValue() {
        assertEquals(null, ChoiceInfo.getDisplayValueForActualValue("dupeValue", formalParam));
    }
    
    @Test
    public void getDisplayValue_duplicateDisplayValue() {
        assertEquals("dupeDisplay", ChoiceInfo.getDisplayValueForActualValue("value one", formalParam));
        assertEquals("dupeDisplay", ChoiceInfo.getDisplayValueForActualValue("value two", formalParam));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void getDisplayValue_staticChoices() {
        formalParam.setValue(""); //<--- unset the setup
        formalParam.getAttributes().put(ChoiceInfo.PROP_CHOICE, choiceSpec);
        assertEquals("yes", ChoiceInfo.getDisplayValueForActualValue("-ng", formalParam));
        assertEquals("no", ChoiceInfo.getDisplayValueForActualValue("", formalParam));
        assertEquals(null, ChoiceInfo.getDisplayValueForActualValue("missingValue", formalParam));
        assertEquals(null, ChoiceInfo.getDisplayValueForActualValue("dupeValue", formalParam));
        assertEquals("dupeDisplay", ChoiceInfo.getDisplayValueForActualValue("value one", formalParam));
        assertEquals("dupeDisplay", ChoiceInfo.getDisplayValueForActualValue("value two", formalParam));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void getDisplayValue_skipDynamicDropDown() {
        //         final String choiceDir = (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR);
        HashMap attributes=new HashMap();
        attributes.put(ChoiceInfo.PROP_CHOICE_DIR, "ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/");
        formalParam=new ParameterInfo();
        formalParam.setAttributes(attributes);

        assertEquals(null,
            ChoiceInfo.getDisplayValueForActualValue("ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt", 
                formalParam));
    }
    
    @Test
    public void getDisplayValue_skipNoDropDown() {
        formalParam.setValue("");
        assertEquals(null,
            ChoiceInfo.getDisplayValueForActualValue("user_value", 
                formalParam));
    }

}
