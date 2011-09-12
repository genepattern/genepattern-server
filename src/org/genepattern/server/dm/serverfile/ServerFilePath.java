package org.genepattern.server.dm.serverfile;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;

/**
 * Implement GpFilePath for server files.
 * 
 * @author pcarr
 */
public class ServerFilePath extends GpFilePath {
    private static Logger log = Logger.getLogger(ServerFilePath.class);

    private File serverFile;
    private URI relativeUri;

    /**
     * Create a new ServerFilePath from the given file object.
     * @param userContext
     * @param file, can be relative or absolute. Relative paths are relative to the working directory
     *     for the GP server.
     */
    public ServerFilePath(File file) {
        if (file == null) {
            throw new IllegalArgumentException("invalid null arg, serverFile");
        }
        if (!file.isAbsolute()) {
            log.warn("Relative file arg to ServerFilePath constructor");
        }
        this.serverFile = file;
        //init the relativeUri
        String uriPath = "/data/" + UrlUtil.encodeFilePath(serverFile);
        try {
            relativeUri = new URI( uriPath );
        }
        catch (URISyntaxException e) {
            log.error(e);
            throw new IllegalArgumentException(e);
        }
    }

    public URI getRelativeUri() {
        return relativeUri;
    }

    public File getServerFile() {
        return serverFile;
    }

    public File getRelativeFile() {
        return serverFile;
    }

    public String getFormFieldValue() {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    public String getParamInfoValue() {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    public String getTasklibValue() {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

}
