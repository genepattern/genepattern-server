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

package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.CLThread;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.maindisplay.CenteredDialog;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Runs tasks for the GPGE
 * 
 * @author Joshua Gould
 */
public class TaskLauncher {

    private TaskLauncher() {
    }

    public static void submitVisualizer(final AnalysisService svc,
            ParameterInfo[] paramInfos, String username, String password,
            AnalysisWebServiceProxy proxy) {
        submitVisualizer(svc, paramInfos, username, password, proxy, true);
    }

    /**
     * @param svc
     *            the analysis service to run
     * @param paramInfos
     *            Description of the Parameter
     * @param username
     *            Description of the Parameter
     */
    public static void submitVisualizer(final AnalysisService svc,
            ParameterInfo[] paramInfos, String username, String password,
            AnalysisWebServiceProxy proxy, boolean addToHistory) {
        try {
            Map substitutions = new HashMap();
            substitutions
                    .putAll(org.genepattern.gpge.ui.tasks.JavaGELocalTaskExecutor
                            .loadGPProperties());
            for (int i = 0, length = paramInfos.length; i < length; i++) {
                Map attributes = paramInfos[i].getAttributes();
                if (attributes != null
                        && ParameterInfo.CACHED_INPUT_MODE.equals(attributes
                                .get(ParameterInfo.MODE))) {// server file
                    String value = paramInfos[i].getValue();
                    int index1 = value.lastIndexOf('/');
                    int index2 = value.lastIndexOf('\\');
                    int index = (index1 > index2 ? index1 : index2);
                    if (index == -1) {
                        GenePattern
                                .showErrorDialog("An error occurred while running "
                                        + svc.getTaskInfo().getName());
                        return;
                    }

                    String jobNumber = value.substring(0, index);
                    String fileName = value
                            .substring(index + 1, value.length());
                    String downloadURL = svc.getServer()
                            + "/gp/retrieveResults.jsp?job=" + jobNumber
                            + "&filename="
                            + java.net.URLEncoder.encode(fileName, "UTF-8");
                    substitutions.put(paramInfos[i].getName(), downloadURL);
                } else {
                    substitutions.put(paramInfos[i].getName(), paramInfos[i]
                            .getValue());
                }
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final JDialog d = new CenteredDialog(GenePattern
                            .getDialogParent());
                    d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    JLabel l = new JLabel("Launching "
                            + svc.getTaskInfo().getName() + "...",
                            JLabel.CENTER);

                    d.getContentPane().add(l);
                    d.setSize(250, 80);

                    d.setVisible(true);
                    Timer t = new Timer(1000, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            d.setVisible(false);
                        }
                    });
                    t.setRepeats(false);
                    t.start();
                }
            });

            new org.genepattern.gpge.ui.tasks.JavaGELocalTaskExecutor(svc
                    .getTaskInfo(), substitutions, username, password, svc.getServer())
                    .exec();

            if (addToHistory) {
                JobInfo jobInfo = proxy.recordClientJob(svc.getTaskInfo()
                        .getID(), paramInfos);
                AnalysisJob job = new AnalysisJob(svc.getServer(), jobInfo,
                        true);
                job.getJobInfo().setDateCompleted(
                        job.getJobInfo().getDateSubmitted());
                job.getJobInfo().setStatus(JobStatus.FINISHED);
                JobModel.getInstance().add(job);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            GenePattern.showErrorDialog("An error occurred while running "
                    + svc.getTaskInfo().getName());
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
        new CLThread() {
            public void run() {
                try {
                    Map taskInfoAttributes = svc.getTaskInfo()
                            .getTaskInfoAttributes();
                    String xml = (String) taskInfoAttributes
                            .get(GPConstants.SERIALIZED_MODEL);
                    Map visualizerTaskNumber2LSID = new HashMap();
                    if (xml != null && !xml.equals("")) {
                        try {
                            PipelineModel model = PipelineModel
                                    .toPipelineModel(xml);
                            List tasks = model.getTasks();

                            for (int i = 0; i < tasks.size(); i++) {
                                JobSubmission js = (JobSubmission) tasks.get(i);
                                if (js.isVisualizer()) {
                                    visualizerTaskNumber2LSID.put(
                                            new Integer(i), js.getLSID());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                    submitAndWaitUntilCompletion(paramInfos, serviceProxy, svc,
                            visualizerTaskNumber2LSID);
                } catch (WebServiceException wse) {
                    if (!GenePattern.disconnectedFromServer(wse, svc
                            .getServer())) {
                        GenePattern
                                .showErrorDialog("An error occurred while running "
                                        + svc.getTaskInfo().getName());
                    }
                }
            }
        }.start();
    }

    /**
     * Waits for the given job to complete in a new thread. Used when user
     * refreshes jobs from the server and a job is in progress
     */
    public static void waitUntilCompletionInNewThread(final AnalysisJob job) {
        new CLThread() {
            public void run() {
                try {
                    waitUntilCompletion(job, null, null);
                } catch (WebServiceException wse) {
                    // if(!GenePattern.disconnectedFromServer(wse,
                    // svc.getServer())) {
                    GenePattern
                            .showErrorDialog("An error occurred while running "
                                    + job.getTaskName());
                    // }
                }
            }
        }.start();
    }

    private static AnalysisJob submitAndWaitUntilCompletion(
            ParameterInfo[] paramInfos,
            final AnalysisWebServiceProxy serviceProxy,
            final AnalysisService svc, Map visualizerTaskNumber2LSID)
            throws WebServiceException {

        TaskInfo tinfo = svc.getTaskInfo();
        final JobInfo jobInfo = serviceProxy.submitJob(tinfo.getID(),
                paramInfos);
        final AnalysisJob job = new AnalysisJob(svc.getServer(), jobInfo);
        JobModel.getInstance().add(job);
        return waitUntilCompletion(job, serviceProxy, visualizerTaskNumber2LSID);
    }

    private static void runVisualizerInPipeline(
            AnalysisWebServiceProxy serviceProxy, AnalysisJob job,
            Map visualizerTaskNumber2LSID) {
        if (visualizerTaskNumber2LSID != null
                && visualizerTaskNumber2LSID.size() > 0) {
            try {
                int[] children = serviceProxy.getChildren(job.getJobInfo()
                        .getJobNumber());

                for (int i = 0; i < children.length; i++) {
                    if (visualizerTaskNumber2LSID.containsKey(new Integer(i))) {
                        String lsid = (String) visualizerTaskNumber2LSID
                                .remove(new Integer(i));

                        ParameterInfo[] params = serviceProxy.checkStatus(
                                children[i]).getParameterInfoArray();
                        submitVisualizer(AnalysisServiceManager.getInstance()
                                .getAnalysisService(lsid), params, job
                                .getJobInfo().getUserId(),
                                AnalysisServiceManager.getInstance()
                                        .getPassword(), serviceProxy, false);
                    }
                }
            } catch (Exception e) { // don't break out of loop if running
                // visualizer fails
                e.printStackTrace();
            }

        }
    }

    private static AnalysisJob waitUntilCompletion(AnalysisJob job,
            AnalysisWebServiceProxy serviceProxy, Map visualizerTaskNumber2LSID)
            throws WebServiceException {

        String status = "";

        if (serviceProxy == null) {
            serviceProxy = new AnalysisWebServiceProxy(job.getServer(), job
                    .getJobInfo().getUserId(), AnalysisServiceManager
                    .getInstance().getPassword());
        }
        int initialSleep = 1000;
        int sleep = initialSleep;
        int tries = 0;
        int maxTries = 20;

        while (!(status.equalsIgnoreCase("ERROR") || (status
                .equalsIgnoreCase("Finished")))) {
            tries++;
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ie) {
            }

            JobInfo info = serviceProxy.checkStatus(job.getJobInfo()
                    .getJobNumber());
            job.setJobInfo(info);
            String currentStatus = info.getStatus();
            if (!(status.equals(currentStatus))) {
                JobModel.getInstance().jobStatusChanged(job);
            }
            runVisualizerInPipeline(serviceProxy, job,
                    visualizerTaskNumber2LSID);
            JobModel.getInstance().addChildOutputFiles(job);
            status = currentStatus;
            sleep = incrementSleep(initialSleep, tries, maxTries);
        }
        runVisualizerInPipeline(serviceProxy, job, visualizerTaskNumber2LSID);
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

    public static boolean isPipeline(AnalysisService service) {
        return "pipeline".equalsIgnoreCase((String) service.getTaskInfo()
                .getTaskInfoAttributes().get(GPConstants.TASK_TYPE));
    }

    public static boolean isVisualizer(AnalysisService service) {
        return "visualizer".equalsIgnoreCase((String) service.getTaskInfo()
                .getTaskInfoAttributes().get(GPConstants.TASK_TYPE));
    }

    public static boolean isVisualizer(TaskInfo taskInfo) {
        return "visualizer".equalsIgnoreCase((String) taskInfo
                .getTaskInfoAttributes().get(GPConstants.TASK_TYPE));
    }
}