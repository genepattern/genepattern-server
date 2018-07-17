/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.collection;

import org.genepattern.server.job.input.GroupId;


public interface GroupVisitor extends ParamValueVisitor {
    void startGroup(GroupId groupId);
    void finishGroup(GroupId groupId);
}
