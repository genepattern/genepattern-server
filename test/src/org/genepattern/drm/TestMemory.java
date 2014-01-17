package org.genepattern.drm;

import org.genepattern.drm.Memory.Unit;
import org.junit.Assert;
import org.junit.Test;

/**
 * jUnit tests for the Memory class.
 * @author pcarr
 *
 */
public class TestMemory {
    @Test
    public void testNullArg() {
        final Memory actual=Memory.fromString(null);
        Assert.assertNull("null string should convert to null instance", actual);
    }
    
    @Test
    public void testEmptyString() {
        final Memory actual=Memory.fromString("");
        Assert.assertNull("empty string should convert to null instance", actual);
    }
    
    @Test
    public void testNoUnits() {
        //default units are Gb
        final Memory actual=Memory.fromString("8");  // equivalent to "8 Gb"
        Assert.assertEquals("expecting 8589934592 bytes", 8589934592L, actual.getNumBytes());
    }
    
    @Test
    public void testBytes() {
        final String spec="1024 b";
        Assert.assertEquals("numBytes for '"+spec+"'", 1024L, Memory.fromString(spec).getNumBytes());        
    }
    
    @Test
    public void testKb() {
        final String spec="8 kb";
        Assert.assertEquals("numBytes for '"+spec+"'", 8192L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testMb() {
        final String spec="512 Mb";
        Assert.assertEquals("numBytes for '"+spec+"'", 536870912L, Memory.fromString(spec).getNumBytes());
    }

    @Test
    public void testGb() {
        Memory actual=Memory.fromString("8 Gb");
        Assert.assertEquals("8 Gb", 8589934592L, actual.getNumBytes());
    }

    @Test
    public void testTb() {
        final String spec="2 tb";
        Assert.assertEquals("numBytes for '"+spec+"'", 2199023255552L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testGpNoSeparator() {
        final String spec="8gb";
        Memory actual=Memory.fromString(spec);
        Assert.assertEquals(spec, 8589934592L, actual.getNumBytes());        
    }
    
    @Test
    public void testWhitespace() {
        Memory actual=Memory.fromString(" 8 Gb ");
        Assert.assertEquals("8 Gb", 8589934592L, actual.getNumBytes());
    }
    
    @Test
    public void testFractionalTb() {
        final String spec="1.2 TB";
        Assert.assertEquals("numBytes for '"+spec+"'", 1319413953331L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testFromBytesToGb_exact() {
        final String spec="536870912 b"; // 0.5 Gb
        Memory actual=Memory.fromString(spec);
        Assert.assertEquals("numBytes for '"+spec+"'", 536870912L, actual.getNumBytes());
        
        double numGb=(double) actual.getNumBytes() / (double) Unit.gb.getMultiplier();
        Assert.assertEquals("numGb", 0.5, numGb, 0);
    }
    
    @Test
    public void testFromBytesToGb_estimate() {
        final String spec="500000000 b"; 
        Memory actual=Memory.fromString(spec);
        Assert.assertEquals("numBytes for '"+spec+"'", 500000000L, actual.getNumBytes());
        
        double numGb=(double) actual.getNumBytes() / (double) Unit.gb.getMultiplier();
        Assert.assertEquals("numGb", 0.46566128730774, numGb, 0.00001);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNegativeNumberNoUnit() {
        final String spec="-5";
        Memory.fromString(spec);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNegativeNumber() {
        Memory.fromString("-5 kb");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidUnits() {
        Memory.fromString("2 gigabytes");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testExtraTokens() {
        Memory.fromString(" 8 Gb max");
    }

}
