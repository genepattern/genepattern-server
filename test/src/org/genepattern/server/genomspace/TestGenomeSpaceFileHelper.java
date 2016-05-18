package org.genepattern.server.genomspace;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.genepattern.server.genomespace.GenomeSpaceFileHelper;
import org.junit.Test;

public class TestGenomeSpaceFileHelper {
    // default GS url
    final String gsUrl="https://dm.genomespace.org";
    // default data file path, relative to GS url file prefix
    final String filePath="/Home/userId/all_aml_test.tab";

    @Test
    public void httpProtocol() throws MalformedURLException, URISyntaxException {
        final String inputSpec="http://dm.genomespace.org/datamanager/file"+filePath;
        final String  expected="http://dm.genomespace.org/datamanager/v1.0/file"+filePath;
        assertEquals(new URL(expected), GenomeSpaceFileHelper.insertProtocolVersion(new URL(inputSpec)));
    }

    @Test
    public void noDataFormat() throws MalformedURLException, URISyntaxException {
        final String inputSpec=gsUrl+"/datamanager/file"+filePath;
        final String  expected=gsUrl+"/datamanager/v1.0/file"+filePath;
        assertEquals(new URL(expected), GenomeSpaceFileHelper.insertProtocolVersion(new URL(inputSpec)));
    }
    
    @Test
    public void withDataFormat() throws MalformedURLException, URISyntaxException {
        final String inputSpec=gsUrl+"/datamanager/file"+filePath+"?dataformat=http://www.genomespace.org/datamanager/dataformat/gct";
        final String  expected=gsUrl+"/datamanager/v1.0/file"+filePath+"?dataformat=http://www.genomespace.org/datamanager/dataformat/gct";
        assertEquals(new URL(expected), GenomeSpaceFileHelper.insertProtocolVersion(new URL(inputSpec)));
    }
    
    @Test
    public void fromGoogleDrive() throws MalformedURLException {
        final String inputSpec=gsUrl+"/datamanager/file/Home/googledrive:test@email.com(lHv4L0eliPcV19HRCqwWQg==)/GenomeSpacePublic/all_aml(0Bx1oidMlPWQtN2RlQV8zd25Md1E)/all_aml_test.cls?dataformat=http://www.genomespace.org/datamanager/dataformat/gct";
        final String  expected=gsUrl+"/datamanager/v1.0/file/Home/googledrive:test@email.com(lHv4L0eliPcV19HRCqwWQg==)/GenomeSpacePublic/all_aml(0Bx1oidMlPWQtN2RlQV8zd25Md1E)/all_aml_test.cls?dataformat=http://www.genomespace.org/datamanager/dataformat/gct";
        assertEquals(new URL(expected), GenomeSpaceFileHelper.insertProtocolVersion(new URL(inputSpec)));
    }

    // input.path already starts with /datamanager/v1.0
    @Test
    public void noInsertNeeded() throws MalformedURLException {
        final String inputSpec=gsUrl+"/datamanager/v1.0/file"+filePath+"?dataformat=http://www.genomespace.org/datamanager/dataformat/gct";
        final String  expected=gsUrl+"/datamanager/v1.0/file"+filePath+"?dataformat=http://www.genomespace.org/datamanager/dataformat/gct";
        assertEquals(new URL(expected), GenomeSpaceFileHelper.insertProtocolVersion(new URL(inputSpec)));
    }
    
    
}
