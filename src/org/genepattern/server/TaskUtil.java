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

package org.genepattern.server;

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.genepattern.util.IGPConstants;

import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Utility class for GenePattern task zip files.
 * 
 * @author Joshua Gould
 */
public class TaskUtil {

    private TaskUtil() {
    }

    /**
     * Returns <code>true</code> if the given file is a zip of zips,
     * <code>false</code> otherwise.
     * 
     * @param file
     *            A zip file.
     * @return whether the given file is a zip of zips.
     * @exception IOException
     *                if an error occurs during reading
     */
    public static boolean isZipOfZips(File file) throws IOException {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            if (zipFile.getEntry(GPConstants.MANIFEST_FILENAME) != null) {
                return false;
            }

            // is it a zip of zips?
            for (Enumeration eEntries = zipFile.entries(); eEntries
                    .hasMoreElements();) {
                ZipEntry zipEntry = (ZipEntry) eEntries.nextElement();
                if (!zipEntry.getName().toLowerCase().endsWith(".zip")) {
                    return false;
                }
            }
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }

        // if we get here, the zip file contains only other zip files
        return true;
    }

    public static boolean isSuiteZip(File file) throws IOException {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            if (zipFile.getEntry(GPConstants.SUITE_MANIFEST_FILENAME) != null) {
                return true;
            }
            return false;
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    public static boolean isPipelineZip(File file) throws IOException {
        if (isZipOfZips(file))
            return false;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);

            if (zipFile.getEntry(GPConstants.MANIFEST_FILENAME) == null) {
                return false;
            }

            TaskInfo ti = getTaskInfoFromZip(file);
            String type = (String) ti.getTaskInfoAttributes().get(
                    IGPConstants.TASK_TYPE);

            return type.endsWith(".pipeline");
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

   

    /**
     * Creates a new <code>TaskInfo</code> instance from the given zip file.
     * Note that the returned <code>TaskInfo</code> will have getID() equal to
     * -1, getParameterInfo() will be <code>null</code>, getUserId is
     * <code>null</code>, and getAccessId is 0.
     * 
     * @param file
     *            a <code>File</code> to read from. The file must be a
     *            GenePattern task zip file. Zip of zips is not supported.
     * @return the new <code>TaskInfo</code> instance
     * @exception IOException
     *                if an error occurs during reading
     */

    public static TaskInfo getTaskInfoFromZip(File file) throws IOException {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            ZipEntry manifestEntry = zipFile
                    .getEntry(org.genepattern.util.IGPConstants.MANIFEST_FILENAME);
            if (manifestEntry == null) {
                zipFile.close();
                throw new IOException(file.getName()
                        + " is missing a GenePattern manifest file.");
            }
            return getTaskInfoFromManifest(zipFile
                    .getInputStream(manifestEntry));
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    /**
     * Creates a new <code>TaskInfo</code> instance from the file. Note that
     * the returned <code>TaskInfo</code> will have getID() equal to -1,
     * getParameterInfo() will be <code>null</code>, getUserId is
     * <code>null</code>, and getAccessId is 0.
     * 
     * @param manifestFile
     *            a <code>File</code> to read from
     * @return the new <code>TaskInfo</code> instance
     * @exception IOException
     *                if an error occurs during reading
     */
    public static TaskInfo getTaskInfoFromManifest(File manifestFile)
            throws IOException {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(manifestFile);
            return getTaskInfoFromManifest(fis);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }

    }

    /**
     * Creates a new <code>TaskInfo</code> instance from the input stream.
     * Note that the returned <code>TaskInfo</code> will have getID() equal to
     * -1, getParameterInfo() will be <code>null</code>, getUserId is
     * <code>null</code>, and getAccessId is 0.
     * 
     * @param manifestURL
     *            a <code>URL</code> to read from
     * @return the new <code>TaskInfo</code> instance
     * @exception IOException
     *                if an error occurs during reading
     */
    public static TaskInfo getTaskInfoFromManifest(java.net.URL manifestURL)
            throws IOException {
        InputStream is = null;
        try {
            is = manifestURL.openStream();
            return getTaskInfoFromManifest(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Creates a new <code>TaskInfo</code> instance from the input stream.
     * Note that the returned <code>TaskInfo</code> will have getID() equal to
     * -1, getParameterInfo() will be <code>null</code>, getUserId is
     * <code>null</code>, and getAccessId is 0.
     * 
     * @param is
     *            input stream to a manifest file
     * @return the new <code>TaskInfo</code> instance
     * @exception IOException
     *                if an error occurs during reading
     */
    public static TaskInfo getTaskInfoFromManifest(java.io.InputStream is)
            throws IOException {
        Properties props = new Properties();
        props.load(is);

        String taskName = (String) props.remove(GenePatternAnalysisTask.NAME);
        String lsid = (String) props.get(GenePatternAnalysisTask.LSID);
        org.genepattern.util.LSID l = new org.genepattern.util.LSID(lsid);// ;
        // throw
        // MalformedURLException
        // if
        // this
        // is a
        // bad
        // LSID
        if (taskName == null || taskName.trim().equals("")) {
            throw new IOException("Missing task name");
        }
        // FIXME add check for other required attributes
        String taskDescription = (String) props
                .remove(GenePatternAnalysisTask.DESCRIPTION);

        // ParameterInfo entries consist of name/value/description triplets, of
        // which the value and description are optional
        // It is assumed that the names are p[1-n]_name, p[1-n]_value, and
        // p[1-n]_description
        // and that the numbering runs consecutively. When there is no p[m]_name
        // value, then there are m-1 ParameterInfos

        List vParams = new ArrayList();

        for (int i = 1; i <= GenePatternAnalysisTask.MAX_PARAMETERS; i++) {
            String name = (String) props.remove("p" + i + "_name");
            if (name == null) {
                continue;
            }
            if (name == null || name.length() == 0) {
                throw new IOException("missing parameter name for " + "p" + i
                        + "_name");
            }
            String value = (String) props.remove("p" + i + "_value");
            if (value == null) {
                value = "";
            }
            String description = (String) props
                    .remove("p" + i + "_description");
            if (description == null) {
                description = "";
            }
            ParameterInfo pi = new ParameterInfo(name, value, description);
            HashMap attributes = new HashMap();
            for (int attribute = 0; attribute < GenePatternAnalysisTask.PARAM_INFO_ATTRIBUTES.length; attribute++) {
                name = (String) GenePatternAnalysisTask.PARAM_INFO_ATTRIBUTES[attribute][0];
                value = (String) props.remove("p" + i + "_" + name);
                if (value != null) {
                    attributes.put(name, value);
                }
                if (name.equals(GenePatternAnalysisTask.PARAM_INFO_TYPE[0])
                        && value != null
                        && value
                                .equals(GenePatternAnalysisTask.PARAM_INFO_TYPE_INPUT_FILE)) {
                    attributes
                            .put(ParameterInfo.MODE, ParameterInfo.INPUT_MODE);
                    attributes.put(ParameterInfo.TYPE, ParameterInfo.FILE_TYPE);
                }
            }

            for (Enumeration p = props.propertyNames(); p.hasMoreElements();) {
                name = (String) p.nextElement();
                if (!name.startsWith("p" + i + "_")) {
                    continue;
                }
                value = (String) props.remove(name);
                name = name.substring(name.indexOf("_") + 1);
                attributes.put(name, value);
            }

            if (attributes.size() > 0) {
                pi.setAttributes(attributes);
            }
            vParams.add(pi);
        }
        ParameterInfo[] params = (ParameterInfo[]) vParams
                .toArray(new ParameterInfo[0]);

        // all remaining properties are assumed to be TaskInfoAttributes
        TaskInfoAttributes tia = new TaskInfoAttributes();
        for (Enumeration eProps = props.propertyNames(); eProps
                .hasMoreElements();) {
            String name = (String) eProps.nextElement();
            String value = props.getProperty(name);
            tia.put(name, value);
        }
        TaskInfo task = new TaskInfo(-1, taskName, taskDescription, null, tia,
                null, 0);
        task.setParameterInfoArray(params);
        return task;
    }
}