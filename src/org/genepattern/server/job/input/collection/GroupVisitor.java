/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.collection;

import org.genepattern.server.job.input.GroupId;


public interface GroupVisitor extends ParamValueVisitor {
    void startGroup(GroupId groupId);
    void finishGroup(GroupId groupId);
}
