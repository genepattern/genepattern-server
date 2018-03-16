package org.genepattern.server.executor.awsbatch;

import static org.genepattern.server.executor.awsbatch.AwsS3Filter.PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Arrays;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;

import org.junit.Test;

public class TestAwsS3Filter {
    private static File dataDir=initDataDir();

    /** 
     * Get the path to the test data directory, accessed by some junit tests.
     * This is required because we are using the same exact files as in the parent
     * genepattern-server project. 
     * This must be configured as an environment variable.
     *    Default location: /genepattern-server/test/data
     *    Customized in pom.xml: GP_DATA_DIR="../test/data"
     */
    protected static File initDataDir() { 
        return new File(System.getProperty("GP_DATA_DIR", "test/data")).getAbsoluteFile();
    }

    /**
     * Load a file from the standard maven location:
     *     gp-jobrunner-awsbatch/src/test/resources
     */
    public File getTestResource(final String filename) throws UnsupportedEncodingException {
        final URL url=getClass().getResource(filename);
        if (url==null) {
            fail("test resource not found: "+filename);
        }
        final File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        if (!file.exists()) {
            fail("test resource file doesn't exist: "+file);
        }
        return file;
    }

    /**
     * factory method, create a new GpConfig instance from the given config_yaml file.
     * Initialize a GpConfig instance from a config_yaml file.
     * The returned value is not a mock.
     * 
     * Note: this automatically turns off log4j logging output.
     */
    public static GpConfig initGpConfig(final File configFile) throws Throwable {
        if (!configFile.exists()) { 
            fail("configFile doesn't exist: "+configFile);
        }
        //LogManager.getRootLogger().setLevel(Level.OFF);
        final File webappDir=new File("website").getAbsoluteFile();
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .configFile(configFile)
        .build();
        if (gpConfig.hasInitErrors()) {
            throw gpConfig.getInitializationErrors().get(0);
        }
        return gpConfig;
    }

    /*
     * Example: pre-cache datasets.genepattern.org subdirectory
     */
    final String userRootDir="/opt/gpbeta/gp_home/users";
    final String cachePrefix="/.cache/uploads/cache";
    final String cacheDirPath=userRootDir+cachePrefix;
    final String dataPrefix="/datasets.genepattern.org/data";
    final String dataDirPath=cacheDirPath+dataPrefix;
    final String userUploadPath=userRootDir+"/test-user/uploads";

    /** for testing basic java.nio.file.PathMatcher */
    protected static void assertPathMatcher(final String globPattern, final File inputFile, final boolean expected) {
        final PathMatcher matcher =
            FileSystems.getDefault().getPathMatcher("glob:"+globPattern);
        assertEquals("globPattern='"+globPattern+"', matcher.matches('"+inputFile.toPath()+"')",
            expected,
            matcher.matches(inputFile.toPath())
        );
    }

    /** for testing GenericFileFilter */
    protected static void assertGlobFilter(final boolean expected, final File file, final String... globPatterns) {
        FileFilter fileFilter=GenericFileFilter.initGlobFilter(Arrays.asList(globPatterns));
        assertEquals("accept('"+file+"')", expected, fileFilter.accept(file));
    }

    /** for testing AwsS3Filter.skipS3Upload */
    protected static void assertSkipUpload(final boolean expected, final AwsS3Filter awsS3filter, final File file) {
        assertEquals("skipS3Upload('"+file+"')",
            expected, 
            awsS3filter.skipS3Upload(file));
    }

