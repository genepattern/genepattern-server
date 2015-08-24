/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.junitutil.ParameterInfoUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.job.input.TestLoadModuleHelper;
import org.genepattern.server.job.input.cache.CachedFtpFileType;
import org.genepattern.server.job.input.choice.ftp.FtpDirLister;
import org.genepattern.server.job.input.choice.ftp.FtpDirListerEdtFtpJ;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FileTransferClient;


/**
 * jUnit tests for the DynamicChoiceInfoParser class.
 * @author pcarr
 *
 */
public class TestDynamicChoiceInfoParser {
    private static final String gseaGeneSetsDatabaseChoicesFromManifest="noGeneSetsDB=;c1.all.v4.0.symbols.gmt=c1.all.v4.0.symbols.gmt [Positional];c2.all.v4.0.symbols.gmt=c2.all.v4.0.symbols.gmt [Curated];c2.cgp.v4.0.symbols.gmt=c2.cgp.v4.0.symbols.gmt [Curated];c2.cp.v4.0.symbols.gmt=c2.cp.v4.0.symbols.gmt [Curated];c2.cp.biocarta.v4.0.symbols.gmt=c2.cp.biocarta.v4.0.symbols.gmt [Curated];c2.cp.kegg.v4.0.symbols.gmt=c2.cp.kegg.v4.0.symbols.gmt [Curated];c2.cp.reactome.v4.0.symbols.gmt=c2.cp.reactome.v4.0.symbols.gmt [Curated];c3.all.v4.0.symbols.gmt=c3.all.v4.0.symbols.gmt [Motif];c3.mir.v4.0.symbols.gmt=c3.mir.v4.0.symbols.gmt [Motif];c3.tft.v4.0.symbols.gmt=c3.tft.v4.0.symbols.gmt [Motif];c4.all.v4.0.symbols.gmt=c4.all.v4.0.symbols.gmt [Computational];c4.cgn.v4.0.symbols.gmt=c4.cgn.v4.0.symbols.gmt [Computational];c4.cm.v4.0.symbols.gmt=c4.cm.v4.0.symbols.gmt [Computational];c5.all.v4.0.symbols.gmt=c5.all.v4.0.symbols.gmt [Gene Ontology];c5.bp.v4.0.symbols.gmt=c5.bp.v4.0.symbols.gmt [Gene Ontology];c5.cc.v4.0.symbols.gmt=c5.cc.v4.0.symbols.gmt [Gene Ontology];c5.mf.v4.0.symbols.gmt=c5.mf.v4.0.symbols.gmt [Gene Ontology];c6.all.v4.0.symbols.gmt=c6.all.v4.0.symbols.gmt [Oncogenic Signatures];c7.all.v4.0.symbols.gmt=c7.all.v4.0.symbols.gmt [Immunologic signatures]";
    private static List<Choice> gseaGeneSetsDatabaseChoices;
    
    private DynamicChoiceInfoParser choiceInfoParser=null;
    private File webappDir;
    private GpConfig gpConfig=null;
    private GpContext gpContext=null;
    private static final String choiceDir="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/";
    
