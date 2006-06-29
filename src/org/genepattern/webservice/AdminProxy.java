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


package org.genepattern.webservice;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;

import javax.activation.DataHandler;

import org.apache.axis.client.Service;
import org.apache.axis.configuration.BasicClientConfig;

/**
 * @author Joshua Gould
 */
public class AdminProxy {
	String endpoint = null;

	org.apache.axis.client.Service service = null;

	AdminSoapBindingStub proxy;

	public AdminProxy(String url, String userName) throws WebServiceException {
		this(url, userName, true);
	}

	public AdminProxy(String url, String userName, boolean maintainSession)
			throws WebServiceException {
		try {
			this.endpoint = url;
			if (!(endpoint.startsWith("http://") || endpoint.startsWith("https://"))) {
				this.endpoint = "http://" + this.endpoint;
			}
			String context = (String)System.getProperty("GP_Path", "/gp");
			
			this.endpoint = this.endpoint + context + "/services/Admin";
			this.service = new Service(new BasicClientConfig());
			proxy = new AdminSoapBindingStub(new URL(endpoint), service);
			proxy.setUsername(userName);
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

	public TaskInfo[] getLatestTasks() throws WebServiceException {
		try {
			return proxy.getLatestTasks();
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
}