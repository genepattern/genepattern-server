package org.genepattern.server.executor.lsf;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.genepattern.server.executor.CommandProperties;
import org.genepattern.webservice.JobInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * junit tests for setting a custom job group with the original (<= 3.8.1) LsfCommandExecutor.
 * @author pcarr
 *
 */
public class TestCustomJobGroup {
    @Test
    public void customLsfGroup() {
        String[] commandLine={"echo", "Hello, World!"};
        Map<String, String> env=Collections.emptyMap();
        File runDir=new File("jobResults/1");
        File stdoutFile=null;
        File stderrFile=null;
        JobInfo jobInfo=null;
        File stdinFile=null;
        
        Properties jobProps=new Properties();
        jobProps.put("lsf.group", "/genepattern/gpprod/long");
            

        LsfCommand lsfCommand=new LsfCommand();
        lsfCommand.setLsfProperties(new CommandProperties(jobProps));
        lsfCommand.runCommand(commandLine, env, runDir ,stdoutFile, stderrFile, jobInfo, stdinFile, LsfJobCompletionListener.class);
        lsfCommand.getLsfJob().getCommand();
        
        Assert.assertEquals(
            "custom -m and -g args",
            Arrays.asList(new String[]{"-R", "rusage[mem=2]", "-M", "2", "-g", "/genepattern/gpprod/long"}),
            lsfCommand.getLsfJob().getExtraBsubArgs());
    }

}
