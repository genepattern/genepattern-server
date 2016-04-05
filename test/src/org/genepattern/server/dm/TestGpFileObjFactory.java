package org.genepattern.server.dm;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.*;

import java.net.MalformedURLException;

import org.genepattern.junitutil.Demo;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.util.LSID;

import org.junit.Before;
import org.junit.Test;

public class TestGpFileObjFactory {
    private GpConfig gpConfig;
    private static final String filename="all_aml_test.gct";
    private GpFilePath gpFilePath;
    
    @Before
    public void setUp() {
        gpConfig=gpConfig();
    }
    
    @Test
    public void getRequestedGpFileObj() throws MalformedURLException, Exception {
        gpFilePath=GpFileObjFactory.getRequestedGpFileObj(gpConfig, gpHref+uploadPath(filename), new LSID(cleLsid));
        assertEquals("gpFilePath.name", filename, gpFilePath.getName());
    }

    @Test
    public void getRequestedGpFileObj_fromProxyHref() throws MalformedURLException, Exception {
        gpFilePath=GpFileObjFactory.getRequestedGpFileObj(gpConfig, //proxyHref, 
                proxyHref+uploadPath(filename), new LSID(cleLsid));
        assertEquals("gpFilePath.name", filename, gpFilePath.getName());
    }
    
    @Test
    public void getRequestedGpFileObj_jobResult() throws MalformedURLException, Exception {
        gpFilePath=GpFileObjFactory.getRequestedGpFileObj(gpConfig, //proxyHref,
                proxyHref+Demo.jobResultPath(jobId, filename), new LSID(cleLsid));
        
        assertEquals("gpFilePath.name", filename, gpFilePath.getName());
        assertEquals("servletPath", "/jobResults", 
                "/"+gpFilePath.getRelativeUri().toString().split("/")[1]);
        assertEquals("instanceof JobResultFile", true, (gpFilePath instanceof JobResultFile));
    }
    
    @Test
    public void relativeUri_serverFile() throws Exception {
        gpFilePath=GpFileObjFactory.getRequestedGpFileObj("/data", "/"+Demo.localDataDir+filename);
        assertEquals("gpFilePath.name", filename, gpFilePath.getName());
    }

    @Test(expected=Exception.class)
    public void getRequestedGpFileObj_fromExternalUrl_ftp() throws MalformedURLException, Exception {
        //gpFilePath=
                GpFileObjFactory.getRequestedGpFileObj(gpConfig, //proxyHref, 
                        dataFtpDir+filename, new LSID(cleLsid));
    }

    @Test(expected=Exception.class)
    public void getRequestedGpFileObj_fromExternalUrl_http() throws MalformedURLException, Exception {
        //gpFilePath=
                GpFileObjFactory.getRequestedGpFileObj(gpConfig, //proxyHref, 
                        dataHttpDir +filename, new LSID(cleLsid));
    }

    @Test(expected=Exception.class)
    public void getRequestedGpFileObj_fromExternalUrl_genomespace() throws MalformedURLException, Exception {
        //gpFilePath=
                GpFileObjFactory.getRequestedGpFileObj(gpConfig, //proxyHref, 
                        dataGsDir +filename, new LSID(cleLsid));
    }
    
    @Test
    public void getRequestedJobResultFileObj_gpUrl_literal() throws Exception {
        final GpFilePath gpFilePath=GpFileObjFactory.getRequestedJobResultFileObj(
                gpConfig, "<GenePatternURL>jobResults/1/all_aml_test.gct");
        assertNotNull(gpFilePath);
        assertEquals("gpFilePath.name", "all_aml_test.gct", gpFilePath.getName());
        assertTrue(gpFilePath instanceof JobResultFile);
    }

    @Test
    public void getRequestedJobResultFileObj_gpUrl() throws Exception {
        final GpFilePath gpFilePath=GpFileObjFactory.getRequestedJobResultFileObj(
                gpConfig, Demo.gpUrl + "jobResults/1/all_aml_test.gct");
        assertNotNull(gpFilePath);
        assertEquals("gpFilePath.name", "all_aml_test.gct", gpFilePath.getName());
        assertTrue(gpFilePath instanceof JobResultFile);
    }

    @Test
    public void getRequestedJobResultFileObj_proxyUrl() throws Exception {
        final GpFilePath gpFilePath=GpFileObjFactory.getRequestedJobResultFileObj(
                gpConfig, Demo.proxyUrl + "jobResults/1/all_aml_test.gct");
        assertNotNull(gpFilePath);
        assertEquals("gpFilePath.name", "all_aml_test.gct", gpFilePath.getName());
        assertTrue(gpFilePath instanceof JobResultFile);
    }
    
}
