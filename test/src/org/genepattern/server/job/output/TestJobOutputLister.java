package org.genepattern.server.job.output;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.util.JobResultsFilenameFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestJobOutputLister {

    private String jobId="0";
    private File jobDir;
    private DefaultGpFileTypeFilter filter;
    // check sort by path
    private final String[] expectedPaths=new String[] {
            "./", // <-- working dir, hidden by default filter
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
            "gp_execution_log.txt",   // <-- hidden by the default filter
            "stderr.txt",
            "stdout.txt"
    };

    @Before
    public void setUp() {
        jobDir=FileUtil.getDataFile("jobResults/0/").getAbsoluteFile();
        filter=new DefaultGpFileTypeFilter();
    }
    
    @Test
    public void listResultFiles_noFilter() throws IOException {
        JobResultsLister lister=new JobResultsLister(jobId, jobDir);
        lister.walkFiles();
        lister.sortByPath();
        List<JobOutputFile> outputFiles=lister.getOutputFiles();
        
        String[] actualPaths=new String[outputFiles.size()];
        for(int i=0; i<outputFiles.size(); ++i) {
            actualPaths[i] = outputFiles.get(i).getPath();
        }
        Assert.assertArrayEquals("expected paths", expectedPaths, actualPaths);
        
        assertEquals("num files", 15, outputFiles.size());
        
        //expect the first entry to be the job_dir
        assertEquals("outputFiles[0].path", "./", outputFiles.get(0).getPath());
        assertEquals("outputFiles[0].gpFileType", GpFileType.GP_JOB_DIR.name(), outputFiles.get(0).getGpFileType());

        //expect the next entry to be an output_dir
        assertEquals("outputFiles[1].path", ".gp_job_status", outputFiles.get(1).getPath());
        assertEquals("outputFiles[1].gpFileType", GpFileType.DIR.name(), outputFiles.get(1).getGpFileType());

        //expect the next entry to be an output_file
        assertEquals("outputFiles[2].path", ".gp_job_status/readme.txt", outputFiles.get(2).getPath());
        assertEquals("outputFiles[2].gpFileType", GpFileType.FILE.name(), outputFiles.get(2).getGpFileType());

        //expect the next entry to be an output_dir
        assertEquals("outputFiles[4].path", "a", outputFiles.get(4).getPath());
        assertEquals("outputFiles[4].gpFileType", GpFileType.DIR.name(), outputFiles.get(4).getGpFileType());
    }
    
    @Test
    public void listResultFiles_defaultFilter() throws IOException {
        JobResultsLister lister=new JobResultsLister(jobId, jobDir, filter);
        lister.walkFiles();
        List<JobOutputFile> outputFiles=lister.getOutputFiles();
        assertEquals("num files", 15, outputFiles.size());
        List<JobOutputFile> hiddenFiles=lister.getHiddenFiles();
        assertEquals("num hidden", 2, hiddenFiles.size());
        assertEquals("hidden[0].isHidden", true, hiddenFiles.get(0).isHidden());
        assertEquals("hidden[0].gpFileType", GpFileType.GP_JOB_DIR.name(), hiddenFiles.get(0).getGpFileType());
        assertEquals("hidden[1].isHidden", true, hiddenFiles.get(1).isHidden());
        assertEquals("hidden[1].gpFileType", GpFileType.GP_EXECUTION_LOG.name(), hiddenFiles.get(1).getGpFileType());
    }

    @Test
    public void listResultFiles_customFilter() throws IOException, ConfigurationException {
        Value globPatterns=new Value(Arrays.asList(".lsf.out", ".gp_job_status"));
        GpConfig gpConfig=Mockito.mock(GpConfig.class);
        GpContext gpContext = GpContext.getServerContext();
        Mockito.when(gpConfig.getValue(gpContext, "job.FilenameFilter")).thenReturn(globPatterns);
        
        JobResultsFilenameFilter filenameFilter = JobOutputFile.initFilterFromConfig(gpConfig,gpContext);
        filter.setJobResultsFilenameFilter(filenameFilter);
        JobResultsLister lister=new JobResultsLister(jobId, jobDir, filter);
        lister.walkFiles();
        List<JobOutputFile> outputFiles=lister.getOutputFiles();
        assertEquals("num files", 15, outputFiles.size());
        List<JobOutputFile> hiddenFiles=lister.getHiddenFiles();
        assertEquals("num hidden", 4, hiddenFiles.size());
        assertEquals("hidden[0].isHidden", true, hiddenFiles.get(0).isHidden());
    }
    
}
