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


/**
 * Utility methods for converting between Files (on the GP server file system) and URIs (to be be used as external references to those files).
 * 
 * @author pcarr
 */
public class UrlUtil {
    public static Logger log = Logger.getLogger(UrlUtil.class);

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
    public static String getGpUrl(final HttpServletRequest request) {
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
