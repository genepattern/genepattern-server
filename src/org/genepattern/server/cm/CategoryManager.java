/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2013) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.cm;

import java.util.List;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for managing categories for installed modules and pipelines.
 * 
 * Refactored from the ModuleHelper class for the JSF implementation of the 
 * Modules & Pipelines panel.
 * 
 * @author pcarr
 *
 */
public class CategoryManager {
    
    private static final CategoryManagerImpl cmImpl=new CategoryManagerImpl();

    /**
     * Get the list of categories for the given task, by default based on the contents of the manifest file for the module,
     * and optionally over-ridden by server configuration settings.
     * 
     * This method was added for GP-4672, which requires a way to associate more than one category to a task.
     * In GP <= 3.7.0 there is one and only one category for a task, as set by the 'taskType' property in the manifest file.
     * In GP > 3.7.0 there can optionally be additional categories for a visualizer or pipeline, as set by the 'categories' property
     * in the manifest file. E.g.
     * <pre>
     *     # pipeline in a custom category
     *     categories=pipeline;RNA-seq
     *     # visualizer in a custom category
     *     categories=Visualizer;RNA-seq
     *     # pipeline moved to a custom category, it won't be in the pipeline category
     *     categories=RNA-seq
     * </pre>
     * 
     * @param taskInfo
     * @return
     */
    public final static List<String> getCategoriesForTask(final Context userContext, final TaskInfo taskInfo) {
        return cmImpl.getCategoriesForTask(userContext, taskInfo);
    }

    /**
     * Get the default list of one or more category for the given task, based on the manifest file
     * for the module (e.g. tied to a particular version of the module).
     * 
     * @return
     */
    public static List<String> getCategoriesFromManifest(final TaskInfo taskInfo) {
        return cmImpl.getCategoriesFromManifest(taskInfo);
    }
    
    public static List<String> getAllCategories() {
        return CategoryManagerImpl.getAllCategories();
    }
}
