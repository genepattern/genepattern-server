package org.genepattern.server.genomespace;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.util.SemanticUtil;

public class GenomeSpaceFileManager {
    private static Logger log = Logger.getLogger(GenomeSpaceFileManager.class);
    
    public static boolean isGenomeSpaceFile(URL url) {
        return url.getHost().contains("genomespace.org");
    }

    public static GpFilePath createFile(String url) {
        return createFile(url, null);
    }
    
    public static GpFilePath createFile(URL url) {
        return createFile(url, null);
    }
    
    public static GpFilePath createFile(String url, Object metadata) {
        try {
            return createFile(new URL(url), null);
        }
        catch (MalformedURLException e) {
            log.error("Unable to create URL object from provided url: " + url);
            return null;
        }
    }
    
    public static GpFilePath createFile(URL url, Object metadata) {
        if (!isGenomeSpaceFile(url)) {
            log.error("URL is not a GenomeSpace URL in GenomeSpaceFileManager: " + url);
            return null;
        }
        
        try {
            GenomeSpaceFile file = new GenomeSpaceFile();
            String filename = extractFilename(url);
            String kind = extractKind(url, filename);
            String extension = SemanticUtil.getExtension(new File(filename));
            boolean converted = determineConversion(kind, extension);
            
            file.setUrl(url);
            file.setName(filename);
            file.setExtension(extension);
            file.setKind(kind);
            file.setMetadata(metadata);
            file.setConverted(converted);
            
            if (metadata != null) {
                Date lastModified = GenomeSpaceClientFactory.getGenomeSpaceClient().getModifiedFromMetadata(metadata);
                Long length = GenomeSpaceClientFactory.getGenomeSpaceClient().getSizeFromMetadata(metadata);
                
                file.setLastModified(lastModified);
                file.setFileLength(length);
            }
            
            return file;
        } 
        catch (IOException e) {
            log.error("Error extracting file from GS url: " + e.getMessage());
            return null;
        }
    }
    
    private static boolean determineConversion(String kind, String ext) {
        if (kind == null || ext == null) return false;
        
        if (kind.equals(ext)) {
            return false;
        }
        else {
            return true;
        }
    }
    
    private static String extractKind(URL url, String filename) throws UnsupportedEncodingException {
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
    
    private static String extractFilename(URL url) throws IOException {
        String urlString = URLDecoder.decode(url.toString(), "UTF-8");
        int question = urlString.indexOf("?");
        String baseUrl = null;
        if (question < 0) baseUrl = urlString;
        else baseUrl = urlString.substring(0, question);

        int lastSlash = baseUrl.lastIndexOf("/");
        if (lastSlash < 0 || lastSlash == baseUrl.length()) throw new IOException("Unable to extract filename from " + url);
        
        return baseUrl.substring(lastSlash + 1);
    }
}
