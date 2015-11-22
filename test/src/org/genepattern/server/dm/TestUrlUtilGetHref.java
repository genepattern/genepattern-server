package org.genepattern.server.dm;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.genepattern.junitutil.DemoGpFilePath;
import org.genepattern.server.config.GpConfig;
import org.junit.Before;
import org.junit.Test;

public class TestUrlUtilGetHref {
    // example path to user upload file
    private static final String uriPrefix="/users/"+testUserId;
    private static final String relativeFilePath="gp_tutorial/all_aml_test.gct";

    private GpConfig gpConfig=null;
    private HttpServletRequest request;
    private GpFilePath gpFilePath;
    
    @Before
    public void setUp() throws URISyntaxException {
        gpConfig=mock(GpConfig.class);
        when(gpConfig.getGpUrl()).thenReturn(gpUrl);
        request=clientRequest("/users/"+testUserId+"/"+relativeFilePath);
        gpFilePath=new DemoGpFilePath.Builder()
            .uriPrefix(uriPrefix)
            .relativeFile(relativeFilePath)
        .build();
    }
    
    @Test
    public void getHref() {
        assertEquals(proxyHref+uriPrefix+"/"+relativeFilePath,
                UrlUtil.getHref(request, gpFilePath));
    }
    
    @Test
    public void getHref_noHttpRequest() throws URISyntaxException {
        assertEquals("client http request is null, expecting GenePatternURL baseUrl",
                gpHref+uriPrefix+"/"+relativeFilePath, 
                UrlUtil.getHref(gpConfig, gpFilePath));
    }

    @Test(expected=IllegalArgumentException.class)
    public void getHref_nullArgs() {
        String href=UrlUtil.getHref(null, null, null);
        assertEquals("return empty string for unexpected input", "", href);
    }

    /**
     * run junit test on UrlUtil.resolveBaseUrl, with Demo defaults
     *     contextPath: '/gp'
     *     baseUrl: proxyHref
     * 
     * @param relativeUri, relative to the contextPath.
     */
    protected void checkResolveBaseUrl(final String baseUrl, final String contextPath, final String requestUri) {
        final String requestUrlSpec=baseUrl+requestUri;
        
        final Level pre=LogManager.getRootLogger().getLevel();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        assertEquals("resolveBaseUrl('"+requestUrlSpec+"', '"+contextPath+"')",
                baseUrl, 
                UrlUtil.resolveBaseUrl(requestUrlSpec, contextPath));
        
        LogManager.getRootLogger().setLevel(pre);
    }
    
    /**
     * test UrlUtil.resolveBaseUrl for web app in default location,
     * serverContext="/gp" 
     * @param requestUri, relative to the contextPath.
     */
    @Test
    public void getBaseGpHref_default() {
        checkResolveBaseUrl(proxyHref, gpPath, "/");
        checkResolveBaseUrl(proxyHref, gpPath, "/index.html");
        checkResolveBaseUrl(proxyHref, gpPath, "/pages/index.jsf");
        checkResolveBaseUrl(proxyHref, gpPath, "/pages/index.jsf&param=value#here");
    }

    /**
     * test UrlUtil.resolveBaseUrl for web app in ROOT context,
     * serverContext="" 
     * @param requestUri, relative to the contextPath.
     */
    @Test
    public void getBaseGpHref_ROOT() {
        checkResolveBaseUrl(proxyHref_ROOT, "", "");
        checkResolveBaseUrl(proxyHref_ROOT, "", "/");
        checkResolveBaseUrl(proxyHref_ROOT, "", "/users/test_user/all_aml_test.gct");
    }
    
    @Test
    public void demoClientRequest_default() {
        HttpServletRequest request=clientRequest();
        assertEquals("requestURL", proxyHref+"/", request.getRequestURL().toString());
        assertEquals("servletPath", "/", request.getServletPath());
        assertEquals("pathInfo", null, request.getPathInfo());
        assertEquals("queryString", null, request.getQueryString());
        assertEquals("requestURI", "/gp/", request.getRequestURI());
    }
    
    public void demoClientRequest_fromUriPath() {
        HttpServletRequest request=clientRequest("/users/test_user/all_aml_test.gct?param01=value01&param02=value02");
        assertEquals("servletPath", "/users", request.getServletPath());
        assertEquals("pathInfo", "/test_user/all_aml_test.gct", request.getPathInfo());
        assertEquals("queryString", "param01=value01&param02=value02", request.getQueryString());
    }

    @Test
    public void demoClientRequest_customServletPath() {
        HttpServletRequest request=clientRequest("/nested/servlet_path", "/all_aml_test.gct", null);
        assertEquals("servletPath", "/nested/servlet_path", request.getServletPath());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void demoClientRequest_null() {
        //HttpServletRequest request=
                clientRequest(null);
    }
}
