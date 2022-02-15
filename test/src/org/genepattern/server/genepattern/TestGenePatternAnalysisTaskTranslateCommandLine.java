/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit test {@link GenePatternAnalysisTask#translateCommandline(String[])}.
 * @author pcarr
 */
public class TestGenePatternAnalysisTaskTranslateCommandLine {
    /**
     * Unit test based on GP-2936, RT-130573.
     * A module command line with double quotes surrounding more than one input parameter must collapse 
     * those into a single command line arg, quotes included, to the ProcessBuilder.
     */
    @Test
    public void testMatlabExample() {
        //final String moduleCommandLineSpec = "/broad/tools/apps/matlab2009b/bin/matlab -nosplash -r \"MTestCase <p1> <p2> <p3> <p4> <p5>\"";
        //final String moduleCommandLineWithArgs = "/broad/tools/apps/matlab2009b/bin/matlab -nosplash -r \"MTestCase Hello! this is my testcase.\"";
        
        final String[] inputCommandLine = {
                "/broad/tools/apps/matlab2009b/bin/matlab",
                "-nosplash",
                "-r",
                "\"MTestCase",
                "Hello!",
                "this", 
                "is",
                "my",
                "testcase.\"",
        };
        
        String[] outputCommandLine = GenePatternAnalysisTask.translateCommandline(inputCommandLine);
        assertEquals("num args", 4, outputCommandLine.length);
        String output = outputCommandLine[3];
        String expected = "\"MTestCase Hello! this is my testcase.\"";
        assertEquals(expected, output);
    }
    
    /**
     * Test with spaces in arguments.
     */
    public void testSpacesInArgs() {
        final String[] inputCommandLine = {
                "/usr/bin/java",
                "-cp",
                "~/classes/my libraries/a.jar:~/classes/my libraries/b.jar",
                "MyModule",
                "arg one",
                "arg two",                
        };
        final String[] outputCommandLine = GenePatternAnalysisTask.translateCommandline(inputCommandLine);
        assertEquals("num args", 6, outputCommandLine.length);
    }
    
    public void testCornerCasesWithQuotesInArgs() {
        final String[] inputCommandLine = {
                "arg1",
                "arg2 \"startspan and end span\" arg3",
                "arg4 with single \" quote char",
        };
        
        String[] outputCommandLine = GenePatternAnalysisTask.translateCommandline(inputCommandLine);
        assertEquals("num args", 3, outputCommandLine.length);
        String output = outputCommandLine[2];
        String expected = "arg4 with single \" quote char";
        assertEquals(expected, output);        
    }
}
