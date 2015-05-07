/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.output;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestJobOutputDao {

    private Integer gpJobNo=null;
    private File jobDir;
    private List<JobOutputFile> jobOutputFiles=Collections.emptyList();
    
    // check sort by path
    final String[] expectedPaths=new String[] {
            // "", <-- working dir hidden by default
            ".gp_job_status",
            ".gp_job_status/readme.txt",
            ".lsf.out",
            "a",
            "a/b",
            "a/b/01.txt",
            "a/b/02.txt",
            "a/file1.txt",
            "a/file2.txt",
            "all_aml_test.comp.marker.odf",
            "all_aml_test.preprocessed.gct",
            // "gp_execution_log.txt",   <-- hidden by the default filter
            "stderr.txt",
            "stdout.txt"
    };

    @Before
    public void setUp() throws Exception {
        DbUtil.initDb();
        gpJobNo=new AnalysisJobUtil().addJobToDb();
        jobDir=FileUtil.getDataFile("jobResults/0/").getAbsoluteFile();
        JobResultsLister lister=new JobResultsLister(""+gpJobNo, jobDir, new DefaultGpFileTypeFilter());
        lister.walkFiles();
        jobOutputFiles=lister.getOutputFiles();
    }
    
    @After
    public void tearDown() {
        if (gpJobNo != null) {
            new AnalysisJobUtil().deleteJobFromDb(gpJobNo);
        }
    }

    @Test
    public void recordOutputFiles() throws DbException {
        JobOutputDao dao=new JobOutputDao();
        
        // Create
        assertEquals("num items to save", 15, jobOutputFiles.size());
        dao.recordOutputFiles(jobOutputFiles);
        
        // Read
        final boolean includeHidden=false;
        final boolean includeDeleted=false;
        List<JobOutputFile> results=dao.selectOutputFiles(gpJobNo, includeHidden, includeDeleted);
        assertEquals("num items", 13, results.size());
        
        for(int i=0; i<expectedPaths.length; ++i) {
            assertEquals("results["+i+"]", expectedPaths[i], results.get(i).getPath());
        }
        
        // Select GpExecutionLog
        List<JobOutputFile> gpExecutionLogs=dao.selectGpExecutionLogs(gpJobNo);
        assertEquals("gpExecutionLogs.size", 1, gpExecutionLogs.size());
        assertEquals("gpExecutionLogs[0].gpFileType", GpFileType.GP_EXECUTION_LOG.name(), gpExecutionLogs.get(0).getGpFileType());
        assertEquals("gpExecutionLogs[0].path", "gp_execution_log.txt", gpExecutionLogs.get(0).getPath());
        
        // Update
        // handle, onDeleteFile event by marking the entry as 'deleted'
        String relativePath="a/b/01.txt";
        dao.setDeleted(gpJobNo, relativePath);
        results=dao.selectOutputFiles(gpJobNo, includeHidden, includeDeleted);
        assertEquals("num items after deleting one item", 12, results.size());
        
        JobOutputFile modified=dao.selectOutputFile(gpJobNo,  relativePath);
        assertEquals("modified.deleted", true, modified.isDeleted());
        assertEquals("modified.fileLength", 0L, modified.getFileLength());
       
        // Delete
        boolean deleted=dao.deleteOutputFile(gpJobNo, relativePath);
        assertEquals("deleted", true, deleted);
    }

}
