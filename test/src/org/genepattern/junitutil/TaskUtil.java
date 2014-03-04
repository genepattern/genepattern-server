package org.genepattern.junitutil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Ignore;

/**
 * Helper classes for instantiating TaskInfo for jUnit tests.
 * 
 * @author pcarr
 */
@Ignore
public class TaskUtil {

    public static TaskInfo getTaskInfoFromZip(final Class<?> clazz, final String zipfilename) {
        File zipfile=FileUtil.getSourceFile(clazz, zipfilename);
        return getTaskInfoFromZip(zipfile);
    }

    public static TaskInfo getTaskInfoFromZip(final File zipfile) {
        TaskInfo taskInfo = null;
        try {
            taskInfo = org.genepattern.server.TaskUtil.getTaskInfoFromZip(zipfile);
        }
        catch (IOException e) {
            Assert.fail("Error getting taskInfo from zipFile="+zipfile+". Error: "+e.getLocalizedMessage());
        }
        catch (Throwable t) {
            Assert.fail("Error getting taskInfo from zipFile="+zipfile+". Error: "+t.getLocalizedMessage());
        }
        return taskInfo;
    }

    public static List<TaskInfo> getTaskInfosFromZipPipeline(final Class<?> clazz, final String zipfilename) {
        File zipfile=FileUtil.getSourceFile(clazz, zipfilename);
        return getTaskInfosFromZipPipeline(zipfile);
    }
    
    public static List<TaskInfo> getTaskInfosFromZipPipeline(final File zipfile) {
        List<TaskInfo> taskInfos = new ArrayList<TaskInfo>();
        
        TaskInfo taskInfo = null;
        try {
            taskInfo = getTaskInfoFromZip(zipfile);
            taskInfos.add(taskInfo);
            return taskInfos;
        }
        catch (Throwable t) {
            //must be a pipeline with modules
        }
    
        ZipWalker zipWalker = new ZipWalker(zipfile);
        try {
            zipWalker.walk();
        }
        catch (Exception e) {
            //TODO: Need to hold onto this, and fail the test after we attempt to clean up
        }
        List<File> nestedZips = zipWalker.getNestedZips();
        for(File nestedZip : nestedZips) {
            TaskInfo nestedTask=null;
            try {
                nestedTask = getTaskInfoFromZip(nestedZip);
                taskInfos.add(nestedTask);
            }
            catch (Throwable t) {
                //ignore zip entries
            }
            finally {
                //delete the tmp file
            }
        }
        try {
            zipWalker.deleteTmpFiles();
        }
        catch (Exception e) {
            Assert.fail(e.getLocalizedMessage());
        }
        return taskInfos;
    }

    public static InputStream getSupportFileFromZip(final Class<?> clazz, final String zipfilename, final String filename)
            throws Exception
    {
        File zipSourceFile =FileUtil.getSourceFile(clazz, zipfilename);

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zipSourceFile);
            ZipEntry manifestEntry = zipFile.getEntry(filename);
            if (manifestEntry == null) {
                throw new IOException(zipfilename + " is missing a GenePattern manifest file.");
            }
            return zipFile.getInputStream(manifestEntry);
        }
        catch (IOException io)
        {
            if (zipFile != null) {
                zipFile.close();
            }
        }

        return null;
    }

    public static void writeSupportFileToFile(InputStream inputStream, File toFile)
            throws IOException
    {
        OutputStream outputStream = new FileOutputStream(toFile);
        IOUtils.copy(inputStream, outputStream);
    }
}
