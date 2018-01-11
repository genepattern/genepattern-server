/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import org.genepattern.webservice.OmnigeneException;

public class TaskLSIDNotFoundException extends OmnigeneException {
    public TaskLSIDNotFoundException(String lsid) {
        super("No task found with lsid="+lsid);
    }
}
