package org.genepattern.server.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Arrays;

import org.genepattern.junitutil.Demo;
import org.genepattern.server.util.GenericFileFilter;
import org.junit.Test;

public class TestGenericFileFilter {

    /*
     * Example: pre-cache datasets.genepattern.org subdirectory
     */
    private final String userRootDir="/opt/gpbeta/gp_home/users";
    private final String cachePrefix="/.cache/uploads/cache";
    private final String cacheDirPath=userRootDir+cachePrefix;
    private final String dataPrefix="/datasets.genepattern.org/data";
    private final String dataDirPath=cacheDirPath+dataPrefix;

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

        final File testDir=new File(Demo.dataDir(),"all_aml");
        final File testFileMatch=new File(testDir,"all_aml_test.cls");
        assertEquals("sanity check, testDir.exists()", true, testDir.exists());
        assertPathMatcher(pattern, testDir, true);
        assertPathMatcher(pattern, testFileMatch, true);

        // make sure similarly named directories do not match
        final File testDirSibling=new File(Demo.dataDir(), "all_aml_sibling");
        assertPathMatcher(pattern, testDirSibling, false);
        assertPathMatcher(pattern, new File(testDirSibling,"all_aml_test.cls"), false);
    }

    @Test
    public void globFilter_matchDir_AND_children() {
        final String pattern="**/all_aml{,/**}";

        final File testDir=new File(Demo.dataDir(),"all_aml");
        final File testFileMatch=new File(testDir,"all_aml_test.cls");
        assertEquals("sanity check, testDir.exists()", true, testDir.exists());
        
        assertGlobFilter(true, testDir, pattern);
        assertGlobFilter(true, testFileMatch, pattern);

        // make sure similarly named directories do not match
        final File testDirSibling=new File(Demo.dataDir(), "all_aml_sibling");
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

        final File noMatchDir=new File(userRootDir, "test_user/uploads/cache/datasets.genepattern.org/data/TCGA_BRCA/BRCA_HTSeqCounts");
        assertGlobFilter(false, noMatchDir, globPattern);
        assertGlobFilter(false, new File(noMatchDir, "test.txt"), globPattern);
        assertGlobFilter(false, new File(noMatchDir, "sub/test.txt"), globPattern);
    }

}
