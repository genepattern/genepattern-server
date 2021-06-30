/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import static org.genepattern.server.UserAccountManager.PROP_USERNAME_REGEX;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit tests for validating usernames upon account creation.
 * 
 * Parameterized username tests with configurations of the 'username.regex'
 * <ul>
 *   <li>null gpConfig (use default regex)
 *   <li>default GpConfig (use default regex)
 *   <li>set default regex in GpConfig (for A/B testing)
 *   <li>set custom regex in GpConfig (alternate regex)
 * </ul>
 * 
 * @author pcarr
 */
@RunWith(Parameterized.class)
public class UserAccountManagerTest {

    private static File basicConfigFile;
    private static File customConfigFile;
    private static GpConfig basicConfig;
    private static GpConfig customConfig;
    private static GpConfig alternateConfig;
    private static GpContext serverContext;
    
    protected static GpConfig initGpConfig(final String key, final String value) {
        final File webappDir=new File("website").getAbsoluteFile();
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .configFile(basicConfigFile)
            .addProperty(key, value)
        .build();
        return gpConfig; 
    }
    
    public static GpConfig initGpConfig(final File configFile) {
        final File webappDir=new File("website").getAbsoluteFile();
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .configFile(configFile)
        .build();
        return gpConfig;
    }
    
    @BeforeClass
    public static void initGpConfig() {
        basicConfigFile=FileUtil.getSourceFile(UserAccountManagerTest.class, "config_basic.yaml");
        basicConfig=initGpConfig(basicConfigFile);

        customConfigFile=FileUtil.getSourceFile(UserAccountManagerTest.class, "config_username_validation.yaml");
        customConfig=initGpConfig(customConfigFile);
        
        // an different regex, which does not allow the space character in the username
        alternateConfig=initGpConfig(PROP_USERNAME_REGEX, "[A-Za-z0-9_\\-.@]+"); 
        serverContext=GpContext.getServerContext();
    }

    protected static void assertGpConfig(final File configFile, final GpConfig gpConfig) {
        if (configFile==null) {
            fail("setup error, File not found, configFile="+configFile);
        }
        if (gpConfig==null) {
            fail("setup error, failed to initialize configFile="+configFile);
        }
        if (gpConfig.hasInitErrors()) {
            String message="setup error(s) ...";
            for(final Throwable t : gpConfig.getInitializationErrors()) {
                message += "\n    "+t.getLocalizedMessage();
            }
            fail(message);
        }
    }
    
    @SuppressWarnings("deprecation")
    protected static void assertPropNotSet(final GpConfig gpConfig, final GpContext serverContext, final String key) {
        final String message="test setup, getValue('"+key+"')";
        assertNull(message, 
            gpConfig.getValue(serverContext, key)
        );
        assertNull("test setup, getGPProperty('"+key+"')",
            gpConfig.getGPProperty(serverContext, key)
        );
    }

    @SuppressWarnings("deprecation")
    protected static void assertPropSet(final GpConfig gpConfig, final GpContext serverContext, final String key) {
        assertNotNull("test setup, getValue('"+key+"')", 
            gpConfig.getValue(serverContext, key)
        );
        assertNotNull("test setup, getGPProperty('"+key+"')",
            gpConfig.getGPProperty(serverContext, key)
        );
    }

    protected static void assertValidateUsername(final GpConfig gpConfig, final String username, final boolean expected_isValid) 
    {
        try {
            UserAccountManager.validateUsername(gpConfig, username);
            if (!expected_isValid) {
                fail("Expecting AuthenticationException for username: "+username);
            }
        }
        catch (AuthenticationException e) {
            if (expected_isValid) {
                fail(e.getLocalizedMessage());
            }
        }
    }

    protected static void assertIsValid(final GpConfig gpConfig, final String username) {
        assertValidateUsername(gpConfig, username, true);
    }

    protected static void assertNotValid(final GpConfig gpConfig, final String username) {
        assertValidateUsername(gpConfig, username, false);
    }

    public static void test_regex(final String regex, final List<String> expected_valid, final List<String> expected_not_valid) {
        final GpConfig gpConfig=initGpConfig(PROP_USERNAME_REGEX, regex);
        for(final String username : expected_valid) {
            assertIsValid(gpConfig, username);
        }
        for(final String username : expected_not_valid) {
            assertNotValid(gpConfig, username);
        }
    }

