package org.genepattern.gpge.ui.tasks;
import java.util.*;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;
import org.genepattern.gpge.ui.tasks.*;
/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */
public class AnalysisServiceManager {
   private String server;
   private String username;
   private Map lsidOrTaskName2AnalysisService;
   private String axisServletURL;
   private static Map server2Instance = new HashMap();

   public Collection getLatestAnalysisServices() {
      return lsidOrTaskName2AnalysisService.values();
   }

   public String getServer() {
      return server;  
   }
   
   public String getUsername() {
      return username;  
   }
   
   /**
    * @param  server    A server URL, for example http://127.0.0.1:8080
    * @param  username  The username
    */
   private AnalysisServiceManager(String server, String username) {
      this.server = server;
      axisServletURL = server + "/gp/servlet/AxisServlet";
      this.username = username;
   }
   
   public void refresh() {
      lsidOrTaskName2AnalysisService = new HashMap();
      try {
         TaskInfo[] tasks = new AnalysisWebServiceProxy(axisServletURL, username, "").getTasks();
         for(int i = 0; i < tasks.length; i++) {
            TaskInfo task = tasks[i];
            String lsid = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
            String lsidOrTaskName = lsid != null ? lsid : task.getName();
            lsidOrTaskName2AnalysisService.put(lsidOrTaskName, new AnalysisService(server, axisServletURL, task));
         }
      } catch(WebServiceException wse) {

      }
   }


   public void disconnect() {
      server2Instance.remove(this.getServer());
   }


   public static AnalysisServiceManager getInstance(String server, String username) {
      AnalysisServiceManager registry = (AnalysisServiceManager) server2Instance.get(server);
      if(registry == null) {
         registry = new AnalysisServiceManager(server, username);
         server2Instance.put(server, registry);
      }
      return registry;
   }


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
