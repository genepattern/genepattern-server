/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genomespace;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.genepattern.util.SemanticUtil;

public class GenomeSpaceFileHelper {
    private static Logger log = Logger.getLogger(GenomeSpaceFileHelper.class);
    
    /**
     * Determines whether a given URL is a GenomeSpace URL or not
     * @param url
     * @return
     */
    public static boolean isGenomeSpaceFile(URL url) {
        return url.getHost().contains("genomespace.org");
    }

    /**
     * Creates a GpFilePath object for the given string URL.
     * Since no GenomeSpace metadata is provided for the file the metadata will be null.
     * @param url
     * @return
     */
    public static GenomeSpaceFile createFile(Object gsSession, String url) {
        return createFile(gsSession, url, null);
    }
    
    /**
     * Creates a GpFilePath object for the given URL.
     * Since no GenomeSpace metadata is provided for the file the metadata will be null.
     * @param url
     * @return
     */
    public static GenomeSpaceFile createFile(Object gsSession, URL url) {
        return createFile(gsSession, url, null);
    }
    
    /**
     * Creates a GpFilePath object for the given string URL and attaches the provided 
     * GenomeSpace metadata.  Metadata uses the Object type to keep GenomeSpace classes 
     * out of the core of GenePattern.
     * @param url
     * @param metadata
     * @return
     */
    public static GenomeSpaceFile createFile(Object gsSession, String url, Object metadata) {
        try {
            url = url.replaceAll(" ", "%20");
            return createFile(gsSession, new URL(url), null);
        }
        catch (MalformedURLException e) {
            log.error("Unable to create URL object from provided url: " + url);
            return null;
        }
    }
    
