/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2012) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/

package org.genepattern.server.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.util.KeySortedProperties;

public class ZipTask extends CommandLineAction {

    public ZipTask() {
    }

    public Properties getManifestProps(String name, TaskInfo taskInfo) throws Exception {
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        if (tia == null) {
            throw new Exception(
                    "Sorry, "
                    + name
                    + " is not a GenePattern analysis task and cannot be packaged.");
        }
        ParameterInfo[] parameterInfo = taskInfo.getParameterInfoArray();
        Properties props = new Properties();
        int i;
        String key;
        String value;

        props.setProperty(GPConstants.NAME, taskInfo.getName());
        if (taskInfo.getDescription() != null) {
            props.setProperty(GPConstants.DESCRIPTION, taskInfo.getDescription());
        }

        if (parameterInfo != null) {
            int i2;
            for (i = 0; i < parameterInfo.length; i++) {
                i2 = i + 1;
                props.setProperty("p" + i2 + "_name", parameterInfo[i].getName());
                if (parameterInfo[i].getDescription() != null)
                    props.setProperty("p" + i2 + "_description", parameterInfo[i].getDescription());
                if (parameterInfo[i].getValue() != null) {
                    props.setProperty("p" + i2 + "_value", parameterInfo[i].getValue());
                }
                HashMap attributes = parameterInfo[i].getAttributes();
                if (attributes != null) {
                    for (Iterator eAttributes = attributes.keySet().iterator(); eAttributes.hasNext();) {
                        key = (String) eAttributes.next();
                        value = (String) attributes.get(key);
                        if (value == null) {
                            value = "";
                        }
                        props.setProperty("p" + i2 + "_" + key, value);
                    }
                }
            }
        }
        for (i = 0; i < GPConstants.TASK_INFO_ATTRIBUTES.length; i++) {
            key = GPConstants.TASK_INFO_ATTRIBUTES[i];
            value = tia.get(key);
            if (value == null) {
                value = "";
            }
            props.setProperty(key, value);
        }
        return props;
    }