    @BeforeClass
    public static void initClass() {
        gseaGeneSetsDatabaseChoices=new ArrayList<Choice>();
        //new Choice( <label>, <value> )
        gseaGeneSetsDatabaseChoices.add(new Choice("", "noGeneSetsDB"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c1.all.v4.0.symbols.gmt [Positional]", "c1.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.all.v4.0.symbols.gmt [Curated]", "c2.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cgp.v4.0.symbols.gmt [Curated]", "c2.cgp.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cp.v4.0.symbols.gmt [Curated]", "c2.cp.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cp.biocarta.v4.0.symbols.gmt [Curated]", "c2.cp.biocarta.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cp.kegg.v4.0.symbols.gmt [Curated]", "c2.cp.kegg.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cp.reactome.v4.0.symbols.gmt [Curated]", "c2.cp.reactome.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c3.all.v4.0.symbols.gmt [Motif]", "c3.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c3.mir.v4.0.symbols.gmt [Motif]", "c3.mir.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c3.tft.v4.0.symbols.gmt [Motif]", "c3.tft.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c4.all.v4.0.symbols.gmt [Computational]", "c4.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c4.cgn.v4.0.symbols.gmt [Computational]", "c4.cgn.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c4.cm.v4.0.symbols.gmt [Computational]", "c4.cm.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c5.all.v4.0.symbols.gmt [Gene Ontology]", "c5.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c5.bp.v4.0.symbols.gmt [Gene Ontology]", "c5.bp.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c5.cc.v4.0.symbols.gmt [Gene Ontology]", "c5.cc.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c5.mf.v4.0.symbols.gmt [Gene Ontology]", "c5.mf.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c6.all.v4.0.symbols.gmt [Oncogenic Signatures]", "c6.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c7.all.v4.0.symbols.gmt [Immunologic signatures]", "c7.all.v4.0.symbols.gmt"));
    }
    
    @Before
    public void beforeTest() throws MalformedURLException {
        webappDir=new File("website");
        gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .genePatternURL(new URL("http://127.0.0.1:8080/gp/"))
        .build();
        gpContext=new GpContext.Builder().build();
        choiceInfoParser=new DynamicChoiceInfoParser(gpConfig, gpContext);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseChoiceEntry_null() {
        // null arg should cause IllegalArgumentException
        ChoiceInfoHelper.initChoiceFromManifestEntry(null);
    }
    
    @Test
    public void testParseChoiceEntry_empty() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("");
        Choice expected=new Choice("");
        assertEquals("expected.label should be empty", "", expected.getLabel());
        assertEquals("expected.value should be empty", "", expected.getValue());
        assertEquals("choice from '='", expected, choice);
    }
    
    @Test
    public void testParseChoiceEntry_spaces() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry(" ");
        Choice expected=new Choice(" ");
        assertEquals("don't trim space characters", expected, choice);
    }

    @Test
    public void testParseChoiceEntry_spaces02() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("  =  ");
        Choice expected=new Choice("  ");
        assertEquals("don't trim space characters", expected, choice);
    }
    
