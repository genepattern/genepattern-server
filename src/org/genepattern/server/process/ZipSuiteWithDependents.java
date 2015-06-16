/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.process;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;

public class ZipSuiteWithDependents extends ZipSuite {

    /* (non-Javadoc)
     * @see org.genepattern.server.process.ZipSuite#packageSuite(org.genepattern.webservice.SuiteInfo, java.lang.String)
     */
    public File packageSuite(SuiteInfo suiteInfo, String userID) throws Exception {
        String name = suiteInfo.getName();

        // create zip file
        File zipFile = File.createTempFile(name, ".zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        try {
            // find dependent tasks (if a pipeline) and add them to the zip
            // file as zip files
            zipDependentTasks(zos, suiteInfo, userID);
            zos.finish();
            zos.close();
            return zipFile;
        }
        catch (Exception e) {
            zos.close();
            zipFile.delete();
            throw e;
        }
    }

    /**
     * @param zos
     * @param suiteInfo
     * @param userID
     * @throws Exception
     */
    private void zipDependentTasks(ZipOutputStream zos, SuiteInfo suiteInfo,
            String userID) throws Exception {
        File parentTempDir = ServerConfigurationFactory.instance().getTempDir(GpContext.getServerContext());
        File tmpDir = new File(parentTempDir, suiteInfo.getName());
        try {
            tmpDir.mkdir();
            File parent = super.packageSuite(suiteInfo, userID);
            zipFile(zos, parent);
            parent.delete();

            String[] lsids = suiteInfo.getModuleLsids();
            ZipTask zt = new ZipTaskWithDependents();
            Map<Integer, MissingTaskError> errors = new HashMap<Integer, MissingTaskError>();
            int taskNum = 0;
            for (String lsid : lsids) {
                if (lsid == null || lsid.length() == 0) {
                    throw new Exception("Must specify task name as name argument to this page");
                }

                TaskInfo taskInfo = null;
                try {
                    taskInfo = GenePatternAnalysisTask.getTaskInfo(lsid, userID);
                }
                catch (OmnigeneException e) {
                    //this is a new task, no taskID exists
                    // do nothing
                    throw new Exception("no such task: " + lsid);
                }

                if (taskInfo == null) {
                    MissingTaskError error = new MissingTaskError(suiteInfo.getLsid(), lsid, lsid);
                    errors.put(new Integer(taskNum), error);
                }
                else {
                    File packagedTasks;
                    try {
                        packagedTasks = zt.packageTask(taskInfo, userID);
                    }
                    catch (MissingTaskException e) {
                        throw e;
                    }

                    zipFile(zos, packagedTasks);
                    packagedTasks.delete();
                }
                taskNum++;
            }
            if (errors.size() > 0)
                throw new MissingTaskException(errors);
        }
        catch (MissingTaskException e) {
            throw e;
        }
        finally {
            tmpDir.delete();
        }
    }
}
