package org.genepattern.drm;

import static org.junit.Assert.assertEquals;

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
    public void testk() {
        final String spec="8k";
        Assert.assertEquals("numBytes for '"+spec+"'", 8192L, Memory.fromString(spec).getNumBytes());
    }

    @Test
    public void testK() {
        final String spec="8K";
        Assert.assertEquals("numBytes for '"+spec+"'", 8192L, Memory.fromString(spec).getNumBytes());
    }

    @Test
    public void testMb() {
        final String spec="512 Mb";
        Assert.assertEquals("numBytes for '"+spec+"'", 536870912L, Memory.fromString(spec).getNumBytes());
    }

    @Test
    public void testm() {
        final String spec="512m";
        Assert.assertEquals("numBytes for '"+spec+"'", 536870912L, Memory.fromString(spec).getNumBytes());
    }

    @Test
    public void testM() {
        final String spec="512M";
        Assert.assertEquals("numBytes for '"+spec+"'", 536870912L, Memory.fromString(spec).getNumBytes());
    }

    @Test
    public void testGb() {
        final String spec="8 Gb";
        Assert.assertEquals("numBytes for '"+spec+"'", 8589934592L, Memory.fromString(spec).getNumBytes());
    }

    @Test
    public void testg() {
        final String spec="8g";
        Assert.assertEquals("numBytes for '"+spec+"'", 8589934592L, Memory.fromString(spec).getNumBytes());
    }

    @Test
    public void testG() {
        final String spec="8G";
        Assert.assertEquals("numBytes for '"+spec+"'", 8589934592L, Memory.fromString(spec).getNumBytes());
    }

    @Test
    public void testTb() {
        final String spec="2 tb";
        Assert.assertEquals("numBytes for '"+spec+"'", 2199023255552L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testt() {
        final String spec="2t";
        Assert.assertEquals("numBytes for '"+spec+"'", 2199023255552L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testT() {
        final String spec="2T";
        Assert.assertEquals("numBytes for '"+spec+"'", 2199023255552L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testPetabyte() {
        final String spec="1024Tb";
        Assert.assertEquals("numBytes for '"+spec+"'", 1125899906842624L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testPb() {
        final String spec="1 pb";
        Assert.assertEquals("numBytes for '"+spec+"'", 1125899906842624L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testp() {
        final String spec="1p";
        Assert.assertEquals("numBytes for '"+spec+"'", 1125899906842624L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testP() {
        final String spec="1P";
        Assert.assertEquals("numBytes for '"+spec+"'", 1125899906842624L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void testToXmx_512m() {
        final String spec="512m";
        Assert.assertEquals("toXmx for '"+spec+"'", "512m", Memory.fromString(spec).toXmx());
    }

    @Test
    public void testToXmx_2048_Mb() {
        final String spec="2048 Mb";
        Assert.assertEquals("toXmx for '"+spec+"'", "2048m", Memory.fromString(spec).toXmx());
    }
    
    @Test
    public void testToXmx_16_Gb() {
        final String spec="16 Gb";
        Assert.assertEquals("toXmx for '"+spec+"'", "16g", Memory.fromString(spec).toXmx());
    }

    @Test
    public void testToXmx_fractional() {
        final String spec="2.5 gb";
        Assert.assertEquals("toXmx for '"+spec+"'", "2560m", Memory.fromString(spec).toXmx());
    }
    
    @Test
    public void testToXmx_2_0() {
        final String spec="2.0 gb";
        Assert.assertEquals("toXmx for '"+spec+"'", "2g", Memory.fromString(spec).toXmx());
    }
    
    @Test
    public void testToXmx_fractional_Pb() {
        final String spec="0.001 Pb";
        //1.048567 Gb, 1024 + 
        Assert.assertEquals("toXmx for '"+spec+"'", "1073742m", Memory.fromString(spec).toXmx());
    }

    @Test
    public void testToXmx_16_Pb() {
        final String spec="16 Pb";
        Assert.assertEquals("toXmx for '"+spec+"'", "16777216g", Memory.fromString(spec).toXmx());
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
    
    @Test()
    public void testFractionalTb() {
        final String spec="1.2 TB";
        //Memory.fromString(spec);
        Assert.assertEquals("numBytes for '"+spec+"'", 1319413953331L, Memory.fromString(spec).getNumBytes());
    }
    
    @Test
    public void format_toBytes() {
        long numBytes=Memory.fromString("32 b").getNumBytes();
        Memory mem=Memory.fromSizeInBytes(numBytes);
        assertEquals("mem.format", "32 b", mem.format());
    }

    @Test
    public void format_toKb() {
        long numBytes=Memory.fromString("1365 kb").getNumBytes();
        Memory mem=Memory.fromSizeInBytes(numBytes);
        assertEquals("mem.format", "1365 kb", mem.format());
    }

    @Test
    public void format_toMb() {
        long numBytes=Memory.fromString("512 mb").getNumBytes();
        Memory mem=Memory.fromSizeInBytes(numBytes);
        assertEquals("mem.format", "512 mb", mem.format());
    }
    
    @Test
    public void format_toGb() {
        long numBytes=Memory.fromString("2048 Mb").getNumBytes();
        Memory mem=Memory.fromSizeInBytes(numBytes);
        assertEquals("mem.format", "2 gb", mem.format());
    }
    
    @Test
    public void format_toGb_resolution() {
        //long numBytes=Memory.fromString("2832 Mb").getNumBytes();
        long numBytes=2969658452L;
        Memory mem=Memory.fromSizeInBytes(numBytes);
        assertEquals("mem.format", "2832 mb", mem.format());
    }


    @Test
    public void format_toGb_fractional() {
        long numBytes=Memory.fromString("2.5 Gb").getNumBytes();
        Memory mem=Memory.fromSizeInBytes(numBytes);
        assertEquals("mem.format", "2560 mb", mem.format());
    }

    @Test
    public void format_toTb_exactly_one() {
        long numBytes=Memory.fromString("1 Tb").getNumBytes();
        Memory mem=Memory.fromSizeInBytes(numBytes);
        assertEquals("mem.format", "1 tb", mem.format());
    }
    
    @Test
    public void format_toPb() {
        long numBytes=Memory.fromString("1024 Tb").getNumBytes();
        Memory mem=Memory.fromSizeInBytes(numBytes);
        assertEquals("mem.format", "1 pb", mem.format());
        
    }
    
    //1050624
    @Test
    public void format_toPb_overflow() {
        long numBytes=Memory.fromString("1050624 Tb").getNumBytes();
        Memory mem=Memory.fromSizeInBytes(numBytes);
        assertEquals("mem.format", "1026 pb", mem.format());
    }

    @Test
    public void testEquals() {
        final String spec="8 gb";
        assertEquals(spec, Memory.fromString(spec), Memory.fromString(spec)); 
    }
    
    @Test
    public void testEquals_anyUnits() {
        final String spec0="512m";
        final String spec1="0.5g";
        Assert.assertEquals(spec0+"=="+spec1, Memory.fromString(spec0), Memory.fromString(spec1));
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
