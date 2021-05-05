package org.genepattern.server.dm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.genepattern.server.config.GpContext;


/**
 * Abstract class to allow creation of a file downloader for AWS S3 
 * (and eventually maybe other systems).  Handled as an abstract class (and instantiated with reflection)
 * in case we want to add other external file stores in the future
 * 
 * @author liefeld
 *
 */


public abstract class ExternalFileManager {

    public static String downloadListingFileName = ".download.listing.txt";
    public static String nonRetrievedFilesFileName = ".non.retrieved.output.files.json";
    public static String classPropertyKey = "external.file.manager.class";
    
    public ExternalFileManager(   ){
        
    }
    
    public abstract void downloadFile( GpContext context, HttpServletRequest req, HttpServletResponse resp, File file) throws IOException;
    
    public abstract String getDownloadURL(GpContext userContext,  File file) throws Exception;
    
    public abstract boolean moveFile(GpContext userContext,  File fromFile, File toFile) throws IOException;
    
    public abstract boolean moveDirectory(GpContext userContext,  File fromFile, File toFile) throws IOException;

    public abstract boolean copyFile(GpContext userContext,  File fromFile, File toFile) throws IOException;
    
    public abstract boolean copyDirectory(GpContext userContext,  File fromFile, File toFile) throws IOException;
    
    public abstract boolean deleteFile(GpContext userContext,  File file) throws IOException;
    
    public abstract boolean deleteDirectory(GpContext userContext,  File file) throws IOException;
    
    public abstract boolean createSubDirectory(GpContext userContext,  File file) throws IOException;
    
    public abstract ArrayList<GpFilePath> listFiles(GpContext userContext,  File file) throws IOException;
    
    public abstract boolean syncRemoteFileToLocal(GpContext userContext,  File file) throws IOException;
    
    public abstract boolean syncLocalFileToRemote(GpContext userContext,  File file) throws IOException;
    
    
}
