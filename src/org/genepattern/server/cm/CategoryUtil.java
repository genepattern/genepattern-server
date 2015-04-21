package org.genepattern.server.cm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.task.category.dao.TaskCategory;
import org.genepattern.server.task.category.dao.TaskCategoryRecorder;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper methods for multiple categories, custom categories and hidden categories.
 * 
 * @author pcarr
 *
 */
public class CategoryUtil {
    private static final Logger log = Logger.getLogger(CategoryUtil.class);

    public static final String PROP_CHECK_CUSTOM_CATEGORIES="org.genepattern.server.cm.CategoryManager.checkCustomCategories";
    public static final String PROP_HIDDEN_CATEGORIES="org.genepattern.server.cm.CategoryManager.hiddenCategories";
    
    public static String getBaseLsid(final TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return null;
        }
        return getBaseLsid(taskInfo.getLsid());
    }
    
    public static String getBaseLsid(final String lsid) {
        try {
            return new LSID(lsid).toStringNoVersion();
        }
        catch (Throwable t) {
            log.error(t);
        }
        return null;
    }
    
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
    public List<String> getCategoriesForTask(final GpConfig gpConfig, final GpContext userContext, final TaskInfo taskInfo) {
        final boolean includeHidden=false;
        return getCategoriesForTask(gpConfig, userContext, taskInfo, includeHidden);
    }
    
    public List<String> getCategoriesForTask(final GpConfig gpConfig, final GpContext userContext, final TaskInfo taskInfo, final boolean includeHidden) {
        final boolean checkCustomCategories;
        checkCustomCategories=gpConfig.getGPBooleanProperty(userContext, PROP_CHECK_CUSTOM_CATEGORIES, true);

        final List<String> hiddenCategories=new ArrayList<String>();
        if (!includeHidden) {
            final Value value=gpConfig.getValue(userContext, PROP_HIDDEN_CATEGORIES);
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

    /**
     * Get the default list of one or more category for the given task, based on the manifest file
     * for the module (e.g. tied to a particular version of the module).
     * 
     * @return
     */
    public static List<String> getCategoriesFromManifest(final TaskInfo taskInfo) {
        return getCategoriesFromManifest(taskInfo.getTaskInfoAttributes());
    }

    public static List<String> getCategoriesFromManifest(final TaskInfoAttributes tia) {
        //check for custom 'categories' in the manifest ...
        final List<String> categories=parseCategoriesFromManifest(tia);
        if (categories != null) {
            return categories;
        }
        
        //legacy, (<= GP 3.7.2) use the taskType
        String taskType = tia.get(GPConstants.TASK_TYPE);
        if (taskType == null || taskType.length() == 0) {
            taskType = "Uncategorized";
        }
        taskType=taskType.trim();
        List<String> rval=new ArrayList<String>();
        rval.add(taskType);
        return rval;
    }

    public static List<String> parseCategoriesFromManifest(final TaskInfoAttributes tia) {
        //check for custom 'categories' in the manifest ...
        if (!tia.containsKey(GPConstants.CATEGORIES)) {
            //no match, return null
            return null;
        }
        else {
            //found a match, start with zero categories
            String customCategories = tia.get(GPConstants.CATEGORIES);
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
    public List<String> getCustomCategoriesFromDb(final TaskInfo taskInfo) {
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

    /**
     * This lookup table maps custom categories per task, by baseLsid.
     * The key is the baseLsid for a module, the list of values are a list of zero or more categories.
     * 
     * For example, to hide a module, there can be a mapping from the baseLsid to an empty list or
     *     to a list of 'hidden' categories (e.g. '', '.hidden').
     *     
     * @return a Multimap of baseLsid to category name, each entry is a list of zero or more 
     *      categories to be used instead of the category defined in the manifest file for the module.
     */
    public Multimap<String,String> getCustomCategoriesFromDb() {
        final TaskCategoryRecorder recorder=new TaskCategoryRecorder();
        final List<TaskCategory> records=recorder.getAllCustomCategories();
        final Multimap<String,String> customCategoryMap=HashMultimap.create(records.size(), 1);
        for(final TaskCategory record : records) {
            customCategoryMap.put(record.getTask(), record.getCategory());
        }
        return customCategoryMap;
    }

    /**
     * For the given user, get the list of zero or more hidden categories configured by the GP server admin.
     * 
     * @param userContext
     * @return
     */
    public Set<String> getHiddenCategories(final GpConfig gpConfig, final GpContext userContext) {
        final Value value=gpConfig.getValue(userContext, PROP_HIDDEN_CATEGORIES);
        if (value==null || value.getNumValues()==0) {
            return Collections.emptySet();
        }
        final Set<String> hiddenCategories=new HashSet<String>();
        hiddenCategories.addAll(value.getValues());
        return Collections.unmodifiableSet(hiddenCategories);
    }
    
    /**
     * Is the given category name a hidden category, hard-coded rule.
     * A category is hidden if the name is null, the empty string, or begins with the '.' character. 
     * 
     * @param categoryName
     * @return
     */
    public static boolean isHidden(final String categoryName) {
        if (categoryName==null) {
            return true;
        }
        if (categoryName.length()==0) {
            return true;
        }
        if (categoryName.startsWith(".")) {
            return true;
        }
        return false;
    }

}