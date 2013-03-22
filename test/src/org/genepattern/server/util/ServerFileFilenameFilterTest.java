package org.genepattern.server.util;

import java.io.File;
import java.io.FilenameFilter;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Unit test ServerFileFilenameFilterTest class.
 * Look at the 'serverFilenameFilter.yaml' file for the server
 * configuration settings that are being tested.
 * 
 * @author pcarr
 */
public class ServerFileFilenameFilterTest {
    private static final File dir = null; //arg to FilenameFilter.accept

    @Before
    public void setUp() {
        ConfigUtil.loadConfigFile(this.getClass(), "serverFilenameFilter.yaml");
    }
    
    @After
    public void tearDown() {
        //revert back to a 'default' config.file
        ConfigUtil.loadConfigFile(null);
    }
    
    @Test
    public void testNullContext() {
        Context userContext=null;
        testDefault(userContext);
    }
    
    @Test
    public void testNullUserId() {
        Context userContext=ServerConfiguration.Context.getContextForUser("default_user");
        userContext.setUserId(null);
        Assert.assertNull("expecting null userContext.userId", userContext.getUserId());
        testDefault(userContext);
    }
    
    @Test
    public void testUserIdNotSet() {
        Context userContext=ServerConfiguration.Context.getContextForUser("");
        testDefault(userContext);
    }
    
    /**
     * Test default configuration
     */
    @Test
    public void testDefaultConfig() {
        Context userContext=ServerConfiguration.Context.getContextForUser("default_user");
        testDefault(userContext);
    }
    
    @Test
    public void testStringFromConfig() {
        Context userContext=ServerConfiguration.Context.getContextForUser("gp_user");
        FilenameFilter filter = ServerFileFilenameFilter.getServerFilenameFilter(userContext);
        check(filter, true, ".DS_Store");
        check(filter, true, "Thumbs.db");
        check(filter, false, "hide");
        check(filter, false, "hidethisfile.txt");
    }
    
    @Test
    public void testCommaDelimitedString() {
        FilenameFilter filter = ServerFileFilenameFilter.getServerFilenameFilter(".nfs*,.lsf*,a*");
        check(filter, false, ".nfs0123");
        check(filter, false, ".lsf0123");
        check(filter, true, ".DS_Store");
        check(filter, true, "Thumbs.db");
        check(filter, false, "anything_with_an_a");
        check(filter, true, ".hidden");
        check(filter, true, "blast");
    }

    @Test
    public void testCommaDelimitedStringFromConfig() {
        Context userContext=ServerConfiguration.Context.getContextForUser("admin_user");
        FilenameFilter filter = ServerFileFilenameFilter.getServerFilenameFilter(userContext);
        check(filter, false, ".nfs0123");
        check(filter, false, ".lsf0123");
        check(filter, true, ".DS_Store");
        check(filter, true, "Thumbs.db");
        check(filter, false, "anything_with_an_a");
        check(filter, true, ".hidden");
        check(filter, true, "blast");
    }
    
    @Test
    public void testCommaInGlob() {
        Context userContext=ServerConfiguration.Context.getContextForUser("extra_user");
        FilenameFilter filter = ServerFileFilenameFilter.getServerFilenameFilter(userContext);
        check(filter, true, "file");
        check(filter, true, "with");
        check(filter, true, "comma");
        check(filter, false, "file,with,comma");
        
    }
    
    private void testDefault(final Context userContext) {
        FilenameFilter filter = ServerFileFilenameFilter.getServerFilenameFilter(userContext);
        check(filter, false, ".DS_Store");
        check(filter, false, "Thumbs.db");
        check(filter, false, ".hidden");
    }
    
    /**
     * Helper class, for the given filter, does it return the expected result.
     * 
     * @param filter
     * @param expected
     * @param filename
     */
    private void check(final FilenameFilter filter, final boolean expected, final String filename) {
        Assert.assertEquals("filter.accept('"+filename+"')", expected, filter.accept(dir, filename));
    }
    

}
