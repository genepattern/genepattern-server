package org.genepattern.webservice;


import java.net.URL;

import javax.activation.DataHandler;

import java.rmi.RemoteException;

import java.util.Map;

import org.apache.axis.client.Service;


/**
 * @author Joshua Gould
 */
public class AdminProxy {
   String endpoint=null;
   org.apache.axis.client.Service service=null;
   AdminSoapBindingStub proxy;

	public AdminProxy(String url, String userName)
              throws java.net.MalformedURLException, org.apache.axis.AxisFault {
		this(url, userName, true);
	}
	
   public AdminProxy(String url, String userName, boolean maintainSession)
              throws java.net.MalformedURLException, org.apache.axis.AxisFault {
      this.endpoint=url;
		if (!(endpoint.startsWith("http://"))){
			this.endpoint="http://"+this.endpoint;
		}
		this.endpoint= this.endpoint+"/gp/services/Admin";
      this.service=new Service();
      proxy=new AdminSoapBindingStub(new URL(endpoint), service);
      proxy.setUsername(userName);
		proxy.setMaintainSession(maintainSession);
   }

   public Map getServiceInfo()
                      throws WebServiceException, RemoteException {
      return proxy.getServiceInfo();
   }

   public TaskInfo getTask(String lsidOrTaskName)
                    throws WebServiceException, RemoteException {
      return proxy.getTask(lsidOrTaskName);
   }

	public TaskInfo[] getLatestTasks()
                       throws WebServiceException, RemoteException {
      return proxy.getLatestTasks();
   }
	
	public TaskInfo[] getLatestTasksByName()
                       throws WebServiceException, RemoteException {
      return proxy.getLatestTasksByName();
   }
	
   public TaskInfo[] getAllTasks()
                       throws WebServiceException, RemoteException {
      return proxy.getAllTasks();
   }

   public DataHandler getServerLog()
                                                throws WebServiceException, 
                                                       RemoteException {
      return proxy.getServerLog();
   }

   public DataHandler getGenePatternLog()
                                                  throws WebServiceException, 
                                                         RemoteException {
      return proxy.getGenePatternLog();
   }

   public Map getLSIDToVersionsMap()
                            throws WebServiceException, RemoteException {
      return proxy.getLSIDToVersionsMap();
   }
}