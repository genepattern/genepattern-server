package org.genepattern.junitutil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.junit.Ignore;

/**
 * Helper class for working with GpFilePath objects within junit tests.
 * It's a variation of the ServerFilePath object, but which can be modified
 * without worrying about affecting the core GP library.
 * 
 * @author pcarr
 *
 */
@Ignore
public class MockGpFilePath extends GpFilePath {
    private final File localFile;
    private final File relativeFile;
    private final URI relativeUri;

    private final String formFieldValue;
    private final String paramInfoValue;
    private final boolean canRead;
    
    private MockGpFilePath(Builder in) {
        this.localFile=in.localFile;
        this.relativeFile=in.relativeFile;
        try {
            this.formFieldValue=getUrl().toExternalForm();
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Error initializing formFieldValue from getUrl", e);
        }
        this.paramInfoValue=formFieldValue;
        this.canRead=in.canRead;
        this.relativeUri=initRelativeUri("/mock/", localFile);
        this.initMetadata();
    }
    
    public static URI initRelativeUri(final String pathPrefixIn, final File serverFile) {
        // pathPrefix="/data/"
        String pathPrefix;
        if (pathPrefixIn==null) {
            pathPrefix="/data/";
        }
        else {
            pathPrefix=pathPrefixIn;
        }
        if (!pathPrefix.startsWith("/")) {
            pathPrefix="/"+pathPrefix;
        }
        if (!pathPrefix.endsWith("/")) {
            pathPrefix=pathPrefix+"/";
        }
            
        String uriPath = pathPrefix + UrlUtil.encodeFilePath(serverFile);
        try {
            URI relativeUri = new URI( uriPath );
            return relativeUri;
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public URI getRelativeUri() {
        return relativeUri;
    }

    @Override
    public File getServerFile() {
        return localFile;
    }

    @Override
    public File getRelativeFile() {
        return relativeFile;
    }

    @Override
    public boolean canRead(boolean isAdmin, GpContext userContext) {
        return canRead;
    }

    @Override
    public String getFormFieldValue() {
        return formFieldValue;
    }

    @Override
    public String getParamInfoValue() {
        return paramInfoValue;
    }
    
    public static class Builder {
        private final File localFile;
        private File relativeFile;
        private boolean canRead=true;
        
        public Builder relativeFile(final File relativeFile) {
            this.relativeFile=relativeFile;
            return this;
        }
        
        public Builder canRead(final boolean canRead) {
            this.canRead=canRead;
            return this;
        }
        
        public Builder(final File localFile) {
            this.localFile=localFile;
            this.relativeFile=new File(new File("mock"), localFile.getPath());
        }
        public MockGpFilePath build() {
            return new MockGpFilePath(this);
        }
    }

}
