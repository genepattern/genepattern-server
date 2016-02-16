/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm.impl.lsf.core;

import static org.junit.Assert.assertEquals;

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
        List<String> lines=new CommonsExecCmdRunner().runCmd(cmd);
        Assert.assertEquals("lines.size", 7, lines.size());
    }
    
    @Test
    public void noArgTest() throws CmdException {
        List<String> lines=new CommonsExecCmdRunner().runCmd(Arrays.asList(new String[]{"echo"}));
        assertEquals("lines.size", 1, lines.size());
    }

    /** expecting a timeout exception */
    @Test(expected=CmdException.class)
    public void timeoutTest() throws CmdException {
        new CommonsExecCmdRunner(1000L)
            .runCmd(
                Arrays.asList(new String[]{"sleep", "60"}));
    }

}