    /** testing AwsS3Filter with example custom configuration */
    protected void assertCustom(final AwsS3Filter awsS3filter) {
        final String  brca=dataDirPath+"/TCGA_BRCA/BRCA_HTSeqCounts";
        final String ccmi1=dataDirPath+"/ccmi_tutorial/2017-12-15";
        final String ccmi2=dataDirPath+"/ccmi_tutorial/2018-03-14";

        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+brca));
        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+ccmi1));
        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+ccmi2));

        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+brca+"/TCGA_BRCA_SAMPLEINFO.txt"));
        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+ccmi1+"/TCGA_BRCA_SAMPLEINFO.txt"));
        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+ccmi2+"/TCGA_BRCA_SAMPLEINFO.txt")); 
        
        // parent dir, shouldn't skip
        assertSkipUpload(false, awsS3filter, new File(userRootDir+"/"+dataDirPath));

        // different root dir, shouldn't skip
        final String user_brca=userUploadPath+"/TCGA_BRCA/BRCA_HTSeqCounts";
        assertSkipUpload(false, awsS3filter, new File(user_brca));
        assertSkipUpload(false, awsS3filter, new File(user_brca+"/TCGA_BRCA_SAMPLEINFO.txt"));
    }


    @Test
    public void pathMatcher_exactMatch() {
        assertPathMatcher(cacheDirPath+"/test.txt", new File(cacheDirPath+"/test.txt"), true);
    }

    @Test
    public void pathMatcher_exactMatch_dir() {
        assertPathMatcher(cacheDirPath+"/", new File(cacheDirPath+"/"), false);
    }
    
    /**
     * For testing directory matching
     *   use-case: filter out all '.git' directories, including children
     * Braces '{}' specify a collection of subpatterns.
     * <pre>
     * These two patterns are equivalent:
     *    "{.git,.git/**,**&#x2F;.git,**&#x2F;.git/**}"
     *    "{,**&#x2F;}.git{,/**}"
     * Note: &#x2F; denotes '/', forward slash
     * </pre>
     * 
     * See: https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
     */
    @Test
    public void pathMatcher_match_dot_gitDir_AND_children() {
        // These two patterns are equivalent:
        //   "{.git,.git/**,**/.git,**/.git/**}"
        //   "{,**/}.git{,/**}"
        final String pattern="{,**/}.git{,/**}";
        assertPathMatcher(pattern, new File(".git"), true);
        assertPathMatcher(pattern, new File(".git").getAbsoluteFile(), true);
        assertPathMatcher(pattern, new File(".git/HEAD"), true);
        assertPathMatcher(pattern, new File(".git/HEAD").getAbsoluteFile(), true);
        
        assertPathMatcher(pattern, new File(".gitignore"), false);
        assertPathMatcher(pattern, new File(".gitignore").getAbsoluteFile(), false);
        assertPathMatcher(pattern, new File("test.txt"), false);
        assertPathMatcher(pattern, new File("test.txt").getAbsoluteFile(), false);
    }
    
    @Test
    public void pathMatcher_matchDir_AND_children() {
        final String pattern="**/all_aml{,/**}";

        final File testDir=new File(dataDir,"all_aml");
        final File testFileMatch=new File(testDir,"all_aml_test.cls");
        assertEquals("sanity check, testDir.exists()", true, testDir.exists());
        assertPathMatcher(pattern, testDir, true);
        assertPathMatcher(pattern, testFileMatch, true);

        // make sure similarly named directories do not match
        final File testDirSibling=new File(dataDir, "all_aml_sibling");
        assertPathMatcher(pattern, testDirSibling, false);
        assertPathMatcher(pattern, new File(testDirSibling,"all_aml_test.cls"), false);
    }

    @Test
    public void globFilter_matchDir_AND_children() {
        final String pattern="**/all_aml{,/**}";

        final File testDir=new File(dataDir,"all_aml");
        final File testFileMatch=new File(testDir,"all_aml_test.cls");
        assertEquals("sanity check, testDir.exists()", true, testDir.exists());
        
        assertGlobFilter(true, testDir, pattern);
        assertGlobFilter(true, testFileMatch, pattern);

        // make sure similarly named directories do not match
        final File testDirSibling=new File(dataDir, "all_aml_sibling");
        assertPathMatcher(pattern, testDirSibling, false);
        assertPathMatcher(pattern, new File(testDirSibling,"all_aml_test.cls"), false);
        assertGlobFilter(false, testDirSibling, pattern);
        assertGlobFilter(false, new File(testDirSibling,"all_aml_test.cls"), pattern);
    }

    @Test
    public void globFilter_match_java() {
        final String[] globPatterns = {"*.java", "!Test*.java"};
        assertGlobFilter(true,  new File("MyClass.java"),     globPatterns);
        assertGlobFilter(false, new File("TestMyClass.java"), globPatterns);
    }

    @Test
    public void globFilter_datasets_brca() {
        final String globPattern="**/.cache/uploads/cache/datasets.genepattern.org/data/TCGA_BRCA/BRCA_HTSeqCounts{,/**}";
        final File matchDir=new File(dataDirPath, "TCGA_BRCA/BRCA_HTSeqCounts");
        assertGlobFilter(true, matchDir, globPattern);
        assertGlobFilter(true, new File(matchDir, "test.txt"), globPattern);
        assertGlobFilter(true, new File(matchDir, "sub/test.txt"), globPattern);

        final File noMatchDir=new File(userRootDir, "test_user/uploads/TCGA_BRCA/BRCA_HTSeqCounts");
        assertGlobFilter(false, noMatchDir, globPattern);
        assertGlobFilter(false, new File(noMatchDir, "test.txt"), globPattern);
        assertGlobFilter(false, new File(noMatchDir, "sub/test.txt"), globPattern);
    }

    @Test
    public void with_custom_config() {
        final Value customValue=new Value(Arrays.asList(
            "**/.cache/uploads/cache/datasets.genepattern.org/data/ccmi_tutorial/2017-12-15{,/**}",
            "**/.cache/uploads/cache/datasets.genepattern.org/data/ccmi_tutorial/2018-03-14{,/**}",
            "**/.cache/uploads/cache/datasets.genepattern.org/data/TCGA_BRCA/BRCA_HTSeqCounts{,/**}"
        ));
        final GpConfig gpConfig=mock(GpConfig.class);
        final GpContext serverContext=GpContext.getServerContext();
        when(gpConfig.getValue(serverContext, PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER, AwsS3Filter.DEFAULT_JOB_AWSBATCH_S3_UPLOAD_FILTER))
          .thenReturn(customValue);
        final AwsS3Filter awsS3filter=AwsS3Filter.initAwsS3Filter(gpConfig, serverContext);
        assertCustom(awsS3filter);
    }

    @Test
    public void with_custom_config_from_yaml() throws Throwable {
        final File configFile=getTestResource("/config_awsbatch_test.yaml");
        final GpConfig gpConfig=initGpConfig(configFile);
        assertNotNull(gpConfig);
        final GpContext serverContext=GpContext.getServerContext();
        final Value s3UploadFilter=gpConfig.getValue(serverContext,PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER);
        assertNotNull("expecting non-null "+PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER, s3UploadFilter); 
        final AwsS3Filter awsS3filter=AwsS3Filter.initAwsS3Filter(gpConfig, serverContext);
        assertCustom(awsS3filter);
    }

}
