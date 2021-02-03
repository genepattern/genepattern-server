package org.genepattern.server.dm;

import java.io.File;
import java.io.IOException;

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

    public ExternalFileManager(   ){
        
    }
    
    public abstract void downloadFile( GpContext context, HttpServletRequest req, HttpServletResponse resp, File file) throws IOException;

    public abstract boolean MoveFile(GpContext userContext,  File fromFile, File toFile) throws IOException;
    
        
}
