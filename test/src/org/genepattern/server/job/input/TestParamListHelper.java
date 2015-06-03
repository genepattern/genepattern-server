package org.genepattern.server.job.input;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.job.input.ParamListHelper.Record;
import org.genepattern.server.job.input.cache.FileCache;
import org.genepattern.server.util.UrlPrefixFilter;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Testing the 'cache.externalDir'
 * @author pcarr
 *
 */
public class TestParamListHelper {
    
    final String userId="testUser";
    private String selectedValue="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt";

    GpConfig gpConfig;
    GpContext jobContext;
    ParameterInfo formalParam;
    ParamValue pval;
    JobInput jobInput;
    
    // setup download folder(s)
    @Rule
    public TemporaryFolder temp= new TemporaryFolder();
    private File gpHomeDir;

    @Before
    public void setUp() {
        jobInput=new JobInput();
        jobInput.addValue("input.files", selectedValue);
        jobContext=mock(GpContext.class);
        when(jobContext.getUserId()).thenReturn(userId);
        pval=jobInput.getParam("input.files").getValues().get(0);
        
        gpHomeDir=temp.newFolder("gpHome");
        gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
        .build();
    }

    @Test
    public void externalUrl_noDownload() throws Exception {
        final boolean downloadExternalUrl=false;
        Record record=ParamListHelper.initFromValue(gpConfig, jobContext, formalParam, pval, downloadExternalUrl);
        
        // by default, externalUrl as cached to user directory
        assertEquals("record.url", selectedValue, record.getUrl().toString());
        assertEquals("gpFilePath.serverFile", null, record.getGpFilePath().getServerFile());
    }
    
    /**
     * by default, externalUrl values are cached to the user tmp directory.
     * @throws Exception
     */
    @Test
    public void externalUrl() throws Exception {
        GpContext mockContext=new GpContext.Builder()
            .userId(userId)
        .build();
        
        final boolean downloadExternalUrl=true;
        Record record=ParamListHelper.initFromValue(gpConfig, mockContext, formalParam, pval, downloadExternalUrl); 
        assertEquals("record.url", selectedValue, record.getUrl().toString());
        assertEquals("record.type",  Record.Type.EXTERNAL_URL, record.type);
        // saved to user's 'tmp/external' directory
        assertEquals("gpFilePath.serverFile, before download", 
                new File(gpHomeDir,"users/"+userId+"/uploads/tmp/external/gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt"), 
                record.getGpFilePath().getServerFile());
    }

    @Test
    public void externalUrl_cached() throws Exception {
        GpContext mockContext=new GpContext.Builder()
            .userId(userId)
        .build();
        
        // need to customize the properties
        gpConfig=new GpConfig.Builder()
            .addProperty(UrlPrefixFilter.PROP_CACHE_EXTERNAL_URL, "ftp://gpftp.broadinstitute.org/")
            .gpHomeDir(gpHomeDir)
        .build();
        
        Value value=gpConfig.getValue(mockContext, UrlPrefixFilter.PROP_CACHE_EXTERNAL_URL);
        assertNotNull(value);
        assertEquals("ftp://gpftp.broadinstitute.org/", value.getValues().get(0));
        
        final boolean downloadExternalUrl=true;
        Record record=ParamListHelper.initFromValue(gpConfig, mockContext, formalParam, pval, downloadExternalUrl); 
        assertEquals("record.url", selectedValue, record.getUrl().toString());
        assertEquals("gpFilePath.serverFile, before download", null, record.getGpFilePath().getServerFile());
        assertEquals("record.type",  Record.Type.EXTERNAL_URL, record.type);

        //TODO: run the download test
        //assertEquals("gpFilePath.owner", userId, record.getGpFilePath().getOwner());
        
        //ParamListHelper.downloadFromRecord(gpConfig, mockContext, record);
        //assertEquals("gpFilePath.serverFile, after download", 
        //        new File(gpHomeDir,"users/"+FileCache.CACHE_USER_ID+"/uploads/cache/gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt"), 
        //        record.getGpFilePath().getServerFile());

        
    }
}
