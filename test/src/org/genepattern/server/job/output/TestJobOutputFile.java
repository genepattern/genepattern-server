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
    private File outputDir;
    private File odfFile;
    private File gctFile;
    
    @Before
    public void setUp() {
        jobDir=FileUtil.getDataFile("jobResults/"+jobId+"/");
        jobDirAbs=jobDir.getAbsoluteFile();
        resultFile=new File(jobDirAbs, "all_aml_test.preprocessed.gct");
        resultFileAbs=resultFile.getAbsoluteFile();
        
        outputDir=FileUtil.getDataFile("jobResults/0/a/");
        odfFile=FileUtil.getDataFile("jobResults/0/all_aml_test.comp.marker.odf");
        gctFile=FileUtil.getDataFile("jobResults/0/all_aml_test.preprocessed.gct");

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
        assertEquals("out.kind", "gct", out.getKind());
        assertEquals("out.extension", "gct", out.getExtension());
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

    @Test
    public void extensionFromDirectory() {
        String extension=JobOutputFile.initExtension(outputDir);
        assertEquals("outputDir.extension", "", extension);
    }
    
    @Test
    public void extensionsFromOdf() {
        assertEquals("odfFile.extension", "odf", JobOutputFile.initExtension(odfFile));
    }
    
    @Test 
    public void extensionFromGct() {
        assertEquals("gctFile.extension", "gct", JobOutputFile.initExtension(gctFile));
    }
    
    @Test
    public void extensionFromNull() {
        assertEquals("null.extension", "", JobOutputFile.initExtension(null));
    }
    
    
    @Test
    public void kindFromDirectory() {
        String kind=JobOutputFile.initKind(outputDir);
        assertEquals("outputDir.kind", "directory", kind);
    }
    
    @Test
    public void kindFromOdf() {
        assertEquals("odfFile.kind", "Comparative Marker Selection", JobOutputFile.initKind(odfFile));
    }
    
    @Test 
    public void kindFromGct() {
        assertEquals("gctFile.kind", "gct", JobOutputFile.initExtension(gctFile));
    }

    @Test
    public void kindFromNull() {
        assertEquals("null.kind", "", JobOutputFile.initKind(null));
    }

}
