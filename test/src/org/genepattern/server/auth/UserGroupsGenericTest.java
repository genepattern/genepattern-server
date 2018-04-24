package org.genepattern.server.auth;

import java.io.File;

public class UserGroupsGenericTest extends IGroupMembershipPluginTest {
    @Override
    protected IGroupMembershipPlugin createInstance(final File userGroupsXml) throws Exception {
        return UserGroups.initFromXml(userGroupsXml);
    } 
}
