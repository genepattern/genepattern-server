/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.process;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipOutputStream;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public class ZipTaskWithDependents extends ZipTask {

    public ZipTaskWithDependents() {
    }

    public File packageTask(String name, String userID) throws Exception {
        if (name == null || name.length() == 0) {
            throw new Exception("Must specify task name as name argument to this page");
        }

        TaskInfo taskInfo = null;
        try {
            taskInfo = GenePatternAnalysisTask.getTaskInfo(name, userID);
        } 
        catch (OmnigeneException e) {
            //this is a new task, no taskID exists do nothing
            throw new Exception("no such task: " + name);
        }
        File packagedTasks;
        try {
            packagedTasks = packageTask(taskInfo, userID);
        } 
        catch (MissingTaskException e) {
            throw e;
        }
        return packagedTasks;
    }

    public File packageTask(final TaskInfo taskInfo, final String userID) throws Exception {
        return packageTask(new Vector(), taskInfo, userID);
    }
    
    public File packageTask(final Vector vIncludedLSIDs, final TaskInfo taskInfo, final String userID) throws Exception {
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
        if (serializedModel != null && serializedModel.trim().length() > 0) {
            String name = taskInfo.getName();
            // use an LSID-unique name so that different versions of same named
            // task don't collide within zip file
            String suffix = "_"
                    + Integer.toString(Math.abs(tia.get(GPConstants.LSID)
                            .hashCode()), 36); // [a-z,0-9]
            // create zip file
            File zipFile = File.createTempFile(name + suffix, ".zip");
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
            try {
                // find dependent tasks (if a pipeline) and add them to the zip
                // file as zip files
                zipDependentTasks(vIncludedLSIDs, zos, taskInfo, userID);
                zos.finish();
                zos.close();
                return zipFile;
            } 
            catch (MissingTaskException e) {
                zos.close();
                zipFile.delete();
                throw e;
            }
        } 
        else {
            return super.packageTask(taskInfo, userID);
        }
    }

    public void zipDependentTasks(Vector vIncludedLSIDs, ZipOutputStream zos, TaskInfo taskInfo, String userID) throws Exception {
        File parentTempDir = ServerConfigurationFactory.instance().getTempDir(GpContext.getServerContext());
        File tmpDir = new File(parentTempDir, taskInfo.getName() + "_dep_" + System.currentTimeMillis());
        try {
            tmpDir.mkdir();
            File parent = super.packageTask(taskInfo, userID);
            zipTaskFile(zos, parent);
            parent.delete();

            String serializedModel = (String) taskInfo.getTaskInfoAttributes().get(GPConstants.SERIALIZED_MODEL);
            if (serializedModel != null && serializedModel.trim().length() > 0) {
                PipelineModel model = null;
                try {
                    model = PipelineModel.toPipelineModel(serializedModel);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                Vector vTasks = model.getTasks();
                int taskNum = 0;
                //String message = "";

                Map<Integer, MissingTaskError> errors = new HashMap<Integer, MissingTaskError>();
                MissingTaskError error;
                // validate availability of all dependent tasks
                for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
                    JobSubmission jobSubmission = (JobSubmission) eTasks.nextElement();
                    String taskLsid = jobSubmission.getLSID();

                    TaskInfo depti = GenePatternAnalysisTask.getTaskInfo(taskLsid, userID);
                    if (depti == null) {
                        /*message = message + taskInfo.getName()
                        		+ " refers to task # " + (taskNum + 1) + " "
                        		+ jobSubmission.getName();*/
                        depti = GenePatternAnalysisTask.getTaskInfo(jobSubmission.getName(), userID);
                        error = new MissingTaskError(taskInfo.getName(), jobSubmission.getName(), jobSubmission.getLSID());

                        if (depti != null) {
                            LSID available = new LSID(depti.giveTaskInfoAttributes().get(GPConstants.LSID));
                            LSID requested = new LSID(taskLsid);
                            error.setAvailableVersion(available.getVersion());
                            if (available.isSimilar(requested)) {
                                /*message = message + " version "
                                		+ requested.getVersion()
                                		+ " but version "
                                		+ available.getVersion()
                                		+ " is available.\n";*/
                                error.setErrorType(MissingTaskError.errorTypes[1]);
                            }
                            else {
                                //message = message + " (" + taskLsid + ").\n";
                                error.setErrorType(MissingTaskError.errorTypes[2]);
                            }
                        }
                        else {
                            /*message = message + " which does not exist " + " ("
                            		+ taskLsid + ").\n";*/
                            error.setErrorType(MissingTaskError.errorTypes[0]);
                        }
                        errors.put(new Integer(taskNum), error);
                    }
                }
                //if (message.length() > 0)
                if (errors.size() > 0) {
                    throw new MissingTaskException(errors);
                }
                // done validating, now actually do the zipping
                taskNum = 0;
                for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
                    JobSubmission jobSubmission = (JobSubmission) eTasks.nextElement();
                    String taskLsid = jobSubmission.getLSID();
                    // don't include the same LSID more than once
                    if (vIncludedLSIDs.contains(taskLsid)) {
                        continue;
                    }
                    vIncludedLSIDs.add(taskLsid);
                    TaskInfo depti = GenePatternAnalysisTask.getTaskInfo(taskLsid, userID);
                    File dependent = packageTask(vIncludedLSIDs, depti, userID);
                    dependent.deleteOnExit();
                    String comment = taskLsid;
                    zipTaskFile(zos, dependent, comment);
                    dependent.delete();
                }
            }
        }
        finally {
            tmpDir.delete();
        }
    }
}
