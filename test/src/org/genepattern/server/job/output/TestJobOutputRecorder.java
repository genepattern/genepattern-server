package org.genepattern.server.job.output;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.genepattern.server.util.JobResultsFilenameFilter;
import org.junit.Before;
import org.junit.Test;

/**
 * junit test cases for recording job output files.
 * @author pcarr
 *
 */
public class TestJobOutputRecorder {
    GpConfig gpConfig=null;
    GpContext gpContext=null;
    Integer gpJobNo=0;
    File jobDir;
    
    @Before
    public void setUp() throws Exception {
        DbUtil.initDb();
        jobDir=FileUtil.getDataFile("jobResults/"+gpJobNo+"/").getAbsoluteFile();
    }

    @Test
    public void onJobCompletion() throws IOException {
        JobResultsFilenameFilter filenameFilter = JobOutputFile.initFilterFromConfig(gpConfig,gpContext);
        JobResultsLister lister=new JobResultsLister(""+gpJobNo, jobDir, filenameFilter);
        lister.walkFiles();
        List<JobOutputFile> jobOutputFiles=lister.getOutputFiles();

        List<JobOutputFile> results;
        try {
            HibernateUtil.beginTransaction();
            JobOutputDao dao=new JobOutputDao();
            dao.recordOutputFiles(jobOutputFiles);
            HibernateUtil.commitTransaction();
        
            HibernateUtil.beginTransaction();

            boolean includeHidden=true;
            boolean includeDeleted=false;
            results=dao.selectOutputFiles(gpJobNo, includeHidden, includeDeleted);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        
        long total=0L;
        for(final JobOutputFile out : results) {
            total+=out.getFileLength();
        }
        assertEquals("total size in bytes", 3534258, total);
    }
}
