/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

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

    public boolean isCreatePrivatePipelineAllowed() {
	return AuthorizationHelper.createPrivatePipeline();
    }

    public boolean isCreatePublicPipelineAllowed() {
	return AuthorizationHelper.createPublicPipeline();
    }

    public boolean isCreatePrivateSuiteAllowed() {
	return AuthorizationHelper.createPrivateSuite();
    }

    public boolean isCreatePublicSuiteAllowed() {
	return AuthorizationHelper.createPublicSuite();
    }
}
