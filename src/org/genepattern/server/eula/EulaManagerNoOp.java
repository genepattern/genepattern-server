package org.genepattern.server.eula;

import java.util.Collections;
import java.util.List;

import org.genepattern.server.config.ServerConfiguration.Context;

/**
 * When the server is configured to ignore all EULA info.
 * @author pcarr
 *
 */
public class EulaManagerNoOp implements IEulaManager {

    public void setGetEulaFromTask(GetEulaFromTask impl) {
        //ignore
    }

    public void setGetTaskStrategy(GetTaskStrategy impl) {
        //ignore
    }

    public void setRecordEulaStrategy(RecordEula impl) {
        //ignore
    }

    public boolean requiresEula(Context taskContext) {
        return false;
    }

    public List<EulaInfo> getAllEulaForModule(Context taskContext) {
        return Collections.emptyList();
    }

    public List<EulaInfo> getPendingEulaForModule(Context taskContext) {
        return Collections.emptyList();
    }

    public void recordEula(Context taskContext) throws IllegalArgumentException {
        //ignore
    }

}
