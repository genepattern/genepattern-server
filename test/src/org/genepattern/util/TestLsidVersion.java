package org.genepattern.util;

import static org.genepattern.util.LsidVersion.fromString;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class TestLsidVersion {
    @Before
    public void setUp() {
    }
    
    @Test(expected=Exception.class)
    public void invalidVersion_mustBeAnInteger() {
        fromString("1-beta");
    }

    @Test(expected=Exception.class)
    public void invalidVersion_mustBeAPositiveInteger() {
        fromString("-1");
    }
    
    private void doCompareTest(final int expected, final String lhs, final String rhs) {
        final LsidVersion left=fromString(lhs);
        final LsidVersion right=fromString(rhs);
        
        // self test
        assertEquals("self: compareTo(left)", 0, left.compareTo(left));
        assertEquals("self: compareTo(right)", 0, right.compareTo(right));
        assertEquals("'"+lhs+"'.compareTo('"+lhs+"')", 0, left.compareTo(fromString(lhs)));
        assertEquals("'"+rhs+"'.compareTo('"+rhs+"')", 0, right.compareTo(fromString(rhs)));
        
        // test
        assertEquals("'"+lhs+"'.compareTo('"+rhs+"')", expected, 
                left.compareTo(right));

        // flip
        assertEquals("'"+rhs+"'.compareTo('"+lhs+"')", 
                -1*expected, // flip
                right.compareTo(left));
    }
    
    @Test
    public void compareTo() {
        doCompareTest(0, "", "");
        doCompareTest(-1, "", "1");
        doCompareTest(-1, "", "1.0");
        doCompareTest(-1, "", "1.1");
        doCompareTest(-1, "", "1.2.3");
        doCompareTest(-1, "", "1.2.3.4");
        
        doCompareTest(0, "1", "1");
        doCompareTest(0, "1.1", "1.1");
        doCompareTest(0, "1.1.1", "1.1.1");
        
        // 10 > 2
        doCompareTest(1, "10", "2");
        doCompareTest(1, "1.10", "1.2");
        doCompareTest(1, "1.2.10", "1.2.2");
        
        // levels, 1.1 > 1
        doCompareTest(1, "1.1", "1");
        doCompareTest(1, "1.1.10", "1.1");
        doCompareTest(1, "1.1.1.10", "1.1.1");
    }
    
    // special-case, is '0' the same as not set?
    /** ambiguous comparisons, re: LSID version vs. SemVer */
    @Test
    public void compareTo_withZero() {
        // "" < 0
        doCompareTest(-1, "", "0");
        // 1 < 1.0
        doCompareTest(-1, "1", "1.0");
        // 1.0 < 1.0.0
        doCompareTest(-1, "1.0", "1.0.0");
        // 0.1 < 0.1.0
        doCompareTest(-1, "0.1", "0.1.0");
    }

    @Test
    public void compareToSemVer_withZero() {
        assertEquals("'' == '0'", 
                0, fromString("").compareToSemVer(fromString("0")));
        assertEquals("'1' == '1.0'", 
                0, fromString("1").compareToSemVer(fromString("1.0")));
        assertEquals("'1.0' == '1.0.0'", 
                0, fromString("1.0").compareToSemVer(fromString("1.0.0")));
        assertEquals("'0.1' == '0.1.0'", 
                0, fromString("1.0").compareToSemVer(fromString("1.0.0")));
    }

    @Test
    public void nullVersion() {
        // this is the version before the first version
        LsidVersion v=new LsidVersion();
        assertEquals("<init version>", "", v.toString());
        
        assertEquals("<init>.nextMinor", "0.1", v.nextMinor().toString());
        assertEquals("<init>.nextPatch", "0.0.1", v.nextPatch().toString());
        
        assertEquals("<init>.nextMajor as nextSequence(0)", "1", v.nextSequence(0).toString());
        assertEquals("<init>.nextMinor as nextSequence(1)", "0.1", v.nextSequence(1).toString());
        assertEquals("<init>.nextPatch as nextSequence(2)", "0.0.1", v.nextSequence(2).toString());
        assertEquals("<init>.nextSequence(3)", "0.0.0.1", v.nextSequence(3).toString());
    }
    
    @Test
    public void initialMajorVersion() {
        final LsidVersion init=new LsidVersion();
        assertEquals("<init>.nextMajor", fromString("1"), init.nextMajor());
        assertEquals("LsidVersion.compare", fromString("1"), init.nextMajor());
    }

    // 1 -> 2
    @Test
    public void incrementMajorVersion() { 
        String from="1";
        assertEquals("nextMajor("+from+")", 
                fromString("2"), 
                fromString(from).nextMajor());
    }

    // 1.3 -> 2
    @Test
    public void nextMajorVersion_fromMinor() {
        final String from="1.3";
        final String expected="2";
        assertEquals("("+from+").nextMajor", 
                fromString(expected), fromString(from).nextMajor());
    }

    // 1.3.2 -> 2
    @Test
    public void nextMajorVersion_fromPatch() { 
        final String from="1.3.2";
        final String expected="2";
        assertEquals("("+from+").nextMajor", 
                fromString(expected), fromString(from).nextMajor());
    }

    // 1 -> 1.1
    @Test
    public void nextMinorVersion_fromMajor() { 
        String from="1";
        assertEquals("nextMinor("+from+")", 
                fromString("1.1"), 
                fromString(from).nextMinor());
    }

    // 1.3 -> 1.4
    @Test
    public void incrementMinorVersion() {
        final String from="1.3";
        final String expected="1.4";
        assertEquals("("+from+").nextMinor", 
                fromString(expected), 
                fromString(from).nextMinor());
    }

    // 1.3.2 -> 1.4
    @Test
    public void nextMinorVersion_fromPatch() { 
        final String from="1.3.2";
        final String expected="1.4";
        assertEquals("("+from+").nextMinor", 
                fromString(expected), fromString(from).nextMinor());
    }

    // 1 -> 1.0.1
    @Test
    public void nextPatchVersion_fromMajor() { 
        String from="1";
        assertEquals("nextPath("+from+")", 
                fromString("1.0.1"), 
                fromString(from).nextPatch());
    }

    // 1.3 -> 1.3.1
    @Test
    public void nextPatchVersion_fromMinor() { 
        String from="1.3";
        assertEquals("nextPatch("+from+")", 
                fromString("1.3.1"), 
                fromString(from).nextPatch());
    }
    
    // 1.3.2 -> 1.3.3
    @Test
    public void incrementPatch() { 
        String from="1.3.2";
        assertEquals("nextPatch("+from+")", 
                fromString("1.3.3"), 
                fromString(from).nextPatch());
    }

    // 1.3.9 -> 1.3.10
    @Test
    public void incrementPatch_digits() { 
        String from="1.3.9";
        assertEquals("nextPatch("+from+")", 
                fromString("1.3.10"), 
                fromString(from).nextPatch());
    }

}
