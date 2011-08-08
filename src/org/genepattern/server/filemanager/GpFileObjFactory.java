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
     * @param parentDir, the relative path to the parent directory for the uploaded file. 
     *     The path can be null, which means the file is uploaded to the user's upload directory.
     *     When it is not null, the path must be a relative path to the user's upload directory.
     *     Subdirectories must be separated by forward slashes ('/'). No need to append a trailing slash.
     * @param filename, the filename
     * @return
     */
    static public GpFileObj getUserUploadFile(ServerConfiguration.Context userContext, String parentDir, String filename) {
        File userUploadDir = ServerConfiguration.instance().getUserUploadDir(userContext);
        
        File relativeFile = null;
        if (parentDir == null) {
            relativeFile = new File(filename);
        }
        else {
            relativeFile = new File(parentDir, filename);
        }
        //1) construct a file reference to the server file
        //   e.g. serverFile=<user.upload.dir>[/relativeParentDir]/filename
        File serverFile = new File(userUploadDir, relativeFile.getPath());
        
        //2) construct a URI from the file, to get the relative uri path
        //   e.g. uriPath=/users/<user_id>[/relativeParentDir]/filename
        //Note: creating a File obj so that we can use UrlUtil methods to encode the path
        File tmp = new File("/users/"+userContext.getUserId()+"/"+relativeFile.getPath());
        String tmpPath = UrlUtil.encodeFilePath(tmp);
        URI relativeUri = null;
        try {
            relativeUri = new URI(tmpPath);
        }
        catch (URISyntaxException e) {
            log.error("Invalid URI: "+tmpPath, e);
        }
        UserUploadFile userUploadFile = new UserUploadFile( relativeUri );
        userUploadFile.setServerFile( serverFile );
        return userUploadFile;
    }
}
