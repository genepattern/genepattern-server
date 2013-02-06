package org.genepattern.server.job.input;

import java.util.HashMap;

import org.genepattern.webservice.ParameterInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * jUnit tests for initializing numValues from a ParameterInfo.
 * @author pcarr
 *
 */
public class TestParamListHelperInitNumValues {
    ParameterInfo pinfo;
    
    @Before
    public void init() {
        pinfo=new ParameterInfo();
        pinfo.setName("input.files");
        pinfo.setAttributes(new HashMap<String,String>());
    }
    
    private void doTest(final boolean expectedOptional, final boolean expectedAcceptsList, final Integer expectedMin, final Integer expectedMax) {
        NumValues numValues=ParamListHelper.initNumValues(pinfo);
        Assert.assertNotNull("numValues", numValues);

        Assert.assertEquals("numValues.optional", expectedOptional, numValues.isOptional());
        Assert.assertEquals("numValues.acceptsList", expectedAcceptsList, numValues.acceptsList());
        Assert.assertEquals("numValues.min", numValues.getMin(), expectedMin);
        Assert.assertEquals("numValues.max", numValues.getMax(), expectedMax);
    }

    @Test
    public void numValuesNotSet() {
        doTest(false, false, 1, 1);
    }
    
    @Test
    public void emptyString() {
        pinfo.getAttributes().put("numValues", "");
        doTest(false, false, 1, 1);
    }

    @Test
    public void optionalNumValuesNotSet() {
        pinfo.getAttributes().put("optional", "on");
        doTest(true, false, 0, 1);
    }
    
    @Test
    public void one() {
        pinfo.getAttributes().put("numValues", "1");
        doTest(false, false, 1, 1);
    }

    @Test
    public void two() {
        pinfo.getAttributes().put("numValues", "2");
        doTest(false, true, 2, 2);
    }
    
    @Test
    public void zeroPlus() {
        pinfo.getAttributes().put("numValues", "0+");
        doTest(true, true, 0, null);
    }
    
    @Test
    public void onePlus() {
        pinfo.getAttributes().put("numValues", "1+");
        doTest(false, true, 1, null);
    }
    
    @Test
    public void twoPlus() {
        pinfo.getAttributes().put("numValues", "2+");
        doTest(false, true, 2, null);
    }
    
    @Test
    public void zeroThroughOne() {
        pinfo.getAttributes().put("numValues", "0..1");
        doTest(true, false, 0, 1);
    }
    
    @Test
    public void zeroThroughTwo() {
        pinfo.getAttributes().put("numValues", "0..2");
        doTest(true, true, 0, 2);
    }
    
    @Test
    public void oneThroughTwo() {
        pinfo.getAttributes().put("numValues", "1..2");
        doTest(false, true, 1, 2);
    }

    @Test
    public void nullAttributes() {
        pinfo.setAttributes(null);
        doTest(false, false, 1, 1);
    }
    
    //some bogus test cases
    @Test
    public void syntaxError_invalidSeparator() {
        final String numValues="1-2";
        try {
            pinfo.getAttributes().put("numValues", numValues);
            doTest(false, false, 1, 1);
            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void syntaxError_invalidNumber() {
        final String numValues="one";
        try {
            pinfo.getAttributes().put("numValues", numValues);
            doTest(false, false, 1, 1);
            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }
    
    @Test
    public void error_lessThanZero() {
        final String numValues="-1";
        try {
            pinfo.getAttributes().put("numValues", numValues);
            doTest(false, false, 1, 1);
            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void error_zero() {
        final String numValues="0";
        try {
            pinfo.getAttributes().put("numValues", numValues);
            doTest(false, false, 1, 1);
            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }
    
    @Test
    public void error_minGreaterThanMax() {
        final String numValues="5..1";
        try {
            pinfo.getAttributes().put("numValues", numValues);
            doTest(false, false, 1, 1);
            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

}
