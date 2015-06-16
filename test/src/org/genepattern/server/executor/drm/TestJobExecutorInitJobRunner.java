/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.drm;

import static org.junit.Assert.assertEquals;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;
import org.junit.Assert;
import org.junit.Test;

public class TestJobExecutorInitJobRunner {
    public static final class MyJobRunner implements JobRunner {
        private static final Logger log = Logger.getLogger(MyJobRunner.class);
        private int numThreads=20;
        private boolean started=false;

        public MyJobRunner() {
        }

        public void setCommandProperties(CommandProperties properties) {
            String numThreadsProp=null;
            if (properties != null) {
                numThreadsProp=properties.getProperty("num.threads");
            }
            if (numThreadsProp != null) {
                try {
                    numThreads = Integer.parseInt(numThreadsProp);
                }
                catch (Throwable t) {
                    log.error("Error parsing num.threads="+numThreadsProp);
                }
            }
        }
        
        public void start() {
            this.started=true;
        }

        public int getNumThreads() {
            return numThreads;
        }

        @Override
        public void stop() {
            // TODO Auto-generated method stub

        }

        @Override
        public String startJob(DrmJobSubmission drmJobSubmission) throws CommandExecutorException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DrmJobStatus getStatus(DrmJobRecord drmJobRecord) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean cancelJob(DrmJobRecord drmJobRecord) throws Exception {
            // TODO Auto-generated method stub
            return false;
        }
    }
    
    @Test
    public void getPropertyFromConfigProperties() {
        CommandProperties props=new CommandProperties();
        props.put("num.threads", "1");
        MyJobRunner jobRunner = (MyJobRunner) JobExecutor.initJobRunner(MyJobRunner.class.getName(), props);
        assertEquals("expected numThreads", 1, jobRunner.getNumThreads());
        assertEquals("is started", true, jobRunner.started);
        
    }
}
