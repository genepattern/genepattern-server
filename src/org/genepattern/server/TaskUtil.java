/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.genepattern.server.process.SuiteRepository;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Utility class for GenePattern zip files.
 *
 * @author Joshua Gould
 */
public class TaskUtil {

    public enum ZipFileType {
        PIPELINE_ZIP_OF_ZIPS, PIPELINE_ZIP, MODULE_ZIP, SUITE_ZIP_OF_ZIPS, SUITE_ZIP, INVALID_ZIP
    };

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
            if (zipFile.getEntry(GPConstants.MANIFEST_FILENAME) != null
                    || zipFile.getEntry(GPConstants.SUITE_MANIFEST_FILENAME) != null) {
                return false;
            }

            // is it a zip of zips?
            for (Enumeration eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
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

    /**
     * Gets the type of the specified zip file.
     *
     * @param file
     *            The zip file.
     * @return The type of zip file.
     */
    public static ZipFileType getZipFileType(File file) {
        ZipFile zipFile = null;
        try {
            try {
                zipFile = new ZipFile(file);
            } catch (IOException e) {
                return ZipFileType.INVALID_ZIP;
            }
            if (zipFile.getEntry(GPConstants.MANIFEST_FILENAME) != null) {
                TaskInfo taskInfo;
                try {
                    taskInfo = getTaskInfoFromZip(file);
                } catch (IOException e) {
                    return ZipFileType.INVALID_ZIP;
                }
                if (isPipeline(taskInfo)) {
                    return ZipFileType.PIPELINE_ZIP;
                } else {
                    return ZipFileType.MODULE_ZIP;
                }
            }
            if (zipFile.getEntry(GPConstants.SUITE_MANIFEST_FILENAME) != null) {
                return ZipFileType.SUITE_ZIP;
            }
            try {
                TaskInfo ti = getTaskInfoFromPipelineZipOfZips(file);
                if (ti != null) {
                    return ZipFileType.PIPELINE_ZIP_OF_ZIPS;
                }
            } catch (Exception x) {
            }

            try {
                SuiteInfo s = getSuiteInfoFromZipOfZips(file);
                if (s != null) {
                    return ZipFileType.SUITE_ZIP_OF_ZIPS;
                }
            } catch (Exception x) {
            }

        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {

                }
            }
        }
        return ZipFileType.INVALID_ZIP;

    }

