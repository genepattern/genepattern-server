/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import org.junit.Assert;
import org.junit.Test;


/**
 * jUnit tests for the Choice class.
 * @author pcarr
 *
 */
public class TestChoice {
    final String value="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt";
    final String dirLabel="A";
    final String dirValueNoSlash="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A";
    final String dirValue=dirValueNoSlash+"/";
    
    @Test
    public void testFileDropdown() {
        final Choice choice=new Choice(value);
        Assert.assertEquals("choice.getValue", value, choice.getValue());
        Assert.assertEquals("default label is the value", value, choice.getLabel());
        Assert.assertEquals("default isRemoteDir flag", false, choice.isRemoteDir());
    }
    
    @Test
    public void testDirectoryDropdown() {
        final boolean isDir=true;
        final Choice choice=new Choice(dirLabel, dirValue, isDir);
        
        Assert.assertEquals(dirValue, choice.getValue());
    }

    @Test
    public void testDirectoryDropdown_appendSlash() {
        final boolean isDir=true;
        final Choice choice=new Choice(dirLabel, dirValueNoSlash, isDir);
        
        Assert.assertEquals("automatically append '/' to value", dirValue, choice.getValue());
    }
    
    @Test
    public void testEquals_fileDropdown() {
        final Choice choice=new Choice(value);
        Assert.assertEquals(new Choice(value), choice);
    }

    /**
     * The equals method should ignore the label.
     */
    @Test
    public void testEqualsWithDifferentLabel() {
        final String value=dirValue;
        final Choice choice1=new Choice("This is a label", value);
        final Choice choice2=new Choice("A different label", value);
        
        Assert.assertTrue("choice1.equals(choice2)", choice1.equals(choice2));
        Assert.assertTrue("choice2.equals(choice1)", choice2.equals(choice1));
    }
    
    /**
     * The equals method should *not* ignore the directory flag.
     */
    @Test
    public void testEqualsWithDirFlag() {
        final String value=dirValue;
        final boolean isDir=false;
        final Choice choice1=new Choice("A", value, isDir);
        final Choice choice2=new Choice("A", value, !isDir);
        
        Assert.assertFalse("choice1.equals(choice2)", choice1.equals(choice2));
        Assert.assertFalse("choice2.equals(choice1)", choice2.equals(choice1));
    }
    
    @Test
    public void testEqualsIgnoreTrailingSlash() {
        final boolean ignoreCase=true;
        slashTest(true, "/", "", !ignoreCase);
        slashTest(true, dirValue, dirValueNoSlash, !ignoreCase);
        slashTest(true, dirValue.toUpperCase(), dirValue.toLowerCase(), ignoreCase); 
        slashTest(false, dirValue.toUpperCase(), dirValue.toLowerCase(), !ignoreCase); 
    }
    
    private void slashTest(final boolean expected, final String lval, final String rval, final boolean ignoreCase) {
        Assert.assertEquals("\""+lval+"\".equals(\""+rval+"\"), ignoreCase="+ignoreCase, expected, Choice.equalsIgnoreTrailingSlash(lval, rval, ignoreCase));
        Assert.assertEquals("\""+rval+"\".equals(\""+lval+"\"), ignoreCase="+ignoreCase, expected, Choice.equalsIgnoreTrailingSlash(rval, lval, ignoreCase));
    }

}
