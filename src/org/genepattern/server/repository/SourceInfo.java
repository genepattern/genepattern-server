package org.genepattern.server.repository;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;

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
    final static private Logger log = Logger.getLogger(SourceInfo.class);
    public enum Type {
        REPOSITORY, // installed from module repository
        ZIP, // installed from zip file
        NEW, // created with Module Integrator (first version)
        EDIT, // edited with Module Integrator (from a new or cloned module)
        CLONE, // cloned on this server
        PREVIOUSLY_INSTALLED, //default setting when updating a GP server from <= 3.6.0 to >= 3.6.1
        UNKNOWN
    }

    final static SourceInfoLoader sourceInfoLoaderSingleton=new StubSourceInfoLoader();
    final static public SourceInfoLoader getSourceInfoLoader(final Context userContext) {
        return sourceInfoLoaderSingleton;
    }
    
    protected boolean showSourceInfo=true;
    final private Type type;
    final private String iconImgSrc;
    final private String label;
    
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
            //TODO: 
            log.error("Not implemented!");
            return repositoryInfo.getDescription();
        }
        
        public String getFullDescription() {
            //TODO: 
            log.error("Not implemented!");
            return repositoryInfo.getDescription();
        }
    }
    
    //TODO: implement FromZip extends SourceInfo class
    //TODO: implement FromModuleIntegrator extends SourceInfo class
    //TODO: implement FromClone extends SourceInfo class

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
            return null;
        }
        public String getFullDescription() {
            return null;
        }
    }

}
