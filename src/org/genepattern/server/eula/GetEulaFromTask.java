package org.genepattern.server.eula;

import java.util.List;

import org.genepattern.webservice.TaskInfo;

/**
 * Instead of adding a new method to the TaskInfo class, e.g. TaskInfo.getEulas(), implement this
 * interface.
 * 
 * @author pcarr
 *
 */
public interface GetEulaFromTask {
    List<EulaInfo> getEulasFromTask(TaskInfo taskInfo);
}
