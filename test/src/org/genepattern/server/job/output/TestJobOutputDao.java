package org.genepattern.server.job.output;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.junit.Before;
import org.junit.Test;

public class TestJobOutputDao {

    private Integer gpJobNo=0;
    private File jobDir;
    private List<JobOutputFile> jobOutputFiles=Collections.emptyList();
    
    // check sort by path
    final String[] expectedPaths=new String[] {
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
        jobDir=FileUtil.getDataFile("jobResults/"+gpJobNo+"/").getAbsoluteFile();
        JobResultsLister lister=new JobResultsLister(""+gpJobNo, jobDir, JobOutputFile.initDefaultFilter());
        lister.walkFiles();
        jobOutputFiles=lister.getOutputFiles();
        DbUtil.initDb();
    }

    @Test
    public void recordOutputFiles() {
        JobOutputDao dao=new JobOutputDao();
        
        // Create
        dao.recordOutputFiles(jobOutputFiles);
        
        // Read
        boolean includeHidden=false;
        boolean includeDeleted=false;
        List<JobOutputFile> results=dao.selectOutputFiles(gpJobNo, includeHidden, includeDeleted);
        assertEquals("num items", 13, results.size());
        
        for(int i=0; i<expectedPaths.length; ++i) {
            assertEquals("results["+i+"]", expectedPaths[i], results.get(i).getPath());
        }
        
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
