/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import org.genepattern.webservice.OmnigeneException;

public class TaskLSIDNotFoundException extends OmnigeneException {
    public TaskLSIDNotFoundException(String lsid) {
        super("No task found with lsid="+lsid);
    }
}
