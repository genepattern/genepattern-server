package org.genepattern.server.job.submit;

import org.genepattern.server.job.submit.NumValues;
import org.genepattern.server.job.submit.NumValuesParser;
import org.genepattern.server.job.submit.NumValuesParserImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * jUnit tests for the default NumValuesParser implementation.
 * 
 * @author pcarr
 */
public class TestNumValuesParserImpl {
    NumValuesParser nvParser;
    
    @Before
    public void init() {
        nvParser=new NumValuesParserImpl();
    }
    
    private void doTest(final String numValuesSpec, final Integer expectedMin, final Integer expectedMax) throws Exception {
        NumValues numValues=nvParser.parseNumValues(numValuesSpec);
        Assert.assertNotNull(numValues);
        Assert.assertEquals("min", numValues.getMin(), expectedMin);
        Assert.assertEquals("max", numValues.getMax(), expectedMax);
    }
    
    @Test
    public void nullInput() throws Exception {
        NumValues numValues=nvParser.parseNumValues(null);
        Assert.assertNull("numValues", numValues);
    }
    
    @Test
    public void emptyInput() throws Exception {
        NumValues numValues=nvParser.parseNumValues("");
        Assert.assertNull("numValues", numValues);
    }
    
    @Test
    public void whitespaceOnlyInput() throws Exception {
        NumValues numValues=nvParser.parseNumValues("     ");
        Assert.assertNull("numValues", numValues);
    }
    
    @Test
    public void zeroPlus() throws Exception {
        doTest("0+", 0, null);
    }

    @Test
    public void onePlus() throws Exception {
        doTest("1+", 1, null);
    }
    
    @Test
    public void twoPlus() throws Exception {
        doTest("2+", 2, null);
    }
    
    @Test
    public void OneValue() throws Exception {
        doTest("1", 1, 1);
    }
    
    @Test
    public void TwoValues() throws Exception {
        doTest("2", 2, 2);
    }
    
    @Test
    public void range0_1() throws Exception {
        doTest("0..1", 0, 1);
    }

    @Test
    public void range0_2() throws Exception {
        doTest("0..2", 0, 2);
    }

    @Test
    public void range1_4() throws Exception {
        doTest("1..4", 1, 4);
    }

    @Test
    public void range2_4() throws Exception {
        doTest("2..4", 2, 4);
    }
    
    @Test
    public void rangeWithWhiteSpace() throws Exception {
        doTest("0 .. 100", 0, 100);
    }

    @Test
    public void errorInvalidRangeSpecifier() {
        try {
            doTest("0-4", 0, 4);
            Assert.fail("Exception expected");
        }
        catch (Exception e) {
            //expected
        }
    }

    @Test
    public void errorInvalidMinValue() {
        try {
            doTest("3.14159+", 3, null);
            Assert.fail("Exception expected");
        }
        catch (Exception e) {
            //expected
        }
    }
  

}
