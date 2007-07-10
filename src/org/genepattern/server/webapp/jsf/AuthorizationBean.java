package org.genepattern.server.webapp.jsf;

public class AuthorizationBean {
    public boolean isAdminJobsAllowed() {
        return AuthorizationHelper.adminJobs();
    }

    public boolean isAdminSuitesAllowed() {
        return AuthorizationHelper.adminSuites();
    }

    public boolean isAdminServerAllowed() {
        return AuthorizationHelper.adminServer();
    }

    public boolean isCreateModuleAllowed() {
        return AuthorizationHelper.createModule();
    }

    public boolean isCreatePipelineAllowed() {
        return AuthorizationHelper.createPipeline();
    }

    public boolean isCreateSuiteAllowed() {
        return AuthorizationHelper.createSuite();
    }

}
