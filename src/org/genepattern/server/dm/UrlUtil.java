package org.genepattern.server.dm;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
     * @param request
     * @return
     */
    public static String getGpUrl(final HttpServletRequest request) {
        String gpUrl = request.getScheme() + "://"+ request.getServerName();
        if (request.getServerPort() > 0) {
            gpUrl += ":"+request.getServerPort();
        }
        gpUrl += request.getContextPath();
        return gpUrl;
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
