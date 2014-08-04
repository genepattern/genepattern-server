package org.genepattern.server.dm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;


public class TestUrlUtil {
    @Test
    public void testGetGpUrl_noPort() {
        HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer().append("http://gpdev.broadinstitute.org/gp/rest/v1/jobs/67262"));
        when(request.getRequestURI()).thenReturn("/gp/rest/v1/jobs/67262");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("gpdev.broadinstitute.org");
        when(request.getContextPath()).thenReturn("/gp");
        when(request.getServletPath()).thenReturn("/rest");
        when(request.getServerPort()).thenReturn(80);
        Assert.assertEquals("",
                "http://gpdev.broadinstitute.org/gp",
                UrlUtil.getGpUrl(request));
    }

    @Test
    public void testGetGpUrl_withPort() {
        HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer().append("http://127.0.0.1:8080/gp/rest/v1/jobs/67262"));
        when(request.getRequestURI()).thenReturn("/gp/rest/v1/jobs/67262");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("127.0.0.1");
        when(request.getContextPath()).thenReturn("/gp");
        when(request.getServletPath()).thenReturn("/rest");
        when(request.getServerPort()).thenReturn(8080);
        Assert.assertEquals("",
                "http://127.0.0.1:8080/gp",
                UrlUtil.getGpUrl(request));
    }

    @Test
    public void testDecode_noOp() {
        final String initialValue="all_aml_test.gct";
        Assert.assertEquals(
                initialValue,
                UrlUtil.decodeURIcomponent(initialValue) );
    }
    
    @Test
    public void testDecode_space() {
        Assert.assertEquals(
                "all aml test.gct",
                UrlUtil.decodeURIcomponent("all%20aml%20test.gct") );
    }

    @Test
    public void testDecode_slashes() {
        Assert.assertEquals(
                "sub/directory/all_aml_test.gct",
                UrlUtil.decodeURIcomponent("sub/directory/all_aml_test.gct") );
    }

}
