package org.genepattern.server.job.input;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by nazaire on 9/3/15.
 */
public class TestRangeValuesParser
{
    RangeValuesParser rvParser;

    @Before
    public void init() {
        rvParser = new RangeValuesParser();
    }

    private void doTest(final String numRangesSpec, final Double expectedMin, final Double expectedMax) throws Exception {
        RangeValues<Integer> rangeValues= rvParser.parseRange(numRangesSpec);
        Assert.assertNotNull(rangeValues);
        Assert.assertEquals("min", rangeValues.getMin(), expectedMin);
        Assert.assertEquals("max", rangeValues.getMax(), expectedMax);
    }

    @Test
    public void nullInput() throws Exception {
        RangeValues rangeValues= rvParser.parseRange(null);
        Assert.assertNull("rangeValues", rangeValues);
    }

    @Test
    public void emptyInput() throws Exception {
        RangeValues rangeValues= rvParser.parseRange("");
        Assert.assertNull("rangeValues", rangeValues);
    }

    @Test
    public void whitespaceOnlyInput() throws Exception {
        RangeValues rangeValues= rvParser.parseRange("     ");
        Assert.assertNull("rangeValues", rangeValues);
    }

    @Test
    public void zeroPlus() throws Exception {
        doTest("0+", 0.0, null);
    }

    @Test
    public void onePlus() throws Exception {
        doTest("1+", 1.0, null);
    }

    @Test
    public void twoPlus() throws Exception {
        doTest("2+", 2.0, null);
    }

    @Test
    public void OneValue() throws Exception {
        doTest("1", 1.0, 1.0);
    }

    @Test
    public void TwoValues() throws Exception {
        doTest("2", 2.0, 2.0);
    }

    @Test
    public void range0_1() throws Exception {
        doTest("0..1", 0.0, 1.0);
    }

    @Test
    public void range0_2() throws Exception {
        doTest("0..2", 0.0, 2.0);
    }

    @Test
    public void range1_4() throws Exception {
        doTest("1..4", 1.0, 4.0);
    }

    @Test
    public void range2_4() throws Exception {
        doTest("2..4", 2.0, 4.0);
    }

    @Test
    public void rangeWithWhiteSpace() throws Exception {
        doTest("0 .. 100", 0.0, 100.0);
    }

    @Test
    public void errorInvalidRangeSpecifier() {
        try {
            doTest("0-4", 0.0, 4.0);
            Assert.fail("Exception expected");
        }
        catch (Exception e) {
            //expected
        }
    }

    @Test
    public void negativePlus() throws Exception{
       doTest("-2.5+", -2.5, null);
    }

    @Test
    public void negativeMinus() throws Exception{
        doTest("-4.8-", null, -4.8);
    }

    @Test
    public void positiveMinus() throws Exception{
        doTest("2-", null, 2.0);
    }

    @Test
    public void errorBadNegativeRange() throws Exception {
        try {
            doTest("-", null, 2.0);
            Assert.fail("Exception expected. Error parsing range=-");
        }
        catch (Exception e) {
            //expected
        }
    }
}
