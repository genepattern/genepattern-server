/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm.impl.lsf.core;

import java.util.Arrays;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.junit.Assert;
import org.junit.Test;

public class TestCommonsExecCmdRunner {
    /**
     * for testing the runCmd method, run 'ls -1 /{path to}/jobResults/0' command line.
     */
    @Test
    public void runCmd_ls() throws Exception {
        String path = FileUtil.getDataFile("jobResults/0").getAbsolutePath();
        List<String> cmd=Arrays.asList(new String[]{"ls", "-1", path});
        int numHeaderLines=1; //skip the 1st line
        List<String> lines=new CommonsExecCmdRunner(numHeaderLines).runCmd(cmd);
        Assert.assertEquals("lines.size", 6, lines.size());
    }

}
