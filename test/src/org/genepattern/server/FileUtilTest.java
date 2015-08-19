/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

/**
 * junit test cases for FileUtil#getRelativePath.
 * @author pcarr
 *
 */
public class FileUtilTest {
    final static String usera = new String("user_a");
    final static String userb = new String("user_b");
    
    File userRootDir = new File("users");
    File aHome = new File(userRootDir, usera);
    File bHome = new File(userRootDir, userb);
    File a01 = new File(aHome, "1.txt");
    File a02 = new File(aHome, "2.txt");
    File b01 = new File(bHome, "1.txt");
    File b02 = new File(bHome, "2.txt");
    File serverFile01 = new File("test1.txt");

    @Test
    public void testGetRelativePath() {
        runTest(aHome, a01, "1.txt");
        runTest(aHome, a02, "2.txt");
        runTest(aHome, b01, null);
        runTest(aHome, b02, null);
        
        //more test cases
        runTest(aHome, userRootDir, null);
        runTest(aHome, aHome, null);
        
        //sub directories
        runTest(aHome, new File(aHome, "sub/test4.txt"), "sub/test4.txt");
        
        //and some corner cases
        File oddPath = new File( new File("users/"), usera );
        runTest(oddPath, new File(oddPath, "test3.txt"), "test3.txt");
    }
    
    private void runTest(File parent, File child, String expected) {
        String rval = FileUtil.getRelativePath(parent, child);
        assertEquals("", expected, rval);
    }

}