    public void zipTaskFiles(ZipOutputStream zos, File dir) throws Exception {
        File[] fileList = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.endsWith(".old") && !name.endsWith(".bak") && !name.equalsIgnoreCase("manifest");
            }
        });

        for (int i = 0; i < fileList.length; i++) {
            zipTaskFile(zos, fileList[i]);
        }
    }

    public void zipTaskFile(ZipOutputStream zos, File f) throws Exception {
        zipTaskFile(zos, f, null);
    }

    public void zipTaskFile(ZipOutputStream zos, File f, String comment) throws Exception {
        try {
            if (f.isDirectory()) {
                return;
            }

            ZipEntry zipEntry = null;
            FileInputStream is = null;
            String value = null;
            String attachmentName = null;

            byte[] buf = new byte[100000];
            zipEntry = new ZipEntry(f.getName());
            zipEntry.setTime(f.lastModified());
            zipEntry.setSize(f.length());
            if (comment != null) {
                zipEntry.setComment(comment);
            }
            zos.putNextEntry(zipEntry);
            long fileLength = f.length();
            long numRead = 0;
            is = new FileInputStream(f);
            int n;
            while ((n = is.read(buf, 0, buf.length)) > 0) {
                zos.write(buf, 0, n);
                numRead += n;
            }
            is.close();
            if (numRead != fileLength) {
                throw new Exception("only read " + numRead + " of "
                        + fileLength + " bytes in " + f.getPath());
            }
            zos.closeEntry();
        }
        catch (Exception t) {
            t.printStackTrace();
            throw t;
        }
    }

    public ZipEntry zipPropertiesAsManifest(ZipOutputStream zos, Properties props, String name) throws Exception {
        // insert manifest
        //	System.out.println("creating manifest");
        ZipEntry zipEntry = new ZipEntry(GPConstants.MANIFEST_FILENAME);
        zos.putNextEntry(zipEntry);
        KeySortedProperties sProps = new KeySortedProperties();
        sProps.putAll(props);
        ByteArrayOutputStream manifestData = new ByteArrayOutputStream(1000);
        sProps.store(manifestData, name); // write properties to stream

        zos.write(manifestData.toByteArray());
        zos.closeEntry();
        return zipEntry;
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
        return packageTask(taskInfo, userID);
    }

    public File packageTask(TaskInfo taskInfo, String userID) throws Exception {
        String name = taskInfo.getName();
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        // use an LSID-unique name so that different versions of same named task
        // don't collide within zip file
        String suffix = "_"
                + Integer.toString(Math.abs(tia.get(GPConstants.LSID)
                        .hashCode()), 36); // [a-z,0-9]

        // create zip file
        File zipFile = File.createTempFile(name + suffix, ".zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));

        // add the manifest
        Properties manifestProps = getManifestProps(name, taskInfo);
        zipPropertiesAsManifest(zos, manifestProps, name);

        // insert attachments
        // find $OMNIGENE_ANALYSIS_ENGINE/taskLib/<taskName> to locate DLLs,
        // other support files
        File dir = new File(DirectoryManager.getTaskLibDir(name, (String) taskInfo.getTaskInfoAttributes().get(GPConstants.LSID), userID));
        zipTaskFiles(zos, dir);
        zos.finish();
        zos.close();
        return zipFile;
    }

    public static Vector zipAllTasks(String userID, String toDirName) throws Exception {
        File toDir = new File(toDirName);
        if (toDir.exists()) {
            if (!(toDir.isDirectory()))
                throw new Exception(toDirName + " exists but is not a directory");
        }
        else {
            toDir.mkdir();
        }

        ZipTask inst = new ZipTask();
        Vector allTaskFiles = new Vector();

        Collection tmTasks = GenePatternAnalysisTask.getTasks(userID);
        for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext();) {
            TaskInfo ti = (TaskInfo) itTasks.next();
            File zip = inst.packageTask(ti, userID);
            File zipFile = new File(toDir, ti.getName() + ".zip");
            zip.renameTo(zipFile);
            allTaskFiles.add(zip);
        }

        return allTaskFiles;
    }

    public static Vector zipNonBroadTasks(String userID, String toDirName) throws Exception {
        File toDir = new File(toDirName);
        if (toDir.exists()) {
            if (!(toDir.isDirectory()))
                throw new Exception(toDirName + " exists but is not a directory");
        }
        else {
            toDir.mkdir();
        }

        ZipTask inst = new ZipTask();
        Vector allTaskFiles = new Vector();

        Collection tmTasks = GenePatternAnalysisTask.getTasks(userID);
        for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext();) {
            TaskInfo ti = (TaskInfo) itTasks.next();
            Map tia = ti.getTaskInfoAttributes();
            String lsid = (String) tia.get("LSID");

            boolean isNonBroadTask = true;
            if (lsid != null) {
                if (lsid.startsWith("urn:lsid:broadinstitute.org") ||
                        lsid.startsWith("urn:lsid:broad.mit.edu")) {
                    isNonBroadTask = false;
                }
            }

            if (isNonBroadTask) {
                System.out.println("\tSaving: " + ti.getName());

                File zip = inst.packageTask(ti, userID);
                File zipFile = new File(toDir, ti.getName() + ".zip");
                zip.renameTo(zipFile);
                allTaskFiles.add(zip);
            }
            else {
                System.out.println("Skipping: " + ti.getName());
            }
        }

        return allTaskFiles;
    }

    public static void main(String[] args) {
        ZipTask zipTasks = new ZipTask();
        zipTasks.run(args);
        return;
    }

    public void run(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: ZipTask [dir where zips are to go]");
            return;
        }
        DEBUG = (System.getProperty("DEBUG") != null);
        try {
            String dirName = args[0];
            preRun(args);

            System.out.println("Zipping files to: " + dirName + "  hts=" + hadToStartDB);

            zipNonBroadTasks(null, dirName);

            postRun(args);

            System.exit(0);
        }
        catch (Throwable e) {
            System.err.println(e.getMessage() + " in ZipTask");
            e.printStackTrace();
            System.exit(1);
        }
    }

}
