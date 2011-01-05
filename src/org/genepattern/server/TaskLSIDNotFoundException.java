package org.genepattern.server;

import org.genepattern.webservice.OmnigeneException;

public class TaskLSIDNotFoundException extends OmnigeneException {
    public TaskLSIDNotFoundException(String lsid) {
        super("No task found with lsid="+lsid);
    }
}