    /**
     * Gets a Map instance from a suite zip of zips.
     *
     * @param file
     *            The file.
     * @return The Map instance.
     * @throws Exception
     */
    public static Map getMapFromZipOfZips(File file) throws Exception {
        File temp = getFirstEntry(file);
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(temp);
            return SuiteRepository.getSuiteMap(zipFile);
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
            temp.delete();
        }
    }

    /**
     * Gets a SuiteInfo instance from a suite zip of zips.
     *
     * @param file
     *            The file.
     * @return The SuiteInfo instance.
     * @throws Exception
     */
    public static SuiteInfo getSuiteInfoFromZipOfZips(File file) throws Exception {
        return new SuiteInfo(getMapFromZipOfZips(file));
    }

    /**
     * Gets a TaskInfo instance from a pipeline zip of zips.
     *
     * @param file
     *            The file.
     * @return The TaskInfo instance.
     * @throws Exception
     */
    public static TaskInfo getTaskInfoFromPipelineZipOfZips(File file) throws Exception {
        File temp = getFirstEntry(file);
        try {
            return getTaskInfoFromZip(temp);
        } finally {
            temp.delete();
        }
    }

    /**
     * Gets the first entry in the specified zip file.
     *
     * @param file
     *            The zip file.
     * @return The first entry.
     * @throws IOException
     *             if an error occurs
     */
    public static File getFirstEntry(File file) throws IOException {
        InputStream is = null;
        File tempFile = null;
        BufferedOutputStream fos = null;
        ZipFile zipOfZips = null;
        try {
            zipOfZips = new ZipFile(file);
            Enumeration e = zipOfZips.entries();
            if (e.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) e.nextElement();
                is = zipOfZips.getInputStream(zipEntry);
                tempFile = File.createTempFile("zip", null);
                byte[] b = new byte[10000];
                int bytesRead = 0;
                fos = new BufferedOutputStream(new FileOutputStream(tempFile));
                while ((bytesRead = is.read(b)) != -1) {
                    fos.write(b, 0, bytesRead);
                }
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
            if (is != null) {
                is.close();
            }

            if (zipOfZips != null) {
                zipOfZips.close();
            }

        }

        return tempFile;

    }

    /**
     * Checks whether the specified file is a pipeline zip file or a pipeline
     * zip of zips.
     *
     * @param file
     *            The ifle.
     * @return <tt>true</tt> if the file is a pipeline.
     * @throws IOException
     */
    public static boolean isPipelineZip(File file) throws IOException {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            TaskInfo ti = null;
            if (zipFile.getEntry(GPConstants.MANIFEST_FILENAME) == null) {
                // check for zip of zips
                if (isZipOfZips(file)) {
                    try {
                        ti = getTaskInfoFromPipelineZipOfZips(file);
                    } catch (Exception e) {

                    }
                }
            } else {
                ti = getTaskInfoFromZip(file);
            }

            return ti != null ? isPipeline(ti) : false;
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    /**
     * Returns whether the specified TaskInfo instance is a pipeline.
     *
     * @param ti
     * @return
     */
    private static boolean isPipeline(TaskInfo ti) {
        Map tia = ti.getTaskInfoAttributes();
        if (tia == null)
            return false; // default to false if unknown

        String type = (String) tia.get(GPConstants.TASK_TYPE);
        if (type == null)
            return false; // default to false if unknown

        return type.endsWith("pipeline");

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
            ZipEntry manifestEntry = zipFile.getEntry(GPConstants.MANIFEST_FILENAME);
            if (manifestEntry == null) {
                throw new IOException(file.getName() + " is missing a GenePattern manifest file.");
            }
            return getTaskInfoFromManifest(zipFile.getInputStream(manifestEntry));
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
    public static TaskInfo getTaskInfoFromManifest(File manifestFile) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(manifestFile);
            return getTaskInfoFromManifest(fis);
        } 
        finally {
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
    private static TaskInfo getTaskInfoFromManifest(URL manifestURL) throws IOException {
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
     * Throws MalformedURLException if the manifest has a bad LSID
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
    public static TaskInfo getTaskInfoFromManifest(InputStream is) throws IOException {
        Properties props = new Properties();
        props.load(is);

        String taskName = (String) props.remove(GPConstants.NAME);
        String lsid = (String) props.get(GPConstants.LSID);
        LSID l = new LSID(lsid);
        if (taskName == null || taskName.trim().equals("")) {
            throw new IOException("Missing task name");
        }
        // FIXME add check for other required attributes
        String taskDescription = (String) props.remove(GPConstants.DESCRIPTION);

        // ParameterInfo entries consist of name/value/description triplets, of
        // which the value and description are optional
        // It is assumed that the names are p[1-n]_name, p[1-n]_value, and
        // p[1-n]_description
        // and that the numbering runs consecutively. When there is no p[m]_name
        // value, then there are m-1 ParameterInfos

        List vParams = new ArrayList();

        for (int i = 1; i <= GPConstants.MAX_PARAMETERS; i++) {
            String name = (String) props.remove("p" + i + "_name");
            if (name == null) {
                continue;
            }
            if (name == null || name.length() == 0) {
                throw new IOException("missing parameter name for " + "p" + i + "_name");
            }
            String value = (String) props.remove("p" + i + "_value");
            if (value == null) {
                value = "";
            }
            String description = (String) props.remove("p" + i + "_description");
            if (description == null) {
                description = "";
            }
            ParameterInfo pi = new ParameterInfo(name, value, description);
            HashMap attributes = new HashMap();
            for (int attribute = 0; attribute < GPConstants.PARAM_INFO_ATTRIBUTES.length; attribute++) {
                name = (String) GPConstants.PARAM_INFO_ATTRIBUTES[attribute][0];
                value = (String) props.remove("p" + i + "_" + name);
                if (value != null) {
                    attributes.put(name, value);
                }
                if (name.equals(GPConstants.PARAM_INFO_TYPE[0]) && value != null
                        && value.equals(GPConstants.PARAM_INFO_TYPE_INPUT_FILE)) {
                    attributes.put(ParameterInfo.MODE, ParameterInfo.INPUT_MODE);
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
        ParameterInfo[] params = (ParameterInfo[]) vParams.toArray(new ParameterInfo[0]);

        // all remaining properties are assumed to be TaskInfoAttributes
        TaskInfoAttributes tia = new TaskInfoAttributes();
        for (Enumeration eProps = props.propertyNames(); eProps.hasMoreElements();) {
            String name = (String) eProps.nextElement();
            String value = props.getProperty(name);
            tia.put(name, value);
        }
        TaskInfo task = new TaskInfo(-1, taskName, taskDescription, null, tia, null, 0);
        task.setParameterInfoArray(params);
        return task;
    }
}
