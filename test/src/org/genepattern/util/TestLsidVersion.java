package org.genepattern.util;

import static org.genepattern.util.LsidVersion.fromString;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestLsidVersion {
    
    @Test
    public void createDefaultVersion() {
        assertEquals("increment from <init>", fromString("1"),  new LsidVersion().increment());
    }
    
    @Test
    public void createNewMajorVersion() {
        assertEquals("1", new LsidVersion().nextMajor().toString());
    }

    @Test
    public void createNewMinorVersion() {
        assertEquals("0.1", new LsidVersion().nextMinor().toString());
    }

    @Test
    public void createNewPatchVersion() {
        assertEquals("0.0.1", new LsidVersion().nextPatch().toString());
    }
    
    protected void incrementTest(final String from, final String to) {
        assertEquals("'"+from+"'.increment", fromString(to),
                fromString(from).increment());
    }

    protected void incrementLeftTest(final String from, final String to) {
        assertEquals("'"+from+"'.incrementLeft", fromString(to),
                fromString(from).incrementLeft());
    }
    
    protected void incrementRightTest(final String from, final String to) {
        assertEquals("'"+from+"'.incrementRight", fromString(to),
                fromString(from).incrementRight());
    }

    @Test
    public void incrementMajor() {
        incrementTest("1", "2");
        incrementTest("2", "3");
    }

    @Test
    public void incrementMinor() {
        incrementTest("0.1", "0.2");
        incrementTest("0.2", "0.3");
        incrementTest("0.9", "0.10");
        incrementTest("0.10", "0.11");
    }

    @Test
    public void incrementPatch() {
        incrementTest("0.0.0", "0.0.1");
        incrementTest("0.0.1", "0.0.2");
        incrementTest("0.0.9", "0.0.10");
        incrementTest("0.0.10", "0.0.11");
        incrementTest("3.1.15", "3.1.16");
    }

    @Test
    public void increment_subPatch() {
        incrementTest("0.0.0.0", "0.0.0.1");
        incrementTest("0.0.0.1", "0.0.0.2");
        incrementTest("0.0.0.9", "0.0.0.10");
        incrementTest("0.0.0.10", "0.0.0.11");
        incrementTest("3.1.15.1", "3.1.15.2");
    }
    
    @Test
    public void incrementLeft() {
        // corner-case: can't shift INITIAL version
        incrementLeftTest("", "1");
        // corner-case: can't shift MAJOR version
        incrementLeftTest("1", "2");
        incrementLeftTest("0.1", "1");
        incrementLeftTest("0.48", "1");
        incrementLeftTest("1.1", "2");
        incrementLeftTest("3.14159", "4");
        incrementLeftTest("0.0.23", "0.1");
        incrementLeftTest("1.3.5", "1.4");
        incrementLeftTest("3.9.7.135", "3.9.8");
        
    }
    
    @Test
    public void incrementRight() {
        // special-case: '' -> 0.1
        incrementRightTest("", "0.1");
        // 0 -> 0.1
        incrementRightTest("0","0.1");
        // 1 -> 1.1
        incrementRightTest("1","1.1");
        // 1.10 -> 1.10.1
        incrementRightTest("1.10","1.10.1");
    }
    
    @Test
    public void example() {
        // from example, "3.1.15"
        incrementTest( "3.1.15", "3.1.16");
        incrementLeftTest("3.1.15", "3.2");
        incrementLeftTest("3.1", "4");
        incrementLeftTest("3", "4"); // <---- corner-case
        // from example, "3.1.15"
        incrementRightTest("3", "3.1");
        incrementRightTest("3.1", "3.1.1");
        incrementRightTest("3.1.15", "3.1.15.1");
    }

    @Test(expected=IllegalArgumentException.class)
    public void incrementVersion_idxLessThanZero() {
        // idx < 0, throws exception
        fromString("3.1.15").incrementVersion(-1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void incrementVersion_idxExceedsMax() {
        // idx > 100, throws exception
        fromString("3.1.15").incrementVersion(101);
    }
    
    @Test(expected=Exception.class)
    public void invalidVersion_mustBeAnInteger() {
        fromString("my version");
    }

    @Test(expected=Exception.class)
    public void invalidVersion_negativeInteger() {
        fromString("1.0.-5");
    }

    /**
     * Semantic Versioning pre-release version not supported
     * Examples: 1.0.0-alpha, 1.0.0-alpha.1, 1.0.0-0.3.7, 1.0.0-x.7.z.92
     */
    @Test(expected=Exception.class)
    public void semVer_pre_release_version() {
        fromString("1.0.0-alpha");
    }

    @Test(expected=Exception.class)
    public void semVer_pre_release_version_02() {
        fromString("1.0.0-alpha.1");
    }

    @Test(expected=Exception.class)
    public void semVer_pre_release_version_03() {
        fromString("1.0.0-0.3.7");
    }

    @Test(expected=Exception.class)
    public void semVer_pre_release_version_04() {
        fromString("1.0.0-x.7.z.92");
    }
    
    /**
     * Semantic Versioning build metadata not supported
     * Examples 1.0.0-alpha+001, 1.0.0+20130313144700, 1.0.0-beta+exp.sha.5114f85
     */
    @Test(expected=Exception.class)
    public void semVer_metadata_01() {
        fromString("1.0.0-alpha+001");
    }

    @Test(expected=Exception.class)
    public void semVer_metadata_02() {
        fromString("1.0.0+20130313144700");
    }

    @Test(expected=Exception.class)
    public void semVer_metadata_03() {
        fromString("1.0.0-beta+exp.sha.5114f85");
    }

    @Test(expected=Exception.class)
    public void invalidVersion_semVer_metaData_example2() {
        //Note: Semantic Versioning meta data not implemented
        fromString("1.0.1+beta");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void invalidIntVersion() {
        new LsidVersion(3, 1, 15, -5, 0, 0, 1);
    }

    @Test
    public void testEquals() {
        String str="3.1.15";
        assertEquals(false, fromString(str).equals(str));
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
    public void compareToSemVer_basic() {
        // '' == ''
        String from="";
        String to="";        
        assertEquals("'"+from+"'.compareTo('"+to+"')", 
                0, fromString(from).compareToSemVer(fromString(to)));

        // '' < 1
        to="1";        
        assertEquals("'"+from+"'.compareTo('"+to+"')", 
                -1, fromString(from).compareToSemVer(fromString(to)));

        from="3.1.15";
        to="3.1.15";        
        assertEquals("'"+from+"'.compareTo('"+to+"')", 
                0, fromString(from).compareToSemVer(fromString(to)));
        from="3";
        assertEquals("'"+from+"'.compareTo('"+to+"')", 
                -1, fromString(from).compareToSemVer(fromString(to)));
        from="3.1";
        assertEquals("'"+from+"'.compareTo('"+to+"')", 
                -1, fromString(from).compareToSemVer(fromString(to)));
        from="3.1.14";
        assertEquals("'"+from+"'.compareTo('"+to+"')", 
                -1, fromString(from).compareToSemVer(fromString(to)));
        from="3.1.14.502";
        assertEquals("'"+from+"'.compareTo('"+to+"')", 
                -1, fromString(from).compareToSemVer(fromString(to)));
        // Note: not sure if this is good idea, '3.1.15.0' == '3.1.15'
        from="3.1.15.0";
        assertEquals("'"+from+"'.compareTo('"+to+"')", 
                0, fromString(from).compareToSemVer(fromString(to)));
        from="3.1.15.1";
        assertEquals("'"+from+"'.compareTo('"+to+"')", 
                1, fromString(from).compareToSemVer(fromString(to)));
    }
    
    @Test
    public void compareToSemVer_self() {
        LsidVersion self=fromString("3.1.15");
        assertEquals(0, self.compareToSemVer(self));
    }

}
