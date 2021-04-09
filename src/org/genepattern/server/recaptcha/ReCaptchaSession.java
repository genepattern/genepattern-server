package org.genepattern.server.recaptcha;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

/**
 * reCAPTCHA integration adds an "I'm not a robot" challenge
 * to the user registration page. 
 * This implementation uses invisible reCAPTCHA. 
 * 
 * <h3>Configuration</h3>
 * Register a new Invisible reCAPTCHA site on the <a 
 *   href="https://www.google.com/recaptcha/admin"
 * >admin page</a>. 
 * 
 * Add the 'Site key' and 'Secret key' to your config_yaml file.
 * Example config file:
 * <pre>
    recaptcha.enabled: "true"
    recaptcha.site-key: "{Site key from admin page}"
    repatcha.secret-key: "{Secret key from admin page}"
 * </pre>
 * 
 * <h3>Links</h3>
 * @see "https://www.google.com/recaptcha"
 * @see "https://developers.google.com/recaptcha/"
 * @see "https://developers.google.com/recaptcha/docs/invisible"
 * @see "https://www.google.com/recaptcha/admin"
 */
public class ReCaptchaSession {
    private static final Logger log = Logger.getLogger(ReCaptchaSession.class);

    /** 
     * set 'recaptcha.enabled' to 'true' to embed a reCAPTCHA challenge on the user registration page. 
     */
    public static final String PROP_ENABLED="recaptcha.enabled";

    /**
     * Set recaptcha.required-for-rest to 'true' to enforce recaptcha tokens on REST user registration calls
     */
    public static final String REQUIRED_FOR_REST="recaptcha.required-for-rest";

    /** 
     * set the 'recaptcha.verify-url' to the REST API endpoint for verifying the user's response. 
     */
    public static final String PROP_VERIFY_URL="recaptcha.verify-url";
    
    public static final String DEFAULT_VERIFY_URL="https://www.google.com/recaptcha/api/siteverify";
    
    /** 
     * set the 'recaptcha.site-key' to the Site key from the admin page 
     */
    public static final String PROP_SITE_KEY="recaptcha.site-key";

    /** 
     * set the 'recaptcha.secret-key' to the Secrect key from the admin page  
     */
    public static final String PROP_SECRET_KEY="recaptcha.secret-key";

    public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";
    
    /**
     * Utility method to copy an InputStream into an in-memory String.
     */
    public static String copyToString(final InputStream in) throws IOException {
        StringWriter writer = new StringWriter();
        final String encoding="UTF-8";
        IOUtils.copy(in, writer, encoding);
        return writer.toString();
    }

    /**
     * Initialize a new session. Can be customized by editing the config file.
     */
    public static ReCaptchaSession init(final GpConfig gpConfig, final GpContext serverContext) {
        return new ReCaptchaSession(gpConfig, serverContext);
    }

    private final boolean enabled;
    private final boolean requiredForRest;
    private final URL verifyUrl;
    private final String siteKey;
    private final String secretKey;
    
    protected ReCaptchaSession(final GpConfig gpConfig, final GpContext serverContext) {
        this.enabled=gpConfig.getGPBooleanProperty(serverContext, ReCaptchaSession.PROP_ENABLED, false);
        this.requiredForRest=gpConfig.getGPBooleanProperty(serverContext, ReCaptchaSession.REQUIRED_FOR_REST, false);
        this.siteKey=gpConfig.getGPProperty(serverContext, ReCaptchaSession.PROP_SITE_KEY, "");
        this.secretKey=gpConfig.getGPProperty(serverContext, PROP_SECRET_KEY, "");
        this.verifyUrl=initVerifyUrl(gpConfig, serverContext);
    }

    protected static URL initVerifyUrl(final GpConfig gpConfig, final GpContext serverContext) {
        String urlProp=gpConfig.getGPProperty(serverContext, PROP_VERIFY_URL, DEFAULT_VERIFY_URL);
        if (Strings.isNullOrEmpty(urlProp)) {
            urlProp=DEFAULT_VERIFY_URL;
        }
        try {
            return new URL(urlProp);
        }
        catch (Throwable t) {
            log.error("Unexpected error initializing verifyUrl from "+PROP_VERIFY_URL+"="+urlProp);
            return null;
        }
    }

    /**
     * Is reCAPTCHA enabled for this server
     * @return true if 'recaptcha.enabled' is true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * If captcha being enforced for REST registration calls?
     *
     * @return true if 'recaptcha.required-for-rest' is true
     */
    public boolean isRequiredForRest() { return this.requiredForRest; }
    
    /**
     * Use this in the HTML code your site serves to users.
     */
    public String getSiteKey() {
        return siteKey;
    }

