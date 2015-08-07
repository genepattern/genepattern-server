/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.scattergather;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.executor.lsf.LsfCommandExecutor;
import org.genepattern.server.executor.lsf.LsfProperties;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.webservice.JobInfo;

public class ScatterGatherCommandExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(ScatterGatherCommandExecutor.class);

    private String lsfCommandExecutorName;
	private String scatterGatherScriptLocation;

	public void setConfigurationFilename(final String filename) {
        log.error("ignoring: setConfigurationFilename("+filename+"): must set configuration.properties directly in the job configuration file!");
	}

	public void setConfigurationProperties(final CommandProperties properties) {
		this.lsfCommandExecutorName = properties.getProperty("lsf.executor");
		this.scatterGatherScriptLocation = properties.getProperty("scatter.gather.script.location");
	}

	public void start() {
	}

	public void stop() {
	}

	public void runCommand(final String[] commandLine,
			final Map<String, String> environmentVariables, final File runDir,
			final File stdoutFile, final File stderrFile, final JobInfo jobInfo, final File stdinFile)
			throws CommandExecutorException {

        final CommandProperties commandProperties = CommandManagerFactory.getCommandManager().getCommandProperties(jobInfo);
        final String libDir = getLibDir(jobInfo);

        final List<String> newCommandLine = new ArrayList<String>(commandLine.length + 8);
        newCommandLine.add(this.scatterGatherScriptLocation);
        newCommandLine.add("--job-suffix");
        newCommandLine.add(String.valueOf(jobInfo.getJobNumber()));
        final String priority = commandProperties.getProperty(LsfProperties.Key.PRIORITY.getKey());
        if (priority != null) {
        	newCommandLine.add("--priority");
        	newCommandLine.add(priority);
        }
        newCommandLine.add(commandProperties.getProperty("scatter.gather.queue"));
        newCommandLine.add(libDir + commandProperties.getProperty("scatter.gather.xml"));
        newCommandLine.add(libDir);
        
        newCommandLine.addAll(Arrays.asList(commandLine));
		
		getLsfCommandExecutor().runCommand(newCommandLine.toArray(new String[newCommandLine.size()]), environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile, LsfScatterGatherJobCompletionListener.class);
	}

    private String getLibDir(final JobInfo jobInfo) throws CommandExecutorException {
    	try {
    		return new File(DirectoryManager.getLibDir(jobInfo.getTaskLSID())).getAbsolutePath() + File.separator;
    	} catch (final Exception e) {
    		throw new CommandExecutorException("Exception getting libdir", e);
    	}
    }

    public void terminateJob(final JobInfo jobInfo) throws Exception {
		getLsfCommandExecutor().terminateJob(jobInfo);
	}

	public int handleRunningJob(final JobInfo jobInfo) throws Exception {
		return getLsfCommandExecutor().handleRunningJob(jobInfo);
	}
	
	private LsfCommandExecutor getLsfCommandExecutor() {
		return (LsfCommandExecutor) CommandManagerFactory.getCommandManager().getCommandExecutorsMap().get(this.lsfCommandExecutorName);
	}
}
