/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.util;

import static org.junit.Assert.*;

import static org.genepattern.util.GPConstants.STDERR;
import static org.genepattern.util.GPConstants.STDOUT;
import static org.genepattern.util.GPConstants.TASKLOG;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test JobResultsFilenameFilter class.
 * @author pcarr
 */
public class JobResultsFilenameFilterTest {
    private static final File dir = null; //arg to FilenameFilter.accept
    private static final String ds_store = ".DS_Store";
    private static final String nfsExample = ".nfs00012.txt";
    private static final String gctExample = "all_aml_out.gct";
    private static final String lsfExample = ".lsf_12.out";
    
    private JobResultsFilenameFilter filter = null;
    
    @Before
    public void setUp() throws Exception {
        filter = new JobResultsFilenameFilter();
        filter.addExactMatch(STDERR);
        filter.addExactMatch(STDOUT);
        filter.addExactMatch(TASKLOG);
    }

    /**
     * Load the glob pattern from a .properties file
     */
    @Test
    public void testGetProperties() throws Exception {
        Properties props = new Properties();
        InputStream properties = this.getClass().getResourceAsStream("jobsFilenameFilter.properties");
        props.load(properties);
        
        String glob = props.getProperty(JobResultsFilenameFilter.KEY);
        filter.setGlob(glob);
        
        assertFalse("accept('"+nfsExample+"')", filter.accept(dir, nfsExample));
        assertFalse("accept('"+lsfExample+"')", filter.accept(dir, lsfExample));
        assertTrue("accept('"+ds_store+"')", filter.accept(dir, ds_store));
        assertTrue("accept('"+gctExample+"')", filter.accept(dir, gctExample));
    }
    
    /**
     * <pre>
       JobResultsFilenameFilter.setGlob(".nfs*").
     * </pre>
     */
    @Test
    public void testDotNfsStar() {
        filter.setGlob(".nfs*");
        assertTrue("accept('"+ds_store+"')", filter.accept(dir, ds_store));
        assertFalse("accept('"+nfsExample+"')", filter.accept(dir, nfsExample));
        assertTrue("accept('"+gctExample+"')", filter.accept(dir, gctExample));
    }
    
    /**
     * <pre>
       JobResultsFilenameFilter.setGlob(".*").
     * </pre>
     */
    @Test
    public void testDotStar() {
        filter.setGlob(".*");
        assertFalse("accept('"+ds_store+"')", filter.accept(dir, ds_store));
        assertFalse("accept('"+nfsExample+"')", filter.accept(dir, nfsExample));
        assertTrue("accept('"+gctExample+"')", filter.accept(dir, gctExample));
    }

    /**
     * <pre>
       JobResultsFilenameFilter.setGlob("*").
     * </pre>
     */
    @Test
    public void testStar() {
        filter.setGlob("*");
        assertFalse("accept('"+ds_store+"')", filter.accept(dir, ds_store));
        assertFalse("accept('"+nfsExample+"')", filter.accept(dir, nfsExample));
        assertFalse("accept('"+gctExample+"')", filter.accept(dir, gctExample));
    }

    /**
     * <pre>
       JobResultsFilenameFilter.setGlob(null).
     * </pre>
     */
    @Test
    public void testNullPattern() {
        filter.setGlob((String)null);
        assertTrue("null pattern: .nfs", filter.accept(dir, nfsExample));
        assertTrue("null pattern: *.gct", filter.accept(dir, gctExample));
    }
    
    /**
     * <pre>
       JobResultsFilenameFilter.setGlob("").
     * </pre>
     */
    @Test
    public void testEmptyPattern() {
        filter.setGlob("");
        assertTrue("empty pattern:", filter.accept(dir, nfsExample));
        assertTrue("empty pattern: *.gct", filter.accept(dir, gctExample));

        filter.setGlob(" ");
        assertTrue("whitespace pattern", filter.accept(dir, nfsExample));
        assertTrue("whitespace pattern", filter.accept(dir, gctExample));        
    }
    
    @Test
    public void testListOfGlobs() {
        filter.setGlob(".lsf*,.nfs*");
        
        assertFalse("accept('"+nfsExample+"')", filter.accept(dir, nfsExample));
        assertFalse("accept('"+lsfExample+"')", filter.accept(dir, lsfExample));
        assertTrue("accept('"+ds_store+"')", filter.accept(dir, ds_store));
        assertTrue("accept('"+gctExample+"')", filter.accept(dir, gctExample));
    }

    /**
     * <pre>
       JobResultsFilenameFilter.setGlob(" *").
       JobResultsFilenameFilter.setGlob("* ").
     * </pre>
     */
    @Test
    public void testWhitespace() {
        String ws0 = "space char in filename.txt";
        String ws1 = ".space char in filename.txt";
        String ws2 = " starts with space char";
        String ws3 = " starts and ends with space ";
        String ws4 = "ends with space ";
        
        filter.setGlob(" *");
        assertTrue("accept('"+ws0+"')", filter.accept(dir, ws0));
        assertTrue("accept('"+ws1+"')", filter.accept(dir, ws1));
        assertFalse("accept('"+ws2+"')", filter.accept(dir, ws2));
        assertFalse("accept('"+ws3+"')", filter.accept(dir, ws3));
        assertTrue("accept('"+ws4+"')", filter.accept(dir, ws4));
        
        filter.setGlob("* ");
        assertTrue("accept('"+ws0+"')", filter.accept(dir, ws0));
        assertTrue("accept('"+ws1+"')", filter.accept(dir, ws1));
        assertTrue("accept('"+ws2+"')", filter.accept(dir, ws2));
        assertFalse("accept('"+ws3+"')", filter.accept(dir, ws3));
        assertFalse("accept('"+ws4+"')", filter.accept(dir, ws4));
    }

}
