package org.genepattern.server.job.output;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Date;

import org.genepattern.junitutil.FileUtil;
import org.junit.Before;
import org.junit.Test;

public class TestJobOutputFile {
    private Integer jobId=0;
    private File jobDir;
    private File jobDirAbs;
    private File resultFile;
    private File resultFileAbs;
    
    @Before
    public void setUp() {
        jobDir=FileUtil.getDataFile("jobResults/"+jobId+"/");
        jobDirAbs=jobDir.getAbsoluteFile();
        resultFile=new File(jobDirAbs, "all_aml_test.preprocessed.gct");
        resultFileAbs=resultFile.getAbsoluteFile();
    }
    
    @Test
    public void fromAbsoluePaths() throws Exception {
        JobOutputFile out=JobOutputFile.from(""+jobId, jobDirAbs, resultFileAbs);
        assertEquals("out.path", "all_aml_test.preprocessed.gct", out.getPath());
    }

    @Test
    public void fromRelativePaths() throws Exception {
        JobOutputFile out=JobOutputFile.from(""+jobId, jobDir, resultFile);
        assertEquals("out.path", "all_aml_test.preprocessed.gct", out.getPath());
    }
    
    @Test
    public void fromAbsDirRelativePath() throws Exception {
        JobOutputFile out=JobOutputFile.from(""+jobId, jobDir, new File("all_aml_test.preprocessed.gct"));
        assertEquals("out.path", "all_aml_test.preprocessed.gct", out.getPath());
        assertEquals("out.gpJobNo", 0, (int) out.getGpJobNo());
        assertEquals("out.fileLength", 1519460L, out.getFileLength());
        
        assertEquals("out.lastModified", new Date(resultFileAbs.lastModified()), out.getLastModified());
        assertEquals("out.hidden", false, out.isHidden());
        assertEquals("out.deleted", false, out.isDeleted());
    }
    
    @Test
    public void nestedResultFile() throws Exception {
        JobOutputFile out=JobOutputFile.from(""+jobId, jobDir, new File("a/file1.txt"));
        assertEquals("out.path", "a/file1.txt", out.getPath());
        assertEquals("out.fileLength", 39L, out.getFileLength());
    }
    
    @Test
    public void relativePath() throws Exception {
        JobOutputFile out=JobOutputFile.from(""+jobId, jobDir, "all_aml_test.preprocessed.gct");
        assertEquals("out.path", "all_aml_test.preprocessed.gct", out.getPath());
        assertEquals("out.gpJobNo", 0, (int) out.getGpJobNo());
        assertEquals("out.fileLength", 1519460L, out.getFileLength());
        
        assertEquals("out.lastModified", new Date(resultFileAbs.lastModified()), out.getLastModified());
        assertEquals("out.hidden", false, out.isHidden());
        assertEquals("out.deleted", false, out.isDeleted());
    }
    
    @Test
    public void jobDir() throws Exception {
        JobOutputFile out=JobOutputFile.from(""+jobId, jobDir, new File(""));
        assertEquals("out.path", "", out.getPath());
        assertEquals("out.kind", "directory", out.getKind());
        assertEquals("out.fileLength", jobDirAbs.length(), out.getFileLength());
    }
    
    @Test
    public void deletedFile() throws Exception {
        JobOutputFile out=JobOutputFile.from(""+jobId, jobDir, "file_does_not_exist.txt");
        assertEquals("out.path", "file_does_not_exist.txt", out.getPath());
        assertEquals("out.fileLength", 0L, out.getFileLength());
    }
}
