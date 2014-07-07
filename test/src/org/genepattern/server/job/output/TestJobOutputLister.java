package org.genepattern.server.job.output;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.util.JobResultsFilenameFilter;
import org.junit.Before;
import org.junit.Test;

public class TestJobOutputLister {

    private String jobId="0";
    private File jobDir;

    @Before
    public void setUp() {
        jobDir=FileUtil.getDataFile("jobResults/"+jobId+"/").getAbsoluteFile();
    }
    
    @Test
    public void listResultFiles_noFilter() throws IOException {
        JobResultsLister lister=new JobResultsLister(jobId, jobDir);
        lister.walkFiles();
        List<JobOutputFile> outputFiles=lister.getOutputFiles();
        assertEquals("num files", 14, outputFiles.size());
    }
    
    @Test
    public void listResultFiles_defaultFilter() throws IOException {
        JobResultsFilenameFilter filenameFilter = JobOutputFile.initFilterFromConfig((GpConfig)null,(GpContext)null);
        JobResultsLister lister=new JobResultsLister(jobId, jobDir, filenameFilter);
        lister.walkFiles();
        List<JobOutputFile> outputFiles=lister.getOutputFiles();
        assertEquals("num files", 13, outputFiles.size());
        List<JobOutputFile> hiddenFiles=lister.getHiddenFiles();
        assertEquals("num hidden", 1, hiddenFiles.size());
        assertEquals("hidden[0].isHidden", true, hiddenFiles.get(0).isHidden());
    }

    @Test
    public void listResultFiles_customFilter() throws IOException {
        GpConfig gpConfig=new GpConfig.Builder()
        .serverProperties(new GpServerProperties.Builder()
            .addCustomProperty("job.FilenameFilter", ".lsf.out")
            .build())
        .build();
        GpContext gpContext = GpContext.getServerContext();
        JobResultsFilenameFilter filenameFilter = JobOutputFile.initFilterFromConfig(gpConfig,gpContext);
        
        JobResultsLister lister=new JobResultsLister(jobId, jobDir, filenameFilter);
        lister.walkFiles();
        List<JobOutputFile> outputFiles=lister.getOutputFiles();
        assertEquals("num files", 12, outputFiles.size());
        List<JobOutputFile> hiddenFiles=lister.getHiddenFiles();
        assertEquals("num hidden", 2, hiddenFiles.size());
        assertEquals("hidden[0].isHidden", true, hiddenFiles.get(0).isHidden());
    }

}
