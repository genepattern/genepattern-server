package org.genepattern.server.genomespace;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.genepattern.junitutil.Demo;
import org.genomespace.client.DataManagerClient;
import org.genomespace.client.GsSession;
import org.genomespace.datamanager.core.GSDataFormat;
import org.genomespace.datamanager.core.GSFileMetadata;
import org.genomespace.datamanager.core.impl.GSDataFormatImpl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * junit tests for {@link GenomeSpaceFile#getConversionUrls()}
 * 
 * @author pcarr
 */
@RunWith(Parameterized.class)
public class TestGenomeSpaceFileGetConversionUrls {
    @Parameters(name="{0}")
    public static Collection<Object[]> tests() {
        return Arrays.asList(new Object[][] {
                // test with unversioned end-point, https://dm.genomespace.org/datamanager/
                {"no version, datamanager/_path_", Demo.dataGsDir_noVersion+"all_aml_test.tab"},
                // test with v1.0 end-point, https://dm.genomespace.org/datamanager/v1.0/
                {"v1.0, datamanager/v1.0/_path_", Demo.dataGsDir+"all_aml_test.tab"},
        });
    }
    
    // mock availableDataFormats
    private static Map<String,GSDataFormat> dataFormatMap;
    private static Set<GSDataFormat> availableDataFormats;

    protected static Map<String,GSDataFormat> initDataFormatMap() {
        Map<String,GSDataFormat> dataFormatMap=new HashMap<String,GSDataFormat>();
        // name, description, url, extension
        GSDataFormat tabFormat=new GSDataFormatImpl("genomicatab", 
                "Tab delimited expression format for Genomica.",
                "http://www.genomespace.org/datamanager/dataformat/genomicatab", 
                "tab" );
        GSDataFormat gctFormat=new GSDataFormatImpl("gct", 
                "Mock gct format",
                "http://www.genomespace.org/datamanager/dataformat/gct", 
                "gct" );
        dataFormatMap.put("tab", tabFormat);
        dataFormatMap.put("gct", gctFormat);
        return dataFormatMap;
    }

    protected static GenomeSpaceFile initGsFile(final GsSession gsSession, final String urlSpec, final GSDataFormat gsDataFormat) throws MalformedURLException {
        URL url=new URL(urlSpec);
        return initGsFile(gsSession, url, gsDataFormat);
    }
    
    protected static GenomeSpaceFile initGsFile(final GsSession gsSession, final URL url, final GSDataFormat gsDataFormat) throws MalformedURLException {
        final GSFileMetadata metadata=mock(GSFileMetadata.class);
        when(metadata.getAvailableDataFormats()).thenReturn(availableDataFormats);
        when(metadata.getUrl()).thenReturn(url);

        final DataManagerClient dmClient=mock(DataManagerClient.class);
        when(dmClient.getFileUrl(metadata, gsDataFormat)).thenReturn(
            new URL(url.toString()+"?dataformat="+gsDataFormat.getUrl().toString()));

        when(gsSession.getDataManagerClient()).thenReturn(dmClient);        
        return GenomeSpaceFileHelper.createFile(gsSession, url, metadata);
    }
    
    @BeforeClass
    public static void beforeClass() throws MalformedURLException {
        dataFormatMap=initDataFormatMap();
        availableDataFormats=new HashSet<GSDataFormat>(dataFormatMap.values());
    }

    private GsSession gsSession;
    private GenomeSpaceFile gsFile;
    private URL urlNoVersion;
    private final String expectedUrl=Demo.dataGsDir+"all_aml_test.tab";

    // constructor
    public TestGenomeSpaceFileGetConversionUrls(final String _testName, final String urlIn) throws MalformedURLException {
        urlNoVersion=new URL(urlIn);
        gsSession=mock(GsSession.class);
        gsFile=initGsFile(gsSession, urlNoVersion, dataFormatMap.get("gct"));
    }

    // parameterized test-cases ... 
    @Test
    public void getName() {
        assertEquals("validate setUp, gsFile.getName", "all_aml_test.tab", gsFile.getName());
    }
    
    @Test
    public void getExtension() {
        assertEquals("validate setUp, gsFile.getExtension", "tab", gsFile.getExtension());
    }
    
    @Test
    public void getConvertedUrl_Tab() {
        assertEquals( "getConvertedUrl('tab'), convert from base fileType", 
                expectedUrl, 
                gsFile.getConvertedUrl("tab"));
    }

    @Test
    public void getConvertedUrl_Gct() {
        assertEquals( "getConvertedUrl('gct'), convert from 'tab' to 'gct'",
                expectedUrl+"?dataformat=http://www.genomespace.org/datamanager/dataformat/gct", 
                gsFile.getConvertedUrl("gct"));
    }

    @Test
    public void getConversionUrls() throws Exception {
        final Map<String,String> urls=gsFile.getConversionUrls();
        
        // expect tab -> tab, tab -> gct
        assertEquals("get, format='tab'", 
                "https%3A%2F%2Fdm.genomespace.org%2Fdatamanager%2Fv1.0%2Ffile%2FHome%2FPublic%2FSharedData%2FDemos%2FSampleData%2Fall_aml_test.tab",  
                urls.get("tab"));
        assertEquals("get, format='gct', converted from 'tab'", 
                "https%3A%2F%2Fdm.genomespace.org%2Fdatamanager%2Fv1.0%2Ffile%2FHome%2FPublic%2FSharedData%2FDemos%2FSampleData%2Fall_aml_test.tab%3Fdataformat%3Dhttp%3A%2F%2Fwww.genomespace.org%2Fdatamanager%2Fdataformat%2Fgct", 
                urls.get("gct"));
    }
    
    @Test
    public void getConvertedUrl_baseFileType() throws Exception {
        assertEquals( "getConvertedUrl('tab'), convert from base fileType", 
                expectedUrl, 
                GenomeSpaceClient.getConvertedUrl(gsSession, gsFile, "tab").toString());
    }

    @Test
    public void getConvertedUrl_convertedFileType() throws Exception { 
        assertEquals( "getConvertedUrl('gct'), convert from 'tab' to 'gct'",
                expectedUrl+"?dataformat=http://www.genomespace.org/datamanager/dataformat/gct", 
                GenomeSpaceClient.getConvertedUrl(gsSession, gsFile, "gct").toString());
    }

    @Test
    public void getConvertedUrl_emptyFormat() throws Exception {
        assertEquals( "gsFile.getConvertedUrl('')",
                expectedUrl, 
                GenomeSpaceClient.getConvertedUrl(gsSession, gsFile, "").toString());
    }

    @Test
    public void getConvertedUrl_nullFormat() throws Exception {
        assertEquals( "getFile.convertedUrl(null)",
                expectedUrl,
                GenomeSpaceClient.getConvertedUrl(gsSession, gsFile, null).toString());
    }
    
    @Test(expected=GenomeSpaceException.class)
    public void getConvertedUrl_nullFile() throws GenomeSpaceException {
        final GsSession mockGsSession=mock(GsSession.class);
        // case 1, file == null
        GenomeSpaceClient.getConvertedUrl(mockGsSession, null, "tab");
    }
    
    @Test(expected=GenomeSpaceException.class)
    public void getConvertedUrl_nullGsSession() throws Exception {
        final GsSession nullGsSession=null;
        // for a converted file type, GsSession must be non-null
        GenomeSpaceClient.getConvertedUrl(nullGsSession, gsFile, "gct");
    }

    @Test(expected=GenomeSpaceException.class)
    public void getConvertedUrl_nullMetadata() throws Exception {
        final GsSession mockGsSession=mock(GsSession.class);
        final GenomeSpaceFile gsFile=mock(GenomeSpaceFile.class);
        when(gsFile.getExtension()).thenReturn("tab");
        assertEquals("converting from 'tab' to 'gct'",
                gsFile.getUrl(), 
                GenomeSpaceClient.getConvertedUrl(mockGsSession, gsFile, "gct"));
    }

}
