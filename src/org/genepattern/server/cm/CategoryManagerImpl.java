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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.executor.CommandProperties.Value;
import org.genepattern.server.task.category.dao.TaskCategory;
import org.genepattern.server.task.category.dao.TaskCategoryRecorder;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

/**
 * Helper class for managing categories for installed modules and pipelines.
 * 
 * Refactored from the ModuleHelper class for the JSF implementation of the 
 * Modules & Pipelines panel.
 * 
 * @author pcarr
 *
 */
public class CategoryManagerImpl {
    private static Logger log = Logger.getLogger(CategoryManagerImpl.class);

    /**
     * @see doc for {@link CategoryManager#getCategoriesForTask(Context, TaskInfo)}
     */
    public static List<String> getCategoriesForTask(final Context userContext, final TaskInfo taskInfo) {
        final boolean includeHidden=false;
        return getCategoriesForTask(userContext, taskInfo, includeHidden);
    }
    
    public static List<String> getCategoriesForTask(final Context userContext, final TaskInfo taskInfo, final boolean includeHidden) {
        final boolean checkCustomCategories;
        checkCustomCategories=ServerConfiguration.instance().getGPBooleanProperty(
                userContext, CategoryManager.class.getName()+".checkCustomCategories", true);

        final List<String> hiddenCategories=new ArrayList<String>();
        if (!includeHidden) {
            final Value value=ServerConfiguration.instance().getValue(userContext, CategoryManager.class.getName()+".hiddenCategories");
            if (value != null) {
                hiddenCategories.addAll(value.getValues());
            }
        }

        List<String> categories=null;
        if (checkCustomCategories) {
            categories=getCustomCategoriesFromDb(taskInfo);
        }
        if (categories==null) {
            categories=getCategoriesFromManifest(taskInfo);
        }
        //check for '.' categories
        if (!includeHidden) {
            for(final String category : categories) {
                if (isHidden(category)) {
                    hiddenCategories.add(category);
                }
            }
            for(final String hidden : hiddenCategories) {
                categories.remove(hidden);
            }
        }
        return categories;
    }
    
    public static boolean isHidden(final String category) {
        if (category==null) {
            return true;
        }
        if (category.length()==0) {
            return true;
        }
        if (category.startsWith(".")) {
            return true;
        }
        return false;
    }

    /**
     * @see doc for {@link CategoryManager#getCategoriesFromManifest(TaskInfo)}
     */
    public static List<String> getCategoriesFromManifest(final TaskInfo taskInfo) {
        //check for custom 'categories' in the manifest ...
        final List<String> categories=parseCategoriesFromManifest(taskInfo);
        if (categories != null) {
            return categories;
        }
        
        //legacy, (<= GP 3.7.2) use the taskType
        String taskType = taskInfo.getTaskInfoAttributes().get("taskType");
        if (taskType == null || taskType.length() == 0) {
            taskType = "Uncategorized";
        }
        taskType=taskType.trim();
        List<String> rval=new ArrayList<String>();
        rval.add(taskType);
        return rval;
    }
    
    protected static List<String> parseCategoriesFromManifest(final TaskInfo taskInfo) {
        //check for custom 'categories' in the manifest ...
        if (!taskInfo.getTaskInfoAttributes().containsKey(GPConstants.CATEGORIES)) {
            //no match, return null
            return null;
        }
        else {
            //found a match, start with zero categories
            String customCategories = taskInfo.getTaskInfoAttributes().get(GPConstants.CATEGORIES);
            String[] arr=customCategories.split(";");
            if (arr.length==0) {
                return Collections.emptyList();
            }
            final List<String> rval=new ArrayList<String>();
            for(final String categoryIn : arr) {
                //trim whitespace from each category
                final String category=categoryIn.trim();
                if (category.length() > 0) {
                    rval.add(category);
                }
            }
            return rval;
        }
    }
    
    /**
     * Get the custom categories for the given task, which can optionally be set by an admin of the server.
     * So that modules can be re-categorized without requiring a new version of the module.
     * 
     * @return null when there is no server-customization for the task;
     *         an empty list if the module should be hidden from the server;
     *         a list of categories 
     */
    protected static List<String> getCustomCategoriesFromDb(final TaskInfo taskInfo) {
        try {
            final TaskCategoryRecorder recorder=new TaskCategoryRecorder();
            final LSID lsid=new LSID(taskInfo.getLsid());
            final String baseLsid=lsid.toStringNoVersion();
            final List<TaskCategory> categories=recorder.query(baseLsid);
            if (categories==null || categories.size() ==0) {
                //null indicates that there are no server-based customizations for the installed module
                return null;
            }

            final List<String> rval=new ArrayList<String>();
            boolean hasHidden=false;
            for(final TaskCategory taskCategory : categories) {
                //empty string or '.' prefix means hidden
                String category=taskCategory.getCategory();
                if (category != null) {
                    category = category.trim();
                    if (category.length()==0 || category.startsWith(".")) {
                        //ignore hidden 
                        hasHidden=true;
                    }
                    else {
                        rval.add(category);
                    }
                }
            }
            if (hasHidden && rval.size()>0) {
                log.error("found hidden and non-hidden entries in 'task_category' table for "+taskInfo.getName()+" ("+taskInfo.getLsid()+")");
            }
            return rval;
        }
        catch (Throwable t) {
            log.error("Error checking 'task_category' table for "+taskInfo.getName()+" ("+taskInfo.getLsid()+")", t);
            return null;
        }
    }
    
    public static List<String> getAllCategories(final Context userContext, final boolean includeHidden) {
        SortedSet<String> categories = new TreeSet<String>(new Comparator<String>() {
            // sort categories alphabetically, ignoring case
            public int compare(String arg0, String arg1) {
                String arg0tl = arg0.toLowerCase();
                String arg1tl = arg1.toLowerCase();
                int rval = arg0tl.compareTo(arg1tl);
                if (rval == 0) {
                    rval = arg0.compareTo(arg1);
                }
                return rval;
            }
        });

        final TaskInfo[] taskInfos = TaskInfoCache.instance().getAllTasks();
        for(final TaskInfo taskInfo : taskInfos) {
            final List<String> categoriesForTask=getCategoriesForTask(userContext, taskInfo, includeHidden);
            categories.addAll(categoriesForTask);
        }
        return new ArrayList<String>(categories);
    }
    
}
