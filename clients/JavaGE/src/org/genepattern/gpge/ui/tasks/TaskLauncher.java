package org.genepattern.gpge.ui.tasks;
import java.util.*;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;

/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */
public class TaskLauncher {

   private TaskLauncher() { }


   /**
    * @param  svc         the analysis service to run
    * @param  paramInfos  Description of the Parameter
    * @param  username    Description of the Parameter
    */
   static void submitVisualizer(AnalysisService svc, ParameterInfo[] paramInfos, String username) {
      try {
         Map substitutions = new HashMap();
         substitutions.putAll(org.genepattern.gpge.ui.tasks.JavaGELocalTaskExecutor.loadGPProperties());
         for(int i = 0, length = paramInfos.length; i < length; i++) {
            if(ParameterInfo.CACHED_INPUT_MODE.equals(paramInfos[i].getAttributes().get(ParameterInfo.MODE))) {// server file
               substitutions.put(paramInfos[i].getName(), org.genepattern.gpge.io.ServerFileDataSource.getFileDownloadURL(paramInfos[i].getValue()));
            } else {
               substitutions.put(paramInfos[i].getName(), paramInfos[i].getValue());
            }
         }

         new org.genepattern.gpge.ui.tasks.JavaGELocalTaskExecutor(null, svc.getTaskInfo(), substitutions, username, svc.getServer()).exec();
      } catch(Exception e1) {
         throw new RunTaskException(e1);
      }
   }



    /**
    * @param  svc            the analysis service to run
    * @param  paramInfos     Description of the Parameter
    * @param  serviceProxy   Description of the Parameter
    * @return                Description of the Return Value
    * @exception  Exception  Description of the Exception
    */
   static void submitAndWaitUntilCompletionInNewThread(final ParameterInfo[] paramInfos,
         final AnalysisWebServiceProxy serviceProxy,
         final AnalysisService svc) throws Exception {
            new Thread() {
               public void run() {
                  try {
                     submitAndWaitUntilCompletion(paramInfos, serviceProxy, svc);
                  } catch(Exception e) {
                     throw new RunTaskException(e); // FIXME
                  }
               }
            }.start();
   }
  
   static AnalysisJob submitAndWaitUntilCompletion(ParameterInfo[] paramInfos,
         final AnalysisWebServiceProxy serviceProxy,
         final AnalysisService svc) throws Exception {

      TaskInfo tinfo = svc.getTaskInfo();
      final JobInfo jobInfo = serviceProxy.submitJob(tinfo.getID(), paramInfos);
      final AnalysisJob job = new AnalysisJob(svc.getServer(), svc.getTaskInfo().getName(), jobInfo);

      JobModel.getInstance().add(job);
      String status = "";
      JobInfo info = null;

      int initialSleep = 100;
      int sleep = initialSleep;
      int tries = 0;
      int maxTries = 20;
      while(!(status.equalsIgnoreCase("ERROR") || (status.equalsIgnoreCase("Finished")))) {
         tries++;
         try {
            Thread.currentThread().sleep(sleep);
         } catch(InterruptedException ie) {
         }
         try {
            info = serviceProxy.checkStatus(job.getJobInfo().getJobNumber());
            job.setJobInfo(info);
            String currentStatus = info.getStatus();
            if(!(status.equals(currentStatus))) {
               JobModel.getInstance().jobStatusChanged(job);
            }
            status = currentStatus;
         } catch(Exception e) {
            throw new RunTaskException(e);
         }
         sleep = incrementSleep(initialSleep, tries, maxTries);

      }
      JobModel.getInstance().jobCompleted(job);
      return job;
   }


   /**
    *  make the sleep time go up as it takes longer to exec. eg for 100 tries
    *  of 1000ms (1 sec) first 20 are 1 sec each next 20 are 2 sec each next 20
    *  are 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
    *
    * @param  init      Description of the Parameter
    * @param  maxTries  Description of the Parameter
    * @param  count     Description of the Parameter
    * @return           Description of the Return Value
    */
   private static int incrementSleep(int init, int maxTries, int count) {
      if(count < (maxTries * 0.2)) {
         return init;
      }
      if(count < (maxTries * 0.4)) {
         return init * 2;
      }
      if(count < (maxTries * 0.6)) {
         return init * 4;
      }
      if(count < (maxTries * 0.8)) {
         return init * 8;
      }
      return init * 16;
   }


   public static boolean isVisualizer(AnalysisService service) {
      return "visualizer"
            .equalsIgnoreCase((String) service.getTaskInfo()
            .getTaskInfoAttributes()
            .get(
            GPConstants.TASK_TYPE));
   }
}
