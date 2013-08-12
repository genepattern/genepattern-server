package org.genepattern.server.repository;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.taskinstall.InstallInfo;
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
    /**
     * Flag for how to deal with modules when there is no 'install_info' entry in the DB.
     * Either return unknown, or guess based on the lsid of the task.
     */
    final static public String PROP_DEDUCE_FROM_LSID=RepositoryInfo.class.getName()+".deduceFromLsid";
    final static private String BLANK_IMG="/gp/images/blank.gif";
    final static private String ZIP_IMG="/gp/images/winzip_icon.png";
    final static private String SERVER_ONLY_PROD_IMG="/gp/images/server_only_prod.gif";
    final static private String SERVER_ONLY_BETA_IMG="/gp/images/server_only_beta.png";

    final static SourceInfoLoader sourceInfoLoaderSingleton=new DbSourceInfoLoader();
    final static public SourceInfoLoader getSourceInfoLoader(final Context userContext) {
        return sourceInfoLoaderSingleton;
    }
    
    protected boolean showSourceInfo=true;
    final private InstallInfo.Type type;
    protected String iconImgSrc;
    protected String label;
    
    private SourceInfo(final InstallInfo.Type type) {
        this.type=type;
    }
    private SourceInfo(final InstallInfo.Type type, final String label, final String iconImgSrc) {
        this.type=type;
        this.label=label;
        this.iconImgSrc=iconImgSrc;
    }

    public boolean getShowSourceInfo() {
        return showSourceInfo;
    }

    public InstallInfo.Type getType() {
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
            super(InstallInfo.Type.REPOSITORY, repositoryInfo.getLabel(), repositoryInfo.getIconImgSrc());
            this.repositoryInfo=repositoryInfo;
            if (this.repositoryInfo.getUrl().toExternalForm().equalsIgnoreCase("http://www.broadinstitute.org/webservices/gpModuleRepository")) {
                this.showSourceInfo=false;
            }
            // if there is no icon for the repository, show a blank image
            if (this.repositoryInfo.getIconImgSrc()==null || this.repositoryInfo.getIconImgSrc().length()==0) {
                this.iconImgSrc=BLANK_IMG;
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
            super(InstallInfo.Type.SERVER, "Created on server", BLANK_IMG);
        }
        
        public CreatedOnServer(final TaskInfo taskInfo) {
            super(InstallInfo.Type.SERVER, "Created on server", BLANK_IMG);
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
            super(InstallInfo.Type.ZIP, "Installed from zip", ZIP_IMG);
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
            super(InstallInfo.Type.UNKNOWN, "N/A", BLANK_IMG);
        }
        public String getBriefDescription() {
            return "Installation source not known, module was installed before the GP 3.6.1 update";
        }
        public String getFullDescription() {
            return null;
        }
    }
    
    /**
     * SourceInfo for a public server only task, one which is installed on the server 
     * (from either a zip file or the dev repository), but which is not available from the public module repository.
     * 
     * @author pcarr
     *
     */
    final static public class ServerOnly extends SourceInfo {
        private final String briefDescription;
        public ServerOnly(final InstallInfo.Type type) {
            super(type);
            if (type.is(InstallInfo.Type.SERVER_ONLY_PROD)) {
                this.label="Server only";
                this.iconImgSrc=SERVER_ONLY_PROD_IMG;
                this.briefDescription="Production quality module curated by the Broad team, not available from the module repository.";
            }
            else if (type.is(InstallInfo.Type.SERVER_ONLY_BETA)) {
                this.label="Server only (beta)";
                this.iconImgSrc=SERVER_ONLY_BETA_IMG;
                this.briefDescription="Beta quality module curated by the Broad team, not available from the module repository.";
            }
            else {
                this.label="Server only";
                this.iconImgSrc=ZIP_IMG;
                this.briefDescription="Module installed on the server, but not available from the module repository.";
            }
        }

        @Override
        public String getBriefDescription() {
            return briefDescription;
        }

        @Override
        public String getFullDescription() {
            return null;
        }
    }

}
