package org.genepattern.gpge.ui.tasks;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 *  Mantains an internal cache of the latest analysis services. Retrieves older
 *  versions of services when requested.
 *
 * @author    Joshua Gould
 */
public class AnalysisServiceManager {
   private String server;
   private String username;
   private String axisServletURL;
   private Map lsidOrTaskName2AnalysisService;


   /**
    *  Creates a new instance
    *
    * @param  server                   A server URL, for example
    *      http://127.0.0.1:8080
    * @param  username                 The username
    */
   public AnalysisServiceManager(String server, String username) {
      this.server = server;
      this.axisServletURL = server + "/gp/servlet/AxisServlet";
      this.username = username;
   }


   /**
    *  Retrieves the latest versions of all analysis services from the server
    *  stores the analysis services internally
    *
    * @exception  WebServiceException  Description of the Exception
    */
   public void refresh() throws WebServiceException {
      lsidOrTaskName2AnalysisService = new HashMap();

      TaskInfo[] tasks = new AnalysisWebServiceProxy(axisServletURL, username, "").getTasks();
      for(int i = 0; i < tasks.length; i++) {
         TaskInfo task = tasks[i];
         String lsid = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
         String lsidOrTaskName = lsid != null ? lsid : task.getName();
         lsidOrTaskName2AnalysisService.put(lsidOrTaskName, new AnalysisService(server, axisServletURL, task));
      }

   }


   /**
    *  Gets the internal cache of the latest analysis services. Invoke refresh
    *  to update the internal cache.
    *
    * @return    the collections of the latest analysis services
    */
   public Collection getLatestAnalysisServices() {
      return lsidOrTaskName2AnalysisService.values();
   }


   /**
    *  Gets the server
    *
    * @return    the server
    */
   public String getServer() {
      return server;
   }


   /**
    *  Gets the username
    *
    * @return    the username
    */
   public String getUsername() {
      return username;
   }


   /**
    *  Gets the analysis service with the given task name or lsid or <code>null</code>
    *  if no such service exists.
    *
    * @param  lsidOrTaskName  an LSID or task name
    * @return                 the analysis service
    */
   public AnalysisService getAnalysisService(String lsidOrTaskName) {
      if(lsidOrTaskName == null) {
         return null;
      }
      AnalysisService service = (AnalysisService) lsidOrTaskName2AnalysisService.get(lsidOrTaskName);
      if(service == null) {
         try {
            TaskInfo task = new org.genepattern.webservice.AdminProxy(server, username, false).getTask(lsidOrTaskName);// old servers don't have this method
            service = new AnalysisService(server, axisServletURL, task);
         } catch(Throwable t) {
            t.printStackTrace();
         }
      }
      return service;
   }
}
