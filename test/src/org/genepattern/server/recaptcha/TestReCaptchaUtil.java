package org.genepattern.server.recaptcha;

import static org.genepattern.server.recaptcha.ReCaptchaSession.G_RECAPTCHA_RESPONSE;
import static org.genepattern.server.recaptcha.ReCaptchaSession.PROP_ENABLED;
import static org.genepattern.server.recaptcha.ReCaptchaSession.PROP_SECRET_KEY;
import static org.genepattern.server.recaptcha.ReCaptchaSession.PROP_SITE_KEY;
import static org.genepattern.server.recaptcha.ReCaptchaSession.PROP_VERIFY_URL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.genepattern.junitutil.Demo;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.recaptcha.ReCaptchaException;
import org.genepattern.server.recaptcha.ReCaptchaSession;
import org.junit.Before;
import org.junit.Test;

/**
 * Test ReCaptcha
 * 
 * From https://developers.google.com/recaptcha/docs/faq
 *   "I'd like to run automated tests with reCAPTCHA v2. What should I do?"
 * 
 *   With the following test keys, you will always get No CAPTCHA and all verification requests will pass.
 *     Site key: 6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI
 *     Secret key: 6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe
 */
public class TestReCaptchaUtil {
    
    public static final String test_site_key="6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI";
    public static final String test_secret_key="6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe";
    public static final String test_response_token="example-recaptcha-response-for-test";
    
    final static File webappDir=new File("website").getAbsoluteFile();
    final GpContext serverContext=GpContext.getServerContext();
    
    @Before
    public void setUp() {
        // to suppress log messages when initializing the GpConfig
        Demo.suppressLog();
    }

    // by default, reCaptcha is not enabled
    public static GpConfig initDefaultConfig() {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        return gpConfig;
    }
    
    // initialize a custom GpConfig for testing reCaptcha
    public static GpConfig initCustomConfig() {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(ReCaptchaSession.PROP_ENABLED, "true")
            .addProperty(ReCaptchaSession.PROP_SITE_KEY, test_site_key)
            .addProperty(ReCaptchaSession.PROP_SECRET_KEY, test_secret_key)
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        return gpConfig;
    }

