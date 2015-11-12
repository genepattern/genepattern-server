package org.genepattern.junitutil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;

/**
 * for creating immutable GpFilePath instances for use in junit tests.
 * @author pcarr
 *
 */
public class DemoGpFilePath extends GpFilePath {
    private final URI relativeUri;
    private final File serverFile;
    private final File relativeFile;
    private final boolean canRead;
    private final String formFieldValue;
    private final String paramInfoValue;
    
    private final boolean isDirectory;
    
    public DemoGpFilePath(final Builder in) {
        this.relativeUri=in.relativeUri;
        this.serverFile=in.serverFile;
        this.relativeFile=in.relativeFile;
        this.canRead=in.canRead;
        this.formFieldValue=in.formFieldValue;
        this.paramInfoValue=in.paramInfoValue;
        this.isDirectory=in.isDirectory;
    }

    @Override
    public URI getRelativeUri() {
        return relativeUri;
    }

    @Override
    public File getServerFile() {
        return serverFile;
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
    
    @Override
    public boolean isDirectory() {
        return isDirectory;
    }
    
    public static class Builder {
        private URI relativeUri=null;
        private File serverFile=null;
        private File relativeFile=null;
        private boolean canRead=true;
        private String formFieldValue="";
        private String paramInfoValue="";
        
        private boolean isDirectory=false;

        private String uriPrefix="";
        private File serverDir=null;
        
        /** e.g. '/users/<user_id>', Note: no trailing slash */
        public Builder uriPrefix(final String uriPrefix) {
            this.uriPrefix=uriPrefix;
            return this;
        }
        
        /** e.g. new File("gp_tutorial/all_aml_test.gct"); Note: no slash prefix */
        public Builder relativeFile(final String relativeFilePath) {
            return relativeFile(new File(relativeFilePath));
        }
        public Builder relativeFile(final File relativeFile) {
            if (relativeFile.isAbsolute()) {
                throw new IllegalArgumentException("relativeFile must not be absolute");
            }
            this.relativeFile=relativeFile;
            return this;
        }

        public Builder serverDir(final File serverDir) {
            this.serverDir=serverDir;
            return this;
        }
        
        public Builder isDir(final boolean isDir) {
            this.isDirectory=isDir;
            return this;
        }

        public Builder canRead(final boolean canRead) {
            this.canRead=canRead;
            return this;
        }
        
        public GpFilePath build() throws URISyntaxException {
            final String relPath=UrlUtil.encodeFilePath(relativeFile);
            
            this.relativeUri=new URI(uriPrefix+"/"+relPath); 
            if (serverDir==null) {
                // by default, use working directory
                serverDir=new File("./").getAbsoluteFile();
            }
            this.serverFile=new File(serverDir, relativeFile.getPath());
            return new DemoGpFilePath(this);
        }
    }

}
