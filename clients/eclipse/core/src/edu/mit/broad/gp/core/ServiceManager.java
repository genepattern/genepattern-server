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


/*
 * Created on Jun 11, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.io.File;
import java.net.MalformedURLException;
import org.apache.axis.AxisFault;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.RequestHandler;
import org.genepattern.webservice.WebServiceException;
import java.util.Set;


/**
 * @author genepattern
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ServiceManager {
	private Map serviceMap = null;
	private Vector categories = null;
	private String server = null;
	private String serverUrl = null;
	private String user = null;
	
	/**
	 * 
	 */
	public ServiceManager(String server, String user) {
		
		this.server = getServerUrl(server);
		this.serverUrl= this.server+"/gp/servlet/AxisServlet";
		if (!(serverUrl.startsWith("http://"))){
			this.serverUrl="http://"+this.serverUrl;
		}
		this.user = user;
	}
	
	/** Gets a sorted map that maps the category name to a vector of analysis services.
	 * 
	 * @return the service map.
	 */
	public  Map getServiceMap(){
		if (serviceMap == null) loadServiceMap();
		return serviceMap;
	}
	
	public  Vector getCategories(){
		if (categories == null) loadServiceMap();
		return categories;
	}
	
	/** Gets the analysis services in the given category. The returned vector is in ascending order
	 * 
	 * @param catName the task category.
	 * @return the analysis services.
	 */
	public  Vector getServicesInCategory(String catName){
		getServiceMap();
		Vector services = new Vector();
		for (Iterator iter = serviceMap.values().iterator(); iter.hasNext(); ){
			Object obj = iter.next();
			AnalysisService service = (AnalysisService)obj;
			String svcCat = getCategory(service);
			if (catName.equals(svcCat)){
				services.add(service);
			}
		}
		java.util.Collections.sort(services,
                new java.util.Comparator() {
                    public int compare(Object obj1, Object obj2) {
                        AnalysisService svc1 = (AnalysisService) obj1;
                        AnalysisService svc2 = (AnalysisService) obj2;
                        return svc1.getTaskInfo().getName().compareTo(
                                svc2.getTaskInfo().getName());
                    }

                    public boolean equals(Object obj1, Object obj2) {
                        AnalysisService svc1 = (AnalysisService) obj1;
                        AnalysisService svc2 = (AnalysisService) obj2;
                        return svc1.getTaskInfo().getName().equals(
                                svc2.getTaskInfo().getName());
                    }
                });

		return services;
	}
	
	private  String getCategory(AnalysisService service){
	   String cat = (String)(service.getTaskInfo().getTaskInfoAttributes().get("taskType"));
	   return cat;
		
	}
	
	public  Map getCategoryMap(){
		if (serviceMap == null) loadServiceMap();
		
		Map map = new java.util.TreeMap(); // sort the categories
		for (Iterator iter = categories.iterator(); iter.hasNext(); ){
			String cat = (String)iter.next();
			map.put(cat, getServicesInCategory(cat));
		}
		return map;
	}
	
	
	public  AnalysisService getService(String name){
		return (AnalysisService)serviceMap.get(name);
	}
	public  String getServiceName(int id){
	    for (Iterator iter = getServiceMap().keySet().iterator(); iter.hasNext(); ){
	        AnalysisService svc = (AnalysisService)getServiceMap().get(iter.next());
	        if (svc.getTaskInfo().getID() == id) return svc.getTaskInfo().getName();
	        
	    }
		return "unknown";
	}
	
	public TaskIntegratorProxy getTaskIntegratorProxy() throws MalformedURLException, AxisFault {
	    return new TaskIntegratorProxy(serverUrl, user);
	}
	
	public RequestHandler getRequestHandler(){
		return new RequestHandler(server, serverUrl, user, "");
	}
	
	private Vector getAllServices() throws WebServiceException {
		Vector tasks = new Vector();
		RequestHandler handler = getRequestHandler();
		try {
			AnalysisService[] services = handler.getAnalysisServices();
			int size = services.length;
			for(int i = 0; i < size; i++) {
				tasks.add(services[i]);
			}
		} catch(WebServiceException wse) {
			wse.printStackTrace();
			throw wse;
		}

		return tasks;
	}
	
	protected Map loadServiceMap(){
		try {
			final String separator = java.io.File.separator;		
			serviceMap = new java.util.HashMap(); 
			categories = new Vector();
			
			if (System.getProperty("omnigene.conf") == null) {
				System.setProperty("omnigene.conf", new File("").getAbsolutePath());
			} 
			System.out.println("Connecting to: " + server);
			
			Vector services = getAllServices();
			for (Iterator iter = services.iterator(); iter.hasNext(); ){
			    AnalysisService svc = (AnalysisService)	iter.next();
			    TaskInfo tinfo = svc.getTaskInfo();
			    String name = tinfo.getName(); 
			    serviceMap.put(name, svc);
			    categories.add(getCategory(svc));
			    //System.out.println("Service is "+ svc.getURL());
			}    
			return serviceMap;
		} catch (Exception e){
			e.printStackTrace();
		}  
	
		return null;
	}

	public String getServerName(){
		return server;	
	}

	protected static HashMap serverNameMap = null;
	
	public static void initServerNameMap(){
		serverNameMap = new HashMap();
		String[] servers = GPGECorePlugin.getDefault().getPreferenceArray(GPGECorePlugin.SERVERS_PREFERENCE);
		for (int i=0; i < servers.length; i++){
			addServer( servers[i]);
		}
	}
	
	public static String addServer(String url){
	    String key = url.replace(':', '-');// hack since eclipse cannot take the ':' in the view secondary id
	    getServerNameMap().put( key, url);
		return key;
	}
	
	public static Set getServerNames(){
		return getServerNameMap().keySet();
	}
	public static HashMap getServerNameMap(){
	    if (serverNameMap == null) initServerNameMap();
	    return serverNameMap;
	}
	
	public static String getServerUrl(String name){
	    
		String url =  (String)getServerNameMap().get(name);
		if (url == null) url = name;
		return url;
	}
	
}