    /**
     * Initialize parameterized tests by reading a list of usernames from a file,
     * where each line of the file is a username.
     */
    public static List<Object[]> initTestsFromFile(final File usernames_file) throws FileNotFoundException, IOException {
        final List<Object[]> tests=new ArrayList<Object[]>();
        final List<String> usernames=FileUtil.readLines(usernames_file);
        for(final String username : usernames) {
            tests.add(new Object[] { 
                    // description
                    "gpprod_user_id", 
                    // username
                    username, 
                    // expected_isValid
                    true,
                    // expected_isValid_alt (just a guess)
                    true,
            });
        }
        return tests;
    }
    
    /**
     * Run parameterized tests from a list of usernames mined from the gpprod database.
     * circa Feb 2018, edited to a smaller list circa April 2018.
     * <p>
     * Note: this is for a one-off test to validate pre-existing user_id from the 
     * production server (gpprod) database. E.g.
     * <pre>
       select user_id from gp_user  where 
           registration_date > to_date('2017/01/01', 'YYYY/MM/DD')
           and 
           total_login_count > 1
       order by registration_date desc
     * </pre>
     * 
     * Note: commented out, because it was used as a one-time test of the username regex
     *   on a list of active users, circa Feb 2018
     * Of 23390 usernames ...
     *   14 failed with the default regex 
     *  232 failed when the space ' ' character is not allowed
     */
    //@Parameters(name="\"{1}\", {0}")
    public static Collection<Object[]> sample_usernames_from_gpprod() throws FileNotFoundException, IOException {
        File file=FileUtil.getDataFile("user_id_test.txt");
        return initTestsFromFile(file);
    }

    /**
     * Run parameterized tests.
     */
    @Parameters(name="\"{1}\", {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // valid usernames ...
            //   { "", "", true },
            { "basic", "myusername", true, true },
            { "space character ' '", "my username", true, false },
            
            { "email", "myusername@example.com", true, true },
            { "email with space", "my username@example.com", true, false },
            { "email with hyphen", "my-username@example.com", true, true },
            { "email with underscore", "my_username@example.com", true, true },
            { "email with dot", "my.username@example.com", true, true },
            
            // invalid usernames ...
            { "empty string", "", false, false },
            { "newline", "myusername\n", false, false },
            { "tab character", "my\tusername", false, false },
            { "double quote", "\"myusername\"", false, false },
            { "double quote at end", "myusername\"", false, false },
            { "double quote in name", "my\"user\"name", false, false },
            { "single quote", "'myusername'", false, false },
            { "single quote at end", "myusername'", false, false },            
            { "user with bracket '<'", "<myusername", false, false },
            { "user with bracket '>'", "<myusername", false, false },
            { "user with brackets '<>'", "<myusername>", false, false },
            { "user with '?'", "?myusername", false, false },
            { "two '@' characters", "user@name@example.com", false, false },
            { "'@' character at beginning", "@name@example.com", false, false },
            { "'@' character at end", "name@example.com@", false, false }, 
            { "single '@'", "@", false, false }, 
            { "two '@@'", "@@", false, false }, 
        });
    }

    @Parameter(0)
    public String description;
    @Parameter(1)
    public String username;
    @Parameter(2)
    public boolean expected_isValid;
    @Parameter(3)
    public boolean expected_isValid_alt;
    
    @Test
    public void validateUsername_nullConfig() {
        GpConfig gpConfig = null;
        assertNull("test requires gpConfig==null", gpConfig);
        assertValidateUsername(gpConfig, username, expected_isValid);
    }

    @Test
    public void validateUsername_regexNotSet() {
        // test setup
        assertGpConfig(basicConfigFile, basicConfig);
        assertPropNotSet(basicConfig, serverContext, PROP_USERNAME_REGEX);
        assertValidateUsername(basicConfig, username, expected_isValid);
    }

    @Test
    public void validateUsername_regexSet() {
        assertGpConfig(customConfigFile, customConfig);
        assertPropSet(customConfig, serverContext, PROP_USERNAME_REGEX);
        assertValidateUsername(customConfig, username, expected_isValid);
    }

    /** 
     * special-case: pattern set to empty string, e.g.
     *     username.regex: "" 
     */
    @Test
    public void validate_regexEmpty() {
        GpConfig gpConfig=initGpConfig(PROP_USERNAME_REGEX, "");
        assertPropSet(customConfig, serverContext, PROP_USERNAME_REGEX);
        assertValidateUsername(gpConfig, username, expected_isValid);        
    }

    @Test
    public void validate_regexCustom_spacesNotAllowed() {
        assertPropSet(alternateConfig, serverContext, PROP_USERNAME_REGEX);
        assertValidateUsername(alternateConfig, username, expected_isValid_alt);
    }

}
