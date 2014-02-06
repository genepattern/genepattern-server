package org.genepattern.junitutil;

import java.util.HashMap;
import java.util.Map;

import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.webservice.TaskInfo;
import org.junit.Ignore;

/**
 * Task loader for use in jUnit tests; It's basically a lookup table
 * of lsid to TaskInfo, initialized by parsing the zip file for a module.
 * 
 * @author pcarr
 *
 */
@Ignore
public class TaskLoader implements GetTaskStrategy {
    private Map<String,TaskInfo> lookup=new HashMap<String,TaskInfo>();
   
    /**
     * Add a taskInfo to the 'db'.
     * @param clazz
     * @param zipfilename
     */
    public void addTask(final Class<?> clazz, final String zipfilename) {
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(clazz, zipfilename);
        lookup.put(taskInfo.getLsid(), taskInfo);
    }

    @Override
    public TaskInfo getTaskInfo(final String lsid) {
        final TaskInfo taskInfo=lookup.get(lsid);
        if (taskInfo==null) {
            throw new TaskLSIDNotFoundException(lsid);
        }
        return taskInfo;
    }

}
