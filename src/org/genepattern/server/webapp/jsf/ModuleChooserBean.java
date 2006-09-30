/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class ModuleChooserBean {
    Logger log = Logger.getLogger(ModuleChooserBean.class);
    private Category[] categories;
    private List<SelectItem> modules;

    public ModuleChooserBean() {
        LocalAdminClient admin = new LocalAdminClient(UIBeanHelper.getUserId());

        try {
            Map taskTypes = admin.getLatestTasksByType();
            modules = asSelectItemList(admin.getLatestTasks());
            this.categories = new Category[taskTypes.size()];
            int i = 0;
            for (Iterator it = taskTypes.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                Collection tasks = (Collection) taskTypes.get(key);
                categories[i++] = new Category(key, asSelectItemList(tasks));
            }

        }
        catch (WebServiceException e) {
            log.error(e);
        }
    }

    public List<SelectItem> getLatestModules() {
        return modules;

    }

    public Category[] getCategories() {
        return categories;
    }

    public static class Category {
        private String name;
        private List<SelectItem> modules;

        public Category(String name, List<SelectItem> modules) {
            this.name = name;
            this.modules = modules;
        }

        public String getName() {
            return name;
        }

        public List<SelectItem> getModules() {
            return modules;
        }

    }

    protected List<SelectItem> asSelectItemList(Collection in) {
        ArrayList<SelectItem> out = new ArrayList<SelectItem>();

        for (Iterator iter = in.iterator(); iter.hasNext();) {
            TaskInfo ti = (TaskInfo) iter.next();
            out.add(new SelectItem(ti.getTaskInfoAttributes().get("LSID"), ti.getName()));
        }

        return out;
    }
}
