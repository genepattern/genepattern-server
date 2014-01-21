package org.genepattern.junitutil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;

/**
 * Helper classes for instantiating TaskInfo for jUnit tests.
 * 
 * @author pcarr
 */
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

}