    @Test
    public void verifyReCaptcha_with_test_key() throws ReCaptchaException {
        final GpConfig gpConfig=initCustomConfig();
        final ReCaptchaSession r = ReCaptchaSession.init(gpConfig, serverContext);
        final HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getParameter(G_RECAPTCHA_RESPONSE)).thenReturn(test_response_token);
        r.verifyReCaptcha(request);
    }
    
    @Test
    public void verifyRecaptcha_default_config() throws ReCaptchaException {
        final GpConfig gpConfig=initDefaultConfig();
        final ReCaptchaSession r = ReCaptchaSession.init(gpConfig, GpContext.getServerContext());
        
        // by default, reCaptcha is not enabled ... all attempts to verify should pass 
        // ... case 1: null input
        r.verifyReCaptcha((String)null);
        // ... case 2: empty string
        r.verifyReCaptcha("");
        // ... case 3: test token
        r.verifyReCaptcha(test_response_token);        
    }
    
    /** expecting ReCaptchaException when secret key is incorrect */
    @Test(expected=ReCaptchaException.class)
    public void verifyReCaptcha_bad_secret_key() throws ReCaptchaException {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(ReCaptchaSession.PROP_ENABLED, "true")
            .addProperty(ReCaptchaSession.PROP_SITE_KEY, test_site_key)
            .addProperty(ReCaptchaSession.PROP_SECRET_KEY, "bad-secret-key")
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        final ReCaptchaSession r = ReCaptchaSession.init(gpConfig, serverContext);
        final HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getParameter(G_RECAPTCHA_RESPONSE)).thenReturn(test_response_token);
        r.verifyReCaptcha(request);
    }

    @Test //(expected=ReCaptchaException.class)
    public void verifyReCaptcha_bad_site_key() throws ReCaptchaException {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(ReCaptchaSession.PROP_ENABLED, "true")
            .addProperty(ReCaptchaSession.PROP_SITE_KEY, "bad-site-key")
            .addProperty(ReCaptchaSession.PROP_SECRET_KEY, test_secret_key)
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        final ReCaptchaSession r = ReCaptchaSession.init(gpConfig, serverContext);
        final HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getParameter(G_RECAPTCHA_RESPONSE)).thenReturn(test_response_token);
        r.verifyReCaptcha(request);
    }

    /** expecting ReCaptchaException when verifyUrl is null */
    @Test(expected=ReCaptchaException.class)
    public void configError_verifyUrl_null() throws ReCaptchaException {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(PROP_ENABLED, "true")
            .addProperty(PROP_SITE_KEY, test_site_key)
            .addProperty(PROP_SECRET_KEY, test_secret_key)
            .addProperty(PROP_VERIFY_URL, "bogus_url")
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        final ReCaptchaSession r = ReCaptchaSession.init(gpConfig, serverContext);
        r.verifyReCaptcha(test_response_token);
    }

    @Test(expected=ReCaptchaException.class)
    public void configError_verifyUrl_site_not_found() throws ReCaptchaException {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(PROP_ENABLED, "true")
            .addProperty(PROP_SITE_KEY, test_site_key)
            .addProperty(PROP_SECRET_KEY, test_secret_key)
            .addProperty(PROP_VERIFY_URL, "http://localhost/recaptcha/api/siteverify")
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        final ReCaptchaSession r = ReCaptchaSession.init(gpConfig, serverContext);
        r.verifyReCaptcha(test_response_token);
    }

    /** expecting ReCaptchaException when site_key is not set */
    @Test(expected=ReCaptchaException.class)
    public void configError_siteKey_not_set() throws ReCaptchaException {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(PROP_ENABLED, "true")
            .addProperty(PROP_SECRET_KEY, test_secret_key)
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        final ReCaptchaSession r = ReCaptchaSession.init(gpConfig, serverContext);
        r.verifyReCaptcha(test_response_token);
    }
    
    /** expecting ReCaptchaException when secret_key is not set */
    @Test(expected=ReCaptchaException.class)
    public void configError_secretKey_not_set() throws ReCaptchaException {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(PROP_ENABLED, "true")
            .addProperty(PROP_SITE_KEY, test_site_key)
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        final ReCaptchaSession r = ReCaptchaSession.init(gpConfig, serverContext);
        r.verifyReCaptcha(test_response_token);
    }

    /** expecting ReCaptchaException when response token not set */
    @Test(expected=ReCaptchaException.class)
    public void configError_missing_response_token() throws ReCaptchaException {
        final GpConfig gpConfig=initCustomConfig();
        final ReCaptchaSession r = ReCaptchaSession.init(gpConfig, serverContext);
        r.verifyReCaptcha((String)null);
    }

    @Test
    public void initResponseTokenFromRequest() {
        final HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getParameter(G_RECAPTCHA_RESPONSE)).thenReturn(test_response_token);
        assertEquals("&"+G_RECAPTCHA_RESPONSE+"='"+test_response_token+"'",
                test_response_token, 
                ReCaptchaSession.initResponseTokenFromRequest(request));
    }

    @Test
    public void initResponseTokenFromRequest_emptyString() {
        String gReCaptchaResponse="";
        HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getParameter(G_RECAPTCHA_RESPONSE)).thenReturn(gReCaptchaResponse);
        assertEquals("&"+G_RECAPTCHA_RESPONSE+"='"+gReCaptchaResponse+"'",
                gReCaptchaResponse, 
                ReCaptchaSession.initResponseTokenFromRequest(request));
    }

    @Test
    public void initResponseTokenFromRequest_null_request() {
        HttpServletRequest request=null;
        assertEquals("when HttpServletRequest is null",
                "", 
                ReCaptchaSession.initResponseTokenFromRequest(request));
    }

    @Test
    public void initVerifyUrl_default() {
        final GpConfig gpConfig=initDefaultConfig();
        final URL url=ReCaptchaSession.initVerifyUrl(gpConfig, serverContext);
        assertEquals("verifyUrl", ReCaptchaSession.DEFAULT_VERIFY_URL, url.toExternalForm());
    }

    @Test
    public void initVerifyUrl_custom() {
        final String customUrlStr="http://localhost/recaptcha/api/siteverify";
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(ReCaptchaSession.PROP_VERIFY_URL, customUrlStr)
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        final URL url=ReCaptchaSession.initVerifyUrl(gpConfig, serverContext);
        assertEquals("verifyUrl", customUrlStr, url.toExternalForm());
    }

    @Test
    public void initVerifyUrl_emptyString() {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(ReCaptchaSession.PROP_VERIFY_URL, "")
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        final URL url=ReCaptchaSession.initVerifyUrl(gpConfig, serverContext);
        assertEquals("verifyUrl", ReCaptchaSession.DEFAULT_VERIFY_URL, url.toExternalForm());
    }
    
    @Test
    public void initVerifyUrl_invalid_url() {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(webappDir)
            .addProperty(ReCaptchaSession.PROP_VERIFY_URL, "bogus_url")
        .build();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        final URL url=ReCaptchaSession.initVerifyUrl(gpConfig, serverContext);
        assertEquals("verifyUrl", null, url);
    }
    
    @Test
    public void initFromConfig_default() {
        final GpConfig gpConfig=initDefaultConfig();
        ReCaptchaSession r=ReCaptchaSession.init(gpConfig, serverContext);
        assertEquals("isEnabled (default)", false, r.isEnabled());
        assertEquals("siteKey (default)", "", r.getSiteKey());
        assertEquals("privateKey (default)", "", r.getSecretKey());
    }

}
