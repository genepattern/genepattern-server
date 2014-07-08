package org.genepattern.server.job.output;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.output.dao.JobOutputDao;
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
        gpContext=new GpContextFactory.Builder()
            .jobNumber(0)
            .build();
    }

    @Test
    public void onJobCompletion() throws IOException {
        JobOutputRecorder.recordOutputFilesToDb(gpConfig, gpContext, jobDir);

        List<JobOutputFile> results;
        try {
            HibernateUtil.beginTransaction();
            JobOutputDao dao=new JobOutputDao();
            boolean includeHidden=true;
            boolean includeDeleted=false;
            results=dao.selectOutputFiles(gpJobNo, includeHidden, includeDeleted);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        
        assertEquals("number of records in database", 14, results.size());
        
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
    }
}
