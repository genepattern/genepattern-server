/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for LSIDVersionComparator.
 * 
 * @author pcarr
 */
public class LSIDVersionComparatorTest {
    private LSIDVersionComparator comparator;
    
    @Before
    public void setUp() {
        comparator = LSIDVersionComparator.INSTANCE;
    }

    /**
     * Test cases when one or both input LSID.version is the empty String.
     */
    @Test
    public void testCompareToEmptyString() {
        assertEquals("compare(empty,empty)", 0, comparator.compare("", ""));
        assertEquals("empty < 2.1", -1, comparator.compare("", "2.1"));
        assertEquals("empty < 1", -1, comparator.compare("", "1"));
        assertEquals("empty < 0", -1, comparator.compare("", "0"));
        assertEquals("empty > -1", 1, comparator.compare("", "-1"));
        assertEquals("2.1 > empty", 1, comparator.compare("2.1", ""));
        assertEquals("1 > empty", 1, comparator.compare("1", ""));
        assertEquals("0 > empty", 1, comparator.compare("0", ""));
        assertEquals("-1 < empty", -1, comparator.compare("-1", ""));
        
        assertEquals("3. == 3.", 0, comparator.compare("3.", "3."));
    }
    
    /**
     * Test cases when one or both input LSID.version are null.
     */
    @Test
    public void testCompareToNull() {
        assertEquals("compare(null,null)", 0, comparator.compare(null, null));
        assertEquals("compare(\"\",null)", 0, comparator.compare("", null));
        assertEquals("compare(null,\"\")", 0, comparator.compare(null, ""));
    }
    
    @Test
    public void testCompare() {
        doTest(0, "1", "1");
        doTest(0, "1.2", "1.2");
        doTest(0, "9999", "9999");
        doTest(-1,  "1", "10" );
        doTest(-1,  "1.1", "10" );
        doTest(-1,  "2", "10" );
        doTest(-1,  "2.8", "10" );
        doTest(-1, "1", "1.2");
        doTest(0, "-1", "-1");
        doTest(0, "1.-1", "1.-1");
    }
 
    /**
     * Test cases where part of the input does not map to an Integer.
     */
    @Test
    public void testExceptions() {
        doTest(0, "1a", "1a");
        doTest(0, "1a", "2b");
        doTest(0, "1.1a", "1.8000b");
        doTest(-1, "5a", "1");
    }
    
    private void doTest(int expected, String arg1, String arg2) {
        assertEquals("compare("+arg1+","+arg2+")", expected, comparator.compare(arg1, arg2));
    }
}
