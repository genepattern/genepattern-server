package org.genepattern.server.job.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.jobinput.ParameterInfoUtil;
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
    private String ftpValue="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt";
    private String httpValue="http://www.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_train.cls";

    private HibernateSessionManager mgr;
    GpConfig gpConfig;
    GpContext jobContext;
    ParameterInfo formalParam;
    ParamValue ftpVal;
    ParamValue httpVal;
    JobInput jobInput;
    
    // setup download folder(s)
    @Rule
    public TemporaryFolder temp= new TemporaryFolder();
    private File gpHomeDir;

    @Before
    public void setUp() throws Exception {
        mgr=DbUtil.getTestDbSession();
        formalParam= ParameterInfoUtil.initFilelistParam("input.files");
        jobInput=new JobInput();
        jobInput.addValue("input.files", ftpValue);
        jobInput.addValue("input.files", httpValue);
        jobContext=mock(GpContext.class);
        when(jobContext.getUserId()).thenReturn(userId);
        ftpVal=jobInput.getParam("input.files").getValues().get(0);
        httpVal=jobInput.getParam("input.files").getValues().get(1);
        
        gpHomeDir=temp.newFolder("gpHome");
        gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
        .build();
    }

    /**
     * Simulate server configured cache.
     * <pre>
     *     cache.externalUrlDirs: ftp://gpftp.broadinstitute.org/
     * </pre>
     * @return
     */
    protected GpContext initCachedContext() {
        GpContext mockContext=new GpContext.Builder()
            .userId(userId)
        .build();
        
        // need to customize the properties
        gpConfig=new GpConfig.Builder()
            .addProperty(UrlPrefixFilter.PROP_CACHE_EXTERNAL_URL, "ftp://gpftp.broadinstitute.org/")
            .gpHomeDir(gpHomeDir)
        .build();
        
        Value value=gpConfig.getValue(mockContext, UrlPrefixFilter.PROP_CACHE_EXTERNAL_URL);
        assertNotNull("expecting non-null '"+UrlPrefixFilter.PROP_CACHE_EXTERNAL_URL+"'", value);
        assertEquals("ftp://gpftp.broadinstitute.org/", value.getValues().get(0));
        return mockContext;
    }
    
    /**
     * by default, externalUrl values are cached to the user tmp directory.
     * @throws Exception
     */
    @Test
    public void ftpInput() throws Exception {
        Record record=ParamListHelper.initFromValue(gpConfig, jobContext, formalParam, ftpVal); 
        assertEquals("record.url", ftpValue, record.getUrl().toString());
        assertEquals("record.type",  Record.Type.EXTERNAL_URL, record.type);
        assertEquals("record.isCached", false, record.isCached);
        assertEquals("record.isPassByReference", false, record.isPassByReference);
        assertEquals("gpFilePath.owner", userId, record.getGpFilePath().getOwner());
        // saved to user's 'tmp/external' directory
        assertEquals("gpFilePath.serverFile, before download", 
                new File(gpHomeDir,"users/"+userId+"/uploads/tmp/external/gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt"), 
                record.getGpFilePath().getServerFile());
    }
    
    @Test
    public void httpInput() throws Exception {
        Record record=ParamListHelper.initFromValue(gpConfig, jobContext, formalParam, httpVal); 
        assertEquals("record.url", httpValue, record.getUrl().toString());
        assertEquals("record.type",  Record.Type.EXTERNAL_URL, record.type);
        assertEquals("record.isCached", false, record.isCached);
        assertEquals("record.isPassByReference", false, record.isPassByReference);
        assertEquals("gpFilePath.owner", userId, record.getGpFilePath().getOwner());
        // saved to user's 'tmp/external' directory
        assertEquals("gpFilePath.serverFile, before download", 
                new File(gpHomeDir,"users/"+userId+"/uploads/tmp/external/www.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_train.cls"),
                record.getGpFilePath().getServerFile());
    }

    @Test
    public void ftpInput_cached() throws Exception {
        GpContext mockContext = initCachedContext();
        
        //final boolean downloadExternalUrl=true;
        Record record=ParamListHelper.initFromValue(gpConfig, mockContext, formalParam, ftpVal); 
        assertEquals("record.url", ftpValue, record.getUrl().toString());
        assertEquals("record.type",  Record.Type.EXTERNAL_URL, record.type);
        assertEquals("record.isCached", true, record.isCached);
        assertEquals("record.isPassByReference", false, record.isPassByReference);
        assertEquals("gpFilePath.owner", FileCache.CACHE_USER_ID, record.getGpFilePath().getOwner());
        assertEquals("gpFilePath.serverFile, before download", 
                new File(gpHomeDir,"users/"+FileCache.CACHE_USER_ID+"/uploads/cache/gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt"), 
                record.getGpFilePath().getServerFile());
        
        assertEquals("gpFilePath.serverFile.exists, before download",
                false,
                record.getGpFilePath().getServerFile().exists());

        DbUtil.initDb();
        ParamListHelper.downloadFromRecord(mgr, gpConfig, mockContext, record);
        assertEquals("gpFilePath.serverFile.exists, after download",
                true,
                record.getGpFilePath().getServerFile().exists());
    }

    @Test
    public void httpInput_not_cached() throws Exception {
        final GpContext mockContext = initCachedContext();

        Record record=ParamListHelper.initFromValue(gpConfig, mockContext, formalParam, httpVal); 
        assertEquals("record.url", httpValue, record.getUrl().toString());
        assertEquals("record.type",  Record.Type.EXTERNAL_URL, record.type);
        assertEquals("record.isCached", false, record.isCached);
        assertEquals("record.isPassByReference", false, record.isPassByReference);
        assertEquals("gpFilePath.owner", userId, record.getGpFilePath().getOwner());
        // saved to user's 'tmp/external' directory
        assertEquals("gpFilePath.serverFile, before download", 
                new File(gpHomeDir,"users/"+userId+"/uploads/tmp/external/www.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_train.cls"),
                record.getGpFilePath().getServerFile());
        
        DbUtil.initDb();
        ParamListHelper.downloadFromRecord(mgr, gpConfig, mockContext, record);
        assertEquals("gpFilePath.serverFile.exists, after download",
                true,
                record.getGpFilePath().getServerFile().exists());

    }

}
