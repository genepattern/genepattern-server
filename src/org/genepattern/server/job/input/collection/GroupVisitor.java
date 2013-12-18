package org.genepattern.server.job.input.collection;

import org.genepattern.server.job.input.GroupId;


public interface GroupVisitor extends ParamValueVisitor {
    void startGroup(GroupId groupId);
    void finishGroup(GroupId groupId);
}
