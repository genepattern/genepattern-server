package org.genepattern.junitutil;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.genepattern.server.config.GpConfig;

import com.google.common.base.Strings;


/**
 * Demo configuration to be used in your junit tests.
 * 
 * @author pcarr
 *
 */
public class Demo {
    /** servlet context path is '/gp' */
    public static final String gpPath="/gp";

    /** gpHref, set in genepattern.properties, no trailing slash. */
    public static final String gpHref="http://127.0.0.1:8080"+gpPath;
    /** gpUrl, set in genepattern.properties, includes trailing slash '/'. */
    public static final String gpUrl=gpHref+"/";

    private static final String proxyScheme="https";
    private static final String proxyHost="gpdev.broadinstitute.org";
    /** proxyHref='https://gpdev.broadinstitute.org/gp', returned by HttpServletRequest.getRequestURL,  */
    public static final String proxyHref=proxyScheme+"://"+proxyHost+gpPath;
    private static final int proxyPort=443;
    
    // common data paths
    public static final String dataFtpDir="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/";
    public static final String dataHttpDir="http://www.broadinstitute.org/cancer/software/genepattern/data/all_aml/";
    public static final String dataGsDir="https://dm.genomespace.org/datamanager/file/Home/Public/SharedData/Demos/SampleData/"; //all_aml_test.gct

    // common user ids
    public static final String adminUserId="admin";
    public static final String devUserId="dev";
    public static final String testUserId="test_user";
    
    protected static String portStr(final String scheme, final int port) {
        if ("http".equals(scheme) && port==80) return "";
        if ("https".equals(scheme) && port==443) return "";
        return port + ":" + port;
    }

    /**
     * Split relativeUri into {servletPath}{pathInfo}?{queryString}, return as a 3-tuple.
     * 
     * @param relativeUri, expecting a leading '/'
     * @return as a 3-tuple, [ servletPath, pathInfo, queryString ]
     */
    protected static String[] splitRelativeUri(final String relativeUri) {
        // default for gp junit tests
        final String[] rval={"", null, null};
        if (Strings.isNullOrEmpty(relativeUri)) {
            return rval;
        }

        String uriRawPath="";
        try {
            final URI uri=new URI(relativeUri);
            uriRawPath=Strings.nullToEmpty(uri.getRawPath());
            // set {servletPath} and {pathInfo}
            String[] urlPathTokens=splitUriPath(uriRawPath);
            System.arraycopy(urlPathTokens, 0, rval, 0, 2);
            // set queryString
            rval[2]=uri.getRawQuery();
        }
        catch (URISyntaxException e) {
            fail("format error, relativeUri='"+relativeUri+"': "+e.getLocalizedMessage());
            return rval; // <---- shouldn't be here
        }
        return rval;
    }
    
    /**
     * Split the uriPath into {servletPath}{pathInfo}, return as a 2-tuple
     * @param uriPath, the  URI#getRawPath() 
     * @return as a 2-tuple, [ servletPath, pathInfo ] 
     */
    protected static String[] splitUriPath(final String uriRawPath) { 
        if (!uriRawPath.startsWith("/")) {
            fail("uriRawPath must start with a '/', uriRawPath="+uriRawPath);
        }
        // Note: could use a regex
        final String rval[]={uriRawPath, null};
        if (uriRawPath.length()==0) {
            // short-circuit
            return rval;
        }
        // always split on first '/'
        int idx=uriRawPath.indexOf(1, '/');
        if (idx>0) {
            //servletPath
            rval[0]=uriRawPath.substring(0, idx);
            //pathInfo
            rval[1]=uriRawPath.substring(idx);
        }
        return rval;
    }

    /** mock GpConfig */
    public static GpConfig gpConfig() {
        return mock(GpConfig.class);
    }
    
    /** mock client HttpRequest to '{proxyHref}/' */
    public static HttpServletRequest clientRequest() {
        return clientRequest("/");
    }

