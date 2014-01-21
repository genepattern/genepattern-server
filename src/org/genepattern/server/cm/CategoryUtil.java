package org.genepattern.server.cm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.executor.CommandProperties.Value;
import org.genepattern.server.task.category.dao.TaskCategory;
import org.genepattern.server.task.category.dao.TaskCategoryRecorder;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper methods for custom and hidden categories.
 * @author pcarr
 *
 */
public class CategoryUtil {
    final static Logger log = Logger.getLogger(CategoryUtil.class);
    
    public static String getBaseLsid(final TaskInfo taskInfo) {
        try {
            return new LSID(taskInfo.getLsid()).toStringNoVersion();
        }
        catch (Throwable t) {
            log.error(t);
        }
        return null;
    }
    
    public List<String> getCategoriesFromManifest(final TaskInfo taskInfo) {
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

    private List<String> parseCategoriesFromManifest(final TaskInfo taskInfo) {
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
    public Set<String> getHiddenCategories(final ServerConfiguration.Context userContext) {
        final Value value=ServerConfiguration.instance().getValue(userContext, CategoryManager.class.getName()+".hiddenCategories");
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
    public boolean isHidden(final String categoryName) {
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