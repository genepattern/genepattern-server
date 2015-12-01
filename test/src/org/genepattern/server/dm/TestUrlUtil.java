/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.genepattern.junitutil.Demo;
import org.genepattern.server.config.GpConfig;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestUrlUtil {
    private static GpConfig gpConfig;
    private static GpFilePath gpFilePath; 
    private static final String relativeHref=jobResultPath("1", "all%20aml%20test.gct");

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        gpConfig=mock(GpConfig.class);
        when(gpConfig.getGpUrl()).thenReturn(gpUrl);
        gpFilePath=mock(GpFilePath.class);
        when(gpFilePath.getRelativeUri()).thenReturn(new URI(relativeHref));
    }

    // sanity check the Demo class
    protected void checkDemo(final String relativeUri, String[] expected) {
        final String[] args=Demo.splitRelativeUri(relativeUri);  // [ servletPath, pathInfo, queryString ]
        assertEquals("splitRelativeUri("+relativeUri+")", Arrays.asList(expected), Arrays.asList(args));
    }
    
    /** sanity check {@link Demo#splitRelativeUri(String)} in junitutil package */
    @Test public void check_splitRelativeUri() {
        checkDemo("/", new String[]{"/", null, null} );
        checkDemo("/users", new String[]{"/users", null, null} );
        checkDemo("/users/test_user", new String[]{"/users", "/test_user", null} );
        checkDemo("/users/test_user/all_aml_test.gct", new String[]{"/users", "/test_user/all_aml_test.gct", null} );
        checkDemo("/users/test_user/all_aml_test.gct?p1=v1&p2=v2", new String[]{"/users", "/test_user/all_aml_test.gct", "p1=v1&p2=v2"} );

        // the '#' fragment is not part of the query
        checkDemo("/users/test_user?p1=v1&p2=v2#fragment", new String[]{"/users", "/test_user", "p1=v1&p2=v2"} );
    }
    
    @Test
    public void getUrl() throws MalformedURLException, URISyntaxException {
        URL expected=new URL(gpHref+relativeHref);
        URL actual=UrlUtil.getUrl(gpHref, gpFilePath);
        assertEquals("mock job result file", 
                expected,
                actual);
        
        assertEquals("uri.path should be de-coded", 
                gpPath+"/jobResults/1/all aml test.gct", 
                actual.toURI().getPath());
    }

    @Test
    public void getUrl_jobResultInSubDir() throws MalformedURLException, URISyntaxException {
        final String testHref="/jobResults/1/my%20dir/all%20aml%20test.gct";
        when(gpFilePath.getRelativeUri()).thenReturn(new URI(testHref));

        URL actual=UrlUtil.getUrl(gpHref, gpFilePath);
        assertEquals("mock job result file in sub directory", 
                new URL(gpHref+testHref),
                actual);
        
        assertEquals("uri.path should be de-coded", 
                gpPath+"/jobResults/1/my dir/all aml test.gct", 
                actual.toURI().getPath());
    }

    @Test
    public void getUrl_removeTrailingSlash() throws MalformedURLException {
        assertEquals("mock job result file", 
                new URL(gpHref+relativeHref),
                UrlUtil.getUrl(gpUrl, gpFilePath));
    }

    @Test
    public void baseGpHref() {
        assertEquals(gpHref, UrlUtil.getBaseGpHref(gpConfig));
    }
    
    @Test
    public void getHref_nullBaseGpHref() {
        assertEquals("expecting relative URL when baseGpHref is null",
                relativeHref,
                UrlUtil.getHref((String)null, gpFilePath));
    }
    
    @Test
    public void getHref_emptyBaseGpHref() {
        assertEquals("expecting relative URL when baseGpHref is empty",
                relativeHref,
                UrlUtil.getHref("", gpFilePath));
    }

    @Test(expected=IllegalArgumentException.class)
    public void getUrl_baseGpHref_notSet() throws MalformedURLException {
        UrlUtil.getUrl((String)null, gpFilePath);
    }
    
    @Test
    public void getBaseGpHref() {
        HttpServletRequest request=clientRequest();
        assertEquals(""+request.getRequestURL(),
                proxyHref,
                UrlUtil.getBaseGpHref(request));
    }

    @Test
    public void getBaseGpHref_withPath() {
        HttpServletRequest request=clientRequest(uploadPath());
        assertEquals(""+request.getRequestURL(),
                proxyHref,
                UrlUtil.getBaseGpHref(request));
    }

    @Test
    public void getBaseGpHref_root() {
        HttpServletRequest request=Demo.rootClientRequest("/users/test_user/all_aml_test.gct");
        assertEquals(""+request.getRequestURL(),
                proxyHref_ROOT,
                UrlUtil.getBaseGpHref(request));
    }

    @Test
    public void testDecode_noOp() {
        final String initialValue="all_aml_test.gct";
        assertEquals(
                initialValue,
                UrlUtil.decodeURIcomponent(initialValue) );
    }
    
    @Test
    public void testDecode_space() {
        assertEquals(
                "all aml test.gct",
                UrlUtil.decodeURIcomponent("all%20aml%20test.gct") );
    }

    @Test
    public void testDecode_slashes() {
        assertEquals(
                "sub/directory/all_aml_test.gct",
                UrlUtil.decodeURIcomponent("sub/directory/all_aml_test.gct") );
    }
    
    @Test
    public void glue() {
        final String pathNoPrefix="jobResults/1/all_aml_test.gct";
        final String path="/"+pathNoPrefix;
        final String expected=gpHref+path;

        assertEquals("default", expected, 
                UrlUtil.glue(gpHref, path));
        assertEquals("path no prefix", expected, 
                UrlUtil.glue(gpHref, pathNoPrefix));
        assertEquals("baseHref with slash", expected, 
                UrlUtil.glue(gpHref+"/", path));
        assertEquals("baseHref with slash, path no prefix", expected, 
                UrlUtil.glue(gpHref+"/", pathNoPrefix));
        assertEquals("special-case: double slash", gpHref+"/data//shared_data/all_aml_test.gct", 
                UrlUtil.glue(gpHref, "/data//shared_data/all_aml_test.gct"));
        
        // corner-case(s)
        assertEquals("special-case: null path", gpHref+"/",
                UrlUtil.glue(gpHref, null));
        assertEquals("special-case: empty path", gpHref+"/",
                UrlUtil.glue(gpHref, ""));
        assertEquals("special-case: path='/'", gpHref+"/",
                UrlUtil.glue(gpHref, "/"));
        assertEquals("special-case: null baseHref", path,
                UrlUtil.glue(null, path));
        assertEquals("special-case: empty baseHref", path,
                UrlUtil.glue("", path));
        assertEquals("special-case: null baseHref, pathNoPrefix", path,
                UrlUtil.glue(null, pathNoPrefix));
        assertEquals("special-case: empty baseHref, pathNoPrefix", path,
                UrlUtil.glue("", pathNoPrefix));
        
        assertEquals("special-case: null baseHref, null path -> {empty}/{empty}}", "/", 
                UrlUtil.glue(null, null));
    }
    
    protected void checkSplit(final String expectedServletPath, final String expectedPathInfo, final String gpContextPath, final String urlSpec) throws MalformedURLException, URISyntaxException {
        String[] actual=UrlUtil.splitUrl(gpContextPath, new URL(urlSpec));
        assertEquals("servletPath from '"+urlSpec+"', gpPath='"+gpContextPath+"'", expectedServletPath, actual[0]);
        assertEquals("pathInfo from '"+urlSpec+"', gpPath='"+gpContextPath+"'", expectedPathInfo, actual[1]);
    }

    @Test
    public void splitUrl_gpPath() throws MalformedURLException, URISyntaxException {
        String gpPath="/gp";
        checkSplit( "/debug", null, gpPath, 
                proxyHref_ROOT+"/gp/debug");
        checkSplit( "/debug", "/", gpPath, 
                proxyHref_ROOT+"/gp/debug/");
        checkSplit("/debug", "/test", gpPath, 
                proxyHref_ROOT+"/gp/debug/test");
        checkSplit("/debug", "/test.ext", gpPath, 
                proxyHref_ROOT+"/gp/debug/test.ext");
        checkSplit("/debug", "/subdir/test.ext", gpPath, 
                proxyHref_ROOT+"/gp/debug/subdir/test.ext");
        
        // corner-cases
        checkSplit("", "/", gpPath, 
                proxyHref_ROOT+"/gp");
        checkSplit("", "/", gpPath, 
                proxyHref_ROOT+"/gp/");
    }
    
    @Test
    public void splitUrl_customGpPath() throws MalformedURLException, URISyntaxException {
        String customContextPath="/custom";
        checkSplit( "/debug", null, customContextPath, 
                proxyHref_ROOT+"/custom/debug");
        checkSplit( "/debug", "/", customContextPath, 
                proxyHref_ROOT+"/custom/debug/");
        checkSplit( "/debug", "/test", customContextPath, 
                proxyHref_ROOT+"/custom/debug/test");
        checkSplit( "/debug", "/test.ext", customContextPath, 
                proxyHref_ROOT+"/custom/debug/test.ext");
        checkSplit( "/debug", "/subdir/test.ext", customContextPath, 
                proxyHref_ROOT+"/custom/debug/subdir/test.ext");
        
        // corner-cases
        checkSplit( "", "/", customContextPath, 
                proxyHref_ROOT+"/custom");
        checkSplit( "", "/", customContextPath, 
                proxyHref_ROOT+"/custom/");
    }
    
    @Test
    public void splitUrl_nestedGpPath() throws MalformedURLException, URISyntaxException {
        String customContextPath="/sub/dir";
        checkSplit( "/debug", null, customContextPath, 
                proxyHref_ROOT+"/sub/dir/debug");
        checkSplit( "/debug", "/", customContextPath, 
                proxyHref_ROOT+"/sub/dir/debug/");
        checkSplit( "/debug", "/test", customContextPath, 
                proxyHref_ROOT+"/sub/dir/debug/test");
        checkSplit( "/debug", "/test.ext", customContextPath, 
                proxyHref_ROOT+"/sub/dir/debug/test.ext");
        checkSplit( "/debug", "/subdir/test.ext", customContextPath, 
                proxyHref_ROOT+"/sub/dir/debug/subdir/test.ext");
        
        // corner-cases
        checkSplit( "", "/", customContextPath, 
                proxyHref_ROOT+"/sub/dir");
        checkSplit( "", "/", customContextPath, 
                proxyHref_ROOT+"/sub/dir/");
    }
    
    @Test
    public void splitUrl_ROOT() throws MalformedURLException, URISyntaxException {
        String customContextPath="";
        checkSplit( "/debug", null, customContextPath, 
                proxyHref_ROOT+"/debug");
        checkSplit( "/debug", "/", customContextPath, 
                proxyHref_ROOT+"/debug/");
        checkSplit( "/debug", "/test", customContextPath, 
                proxyHref_ROOT+"/debug/test");
        checkSplit( "/debug", "/test.ext", customContextPath, 
                proxyHref_ROOT+"/debug/test.ext");
        checkSplit( "/debug", "/subdir/test.ext", customContextPath, 
                proxyHref_ROOT+"/debug/subdir/test.ext");
        
        // corner-cases
        checkSplit( "", "/", customContextPath, 
                proxyHref_ROOT+"");
        checkSplit( "", "/", customContextPath, 
                proxyHref_ROOT+"/");
    }

}
