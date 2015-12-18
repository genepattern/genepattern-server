/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.genomespace.GenomeSpaceFileHelper;
import org.genepattern.util.SemanticUtil;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Utility methods for converting between Files (on the GP server file system) and URIs (to be be used as external references to those files).
 * 
 * @author pcarr
 */
public class UrlUtil {
    public static Logger log = Logger.getLogger(UrlUtil.class);

    /**
     * utility call to get the IP address to the url host. 
     * @param url, presumably a link to a data file
     * @return an InetAddress, or null if errors occur
     */
    protected static InetAddress sys_requestedAddress(final InetUtil inetUtil, final URL url) { 
        if (inetUtil==null) {
            throw new IllegalArgumentException("inetUtil==null");
        }
        try {
            return inetUtil.getByName(url.getHost());
        }
        catch (UnknownHostException e) {
            if (log.isDebugEnabled()) {
                log.debug(e);
            }
        }
        catch (Throwable t) {
            log.error(t);
        }
        return null;
    }

    protected static boolean isLocalIpAddress(final InetUtil inetUtil, final InetAddress addr) {
        if (addr==null) {
            return false;
        }

        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            return true;
        }

        // Check if the address is defined on any interface
        Object ni=null;
        if (inetUtil==null) {
            throw new IllegalArgumentException("inetUtil==null");
        }
        try {
            ni=inetUtil.getByInetAddress(addr);
            return ni != null;
        } 
        catch (final SocketException e) {
            if (log.isDebugEnabled()) {
                log.debug("error in getByInetAddress, addr=", e);
            }
        }
        catch (Throwable t) {
            log.error("unexpected error in getByInetAddress, addr="+addr, t);
        }
        return false;
    }
    
    protected static boolean isLocalIpAddress(final InetAddress addr, final NetworkInterface ni) {
        if (ni != null) {
            return true;
        }
        return false;
    }

    /**
     * Tests whether the specified URL refers to the local host.
     * 
     * @param url, The URL to check whether it refers to the local host.
     * @return <tt>true</tt> if the specified URL refers to the local host.
     */
    public  static boolean isLocalHost(final GpConfig gpConfig, final String baseGpHref, final URL url) {
        return isLocalHost(gpConfig, baseGpHref, InetUtilDefault.instance(), url);
    }
    
    public static boolean isLocalHost(final GpConfig gpConfig, final String baseGpHref, final InetUtil inetUtil, final URL url) {
        if (url==null || Strings.isNullOrEmpty(url.getHost())) {
            return false;
        }
        final String requestedHost=url.getHost().toLowerCase();
        // short-circuit test for 'localhost' and '127.0.0.1' to avoid invoking InetAddress methods
        if (requestedHost.equals("localhost")) {
            return true;
        }
        if (requestedHost.equals("127.0.0.1")) {
            return true;
        }

        // check baseGpHref (from the job input)
        if (!Strings.isNullOrEmpty(baseGpHref)) {
            if (url.toExternalForm().toLowerCase().startsWith(baseGpHref.toLowerCase())) {
                return true;
            }
        }
        
        // check GpConfig.genePatternURL (from genepattern.properties)
        final URL gpUrl = gpConfig.getGenePatternURL();
        if (gpUrl != null && requestedHost.equalsIgnoreCase(gpUrl.getHost())) {
            return true;
        }
        
        // legacy code; requires non-null inetUtil
        if (inetUtil != null) {
            final InetAddress requestedAddress=sys_requestedAddress(inetUtil, url);
            if (requestedAddress != null) {
                if (isLocalIpAddress(inetUtil, requestedAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the baseUrl of the web application, inclusive of the contextPath, but not including the trailing slash.
     * This is the URL for the root of your GenePattern site. 
     * 
     * For a default installation the contextPath is '/gp' and the baseUrl is,
     *     http://127.0.0.1:8080/gp
     * When the web app is installed in the root directory, the context path is the empty string,
     *     http://127.0.0.1:8080
     * 
     * This method uses the client HttpServletRequest to determine baseUrl. The client request in most cases
     * will be to a named host, rather than the internal 127.0.0.1 address. For example,
     *     requestURL=http://gpdev.broadinstitute.org/gp/rest/v1/jobs/0
     *     contextPath=/gp
     * Resolves to
     *     baseUrl= http://gpdev.broadinstitute.org/gp
     * 
     * @param req
     * @return
     */
    public static String getBaseGpHref(final HttpServletRequest req) {
        if (log.isDebugEnabled()) {
            log.debug("requestURL="+req.getRequestURL().toString());
            log.debug("contextPath="+req.getContextPath());
        }
        final String baseUrl=resolveBaseUrl(req.getRequestURL().toString(), req.getContextPath());
        if (log.isDebugEnabled()) {
            log.debug("baseUrl="+baseUrl);
        }
        return  baseUrl;
    }

    /**
     * Helper method, get the list of base urls which call back to the local server.
     * 
     * @param gpConfig
     * @param baseGpHref
     * @return
     */
    public static ImmutableList<String> initBaseGpHrefs(final GpConfig gpConfig, final String baseGpHref) {
        final ImmutableList.Builder<String> b = new ImmutableList.Builder<String>();
        if (!Strings.isNullOrEmpty(baseGpHref)) {
            b.add(baseGpHref);
        }
        else {
            if (log.isDebugEnabled()) {
                // only warn when in debug mode
                log.warn("jobInput.baseGpHref not set");
            }
        }
        if (gpConfig != null) {
            final String baseGpHrefFromConfig=UrlUtil.getBaseGpHref(gpConfig);
            if (!Strings.isNullOrEmpty(baseGpHrefFromConfig)) {
                b.add(baseGpHrefFromConfig);
            }
        }
        return b.build();
    }
    
    /**
     * resolve the baseUrl from the given requestUrlSpec and contextPath.
     * @param requestUrlSpec, e.g. 'http://127.0.0.1:8080/gp/jobResults/0/out.txt'
     * @param contextPath, e.g. '/gp'
     * 
     * @return the baseUrl of the server, e.g. 'http://127.0.0.1:8080/gp'
     */
    protected static String resolveBaseUrl(final String requestUrlSpec, String contextPath) {
        //adjust empty contextPath, must start with "/" in order to resolve it back to root 
        if (Strings.isNullOrEmpty(contextPath)) {
            contextPath="/";
        }
        final URI contextUrl = 
                URI.create(requestUrlSpec)
                    .resolve(contextPath);
        return  removeTrailingSlash( contextUrl.toString() );
    }

    /** @deprecated initialize from HttpServletRequest if possible */
    public static String getBaseGpHref(final GpConfig gpConfig) {
        return removeTrailingSlash(gpConfig.getGpUrl());
    }

    /**
     * @param gpConfig can be null
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
                gpFilePath_internal.getRelativeUri();
        if (gpFilePath_internal.isDirectory()) {
            // append '/' if necessary
            return glue(href, "/");
        }
        else {
            return href;
        }
    }
    
    /**
     * Get the filename from URL, by analogy to File.getName(),
     * by default keepTrailingSlash is true.
     * 
     * @param url
     * @return
     */
    public static String getFilenameFromUrl(final URL url) {
         final boolean keepTrailingSlash=true;
         return getFilenameFromUrl(url, keepTrailingSlash);
    }
    
    /**
     * Get the filename from URL, by analogy to File.getName(),
     * optionally including the slash from the url path.
     * 
     * @param url
     * @param keepTrailingSlash, when true append trailing '/' from url.getPath.
     * @return
     */
    public static String getFilenameFromUrl(final URL url, final boolean keepTrailingSlash) {
        if (url==null) {
            return null;
        }
        String urlPath;
        try {
            urlPath=url.toURI().getPath();
        }
        catch (URISyntaxException e) {
            urlPath=url.getPath();
            if (log.isDebugEnabled()) {
                log.debug("error decoding path from url="+url+", use encoded path instead", e);
                log.debug("urlPath="+urlPath);
            }
        }  
        
        final boolean isGsFile=GenomeSpaceFileHelper.isGenomeSpaceFile(url);
        String filename=new File(urlPath).getName();
        // special-case for GenomeSpace linked to Google drive reference
        if (isGsFile) {
            filename=stripParen(filename);
        }
        if (keepTrailingSlash && urlPath.endsWith("/")) {
            filename += "/";
        }
        return filename;
    }
    
    /**
     * Extracts the file's kind from the URL and filename.  Uses a separate implementation from the one 
     * found in SemanticUtil because we need to handle GenomeSpace file conversions. Example queryString,
     * 
     *     "?dataformat=http://www.genomespace.org/datamanager/dataformat/gct"
     * 
     * @param url
     * @param filename
     * @return
     * @throws URISyntaxException 
     */
    public static String getKindFromUrl(final URL url, final String filename, final String extension) {
        final boolean isGsFile=GenomeSpaceFileHelper.isGenomeSpaceFile(url);
        if (isGsFile) {
            try {
                final String query=url.toURI().getQuery();
                if (query != null) {
                    final String[] pairs=query.split("&");
                    for(final String pair : pairs) {
                        final String[] param=pair.split("=");
                        if (param != null && param.length==2 && "dataformat".equalsIgnoreCase(param[0])) {
                            int idx=param[1].lastIndexOf("/dataformat/");
                            if (idx>=0) {
                                return param[1].substring(idx+"/dataformat/".length());
                            }
                        }
                    }
                }
            }
            catch (Throwable t) {
                log.error("Unexpected error getting query from url="+url, t);
            }
        } 
        // If no format parameter was found, extract from the filename
        return SemanticUtil.getKindForUrl(filename, extension);
    }

    /**
     * hack to strip out text in parenthesis for google drive files,
     * linked to from GenomeSpace.
     * Only matches pattern at end of filename.
     *     
     * @param filename, e.g. 'all_aml(0By2oidMlPWQtQ2RlQV8zd25Md1E)'
     * @return the filename, stripped of paren, e.g. 'all_aml'
     */
    protected static String stripParen(final String filename) {
        if (filename==null) {
            return null;
        }
        if (!filename.endsWith(")")) {
            // only if it ends with ')'
            return filename;
        }
        int parenthesisIndex = filename.lastIndexOf(("("));
        if (parenthesisIndex >= 0) {
            return filename.substring(0, parenthesisIndex);
        }
        return filename;
    }

    public static String[] splitUrl(final String contextPath, final String urlSpec) throws URISyntaxException, MalformedURLException {
        return splitUrl(contextPath, new URL(urlSpec));
    }

    public static String[] splitUrl(final String contextPath, final URL url) throws URISyntaxException {
        return splitUri(contextPath,  url.toURI());
    }

    /**
     * Split the uri path into the {servletPath}, and {pathInfo}, drop the {contextPath}.
     * Example values when servletContext='/gp' for each uri#path below: 
     *     '/gp/debug'          --> [ '/debug', null ] 
     *     '/gp/debug/'         --> [ '/debug', '/' ] 
     *     '/gp/debug/test'     --> [ '/debug', '/test' ] 
     *     '/gp/debug/test.ext' --> [ '/debug', '/test.ext' ] 
     * Special-cases:
     *     '/gp'                --> [ '', '/' ] 
     *     '/gp/'               --> [ '', '/' ] 
     * 
     * @param url
     * @return an array split into [ {contextPath}, {servletPath}, {pathInfo} ]
     * @throws URISyntaxException
     * @throws MalformedURLException 
     */
    public static String[] splitUri(final String contextPath, final URI uri) { 
        if (contextPath==null) {
            throw new IllegalArgumentException("contextPath==null");
        }
        else if (contextPath.length()>0 && !contextPath.startsWith("/")) {
            throw new IllegalArgumentException("contextPath must be the empty String (\"\") or start with \"/\"");
        }
        String path=uri.getPath();
        path=path.substring(contextPath.length());
        if (path.length()>0 && !path.startsWith("/")) {
            throw new IllegalArgumentException("servletPath must be the empty String (\"\") or start with \"/\"");
        }
        
        //special-case for empty string or '/'
        if (path.length()<=1) {
            return new String[]{"", "/"}; 
        }

        String servletPath="";
        String pathInfo=null;
        final String[] pathElements=path.split("/", -1);
        if (pathElements.length>1) {
            servletPath="/"+pathElements[1];
        }
        if (pathElements.length>2) {
            pathInfo="/"+Joiner.on("/").join(
                    Arrays.copyOfRange(pathElements, 2, pathElements.length)
            );
        }
        return new String[]{servletPath, pathInfo}; 
    }

    /** Converts a string into something you can safely insert into a URL. */
    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
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
