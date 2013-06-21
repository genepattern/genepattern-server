package org.genepattern.server.repository;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.webservice.TaskInfo;

/**
 * Java bean representation of the information about the source from which a particular task
 * was installed (or created) on the server.
 * 
 * This is for displaying these details in various parts of the UI, such as the Job Input form
 * and the module properties page.
 * 
 * @author pcarr
 *
 */
public abstract class SourceInfo {
    public enum Type {
        REPOSITORY, // installed from module repository
        ZIP, // installed from zip file
        ON_SERVER, // created on server, for example by ...
                   //     cloning an installed module
                   //     creating a provenance pipeline
                   //     creating a new pipeline with the Pipeline Designer
                   //     editing a new version of a pipeline with the Pipeline Designer
                   //     creating a new module with the Module Integrator
                   //     editing a new version of a module with the Module Integrator
        PREVIOUSLY_INSTALLED, //default setting when updating a GP server from <= 3.6.0 to >= 3.6.1
        UNKNOWN
    }

    //final static SourceInfoLoader sourceInfoLoaderSingleton=new StubSourceInfoLoader();
    final static SourceInfoLoader sourceInfoLoaderSingleton=new LsidSourceInfoLoader();
    final static public SourceInfoLoader getSourceInfoLoader(final Context userContext) {
        return sourceInfoLoaderSingleton;
    }
    
    protected boolean showSourceInfo=true;
    final private Type type;
    protected String iconImgSrc;
    protected String label;
    
    private SourceInfo(final Type type, final String label, final String iconImgSrc) {
        this.type=type;
        this.label=label;
        this.iconImgSrc=iconImgSrc;
    }

    public boolean getShowSourceInfo() {
        return showSourceInfo;
    }

    public Type getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }
    
    public String getIconImgSrc() {
        return iconImgSrc;
    }

    abstract public String getBriefDescription();
    abstract public String getFullDescription();

    /**
     * SourceInfo for a task installed from the module repository.
     * @author pcarr
     *
     */
    final static public class FromRepo extends SourceInfo {
        final private RepositoryInfo repositoryInfo;
        public FromRepo(final RepositoryInfo repositoryInfo) {
            super(Type.REPOSITORY, repositoryInfo.getLabel(), repositoryInfo.getIconImgSrc());
            this.repositoryInfo=repositoryInfo;
            if (this.repositoryInfo.getUrl().toExternalForm().equalsIgnoreCase("http://www.broadinstitute.org/webservices/gpModuleRepository")) {
                this.showSourceInfo=false;
            }
        }
        
        public String getBriefDescription() {
            return repositoryInfo.getBriefDescription();
        }
        
        public String getFullDescription() {
            return repositoryInfo.getFullDescription();
        }
    }
    
    final static public class CreatedOnServer extends SourceInfo {
        private String userId=null;
        public CreatedOnServer() {
            super(Type.ON_SERVER, "Created on server", null);
        }
        
        public CreatedOnServer(final TaskInfo taskInfo) {
            super(Type.ON_SERVER, "Created on server", null);
            this.userId=taskInfo.getUserId();
        }

        public String getBriefDescription() {
            if (userId != null) {
                return "Created on this server by "+userId+", as a clone of an existing module, or with the Module Integrator";
            }
            return "Created on this server, as a clone of an existing module, or with the Module Integrator";
        }
        public String getFullDescription() {
            return null;
        }
    }
    
    final static public class FromZip extends SourceInfo {
        public FromZip() {
            super(Type.ZIP, "Installed from zip", "/gp/images/zip_file.png");
        }

        @Override
        public String getBriefDescription() {
            return null;
        }

        @Override
        public String getFullDescription() {
            return null;
        }
    }

    /**
     * SourceInfo for a task when it is not known where the task was installed from,
     * either because we have not implemented code for this, or because it was from a 
     * module installed before we started recording this information.
     * 
     * @author pcarr
     *
     */
    final static public class FromUnknown extends SourceInfo {
        public FromUnknown() {
            super(Type.UNKNOWN, "?", null);
        }
        public String getBriefDescription() {
            //return "The origin of this module is unknown";
            return null;
        }
        public String getFullDescription() {
            return null;
        }
    }

}