    /**
     * mock client HttpRequest to '{proxyHref}{relativeUriPath}'.
     *     relativeUriPath={servletPath}[{pathInfo}][?{queryString}]
     * should start with a '/', should not include '/gp'.
     * 
     * @param relativeUriPath, relative to the {proxyHref}
     * @return
     */
    public static HttpServletRequest clientRequest(final String relativeUri) {
        if (relativeUri==null) {
            throw new IllegalArgumentException("relativeUri==null");
        }
        if (!relativeUri.startsWith("/")) {
            fail("relativeUri must start with a '/', relativeUri="+relativeUri);
        }
        final String[] args=splitRelativeUri(relativeUri);
        return clientRequest( args[0], args[1], args[2] );
    }

    /**
     * mock client HttpRequest to '{proxyHref}{servletPath}[{pathInfo}][?{queryString}]'
     * 
     * @param servletPath, starts with '/', encoded as HTTP path element, 
     *     @see {@link HttpServletRequest#getContextPath()}
     * @param pathInfoIn, can be null, starts with '/', encoded as HTTP path 
     *     @see {@link HttpServletRequest#getPathInfo()}
     * @param queryString, can be null, does not include '?', encoded as a valid HTTP queryString, 
     *     @see {@link HttpServletRequest#getQueryString()}
     *     
     * Example:
     *     requestURL = https://gpdev.broadinstitute.org/gp/users/test_user/all_aml_test.gct?key=val
     *     requestURI = /gp/users/test_user/all_aml_test.gct
     *     contextPath = /gp
     *     serlvetPath = /users
     *     pathInfo = /test_user/all_aml_test.gct
     *     queryString = key=val
     * 
     * @return a new mock HttpServletRequest, can be modified with Mockito calls
     */
    public static HttpServletRequest clientRequest(final String servletPath, final String pathInfoIn, final String queryString) {
        final String pathInfo=Strings.nullToEmpty(pathInfoIn);
        final HttpServletRequest request=mock(HttpServletRequest.class);
        // url={proxyScheme}://{proxyHost}[:{proxyPort}]{uri}, does not include queryString
        when(request.getRequestURL()).thenReturn(new StringBuffer().append(proxyHref+servletPath+pathInfo));
        // uri={gpPath}{servletPath}{pathInfo}, does not include the queryString
        when(request.getRequestURI()).thenReturn(gpPath+servletPath+pathInfo);
        when(request.getScheme()).thenReturn(proxyScheme);
        when(request.getServerName()).thenReturn(proxyHost);
        when(request.getContextPath()).thenReturn(gpPath);
        when(request.getServletPath()).thenReturn(servletPath);
        when(request.getServerPort()).thenReturn(proxyPort);
        when(request.getQueryString()).thenReturn(queryString);
        
        return request;
    }
    
    // generate server file uri paths
    private static final Random rnd = new Random();
    protected static String randomInt() {
        final int numDigits=4;
        final StringBuilder sb = new StringBuilder(numDigits);
        for(int i=0; i < numDigits; ++i) {
            sb.append((char)('0' + rnd.nextInt(10)));
        }
        return sb.toString();
        
        //return ""+Math.rint(1000.* Math.random());
    }

    /** User Upload File, relative URI path (default userId). */
    public static String uploadPath(final String path) {
        return uploadPath(testUserId, path);
    }

    /** User Upload File, relative URI path, for given userId. */
    public static String uploadPath(final String userId, final String path) {
        if (path==null) {
            fail("path==null");
        }
        if (path.startsWith("/")) {
            fail("path should not start with '/', path="+path);
        }
        return "/users/"+userId+"/"+path;
    }
    
    /** Job Upload File, relative URI path */
    public static String jobUploadPath(final String pname, final String path) {
        return jobUploadPath(pname, 1, path);
    }
    /** Job Upload File, relative URI path, custom index (multiple values per param) */
    public static String jobUploadPath(final String pname, final int idx, final String path) {
        return jobUploadPath(testUserId, pname, idx, path);
    }
    /** Job Upload File, relative URI path, custom userId */
    public static String jobUploadPath(final String userId, final String pname, final int idx, final String path) {
        return uploadPath(userId, "tmp/run"+randomInt()+".tmp/"+pname+"/"+idx+"/"+path);
    }

}
