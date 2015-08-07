/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.output;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.attribute.BasicFileAttributes;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.util.JobResultsFilenameFilter;
import org.junit.Before;
import org.junit.Test;

public class TestDefaultGpFileTypeFilter {
    // need to create actual paths in order to determine if it's a file or directory
    private File jobDir;
    private DefaultGpFileTypeFilter filter;
    
    public BasicFileAttributes initFileAttrs(File jobDir, File relativeFile) {
        BasicFileAttributes attrs=JobOutputFile.initFileAttributes(
                new File(jobDir, relativeFile.getPath()).toPath());
        return attrs;
    }
    
    @Before
    public void setUp() {
        jobDir=FileUtil.getDataFile("jobResults/0/").getAbsoluteFile();
        filter=new DefaultGpFileTypeFilter();
        
        GpConfig gpConfig=new GpConfig.Builder()
            .serverProperties(new GpServerProperties.Builder()
                .addCustomProperty("job.FilenameFilter", ".lsf.out")
            .build())
        .build();
        GpContext gpContext = GpContext.getServerContext();
        JobResultsFilenameFilter filenameFilter = JobOutputFile.initFilterFromConfig(gpConfig,gpContext);
        filter.setJobResultsFilenameFilter(filenameFilter);
    }
    
    private void doTest(final String relativePath, final GpFileType expectedType) {
        File relativeFile=new File(relativePath);
        BasicFileAttributes attrs=initFileAttrs(jobDir, relativeFile);
        GpFileType fileType=filter.getGpFileType(jobDir, relativeFile, attrs);
        assertEquals(relativePath, expectedType, fileType);
    }

    @Test
    public void jobDir() {
        doTest("", GpFileType.GP_JOB_DIR);
    }
    
    @Test
    public void outputFile() {
        doTest("all_aml_test.preprocessed.gct", GpFileType.FILE);
    }
    
    @Test
    public void nestedOutputFile() {
        doTest("a/file1.txt", GpFileType.FILE);
    }

    @Test
    public void outputDir() {
        doTest("a", GpFileType.DIR);
    }

    @Test
    public void nestedOutputDir() {
        doTest("a/b", GpFileType.DIR);
    }
    
    @Test
    public void hiddenFile() {
        doTest(".lsf.out", GpFileType.GP_HIDDEN_FILE);
    }
    
    @Test
    public void gpExecutionLog() {
        doTest("gp_execution_log.txt", GpFileType.GP_EXECUTION_LOG);
    }
    
    @Test
    public void pipelineExecutionLog() {
        doTest("testParallelExec_execution_log.html", GpFileType.GP_PIPELINE_LOG);
    }
    
    @Test
    public void defaultStdout() {
        doTest("stdout.txt", GpFileType.STDOUT);
    }
    
    @Test
    public void defaultStderr() {
        doTest("stderr.txt", GpFileType.STDERR);
    }
    
    @Test
    public void customStdout() {
        filter.setStdoutFilename("my_stdout");
        doTest("my_stdout", GpFileType.STDOUT);
    }
    
}
