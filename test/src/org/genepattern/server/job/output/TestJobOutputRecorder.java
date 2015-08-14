/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.output;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.junit.Before;
import org.junit.Test;

/**
 * junit test cases for recording job output files.
 * @author pcarr
 *
 */
public class TestJobOutputRecorder {
    private HibernateSessionManager mgr;
    private GpConfig gpConfig=null;
    private GpContext gpContext=null;

    private Integer gpJobNo;
    private File jobDir;
    
    @Before
    public void setUp() throws Exception {
        mgr=DbUtil.getTestDbSession();
        gpJobNo=AnalysisJobUtil.addJobToDb(mgr);
        jobDir=FileUtil.getDataFile("jobResults/0/").getAbsoluteFile();
        gpContext=new GpContext.Builder()
            .jobNumber(gpJobNo)
            .build();
    }

    @Test
    public void onJobCompletion() throws Exception {
        JobOutputRecorder.recordOutputFilesToDb(mgr, gpConfig, gpContext, jobDir);

        List<JobOutputFile> results;
        boolean includeHidden=true;
        boolean includeDeleted=false;
        try {
            mgr.beginTransaction();
            JobOutputDao dao=new JobOutputDao(mgr);
            results=dao.selectOutputFiles(gpJobNo, includeHidden, includeDeleted);
        }
        finally {
            mgr.closeCurrentSession();
        }
        
        assertEquals("number of records in database", 15, results.size());
        
        long totalFileSize=0L;
        int numFiles=0;
        for(final JobOutputFile out : results) {
            if (!out.isHidden() && !"directory".equals(out.getKind())) {
                System.out.println(out.getPath());
                ++numFiles;
                totalFileSize += out.getFileLength();
            }
        }
        assertEquals("num non-hidden regular files", 10, numFiles);
        assertEquals("total file size in bytes", 3533748, totalFileSize); 
        
        // test cascade delete
        AnalysisJobUtil.deleteJobFromDb(mgr, gpJobNo);
        try {
            mgr.beginTransaction();
            JobOutputDao dao=new JobOutputDao(mgr);
            results=dao.selectOutputFiles(gpJobNo, includeHidden, includeDeleted);
        }
        finally {
            mgr.closeCurrentSession();
        }
        assertEquals("results.size after cascade delete", 0, results.size());
    }
}