    /** for testing only **/
    protected String getSecretKey() {
        return secretKey;
    }
    /**
     * Get the 'G_RECAPTCHA_RESPONSE' from the HttpServletRequest.
     */
    public static String initResponseTokenFromRequest(final HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return request.getParameter(G_RECAPTCHA_RESPONSE);
    }

    /**
     * Verify the user's response to the reCAPTCHA challenge
     * with the 'g-recaptcha-response' from the request parameter.
     * 
     * @param request the HTTP POST from the user registration form
     * @return true if verified
     * @throws ReCaptchaException if not verified
     */
    public boolean verifyReCaptcha(final HttpServletRequest request) throws ReCaptchaException {
        return verifyReCaptcha(initResponseTokenFromRequest(request));
    }

    /**
     * Verify the user's response to the reCAPTCHA challenge 
     * by making a REST API call and parsing the JSON response.
     * 
     * <pre>
       HTTP POST <recaptcha.verify-url>
           secret=<recaptcha.secret-key>
           response=<g-recaptcha-response>
     * </pre>
     * 
     * @param recaptchaResponseToken The value of 'g-recaptcha-response'
     * 
     * @return true if verified
     * @throws ReCaptchaException if not verified
     */
    public boolean verifyReCaptcha(final String recaptchaResponseToken) throws ReCaptchaException {
        if (!isEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("isEnabled="+isEnabled());
            }
            return true;
        }

        if (log.isDebugEnabled()) {
            log.debug("verifying reCAPTCHA ...");
            log.debug("   g-recaptcha-response="+recaptchaResponseToken);
        }
        
        // potential server configuration errors
        if (verifyUrl==null) {
            throw new ReCaptchaException("reCAPTCHA config error: '"+PROP_VERIFY_URL+"' not valid, contact your server admin");
        }
        if (Strings.isNullOrEmpty(siteKey)) {
            throw new ReCaptchaException("reCAPTCHA config error: '"+PROP_SITE_KEY+"' not set, contact your server admin");
        }
        if (Strings.isNullOrEmpty(secretKey)) {
            throw new ReCaptchaException("reCAPTCHA config error: '"+PROP_SECRET_KEY+"' not set, contact your server admin");
        }
        if (Strings.isNullOrEmpty(recaptchaResponseToken)) {
            log.debug("reCAPTCHA error: reCAPTCHA response not set, '"+G_RECAPTCHA_RESPONSE+"='"+recaptchaResponseToken+"'");
            throw new ReCaptchaException("Missing reCAPTCHA response");
        }
        
        try {
            final StringBuilder postData = new StringBuilder();
            addParam(postData, "secret", secretKey);
            addParam(postData, "response", recaptchaResponseToken);
            final String response=post(verifyUrl, postData.toString());
            if (response==null) {
                throw new ReCaptchaException("reCAPTCHA not verified: no response from '"+verifyUrl+"'");
            }
            final JSONObject jsonResponse=new JSONObject(response);
            if (jsonResponse.getBoolean("success")) {
                log.debug("success");
                return true;
            }
            else if (jsonResponse.has("error-codes")) {
                final String message="reCAPTCHA not verified: "+jsonResponse.get("error-codes");
                log.error(message);
                throw new ReCaptchaException(message);
            }
            else {
                log.debug("reCAPTCHA not verified: no 'error-codes' in response");
                throw new ReCaptchaException("reCAPTCHA not verified");
            }
        }
        catch (ReCaptchaException e) {
            throw e;
        }
        catch (IOException e) {
            log.error("reCAPTCHA error: "+e.getClass().getName()+": "+e.getMessage()+", double check '"+PROP_VERIFY_URL+"' in the config file");
            throw new ReCaptchaException("reCAPTCHA error: "+PROP_VERIFY_URL+" not found, contact your server admin");
        }
        catch (Throwable t) {
            throw new ReCaptchaException("reCAPTCHA not verified: "+t.getLocalizedMessage(), t);
        }
    }

    protected static String post(final URL url, final String postData) throws IOException, JSONException {
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty(
            "Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty(
            "charset", StandardCharsets.UTF_8.displayName());
        urlConnection.setRequestProperty(
            "Content-Length", Integer.toString(postData.length()));
        urlConnection.setUseCaches(false);
        urlConnection.getOutputStream()
            .write(postData.getBytes(StandardCharsets.UTF_8));

        final String jsonResponse=copyToString(urlConnection.getInputStream());
        return jsonResponse;
    }

    protected static StringBuilder addParam(final StringBuilder postData, final String param, final String value)
    throws UnsupportedEncodingException {
        if (postData.length() != 0) {
            postData.append("&");
        }
        return postData.append(
            String.format("%s=%s",
                URLEncoder.encode(param, StandardCharsets.UTF_8.displayName()),
                URLEncoder.encode(value, StandardCharsets.UTF_8.displayName()) 
            )
        );
    }

}
