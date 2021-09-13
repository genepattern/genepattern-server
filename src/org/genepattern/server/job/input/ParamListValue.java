package org.genepattern.server.job.input;

import java.net.URI;
import java.net.URL;

import org.genepattern.server.dm.GpFilePath;

/**
 * Helper class used by ParamListHelper as an intermediate value 
 * when preparing a job with file list parameters.
 * 
 * @author pcarr
 *
 */
public class ParamListValue {
    public enum Type {
        SERVER_PATH,
        EXTERNAL_URL,
        SERVER_URL,
        GENOMESPACE_URL,
        EXRERNAL_URI
    }

    final ParamListValue.Type type;
    private GpFilePath gpFilePath;
    final URL url; //can be null
    final URI uri; //can be null
    boolean isCached; // for external_url, when true it means download to global cache rather than per-user cache
    boolean isPassByReference; // pass by reference values are not downloaded to the local file system; gpFilePath.serverPath is null
    
    public ParamListValue(final ParamListValue.Type type, final GpFilePath gpFilePath, final URL url) {
        this.type=type;
        this.gpFilePath=gpFilePath;
        this.url=url;
        this.uri = null;
    }
    public ParamListValue(final ParamListValue.Type type, final GpFilePath gpFilePath, final URI uri) {
        this.type=type;
        this.gpFilePath=gpFilePath;
        this.uri=uri;
        this.url = null;
    }
    
    public ParamListValue.Type getType() {
        return type;
    }
    
    protected void setGpFilePath(final GpFilePath gpFilePath) {
        this.gpFilePath=gpFilePath;
    }

    public GpFilePath getGpFilePath() {
        return gpFilePath;
    }

    public URL getUrl() {
        return url;
    }
    public URI getUri() {
        return uri;
    }
    
}
