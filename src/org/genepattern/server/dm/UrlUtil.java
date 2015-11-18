/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.ServerConfigurationFactory;

import com.google.common.base.Strings;

/**
 * Utility methods for converting between Files (on the GP server file system) and URIs (to be be used as external references to those files).
 * 
 * @author pcarr
 */
public class UrlUtil {
    public static Logger log = Logger.getLogger(UrlUtil.class);
    
    /** @deprecated renamed to getBaseGpHref */
    public static String getGpUrl(final HttpServletRequest request) {
        return getBaseGpHref(request);
    }

    /**
     * Helper method for getting the base GenePatternURL for a given request.
     * This method is based on the client request rather than the server configuration
     * setting for the GenePatternURL.
     * 
     * Template:
     *     {protocol}:{hostname}[:{port}]{contextPath}
     * Which does not include the trailing slash.
     * 
     * For example, when 
     *     request.requestUrl=http://gpdev.broadinstitute.org/gp/rest/v1/jobs/67262
     *     request.contextPath=/gp
     *     request.servletPath=/rest
     * then return
     *     http://gpdev.broadinstitute.org/gp
     * 
     * @param request
     * @return
     */
    public static String getBaseGpHref(final HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("requestURL="+request.getRequestURL().toString());
            log.debug("requestURI="+request.getRequestURI());
            log.debug("scheme="+request.getScheme());
            log.debug("serverName="+request.getServerName());
            log.debug("serverPort="+request.getServerPort());
            log.debug("contextPath="+request.getContextPath());
            log.debug("servletPath="+request.getServletPath());
            log.debug("pathInfo="+request.getPathInfo());
            log.debug("protocol="+request.getProtocol());
        }
        final StringBuffer sb=request.getRequestURL();
        int idx=sb.indexOf(request.getContextPath());
        if (idx<0) {
            log.error("request.getContextPath not defined: "+request.getRequestURL().toString());
            idx=0;
        }
        idx=sb.indexOf(request.getServletPath());
        if (idx<0) {
            log.error("request.servletPath not defined, requestURL="+request.getRequestURL().toString());
        }
        return sb.substring(0, idx);
    }
    
    /** @deprecated initialize from HttpServletRequest if possible */
    public static String getBaseGpHref(final GpConfig gpConfig) {
        return removeTrailingSlash(gpConfig.getGpUrl());
    }

    /**
     * @param gpConfig must be non-null
     * @param request can be null
     */
    public static String getBaseGpHref(final GpConfig gpConfig, final HttpServletRequest request) {
        if (request != null) {
            return getBaseGpHref(request);
        }
        else if (gpConfig != null) {
            return getBaseGpHref(gpConfig);
        }
        else {
            return getBaseGpHref(ServerConfigurationFactory.instance());
        }
    }

    /**
     * Append the base gpUrl to the relative uri, making sure to not duplicate the '/' character.
     * @param prefix, the base url (expected to not include the trailing slash)
     * @param suffix, the relative url (expected to start with a slash)
     * @return
     */
    public static String glue(final String prefix, final String suffix) {
        return removeTrailingSlash(Strings.nullToEmpty(prefix)) + 
                prependSlash(Strings.nullToEmpty(suffix));
    }

    /** prepend a slash '/' if and only if the input does not already start with one */
    protected static String prependSlash(final String in) {
        if (Strings.isNullOrEmpty(in)) {
            return "/";
        }
        if (in.startsWith("/")) {
            return in;
        }
        return "/"+in;
    }
    
    /** remove the trailing slash '/' if and only if the input ends with one */
    protected static String removeTrailingSlash(final String in) {
        if (in==null) {
            return "";
        }
        if (!in.endsWith("/")) {
            return in;
        }
        return in.substring(0, in.length()-1);
    }

    /**
     * default implementation of GpFilePath#getUrl for all internal URLs.
     * @param baseGpHref
     * @param gpFilePath_internal
     * @return
     * @throws MalformedURLException
     */
    public static URL getUrl(final String baseGpHref, final GpFilePath gpFilePath_internal) throws MalformedURLException {
        if (Strings.isNullOrEmpty(baseGpHref)) {
            throw new IllegalArgumentException("baseGpHref not set");
        }
        final String href=getHref(baseGpHref, gpFilePath_internal);
        return new URL(href);
    }
    
    /**
     * Get the callback href to the given GpFilePath file.
     * @param request
     * @param gpFilePath_internal
     * @return
     */
    public static String getHref(final HttpServletRequest request, final GpFilePath gpFilePath_internal) {
        if (request==null) {
            throw new IllegalArgumentException("request==null");
        }
        return getHref(getBaseGpHref(request), gpFilePath_internal);
    }

    public static String getHref(final GpConfig gpConfig, final GpFilePath gpFilePath_internal) {
        if (gpConfig==null) {
            throw new IllegalArgumentException("gpConfig==null");
        }
        return getHref(getBaseGpHref(gpConfig), gpFilePath_internal);
    }
    
    public static String getHref(final GpConfig gpConfig, final HttpServletRequest request, final GpFilePath gpFilePath_internal) {
        return getHref(getBaseGpHref(gpConfig, request), gpFilePath_internal);
    }

    /**
     * Get the callback href to the given GpFilePath file.
     * 
     * @param baseGpHref
     * @param gpFilePath_internal
     * @return
     */
    public static String getHref(final String baseGpHref, final GpFilePath gpFilePath_internal) {
        if (gpFilePath_internal==null) {
            throw new IllegalArgumentException("gpFilePath==null");
        }
        if (baseGpHref==null) {
            log.warn("baseGpHref==null, convert null to empty, relative url");
        }
        if (baseGpHref != null && baseGpHref.endsWith("/")) {
            log.warn("remove trailing slash from baseGpHref="+baseGpHref);
        }
        final String href=removeTrailingSlash(Strings.nullToEmpty(baseGpHref)) +
                gpFilePath_internal.getRelativeUri() + 
                (gpFilePath_internal.isDirectory() ? "/" : "");
        return href;
    }
    
    /** Converts a string into something you can safely insert into a URL. */
    public static String encodeURIcomponent(String str) {
        String encoded = str;
        try {
            encoded = URLEncoder.encode(str, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException for enc=UTF-8", e);
            encoded = URLEncoder.encode(str);
        }
        
        //replace all '+' with '%20'
        encoded = encoded.replace("+", "%20");        
        return encoded;

    }
    
    /** Converts a string into something you can safely insert into a URL. */
    public static String decodeURIcomponent(final String encoded) {
        String decoded = encoded;
        try {
            decoded = URLDecoder.decode(encoded, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException for enc=UTF-8", e);
            decoded = URLDecoder.decode(encoded);
        }
        catch (Throwable t) {
            log.error("Unexpected error decoding string="+encoded);
        }
        return decoded;
    }

    
    //alternative implementation which doesn't use any other java classes
    private static String encodeURIcomponent_orig(String s) {
        StringBuilder o = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (isUnsafe(ch)) {
                o.append('%');
                o.append(toHex(ch / 16));
                o.append(toHex(ch % 16));
            }
            else {
                o.append(ch);
            }
        }
        return o.toString();
    }
    
    //alternative implementation which uses standard java classes, File, URL, and URI
    private static String encodeURIcomponent_file(String name) {
        final File file=new File(name);
        final URI uri=file.toURI();
        try { 
            final URL url=uri.toURL();
            final String encodedPath=url.toExternalForm();
            int beginIndex=encodedPath.lastIndexOf("/");
            ++beginIndex;
            if (beginIndex<0) {
                beginIndex=0;
            }
            final String encodedName=encodedPath.substring(beginIndex);
            return encodedName;
        }
        catch (MalformedURLException e) {
            log.error(e);
        }
        return name;
    }

    private static char toHex(int ch) {
        return (char)(ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private static boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0) {
            return true;
        }
        return " %$&+,/:;=?@<>#%\\".indexOf(ch) >= 0;
    }

    /**
     * Encode the File into a valid URI path component.
     * @param file
     * @return
     */
    public static String encodeFilePath(File file) {
        if (file == null) {
            log.error("Invalid null arg");
            return "";
        }
        if (file.getParent() == null) {
            return encodeURIcomponent( file.getName() );
        }
        else {
            return encodeFilePath( file.getParentFile() ) + "/" + encodeURIcomponent( file.getName() );
        }
    }

}
