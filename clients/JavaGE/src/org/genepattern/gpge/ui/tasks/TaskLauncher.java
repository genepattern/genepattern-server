package org.genepattern.gpge.ui.tasks;

import java.util.*;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;
import org.genepattern.gpge.GenePattern;

/**
 * Runs tasks for the GPGE
 * 
 * @author Joshua Gould
 */
public class TaskLauncher {

	private TaskLauncher() {
	}

	/**
	 * @param svc
	 *            the analysis service to run
	 * @param paramInfos
	 *            Description of the Parameter
	 * @param username
	 *            Description of the Parameter
	 */
	public static void submitVisualizer(AnalysisService svc,
			ParameterInfo[] paramInfos, String username) {
      try {
			Map substitutions = new HashMap();
			substitutions
					.putAll(org.genepattern.gpge.ui.tasks.JavaGELocalTaskExecutor
							.loadGPProperties());
			for (int i = 0, length = paramInfos.length; i < length; i++) {
				if (ParameterInfo.CACHED_INPUT_MODE.equals(paramInfos[i]
						.getAttributes().get(ParameterInfo.MODE))) {// server
																	// file
					substitutions.put(paramInfos[i].getName(),
							org.genepattern.gpge.io.ServerFileDataSource
									.getFileDownloadURL(paramInfos[i]
											.getValue()));
				} else {
					substitutions.put(paramInfos[i].getName(), paramInfos[i]
							.getValue());
				}
         }

         new org.genepattern.gpge.ui.tasks.JavaGELocalTaskExecutor(null, svc
					.getTaskInfo(), substitutions, username, svc.getServer())
					.exec();
      } catch(Throwable t) {
          GenePattern.showErrorDialog("An error occurred while running " + svc.getTaskInfo().getName());
      }
		
	}

	/**
	 * @param svc
	 *            the analysis service to run
	 * @param paramInfos
	 *            Description of the Parameter
	 * @param serviceProxy
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 * @exception Exception
	 *                Description of the Exception
	 */
	public static void submitAndWaitUntilCompletionInNewThread(
			final ParameterInfo[] paramInfos,
			final AnalysisWebServiceProxy serviceProxy,
			final AnalysisService svc) {
		new Thread() {
			public void run() {
				try {
					submitAndWaitUntilCompletion(paramInfos, serviceProxy, svc);
				} catch (WebServiceException wse) {
					if(!GenePattern.disconnectedFromServer(wse, svc.getServer())) {
                  GenePattern.showErrorDialog("An error occurred while running " + svc.getTaskInfo().getName());
               }
				}
			}
		}.start();
	}

   /**
   * Waits for the given job to complete in a new thread. Used when user refreshes jobs from the server and a job is in progress
   */
   public static void waitUntilCompletionInNewThread(final AnalysisJob job) {
      new Thread() {
         public void run() {
            try {
               waitUntilCompletion(job, null);  
            } catch(WebServiceException wse) {
               //if(!GenePattern.disconnectedFromServer(wse, svc.getServer())) {
                  GenePattern.showErrorDialog("An error occurred while running " + job.getTaskName());
               //} 
            }
         }
      }.start();
   }
   
	private static AnalysisJob submitAndWaitUntilCompletion(ParameterInfo[] paramInfos,
			final AnalysisWebServiceProxy serviceProxy,
			final AnalysisService svc) throws WebServiceException {

		TaskInfo tinfo = svc.getTaskInfo();
		final JobInfo jobInfo = serviceProxy.submitJob(tinfo.getID(),
				paramInfos);
		final AnalysisJob job = new AnalysisJob(svc.getServer(), jobInfo);
      JobModel.getInstance().add(job);
      return waitUntilCompletion(job, serviceProxy);
   }
   
   private static AnalysisJob waitUntilCompletion(AnalysisJob job, AnalysisWebServiceProxy serviceProxy) throws WebServiceException {
      
		String status = "";
		JobInfo info = null;
      if(serviceProxy==null) {
         serviceProxy = new AnalysisWebServiceProxy(job.getServer(), "GenePattern"); // FIXME  
      }
		int initialSleep = 100;
		int sleep = initialSleep;
		int tries = 0;
		int maxTries = 20;
		while (!(status.equalsIgnoreCase("ERROR") || (status
				.equalsIgnoreCase("Finished")))) {
			tries++;
			try {
				Thread.currentThread().sleep(sleep);
			} catch (InterruptedException ie) {
			}
	
         info = serviceProxy
               .checkStatus(job.getJobInfo().getJobNumber());
         job.setJobInfo(info);
         String currentStatus = info.getStatus();
         if (!(status.equals(currentStatus))) {
            JobModel.getInstance().jobStatusChanged(job);
         }
         status = currentStatus;
			
			sleep = incrementSleep(initialSleep, tries, maxTries);

		}
		JobModel.getInstance().jobCompleted(job);
		return job;
	}

	/**
	 * make the sleep time go up as it takes longer to exec. eg for 100 tries of
	 * 1000ms (1 sec) first 20 are 1 sec each next 20 are 2 sec each next 20 are
	 * 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
	 * 
	 * @param init
	 *            Description of the Parameter
	 * @param maxTries
	 *            Description of the Parameter
	 * @param count
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private static int incrementSleep(int init, int maxTries, int count) {
		if (count < (maxTries * 0.2)) {
			return init;
		}
		if (count < (maxTries * 0.4)) {
			return init * 2;
		}
		if (count < (maxTries * 0.6)) {
			return init * 4;
		}
		if (count < (maxTries * 0.8)) {
			return init * 8;
		}
		return init * 16;
	}

	public static boolean isVisualizer(AnalysisService service) {
		return "visualizer".equalsIgnoreCase((String) service.getTaskInfo()
				.getTaskInfoAttributes().get(GPConstants.TASK_TYPE));
	}
}