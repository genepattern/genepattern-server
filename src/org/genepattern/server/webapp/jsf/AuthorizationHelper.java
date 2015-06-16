/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import org.genepattern.server.util.AuthorizationManager;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.util.GPConstants;

public class AuthorizationHelper {
    /** Permissions recognized by {@link AuthorizationManager} */
    private static final String ADMIN_JOBS = "adminJobs";

    private static final String ADMIN_MODULES = "adminModules";

    private static final String ADMIN_PIPELINES = "adminPipelines";

    private static final String ADMIN_SERVER = "adminServer";

    private static final String ADMIN_SUITES = "adminSuites";

    private static IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();

    private static final String CREATE_MODULE = "createModule";

    private static final String CREATE_PRIVATE_PIPELINE = "createPrivatePipeline";

    private static final String CREATE_PRIVATE_SUITE = "createPrivateSuite";

    private static final String CREATE_PUBLIC_PIPELINE = "createPublicPipeline";

    private static final String CREATE_PUBLIC_SUITE = "createPublicSuite";

    private AuthorizationHelper() {
    }

    public static boolean adminJobs() {
        return authManager.checkPermission(ADMIN_JOBS, UIBeanHelper.getUserId());
    }

    public static boolean adminJobs(String userId) {
        return authManager.checkPermission(ADMIN_JOBS, userId);
    }

    public static boolean adminModules() {
        return authManager.checkPermission(ADMIN_MODULES, UIBeanHelper.getUserId());
    }

    public static boolean adminModules(String userId) {
        return authManager.checkPermission(ADMIN_MODULES, userId);
    }

    public static boolean adminPipelines() {
        return authManager.checkPermission(ADMIN_PIPELINES, UIBeanHelper.getUserId());
    }

    public static boolean adminServer() {
        return authManager.checkPermission(ADMIN_SERVER, UIBeanHelper.getUserId());
    }

    public static boolean adminServer(String userId) {
        return authManager.checkPermission(ADMIN_SERVER, userId);
    }

    public static boolean adminSuites() {
        return authManager.checkPermission(ADMIN_SUITES, UIBeanHelper.getUserId());
    }

    public static boolean adminSuites(String userId) {
        return authManager.checkPermission(ADMIN_SUITES, userId);
    }

    public static int checkPipelineAccessId(int accessId) {
        if (accessId == GPConstants.ACCESS_PUBLIC) {
            if (!authManager.checkPermission(CREATE_PUBLIC_PIPELINE, UIBeanHelper.getUserId())) {
                return GPConstants.ACCESS_PRIVATE;
            }
        }
        return accessId;
    }

    public static int checkSuiteAccessId(int accessId) {
        if (accessId == GPConstants.ACCESS_PUBLIC) {
            if (!authManager.checkPermission(CREATE_PUBLIC_SUITE, UIBeanHelper.getUserId())) {
                return GPConstants.ACCESS_PRIVATE;
            }
        }
        return accessId;

    }

    public static boolean createModule() {
        return authManager.checkPermission(CREATE_MODULE, UIBeanHelper.getUserId());
    }

    public static boolean createModule(String userId) {
        return authManager.checkPermission(CREATE_MODULE, userId);
    }

    public static boolean createPipeline() {
        return authManager.checkPermission(CREATE_PUBLIC_PIPELINE, UIBeanHelper.getUserId())
                || authManager.checkPermission(CREATE_PRIVATE_PIPELINE, UIBeanHelper.getUserId());
    }

    public static boolean createPipeline(String userId) {
        return authManager.checkPermission(CREATE_PUBLIC_PIPELINE, userId)
                || authManager.checkPermission(CREATE_PRIVATE_PIPELINE, userId);
    }

    public static boolean createPrivatePipeline() {
        return authManager.checkPermission(CREATE_PRIVATE_PIPELINE, UIBeanHelper.getUserId());
    }

    public static boolean createPrivateSuite() {
        return authManager.checkPermission(CREATE_PRIVATE_SUITE, UIBeanHelper.getUserId());
    }

    public static boolean createPublicPipeline() {
        return authManager.checkPermission(CREATE_PUBLIC_PIPELINE, UIBeanHelper.getUserId());
    }

    public static boolean createPublicSuite() {
        return authManager.checkPermission(CREATE_PUBLIC_SUITE, UIBeanHelper.getUserId());
    }

    public static boolean createSuite() {
        return authManager.checkPermission(CREATE_PUBLIC_SUITE, UIBeanHelper.getUserId())
                || authManager.checkPermission(CREATE_PRIVATE_SUITE, UIBeanHelper.getUserId());
    }

    public static boolean createSuite(String userId) {
        return authManager.checkPermission(CREATE_PUBLIC_SUITE, userId)
                || authManager.checkPermission(CREATE_PRIVATE_SUITE, userId);
    }
}
