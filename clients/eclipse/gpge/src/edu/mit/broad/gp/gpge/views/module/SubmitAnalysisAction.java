/*
 * Created on Jun 17, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.module;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import edu.mit.broad.gp.core.EclipseLocalTaskExecutor;
import edu.mit.broad.gp.core.ServiceManager;
import edu.mit.broad.gp.gpge.JobSubmissionRegistry;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import edu.mit.genome.gp.ui.analysis.AnalysisJob;
import edu.mit.genome.gp.ui.analysis.AnalysisService;
import edu.mit.genome.gp.ui.analysis.RequestHandler;
import edu.mit.wi.omnigene.framework.analysis.JobInfo;
import edu.mit.wi.omnigene.framework.analysis.ParameterInfo;
import edu.mit.wi.omnigene.framework.analysis.TaskInfo;
import edu.mit.wi.omnigene.service.analysis.genepattern.GenePatternAnalysisTask;
import edu.mit.genome.util.GPConstants;

/**
 * @author genepattern
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class SubmitAnalysisAction {
    String name;

    ServiceManager serviceManager;

    HashMap controlMap;

    ParameterInfo[] formalParams;

    Shell shell;

    /**
     *  
     */
    public SubmitAnalysisAction(String name, HashMap controls,
            ServiceManager mgr, ParameterInfo[] formalParams, Shell shell) {
        this.name = name;
        this.serviceManager = mgr;
        this.controlMap = controls;
        this.formalParams = formalParams;
        this.shell = shell;
    }

    public void submitAnalysis() {
        List paramInfoList = new ArrayList();
        final List directoryInputs = new ArrayList();
        List missingParams = new ArrayList();
        final Map paramName2ValueMap = new HashMap();

        for (int i = 0, length = formalParams != null ? formalParams.length : 0; i < length; i++) {
            String pname = formalParams[i].getName();
            Control control = (Control) controlMap.get(pname);
            String actualValue = null;
            if (control.getClass() == Text.class) {
                actualValue = ((Text) control).getText();
            } else {
                Combo cmb = (Combo) control;
                int idx = cmb.getSelectionIndex();
                if (idx != -1) {
                    actualValue = cmb.getItem(idx);
                }
            }
            if (actualValue == null || actualValue.trim().equals("")) {
                java.util.HashMap attrs = formalParams[i].getAttributes();
                String sOptional = (String) attrs
                        .get(GPConstants.PARAM_INFO_OPTIONAL[0]);
                boolean optional = (sOptional != null && sOptional.length() > 0);
                if (!optional) {
                    missingParams.add(ModuleForm
                            .getParameterText(formalParams[i]));
                }
                continue;
            }

            ParameterInfo actualParam = new ParameterInfo();
            actualParam.setName(pname);
            paramInfoList.add(actualParam);

            if (control.getClass() == Text.class) {
                String fileOrUrl = actualValue;
                URL url = null;
                try {
                    url = new URL(fileOrUrl);
                } catch (MalformedURLException mfe) {
                }
                if (url != null && "file".equals(url.getProtocol())) {
                    java.io.File f = new java.io.File(url.getFile());
                    try {
                        fileOrUrl = f.getCanonicalPath();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                actualParam.setValue(fileOrUrl);
                java.io.File file = new java.io.File(fileOrUrl);

                if (file.exists()) {
                    actualParam.setAsInputFile();
                    if (file.isDirectory()) {
                        directoryInputs.add(actualParam);
                    }
                }
            } else {
                actualParam.setValue(actualValue);
            }
            paramName2ValueMap.put(pname, actualParam.getValue());
        }

        if (missingParams.size() > 0) {
            String message = "Missing required fields: ";
            for (int j = 0, size = missingParams.size(); j < size; j++) {
                message += "\n" + missingParams.get(j);
            }
            MessageDialog.openError(shell, "Error", message);
            return;
        }
        if (directoryInputs.size() > 1) {
            String message = "Only one input field can be a folder. The following input fields are folders: ";
            for (int j = 0, size = directoryInputs.size(); j < size; j++) {
                ParameterInfo param = (ParameterInfo) directoryInputs.get(j);
                message += "\n" + param.getName();
            }
            MessageDialog.openError(shell, "Error", message);
            return;
        }

        final RequestHandler handler = serviceManager.getRequestHandler();
        final AnalysisService svc = serviceManager.getService(name);

        final TaskInfo tinfo = svc.getTaskInfo();
        Map tia = tinfo.getTaskInfoAttributes();
        final String serializedModel = (String) tia
                .get(GPConstants.SERIALIZED_MODEL);

        if ((serializedModel != null) && (serializedModel.trim().length() > 0)) { // pipeline
            new Thread() {
                public void run() {

                    try {
                        PipelineModel model = null;
                        try {
                            model = PipelineModel
                                    .toPipelineModel(serializedModel);
                        } catch (Throwable x) {
                            x.printStackTrace();
                        }

                        final Vector tasks = model.getTasks();

                        AnalysisJob[] results = new AnalysisJob[tasks.size()];
                        int _directoryTaskIndex = -1; // index of task that
                                                      // has a directory as
                                                      // input
                        int _directoryParamIndex = -1;
                        boolean taskIndexFound = false;
                        File[] _files = null;
                        if (directoryInputs.size() > 0) {
                            ParameterInfo directoryParam = (ParameterInfo) directoryInputs
                                    .get(0);
                            for (int j = 0, size = tasks.size(); j < size
                                    && !taskIndexFound; j++) {
                                JobSubmission js = (JobSubmission) tasks.get(j);

                                List formalParams = js.getParameters();
                                for (int p = 0, numParams = formalParams.size(); p < numParams
                                        && !taskIndexFound; p++) {
                                    ParameterInfo formalParam = (ParameterInfo) formalParams
                                            .get(p);
                                    String name = getPipelineParameterName(js
                                            .getName(), j, formalParam);
                                    if (name.equals(directoryParam.getName())) {
                                        _directoryTaskIndex = j;
                                        _directoryParamIndex = p;
                                        taskIndexFound = true;

                                    }
                                }

                            }
                            File dir = new File(directoryParam.getValue());
                            _files = dir
                                    .listFiles(new java.io.FileFilter() {
                                        public boolean accept(File f) {
                                            return !f.isDirectory()
                                                    && !f.getName().startsWith(".")
                                                    && !f.getName().endsWith("~");
                                        }
                                    });
                        }

                        final int directoryTaskIndex = _directoryTaskIndex==-1?tasks.size():_directoryTaskIndex; // index of task that has a directory as input
                        final int directoryParamIndex = _directoryParamIndex;
                        final File[] files = _files;
                       

                        for (int i = 0; i < directoryTaskIndex; i++) {
                            String taskName = ((JobSubmission) tasks.get(i))
                                    .getName();
                            AnalysisService svc2 = serviceManager
                                    .getService(taskName);
                            TaskInfo tinfo2 = svc2.getTaskInfo();
                            ParameterInfo[] actualParams = getPipelineParameters(
                                    tasks, i, paramName2ValueMap, results,
                                    taskName);
                            try {
                                if (isVisualizer(svc2)) {
                                    submitVisualizer(tinfo2, actualParams, svc2
                                            .getURL());
                                } else {
                                    results[i] = submitAndWaitUntilCompletion(
                                            actualParams, handler, tinfo2, svc2);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        for (int _fileNum = 0, numFiles = files != null ? files.length
                                : 0; _fileNum < numFiles; _fileNum++) { // run all tasks repeatedly that come after the task that takes a folder as input
                            final AnalysisJob[] pipelineResults = (AnalysisJob[]) results
                                    .clone();
                            final int fileNum = _fileNum;
                            new Thread() {
                                public void run() {
                                    for (int j = directoryTaskIndex; j < tasks
                                            .size(); j++) {
                                        String taskName = ((JobSubmission) tasks
                                                .get(j)).getName();
                                        AnalysisService svc2 = serviceManager
                                                .getService(taskName);
                                        TaskInfo tinfo2 = svc2.getTaskInfo();

                                        ParameterInfo[] actualParams = getPipelineParameters(
                                                tasks, j, paramName2ValueMap,
                                                pipelineResults, taskName);
                                        if (j == directoryTaskIndex) {
                                            actualParams[directoryParamIndex]
                                                    .setAsInputFile();
                                            actualParams[directoryParamIndex]
                                                    .setValue(files[fileNum]
                                                            .getPath());
                                        }
                                        try {
                                            if (isVisualizer(svc2)) {
                                                submitVisualizer(tinfo2,
                                                        actualParams, svc2
                                                                .getURL());
                                            } else {
                                                pipelineResults[j] = submitAndWaitUntilCompletion(
                                                        actualParams, handler,
                                                        tinfo2, svc2);
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }
                            }.start();
                        }

                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                }
            }.start();
            return;
        }
        final ParameterInfo[] paramInfos = (ParameterInfo[]) paramInfoList
                .toArray(new ParameterInfo[0]);

        if (directoryInputs.size() > 0) {
            ParameterInfo directoryParam = (ParameterInfo) directoryInputs
                    .get(0);
            File dir = new File(directoryParam.getValue());
            File[] files = dir.listFiles(new java.io.FileFilter() {
                public boolean accept(File f) {
                    return !f.isDirectory() && !f.getName().startsWith(".")
                            && !f.getName().endsWith("~");
                }
            });
            for (int j = 0, length = files.length; j < length; j++) {
                try {
                    directoryParam
                            .getAttributes()
                            .put(
                                    GPConstants.PARAM_INFO_CLIENT_FILENAME[0],
                                    files[j].getCanonicalPath());
                    directoryParam.setValue(files[j].getCanonicalPath());
                    if (isVisualizer(svc)) {
                        submitVisualizer(tinfo, paramInfos, svc.getURL());
                    } else {
                        new Thread() {
                            public void run() {
                                AnalysisJob job = null;
                                try {
                                    job = submitAndWaitUntilCompletion(
                                            paramInfos, handler, tinfo, svc);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                if (isVisualizer(svc)) {
                    submitVisualizer(tinfo, paramInfos, svc.getURL());
                } else {
                    new Thread() {
                        public void run() {

                            AnalysisJob job = null;
                            try {
                                job = submitAndWaitUntilCompletion(paramInfos,
                                        handler, tinfo, svc);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 
     * @param taskName
     *            The task name.
     * @param taskNumber
     *            The index of the task in the pipeline, starting from 0
     * @param p
     *            The parameter
     * @return The pipeline parameter name
     */
    String getPipelineParameterName(String taskName, int taskNumber,
            ParameterInfo p) {
        return taskName + (taskNumber + 1) + "." + p.getName();
    }

    ParameterInfo[] getPipelineParameters(List tasks, int j,
            Map paramName2ValueMap, AnalysisJob[] results, String taskName) {
        JobSubmission js = (JobSubmission) tasks.get(j);
        List formalParams = js.getParameters();

        ParameterInfo[] actualParams = new ParameterInfo[formalParams.size()];

        for (int k = 0, length = formalParams.size(); k < length; k++) {
            actualParams[k] = new ParameterInfo();
            ParameterInfo formalParam = (ParameterInfo) formalParams.get(k);
            actualParams[k].setValue(formalParam.getValue());
            actualParams[k].setName(formalParam.getName());

            if (formalParam.isInputFile()) {
                actualParams[k].setAsInputFile();
            }
            Map formalAttributes = formalParam.getAttributes();
            String key = getPipelineParameterName(taskName, j, formalParam);
            String runtimePromptValue = (String) paramName2ValueMap.get(key);
            if (runtimePromptValue != null) {

                String fileOrUrl = runtimePromptValue;
                URL url = null;
                try {
                    url = new URL(fileOrUrl);

                } catch (MalformedURLException mfe) {
                }
                if (url != null && "file".equals(url.getProtocol())) {
                    java.io.File f = new java.io.File(url.getFile());
                    try {
                        fileOrUrl = f.getCanonicalPath();

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                actualParams[k].setValue(fileOrUrl);
                java.io.File file = new java.io.File(fileOrUrl);

                if (file.exists()) {
                    actualParams[k].setAsInputFile();
                }

            }
            if (formalAttributes != null) {
                String taskNumber = (String) formalAttributes
                        .get(PipelineModel.INHERIT_TASKNAME);
                if (taskNumber != null) {
                    String outputFileNumber = (String) formalAttributes
                            .get(PipelineModel.INHERIT_FILENAME);
                    int taskNumberInt = Integer.parseInt(taskNumber.trim());
                    String url = getOutputFileURL(results[taskNumberInt],
                            outputFileNumber);
                    actualParams[k].setValue(url);
                    actualParams[k].getAttributes().remove("MODE");
                    actualParams[k].getAttributes().remove("TYPE");
                }
            }

        }
        return actualParams;

    }

    String getOutputFileURL(AnalysisJob job, String fileNumber) {
        String fileName = fileNumber;
        try {
            int index = Integer.parseInt(fileNumber) - 1;
            // find the ith output file
            ParameterInfo[] params = job.getJobInfo().getParameterInfoArray();
            int count = 0;
            for (int j = 0, length = params.length; j < length; j++) {
                if (params[j].isOutputFile()) {
                    if (index == count) {
                        fileName = params[j].getValue();
                        int slashIndex = fileName.lastIndexOf('/');
                        if (slashIndex == -1) {
                            slashIndex = fileName.lastIndexOf('\\');
                        }
                        if (slashIndex != -1) {
                            fileName = fileName.substring(slashIndex + 1,
                                    fileName.length());
                        }
                        break;

                    }
                    count++;
                }
            }

        } catch (NumberFormatException nfe) {
        }
        String server = job.getSiteName();
        return "http://" + server + "/gp/retrieveResults.jsp?job="
                + job.getJobInfo().getJobNumber() + "&filename=" + fileName;

    }

    public static boolean isVisualizer(AnalysisService service) {
        return "visualizer"
                .equalsIgnoreCase((String) service
                        .getTaskInfo()
                        .getTaskInfoAttributes()
                        .get(
                                edu.mit.wi.omnigene.service.analysis.genepattern.GPConstants.TASK_TYPE));

    }

    void submitVisualizer(TaskInfo taskInfo, ParameterInfo[] params, String url) {
        java.net.URL gpURL = null;
        try {
            gpURL = new java.net.URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        java.util.Map paramName2ValueMap = new java.util.HashMap();
        for (int i = 0, length = params.length; i < length; i++) {
            paramName2ValueMap.put(params[i].getName(), params[i].getValue());
        }
        try {
            new EclipseLocalTaskExecutor(shell, taskInfo, paramName2ValueMap,
                    gpURL).exec();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    AnalysisJob submitAndWaitUntilCompletion(ParameterInfo[] paramInfos,
            final RequestHandler handler, final TaskInfo tinfo,
            final AnalysisService svc) throws Exception {
        final JobInfo jobInfo = handler.submitJob(tinfo.getID(), paramInfos);
        final AnalysisJob job = new AnalysisJob(svc.getName(), svc
                .getTaskInfo().getName(), jobInfo);
        String status = "";
        JobInfo info = null;

        int sleep = 100;
        while (!(status.equalsIgnoreCase("ERROR") || (status
                .equalsIgnoreCase("Finished")))) {

            try {
                Thread.currentThread().sleep(sleep);
            } catch (InterruptedException ie) {
            }
            try {

                info = handler.checkStatus(job.getJobInfo().getJobNumber());
                job.setJobInfo(info);
                String currentStatus = info.getStatus();
                if (!(status.equals(currentStatus))) {
                    JobSubmissionRegistry.getDefault().jobStatusChange(info);
                }
                status = currentStatus;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        JobSubmissionRegistry.getDefault().jobCompleted(job);

        return job;

    }

}