    @Test
    public void testParseChoiceEntry_equalsOnly() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("=");
        Choice expected=new Choice("");
        assertEquals("choice from '='", expected, choice);
    }
    
    @Test
    public void testParseChoiceEntry_emptyLhs() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("=NonBlankDisplayValue");
        Choice expected=new Choice("NonBlankDisplayValue", "");
        assertEquals(expected, choice);
    }
    
    @Test
    public void testParseChoiceEntry_emptyRhs() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("NonBlankActualValue=");
        Choice expected=new Choice("", "NonBlankActualValue");
        assertEquals(expected, choice);
    }
    
    @Test
    public void testInitChoicesFromManifestEntry() {
        final List<Choice> choices=ChoiceInfoHelper.initChoicesFromManifestEntry(gseaGeneSetsDatabaseChoicesFromManifest);
        assertEquals(gseaGeneSetsDatabaseChoices, choices);
    }
    
    /**
     * Test the 'gene.sets.database' drop-down for GSEA v. 14.
     * It has an empty display value, 'noGeneSetsDB'
     */
    @Test
    public void testLegacyChoiceGsea14() {
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(TestDynamicChoiceInfoParser.class, "GSEA_v14.zip");
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        final ParameterInfoRecord pinfoRecord=paramInfoMap.get("gene.sets.database");
        
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfoRecord.getFormal());
        
        assertNotNull("Expecting a non-null choiceInfo", choiceInfo);
        final List<Choice> choices=choiceInfo.getChoices();
        assertThat("", choices, is(gseaGeneSetsDatabaseChoices));
    }
    
    /**
     * Legacy test, for some existing modules, such as ExtractComparativeMarkerResults, text drop-down with no default
     * value must default to the 1st item on the list, regardless of whether or not it is optional.
     */
    @Test
    public void testLegacyChoiceExtractComparativeMarkerResults() {
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(TestLoadModuleHelper.class, "ExtractComparativeMarkerResults_v4.zip");
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        final ParameterInfoRecord pinfoRecord=paramInfoMap.get("statistic");
        
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfoRecord.getFormal());
        assertNotNull("Expecting a non-null choiceInfo", choiceInfo);
        Choice selected=choiceInfo.getSelected();
        assertNotNull("Expecting a non-null choiceInfo#selected", selected);
        assertEquals("Checking default label", "Bonferroni", selected.getLabel());
        assertEquals("Checking default value", "Bonferroni", selected.getValue());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testLegacyOptionalDropdown() {
        final String value="arg1;arg2;arg3;arg4";
        
        ParameterInfo pinfo=new ParameterInfo("my.param", value, "A choice parameter with no default value");
        pinfo.setAttributes(new HashMap<String,String>());
        pinfo.getAttributes().put("default_value", "");
        pinfo.getAttributes().put("optional", "on");
        pinfo.getAttributes().put("prefix_when_specified", "");
        pinfo.getAttributes().put("type", "java.lang.String");
        
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);
        assertNotNull("Expecting a non-null choiceInfo", choiceInfo);
        Choice selected=choiceInfo.getSelected();
        assertNotNull("Expecting a non-null choiceInfo#selected", selected);
        assertEquals("Checking default label", "arg1", selected.getLabel());
        assertEquals("Checking default value", "arg1", selected.getValue());
    }
    
    private Choice makeChoice(final String choiceDir, final String entry, final boolean isDir) {
        final String label=entry;
        final String value=choiceDir+""+entry;
        return new Choice(label, value, isDir);
    }
    
    public static void assertChoiceInfo(final ChoiceInfo choiceInfo) {
        if (choiceInfo==null) {
            fail("expecting non-null choiceInfo");
        }
        if (choiceInfo.getStatus().getFlag() != ChoiceInfo.Status.Flag.OK) {
            fail(""+choiceInfo.getStatus());
        }
    }
    
    private void listCompare(final String message, final List<Choice> expected, final List<Choice> actual) {
        assertEquals(message+", num elements", expected.size(), actual.size());
        for(int i=0; i<expected.size(); ++i) {
            Choice expectedChoice=expected.get(i);
            Choice actualChoice=actual.get(i);
            assertEquals(message+" ["+i+"] equals", expectedChoice, actualChoice);
        }
    }

    @Test
    public void testFtpFileDropdown_defaultClient() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);
        assertChoiceInfo(choiceInfo);
        
        assertNotNull("choiceInfo.choices", choiceInfo.getChoices());
        final List<Choice> expected=Arrays.asList(new Choice[] {
                new Choice("", ""),
                makeChoice(choiceDir, "a.txt", false), 
                makeChoice(choiceDir, "b.txt", false), 
                makeChoice(choiceDir, "c.txt", false), 
                makeChoice(choiceDir, "d.txt", false), 
        });
        
        listCompare("drop-down items", expected, choiceInfo.getChoices());
        assertEquals("selected", new Choice("", ""), choiceInfo.getSelected());
    }
    
    @Test
    public void testFtpFileDropdown_edtFtpJClient() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        
        GpServerProperties serverProperties=new GpServerProperties.Builder()
            .addCustomProperty(CachedFtpFileType.PROP_FTP_DOWNLOADER_TYPE, CachedFtpFileType.EDT_FTP_J.name())
            .build();
        GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .serverProperties(serverProperties)
            .build();
        
        choiceInfoParser=new DynamicChoiceInfoParser(gpConfig, gpContext);
        
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);
        
        assertNotNull("choiceInfo.choices", choiceInfo.getChoices());
        final List<Choice> expected=Arrays.asList(new Choice[] {
                new Choice("", ""),
                makeChoice(choiceDir, "a.txt", false), 
                makeChoice(choiceDir, "b.txt", false), 
                makeChoice(choiceDir, "c.txt", false), 
                makeChoice(choiceDir, "d.txt", false), 
        });
        
        listCompare("drop-down items", expected, choiceInfo.getChoices());
        assertEquals("selected", new Choice("", ""), choiceInfo.getSelected());
    }

    @Test
    public void testFtpFileDropdown_commonsNetClient() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(CachedFtpFileType.PROP_FTP_DOWNLOADER_TYPE, CachedFtpFileType.COMMONS_NET_3_3.name())
        .build();
        choiceInfoParser=new DynamicChoiceInfoParser(gpConfig, gpContext);
        
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);
        
        assertNotNull("choiceInfo.choices", choiceInfo.getChoices());
        final List<Choice> expected=Arrays.asList(new Choice[] {
                new Choice("", ""),
                makeChoice(choiceDir, "a.txt", false), 
                makeChoice(choiceDir, "b.txt", false), 
                makeChoice(choiceDir, "c.txt", false), 
                makeChoice(choiceDir, "d.txt", false), 
        });
        
        listCompare("drop-down items", expected, choiceInfo.getChoices());
        assertEquals("selected", new Choice("", ""), choiceInfo.getSelected());
    }

    
    /**
     * test case: don't do a remote listing
     */
    @Test
    public void testFtpFileDropdown_noListing() {
        choiceInfoParser=new DynamicChoiceInfoParser(gpConfig, gpContext, false);
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);
        assertNotNull("choiceInfo.choices", choiceInfo.getChoices());
        assertEquals("Expecting an empty list", 0, choiceInfo.getChoices().size());
        assertEquals("Expecting an un-initialized drop-down", 
                ChoiceInfo.Status.Flag.NOT_INITIALIZED, 
                choiceInfo.getStatus().getFlag());
    }

    /**
     * This is not for automated testing because it fails about half the time.
     * Note the bogus assert statement which always passes.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDynamicDropdownGsea() {
        final String ftpUrl="ftp://gseaftp.broadinstitute.org/pub/gsea/annotations/";
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam(
                "input.file", 
                ftpUrl);
        final String choiceDirFilter="*.chip";
        pinfo.getAttributes().put("choiceDirFilter", choiceDirFilter);

        // set custom.properties to use a 30 second timeout 
        GpServerProperties serverProperties=new GpServerProperties.Builder()
            .addCustomProperty(FtpDirLister.PROP_FTP_DATA_TIMEOUT, "30000")
            .addCustomProperty(FtpDirLister.PROP_FTP_SOCKET_TIMEOUT, "30000")
            .build();
        GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .serverProperties(serverProperties)
            .build();
        GpContext gpContext=new GpContext.Builder().build();
        choiceInfoParser=new DynamicChoiceInfoParser(gpConfig, gpContext);
        
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);
        assertEquals("num choices from "+ftpUrl, 145, choiceInfo.getChoices().size());
    }

    /**
     * A required parameter with a default value should have an empty item at the start of the drop-down.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testFtpFileDropdown_requiredWithDefaultValue() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        pinfo.getAttributes().put("default_value", choiceDir+"a.txt");

        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);
        
        assertNotNull("choiceInfo.choices", choiceInfo.getChoices());
        final List<Choice> expected=Arrays.asList(new Choice[] {
                new Choice("", ""), 
                makeChoice(choiceDir, "a.txt", false), 
                makeChoice(choiceDir, "b.txt", false), 
                makeChoice(choiceDir, "c.txt", false), 
                makeChoice(choiceDir, "d.txt", false), 
        });
        listCompare("drop-down items", expected, choiceInfo.getChoices());
        assertEquals("selected", makeChoice(choiceDir, "a.txt", false), choiceInfo.getSelected());
    }

    /**
     * An optional parameter with a default value should have an empty item at the start of the drop-down.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testFtpFileDropdown_optionalWithDefaultValue() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        pinfo.getAttributes().put("default_value", choiceDir+"a.txt");
        pinfo.getAttributes().put("optional", "on");
        
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);
        
        assertNotNull("choiceInfo.choices", choiceInfo.getChoices());
        final List<Choice> expected=Arrays.asList(new Choice[] {
                new Choice("", ""), 
                makeChoice(choiceDir, "a.txt", false), 
                makeChoice(choiceDir, "b.txt", false), 
                makeChoice(choiceDir, "c.txt", false), 
                makeChoice(choiceDir, "d.txt", false), 
        });
        listCompare("drop-down items", expected, choiceInfo.getChoices());
        assertEquals("selected", makeChoice(choiceDir, "a.txt", false), choiceInfo.getSelected());
    }

    /**
     * Test with a default value which doesn't match any items in the remote directory.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testFtpFileDropdown_defaultValue_noMatch() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        pinfo.getAttributes().put("default_value", choiceDir+"no_match.txt");

        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);
        
        assertNotNull("choiceInfo.choices", choiceInfo.getChoices());
        final List<Choice> expected=Arrays.asList(new Choice[] {
                new Choice("", ""), 
                makeChoice(choiceDir, "a.txt", false), 
                makeChoice(choiceDir, "b.txt", false), 
                makeChoice(choiceDir, "c.txt", false), 
                makeChoice(choiceDir, "d.txt", false), 
        });
        listCompare("drop-down items", expected, choiceInfo.getChoices());
        assertEquals("selected", new Choice("",""), choiceInfo.getSelected());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFtpDirectoryDropdown() throws InterruptedException {        
        final String choiceDirFilter="type=dir";
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        pinfo.getAttributes().put("choiceDirFilter", choiceDirFilter);

        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfo);

        assertNotNull("choiceInfo.choices", choiceInfo.getChoices());
        final List<Choice> expected=Arrays.asList(new Choice[] {
                new Choice("", ""),
                makeChoice(choiceDir, "A", true), 
                makeChoice(choiceDir, "B", true), 
                makeChoice(choiceDir, "C", true), 
                makeChoice(choiceDir, "D", true), 
                makeChoice(choiceDir, "E", true), 
                makeChoice(choiceDir, "F", true), 
                makeChoice(choiceDir, "G", true), 
                makeChoice(choiceDir, "H", true), 
                makeChoice(choiceDir, "I", true), 
                makeChoice(choiceDir, "J", true), 
        });
        
        listCompare("drop-down items", expected, choiceInfo.getChoices());
    }
    
    /**
     * test case: a directory selected from the drop-down, where the value does not include a trailing slash.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testChoiceInfoGetValue_noTrailingSlash() {
        final String valueNoSlash="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A";
        final String choiceDirFilter="type=dir";
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        pinfo.getAttributes().put("choiceDirFilter", choiceDirFilter);
        final ChoiceInfo choiceInfo = choiceInfoParser.initChoiceInfo(pinfo);
        
        final Choice expected=makeChoice(choiceDir, "A", true);
        assertEquals("getValue, no slash", expected, choiceInfo.getValue(valueNoSlash));
    }
 
    /**
     * test case: a directory selected from the drop-down, where the value includes a trailing slash.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testChoiceInfoGetValue_withTrailingSlash() {
        final String value="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/";
        final String choiceDirFilter="type=dir";
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        pinfo.getAttributes().put("choiceDirFilter", choiceDirFilter);
        final ChoiceInfo choiceInfo = choiceInfoParser.initChoiceInfo(pinfo);
        
        final Choice expected=makeChoice(choiceDir, "A", true);
        assertEquals("getValue, no slash", expected, choiceInfo.getValue(value));
    }
    
    @Test
    public void edtftp_concurrent_pasv() throws Exception {
        final URL url=new URL(choiceDir);
        final GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(FtpDirLister.PROP_FTP_USERNAME, "anonymous")
            .addProperty(FtpDirLister.PROP_FTP_PASSWORD, "gp-help@broadinstitute.org")
            .addProperty(FtpDirLister.PROP_FTP_SOCKET_TIMEOUT, "60000")
        .build();
        final boolean passiveMode=true;
        FtpDirListerEdtFtpJ lister01=FtpDirListerEdtFtpJ.createFromConfig(gpConfig, gpContext, passiveMode);
        FtpDirListerEdtFtpJ lister02=FtpDirListerEdtFtpJ.createFromConfig(gpConfig, gpContext, passiveMode);
        FileTransferClient client01=lister01.initFtpClient(url);
        FileTransferClient client02=lister02.initFtpClient(url);
        FTPFile[] ftpFiles01=client01.directoryList(url.getPath());
        assertEquals("num files 01, no filter", 15, ftpFiles01.length);
        FTPFile[] ftpFiles02=client02.directoryList(url.getPath());
        assertEquals("num files 02, no filter", 15, ftpFiles02.length);
        
        assertEquals("num files 01", 4, lister01.listFiles(choiceDir, new DirFilter()).size());
        assertEquals("num files 02", 4, lister02.listFiles(choiceDir, new DirFilter()).size());
    }

    @Test
    public void edtftp_concurrent_active() throws Exception {
        final URL url=new URL(choiceDir);
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(FtpDirLister.PROP_FTP_USERNAME, "anonymous")
            .addProperty(FtpDirLister.PROP_FTP_PASSWORD, "gp-help@broadinstitute.org")
            .addProperty(FtpDirLister.PROP_FTP_SOCKET_TIMEOUT, "60000")
        .build();
        final boolean passiveMode=false;
        FtpDirListerEdtFtpJ lister01=FtpDirListerEdtFtpJ.createFromConfig(gpConfig, gpContext, passiveMode);
        FtpDirListerEdtFtpJ lister02=FtpDirListerEdtFtpJ.createFromConfig(gpConfig, gpContext, passiveMode);
        FileTransferClient client01=lister01.initFtpClient(url);
        FileTransferClient client02=lister02.initFtpClient(url);
        FTPFile[] ftpFiles01=client01.directoryList(url.getPath());
        assertEquals("num files 01, no filter", 15, ftpFiles01.length);
        FTPFile[] ftpFiles02=client02.directoryList(url.getPath());
        assertEquals("num files 02, no filter", 15, ftpFiles02.length);
        
        assertEquals("num files 01", 4, lister01.listFiles(choiceDir, new DirFilter()).size());
        assertEquals("num files 02", 4, lister02.listFiles(choiceDir, new DirFilter()).size());
    }

}
