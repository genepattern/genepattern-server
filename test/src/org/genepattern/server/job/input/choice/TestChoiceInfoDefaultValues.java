package org.genepattern.server.job.input.choice;

import java.util.HashMap;

import org.genepattern.webservice.ParameterInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for initializing the ChoiceInfo, related to default_value, optional flag, and 
 * whether or not to append an empty value.
 * 
 * @author pcarr
 *
 */
public class TestChoiceInfoDefaultValues {
    private DynamicChoiceInfoParser parser;
    
    /*
     * Example from 3.6.1 generated manifest file
    p1_MODE=
    p1_TYPE=TEXT
    p1_default_value=
    p1_description=No default value, no empty value in drop-down
    p1_fileFormat=
    p1_flag=--arg\=
    p1_name=arg
    p1_optional=
    p1_prefix=--arg\=
    p1_prefix_when_specified=--arg\=
    p1_type=java.lang.String
    p1_value=A;B;C;D
     */
    private ParameterInfo initTextParam(final String name, final String value, final String description, final boolean optional) {
        return initTextParam(name, value, description, optional, "");
    }
    private ParameterInfo initTextParam(final String name, final String value, final String description, final boolean optional, final String defaultValue) {
        ParameterInfo pinfo=new ParameterInfo(name, value, description);
        pinfo.setAttributes(new HashMap<String,String>());
        pinfo.getAttributes().put("MODE", "");
        pinfo.getAttributes().put("TYPE", "TEXT");
        pinfo.getAttributes().put("default_value", defaultValue);
        pinfo.getAttributes().put("fileFormat", "");
        pinfo.getAttributes().put("flag", "--"+name+"=");
        if (optional) {
            pinfo.getAttributes().put("optional", "on");
        }
        else {
            pinfo.getAttributes().put("optional", "");
        }
        pinfo.getAttributes().put("prefix", "--"+name+"=");
        pinfo.getAttributes().put("prefix_when_specified", "--"+name+"=");
        pinfo.getAttributes().put("type", "java.lang.String");
        return pinfo;
    }
    
    @Before
    public void beforeTest() {
        parser=new DynamicChoiceInfoParser();
    }

    /*
     * test cases include:
     * - a text drop-down, never append an empty choice
     * - a file drop-down which already has an empty actual value, never append an empty choice
     * - an optional file drop-down with no default value, yes
     * - an optional file drop-down with a default value, yes
     * - a required file drop-down with no default value, yes
     * - a required file drop-down with a default value, maybe ... but we will go with yes
     */

    @Test
    public void testTextOptionalNoDefault() {
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "A;B;C;D", "An optional text drop-down with no default value", optional);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 4, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected value", "A", selected.getValue());
    }

    @Test
    public void testTextOptionalWithDefault() {
        final String defaultValue="C";
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "A;B;C;D", "An optional text drop-down with no default value", optional, defaultValue);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 4, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected value", "C", selected.getValue());
    }

    /**
     * Ignore default value when it's not in the menu.
     * Missing default means select first item in list.
     */
    @Test 
    public void testTextOptionalIncorrectDefault() {
        final String defaultValue="E";
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "A;B;C;D", "An optional text drop-down with no default value", optional, defaultValue);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 4, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        //defer to 1st item in list
        Assert.assertEquals("selected value", "A", selected.getValue());
    }

    /**
     * Test a text drop-down with both actual and display values.
     */
    @Test
    public void testTextWithDisplayValue() {
        final String defaultValue="B";
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "A=select a;B=select b;C=select c;D=select d", "An optional text drop-down with no default value", optional, defaultValue);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 4, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected value", "B", selected.getValue());
    }
    
    /**
     * What if the value is not set?
     *     p1_value=
     */
    @Test
    public void testTextValueNotSet() {
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "", "No ", optional);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNull("Not expecting a choiceInfo for 'value='", choiceInfo);
    }
    
    /**
     * What if there is just one value?
     */
    @Test
    public void testTextOneValue() {
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "item 1", "No ", optional);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 1, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected value", "item 1", selected.getValue());
    }

    @Test
    public void testTextWithEmptyFirstItem() {
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", ";A;B;C;D", "An optional text drop-down with no default value", optional);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 5, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected value", "", selected.getValue());
    }

    @Test
    public void testTextWithEmptyFirstItemAsEqualsSign() {
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "=;A;B;C;D", "An optional text drop-down with no default value", optional);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 5, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected value", "", selected.getValue());
    }

    @Test
    public void testTextWithEmptyFirstItemNonEmptyDisplayValue() {
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "=No selection;A;B;C;D", "An optional text drop-down with no default value", optional);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 5, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected actual value", "", selected.getValue());
        Assert.assertEquals("selected display value", "No selection", selected.getLabel());
    }
    
    /**
     * Test text drop-down with an empty value in the middle of the list.
     */
    @Test
    public void testTextWithEmptyValueInMiddle() {
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "A;;B;C;D", "An optional text drop-down with no default value", optional);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 5, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected value", "A", selected.getValue());
    }

    /**
     * Test text drop-down with an empty value in the middle of the list, which has a display value.
     */
    @Test
    public void testTextWithEmptyValueInMiddleWithDisplayValue() {
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "A;=No selection;B;C;D", "An optional text drop-down with no default value", optional);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 5, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected value", "A", selected.getValue());
    }

    /**
     * Test text drop-down with an empty value in the middle of the list as equals sign.
     */
    @Test
    public void testTextWithEmptyValueInMiddleAsEqualsSign() {
        final boolean optional=true;
        final ParameterInfo textParam=initTextParam("text.input", "A;=;B;C;D", "An optional text drop-down with no default value", optional);
        final ChoiceInfo choiceInfo=parser.initChoiceInfo(textParam);
        Assert.assertNotNull("return value from initChoiceInfo", choiceInfo);
        Assert.assertEquals("num items", 5, choiceInfo.getChoices().size());
        final Choice selected=choiceInfo.getSelected();
        Assert.assertNotNull("selected choice", selected);
        Assert.assertEquals("selected value", "A", selected.getValue());
    }

}