    /**
     * Creates a GpFilePath object for the given URL and attaches the provided 
     * GenomeSpace metadata.  Metadata uses the Object type to keep GenomeSpace classes 
     * out of the core of GenePattern.
     * @param url
     * @param metadata
     * @return
     */
    public static GenomeSpaceFile createFile(Object gsSession, URL url, Object metadata) {
        if (!isGenomeSpaceFile(url)) {
            log.warn("URL is not a GenomeSpace URL in GenomeSpaceFileManager: " + url);
            //return null;
        }
        
        if (gsSession == null) {
            log.error("ERROR: null gsSession passed into GenomeSpaceFileManager.createFile()");
        }
        
        try {
            GenomeSpaceFile file = new GenomeSpaceFile(gsSession);
            String filename = extractFilename(url);
            String kind = extractKind(url, filename);
            String extension = SemanticUtil.getExtension(new File(filename));

            // Handle nulls
            if (kind == null) {
                kind = filename;
            }
            if (extension == null) {
                extension = filename;
            }
            
            boolean converted = determineConversion(kind, extension);

            // Obtain the metadata if necessary
            if (metadata == null) {
                try {
                    metadata = GenomeSpaceClientFactory.instance().obtainMetadata(gsSession, url);
                }
                catch (Exception e) {
                    log.error("Error obtaining metadata for: " + url);
                }
            }
            
            file.setUrl(url);
            file.setName(filename);
            file.setExtension(extension);
            file.setKind(kind);
            file.setMetadata(metadata);
            file.setConverted(converted);
            
            if (metadata != null) {
                Date lastModified = GenomeSpaceClientFactory.instance().getModifiedFromMetadata(metadata);
                Long length = GenomeSpaceClientFactory.instance().getSizeFromMetadata(metadata);
                Set<String> conversions = GenomeSpaceClientFactory.instance().getAvailableFormats(metadata);
                
                file.setLastModified(lastModified);
                file.setFileLength(length);
                file.setConversions(conversions);
            }
            
            return file;
        } 
        catch (IOException e) {
            log.error("Error extracting file from GS url: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Determines whether a GenomeSpace file created from a URL is a converted file or not.
     * If it is a converted file the kind will not match the extension.
     * @param kind
     * @param ext
     * @return
     */
    private static boolean determineConversion(String kind, String ext) {
        if (kind == null || ext == null) return false;
        
        if (kind.equals(ext)) {
            return false;
        }
        else {
            return true;
        }
    }
    
    /**
     * Extracts the file's kind from the URL and filename.  Uses a separate implementation from the one 
     * found in SemanticUtil because we need to handle GenomeSpace file conversions.
     * @param url
     * @param filename
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String extractKind(URL url, String filename) throws UnsupportedEncodingException {
        String urlString = URLDecoder.decode(url.toString(), "UTF-8");
        int question = urlString.indexOf("?");
        String formatUrl = null;
        
        // Assuming a question mark has been found in the url
        if (question > 0) {
            formatUrl = urlString.substring(question + 1);
            
            boolean nextIsIt = false;
            for (String i : formatUrl.split("/")) {
                if (nextIsIt) {
                    return i;
                }
                if (i.equals("dataformat")) {
                    nextIsIt= true;
                }
            }
        }

        // If no format parameter was found, extract from the filename
        return SemanticUtil.getKind(new File(filename));

    }
    
    /**
     * Extracts the filename of the file from the URL
     * @param url
     * @return
     * @throws IOException
     */
    public static String extractFilename(URL url) throws IOException {
        String urlString = URLDecoder.decode(url.toString(), "UTF-8");
        int question = urlString.indexOf("?");
        String baseUrl = null;
        if (question < 0) baseUrl = urlString;
        else baseUrl = urlString.substring(0, question);

        int lastSlash = baseUrl.lastIndexOf("/");
        if (lastSlash < 0 || lastSlash == baseUrl.length()) throw new IOException("Unable to extract filename from " + url);

        baseUrl = baseUrl.substring(lastSlash + 1);

        //hack to string out text in parenthesis for google drive files
        if(urlString.toLowerCase().contains("/googledrive:"))
        {
            int parenthesisIndex = baseUrl.lastIndexOf(("("));
            if(parenthesisIndex >= 0)
            {
                baseUrl = baseUrl.substring(0, parenthesisIndex);
            }
        }

        return baseUrl;
    }

    public static String urlToPath(URL gsUrl) {
        // Parse using the new DM pattern for files
        String[] parts = gsUrl.getPath().split("/file/");

        // Check to see if this parsing was successful, if not try old pattern
        if (parts.length < 2) {
            parts = gsUrl.getPath().split("/users/");
        }

        // If there is still an issue with the parsing, throw an error
        if (parts.length < 2) {
            log.error("GenomeSpace file URL does not match URL pattern: " + gsUrl.getPath());
            return null;
        }

        return parts[1];
    }

    public static boolean hasProtocolVersion(URL url) {
        String path = url.getPath();
        String[] parts = path.split("/");
        if (parts.length >= 3) {
            return Pattern.matches("^v?[0-9]*\\.?[0-9]+$", parts[2]);
        }
        else {
            return false;
        }
    }

    public static URL insertProtocolVersion(final URL in) {
        if (in==null) {
            log.error("Invalid input: in==null");
            return in;
        }
        URI u;
        try {
            u=in.toURI();
        }
        catch (URISyntaxException e) {
            log.error("Invalid input, in="+in, e);
            return in;
        }
        try {
            if (u.getPath().startsWith("/datamanager/v1.0")) {
                // no modification needed
                if (log.isDebugEnabled()) {
                    log.debug("input.path already starts with '/datamanager/v1.0', in="+in);
                }
                return in;
            }
            return new URI(
                    u.getScheme(),
                    u.getAuthority(),
                    u.getPath().replaceFirst("/datamanager", "/datamanager/v1.0"), 
                    u.getQuery(),
                    u.getFragment())
            .toURL();
        }
        catch (MalformedURLException | URISyntaxException e) {
            log.error("Unexpected exception, in="+in, e);
            return in;
        }
    }
}
