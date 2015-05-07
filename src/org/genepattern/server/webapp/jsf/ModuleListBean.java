/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.SelectItem;

import org.genepattern.server.webservice.server.AdminService;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class ModuleListBean {

    public List getLatestModules() {

        IAdminClient admin = new LocalAdminClient(UIBeanHelper.getUserId());

        try {
            return asSelectItemList(admin.getLatestTasks());

        }
        catch (WebServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new ArrayList();
    }

    public List getAllModules() {

        IAdminClient admin = new LocalAdminClient(UIBeanHelper.getUserId());

        try {
            return asSelectItemList(admin.getTaskCatalog());

        }
        catch (WebServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new ArrayList();
    }

    protected List<SelectItem> asSelectItemList(Collection in) {
        ArrayList<SelectItem> out = new ArrayList();
        new SelectItem();
        for (Iterator iter = in.iterator(); iter.hasNext();) {
            TaskInfo ti = (TaskInfo) iter.next();
            out.add(new SelectItem(ti.getTaskInfoAttributes().get("LSID"), ti.getName()));
        }

        return out;
    }

}
