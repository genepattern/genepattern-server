package org.genepattern.server.webapp.jsf;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

public class NotebookBean {
    private boolean notebookEnabled = true;
    private String repositoryURL = null;

    public NotebookBean() {
        String userId = UIBeanHelper.getUserId();
        GpContext userContext = GpContext.getContextForUser(userId);
        notebookEnabled = ServerConfigurationFactory.instance().getGPBooleanProperty(userContext, "notebook.enabled", true);
        repositoryURL = ServerConfigurationFactory.instance().getGPProperty(userContext, "notebook.url", "https://notebook.genepattern.org");
    }

    public boolean isNotebookEnabled() {
        return notebookEnabled;
    }

    public String getRepositoryURL() {
        return repositoryURL;
    }

    public void setNotebookEnabled(boolean notebookEnabled) {
        this.notebookEnabled = notebookEnabled;
    }

    public void setRepositoryURL(String repositoryURL) {
        this.repositoryURL = repositoryURL;
    }
}
