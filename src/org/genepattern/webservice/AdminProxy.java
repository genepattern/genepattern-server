/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.webservice;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.activation.DataHandler;

import org.apache.axis.client.Service;
import org.apache.axis.configuration.BasicClientConfig;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.util.LSID;

/**
 * @author Joshua Gould
 */
public class AdminProxy implements IAdminClient {
    String endpoint = null;

    org.apache.axis.client.Service service = null;

    AdminSoapBindingStub proxy;

    public AdminProxy(String url, String userName, String password)
    throws WebServiceException 
    {
        this(url, userName, password, true);
    }

    public AdminProxy(String url, String userName, String password, boolean maintainSession) 
    throws WebServiceException 
    {
        try {
            this.endpoint = ProxyUtil.createEndpoint(url, "/services/Admin");

            this.service = new Service(new BasicClientConfig());
            proxy = new AdminSoapBindingStub(new URL(endpoint), service);
            proxy.setUsername(userName);
            proxy.setPassword(password);
            proxy.setMaintainSession(maintainSession);
        } catch (java.net.MalformedURLException mue) {
            throw new WebServiceException(mue);
        } catch (org.apache.axis.AxisFault af) {
            throw new WebServiceException(af);
        }
    }

    public Map getServiceInfo() throws WebServiceException {
        try {
            return proxy.getServiceInfo();
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public TaskInfo getTask(String lsidOrTaskName) throws WebServiceException {
        try {
            return proxy.getTask(lsidOrTaskName);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public SuiteInfo getSuite(String lsidOrTaskName) throws WebServiceException {
        try {
            return proxy.getSuite(lsidOrTaskName);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public Collection<TaskInfo> getLatestTasks() throws WebServiceException {
        try {
            TaskInfo[] array = proxy.getLatestTasks();
            return Arrays.asList(array);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public TaskInfo[] getLatestTasksByName() throws WebServiceException {
        try {
            return proxy.getLatestTasksByName();
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public TaskInfo[] getAllTasks() throws WebServiceException {
        try {
            return proxy.getAllTasks();
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public DataHandler getServerLog() throws WebServiceException {
        try {
            return proxy.getServerLog();
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public DataHandler getGenePatternLog() throws WebServiceException {
        try {
            return proxy.getGenePatternLog();
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public Map getLSIDToVersionsMap() throws WebServiceException {
        try {
            return proxy.getLSIDToVersionsMap();
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public Map getSuiteLsidToVersionsMap() throws WebServiceException {
        try {
            return proxy.getSuiteLsidToVersionsMap();
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public SuiteInfo[] getLatestSuites() throws WebServiceException {
        try {
            return proxy.getLatestSuites();
        } catch (RemoteException e) {
            throw new WebServiceException(e);
        }
    }

    public SuiteInfo[] getAllSuites() throws WebServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public TaskInfo[] getAllTasksForModuleAdmin() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Collection<TaskInfo>> getLatestTasksByType()
            throws WebServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<TaskInfo> getTaskCatalog() throws WebServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public TreeMap<String, TaskInfo> getTaskCatalogByLSID(
            Collection<TaskInfo> tasks) {
        // TODO Auto-generated method stub
        return null;
    }

    public TreeMap<String, TaskInfo> getTaskCatalogByLSID()
            throws WebServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public TaskInfo[] getTasksOwnedBy() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUserProperty(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getVersions(LSID lsid) {
        // TODO Auto-generated method stub
        return null;
    }
}
