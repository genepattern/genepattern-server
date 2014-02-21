package org.genepattern.server.eula;

import java.util.Collections;
import java.util.List;

import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.TaskInfo;

/**
 * When the server is configured to ignore all EULA info.
 * @author pcarr
 *
 */
public class EulaManagerNoOp implements IEulaManager {
    
    private GetEulaFromTask getEulaFromTask = null;

    public List<EulaInfo> getEulas(TaskInfo taskInfo) {
        GetEulaFromTask impl = getGetEulaFromTask();
        return impl.getEulasFromTask(taskInfo);
    }
    
    private GetEulaFromTask getGetEulaFromTask() {
        //allow for dependency injection, via setGetEulaFromTask
        if (getEulaFromTask != null) {
            return getEulaFromTask;
        }
        
        //otherwise, hard-coded rule        
        //option 1: license= in manifest
        return new GetEulaAsManifestProperty();
        //option 2: support file named '*license*' in tasklib
        //return new GetEulaAsSupportFile();
    }

    public void setEula(EulaInfo eula, TaskInfo taskInfo) {
        //ignore
    }

    public void setEulas(List<EulaInfo> eulas, TaskInfo taskInfo) {
        //ignore
    }

    public boolean requiresEula(final GpContext taskContext) {
        return false;
    }

    public List<EulaInfo> getAllEulaForModule(final GpContext taskContext) {
        return Collections.emptyList();
    }

    public List<EulaInfo> getPendingEulaForModule(final GpContext taskContext) {
        return Collections.emptyList();
    }

    public void recordEula(final GpContext taskContext) throws IllegalArgumentException {
        //ignore
    }

}
