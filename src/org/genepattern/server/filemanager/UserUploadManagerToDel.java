package org.genepattern.server.filemanager;

import java.io.File;
import java.util.List;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.GpDirectory;
import org.genepattern.server.dm.GpFilePath;

/**
 * Utility class for working with user upload files.
 * @author pcarr
 *
 */
public abstract class UserUploadManagerToDel {

    /**
     * Create an instanceof a FileObj, does not persist it in the DB.
     * @param userContext
     * @param relativePath
     * @return
     */
    abstract public GpFilePath getGpFileObjFromFile(ServerConfiguration.Context userContext, File relativePath);
    
    //file browser (JSF) support
    /**
     * Get the list of files in the give user upload directory, the 'inDir' must be a directory path
     * relative to the user's upload directory.
     * 
     * @param userContext
     * @param inDir
     * @return an empty list if the inDir is empty or if it is a file rather than a directory.
     */
    abstract public List<GpFilePath> listFiles(ServerConfiguration.Context userContext, GpFilePath inDir);
    
    /**
     * Get the entire tree of user upload files, rooted at the given 'inDir', if inDir is null, then the root is
     * the upload directory for the given user.
     * @param userContext
     * @param inDir
     * @return
     */
    abstract public GpDirectory getFileTree(ServerConfiguration.Context userContext, GpFilePath inDir);
    
    //jumploader support
    /**
     * Create a new file upload object in the user's upload directory.
     * 
     * @param userContext, must contain a valid user_id
     * @param partitionCount, the number of parts the file upload is divided into
     * @param relativePath, the path to the file, relative to the user's upload directory.
     * @return
     * 
     * @throws Exception, exceptions include,
     *     1) server configuration problems,
     *     2) invalid or missing user_id,
     *     3) invalid filename or path
     *     4) file already exists
     */
    abstract public File initFileUpload(ServerConfiguration.Context userContext, int partitionCount, File relativePath) throws Exception;
    
    /**
     * Called from upload receiver before appending uploaded data to the requested file. Call this primarily to validate that the file
     * exists, and to notify the GP server that a part has been received and processed.
     * 
     * @param userContext
     * @param partitionIndex
     * @param partitionCount
     * @param relativePath
     */
    abstract public void handleFileUploadPart(ServerConfiguration.Context userContext, int partitionIndex, int partitionCount, File relativePath); 
    

}
