package org.genepattern.junitutil;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    /** baseUrl of ROOT web app, 'http://127.0.0.1:8080' */
    protected static final String gpHref_ROOT="http://127.0.0.1:8080";
    /** gpHref, set in genepattern.properties, no trailing slash. */
    public static final String gpHref="http://127.0.0.1:8080"+gpPath;
    /** gpUrl, set in genepattern.properties, includes trailing slash '/'. */
    public static final String gpUrl=gpHref+"/";

    public static final String proxyScheme="https";
    public static final String proxyHost="gpdev.broadinstitute.org";
    /** baseUrl of ROOT web app, 'https://gpdev.broadinstitute.org' */
    public static final String proxyHref_ROOT=proxyScheme+"://"+proxyHost; 
    /** proxyHref='https://gpdev.broadinstitute.org/gp', returned by HttpServletRequest.getRequestURL,  */
    public static final String proxyHref=proxyScheme+"://"+proxyHost+gpPath;
    public static final String proxyUrl=proxyHref+"/";
    private static final int proxyPort=443;
    
    // common user ids
    public static final String adminUserId="admin";
    public static final String devUserId="dev";
    public static final String testUserId="test_user";
    
    // common job ids
    /** default jobId for basic junit tests */
    public static final String jobId="1001";
    
    // common module lsids
    /** LSID for ConvertLineEndings, cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2" */
    public static final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";

    // common data hrefs, external
    public static final String dataFtpDir="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/";
    public static final String dataHttpDir="http://www.broadinstitute.org/cancer/software/genepattern/data/all_aml/";
    public static final String dataGsDir="https://dm.genomespace.org/datamanager/file/Home/Public/SharedData/Demos/SampleData/"; //all_aml_test.gct
    
    // common file system paths, (as opposed to URI paths)
    /** 
     * File system path to example data on server's file system, includes trailing slash. 
     *     href template: {gpUrl}/data/{localDataDir}{relativePath}
     *     example: http://127.0.0.1:8080/gp/data//Users/my_user/genepattern/test/data/all_aml/all_aml_test.gct
     * 
     * Note: file name encoding has not been worked out, be careful. When constructing href's should use valid href path.
     *     when access actual file should use file system paths.
     * 
     * The actual path is to the ./test/data folder of the project; the all_aml folder includes
     * example data files.
     */
    public static final String localDataDir=FileUtil.getDataDir().getAbsolutePath() + "/";
    
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
    public static String[] splitRelativeUri(final String relativeUri) {
        if (relativeUri==null) {
            throw new IllegalArgumentException("relativeUri==null");
        }
        if (!relativeUri.startsWith("/")) {
            fail("relativeUri must start with a '/', relativeUri="+relativeUri);
        }

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
        // split on second '/'
        int idx=uriRawPath.indexOf('/', 1);
        if (idx>0) {
            //servletPath
            rval[0]=uriRawPath.substring(0, idx);
            //pathInfo
            rval[1]=uriRawPath.substring(idx);
        }
        return rval;
    }

    /** 
     * mock GpConfig, initialized with defaults:
     * - gpPath=/gp
     * - gpUrl=http://127.0.0.1:8080/gp/
     */
    public static GpConfig gpConfig() {
        //return mock(GpConfig.class);
        GpConfig gpConfig=mock(GpConfig.class);
        when(gpConfig.getGpPath()).thenReturn(gpPath);
        when(gpConfig.getGpUrl()).thenReturn(gpUrl);
        return gpConfig;
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
        return mockRequest(proxyHref_ROOT, gpPath, relativeUri);
    }

    /** mock client HttpRequest to '{gpHref}/', 'http://127.0.0.1:8080/gp/' */
    public static HttpServletRequest localRequest() {
        return Demo.mockRequest(gpHref_ROOT, "/gp", "/");
    }

    /** mock client HttpRequest to '{gpHref}{relativeUri}' */
    public static HttpServletRequest localRequest(final String relativeUri) {
        return Demo.mockRequest(gpHref_ROOT, "/gp", relativeUri);
    }
    
    /** mock client request to ROOT web application, contextPath is the empty String. */
    public static HttpServletRequest rootClientRequest(final String relativeUri) {
        return mockRequest(proxyHref_ROOT, "", relativeUri);
    }
    
    /**
     * mock client http request to {rootUrl}{contextPath}{relativeUri}
     * 
     * @param rootUrl, e.g. 'https://gpdev.broadinstitute.org'
     * @param contextPath, e.g. '/gp'
     * @param relativeUri, e.g. '/users/test_user/all_aml_test.gct'
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
    protected static HttpServletRequest mockRequest(final String rootUrl, final String contextPath, final String relativeUri) {
        final String[] args=splitRelativeUri(relativeUri);  // [ servletPath, pathInfo, queryString ]
        final String servletPath=Strings.nullToEmpty(args[0]);
        final String pathInfo=Strings.nullToEmpty(args[1]);
        final String queryString=args[2];

        // uri={contextPath}{servletPath}{pathInfo}, does not include the queryString
        final String uri=contextPath+servletPath+pathInfo;
        // url={rootUrl}{uri}, does not include queryString
        final String url=rootUrl+uri;

        final HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer().append(url));
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getScheme()).thenReturn(proxyScheme);
        when(request.getServerName()).thenReturn(proxyHost);
        when(request.getContextPath()).thenReturn(contextPath);
        when(request.getServletPath()).thenReturn(servletPath);
        when(request.getServerPort()).thenReturn(proxyPort);
        when(request.getQueryString()).thenReturn(queryString);
        
        return request;
    }
    
    // generate server file uri paths
    private static final Random rnd = new Random();
    protected static String randomInt() {
        final int numDigits=6;
        final StringBuilder sb = new StringBuilder(numDigits);
        for(int i=0; i < numDigits; ++i) {
            sb.append((char)('0' + rnd.nextInt(10)));
        }
        return sb.toString();
    }

    /** default User Upload File, relative URI path */
    public static String uploadPath() {
        return uploadPath(testUserId, "all_aml_test.cls");
    }
    
    /** User Upload File, relative URI path (default userId). */
    public static String uploadPath(final String path) {
        return uploadPath(testUserId, path);
    }

    /** User Upload File, relative URI path, for given userId; path should not start with '/'. */
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
    
    /** default Job Result Dir, relative URI path */
    public static String jobResultDir() {
        return jobResultPath(jobId, "");
    }

    /** default Job Result File, relative URI path, all_aml_test.gct */
    public static String jobResultFile() {
        return jobResultPath(jobId, "all_aml_test.gct");
    }
    
    /**
     * Job Result File, relative URI path
     * @param jobId
     * @param path, a URI encoded path, suitable for constructing an href (as opposed to a File).
     * @return
     */
    public static String jobResultPath(final String jobId, final String path) {
        return "/jobResults/"+jobId+"/"+Strings.nullToEmpty(path);
    }

    /** default Server File Path, relative URI path */
    public static String serverFile() {
        return serverFile("all_aml_test.cls");
    } 

    /**
     * custom Server File, relative URI path 
     * @param path a relative URI path to be appended to the default localDataDir, should not start with '/'
     * @return
     */
    public static String serverFile(final String path) {
        return "/data/"+localDataDir+path;
    }

}
