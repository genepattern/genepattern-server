package org.genepattern.server.filemanager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.UrlUtil;

public class GpFileObjFactory {
    private static Logger log = Logger.getLogger(GpFileObjFactory.class);

    /**
     * Create a new UserUploadFile object for the given user.
     * 
     * @param userContext, must contain a valid user_id for the currentUser
     * @param parentDir, can be null, otherwise must be a valid URI to the relative path to the parent directory
     *                   for the upload file.
     * @param filename, the filename
     * @return
     */
    static public GpFileObj getUserUploadFile(ServerConfiguration.Context userContext, String parentDir, String filename) {
        File userUploadDir = ServerConfiguration.instance().getUserUploadDir(userContext);
        
        URI parentDirUri = null;
        if (parentDir != null) {
            try {
                parentDirUri = new URI(parentDir);
            }
            catch (URISyntaxException e) {
                //TODO: throw exception
            }
        }
        return getUserUploadFile(userContext.getUserId(), userUploadDir, parentDirUri, filename);
    }

    static private UserUploadFile getUserUploadFile(String userId, File userUploadDir, URI parentDir, String filename) {
        UserUploadFile userUpload = null;
        
        String parentPath = null;
        if (parentDir != null) {
            parentPath = parentDir.getPath();
            if (parentPath.endsWith("/")) {
                //drop the trailing '/'
                parentPath = parentPath.substring(0, parentPath.length()-1);
            }
        }
        String relativePath = parentPath != null ? parentPath + "/" + filename : filename;
        //String path = "/users/"+userId+relativePath;
        try {
            File file = new File("/users/"+userId+"/"+relativePath);
            String encodedPath = UrlUtil.encodeFilePath( file );
            URI relativeUri = new URI(encodedPath);
            userUpload = new UserUploadFile(relativeUri);
        }
        catch (URISyntaxException e) {
            //TODO: throw an exception rather than return null
            log.error("Error initializing UserUploadFile from relativePath="+relativePath, e);
            return null;
        }
        
        //set the server file path
        //ServerConfiguration config = ServerConfiguration.instance();
        //File userUploadDir = null;
        //try {
        //    userUploadDir = config.getUserUploadDir(userContext);
        //}
        //catch (Throwable t) {
        //    //TODO: handle exception
        //    log.error("Unable to get userUploadDir for userId="+userContext.getUserId(), t);
        //    return null;
        //}
        File serverFile = new File(userUploadDir, relativePath);
                    UrlUtil.encodeFilePath( serverFile );

        userUpload.setServerFile( serverFile );
        return userUpload;
    }
}
