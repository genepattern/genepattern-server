package org.genepattern.junitutil;

import java.io.File;
import java.io.IOException;

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

}
