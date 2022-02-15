/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.auth.UserGroups;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for custom setting of module properties in the config.yaml file.
 * 
 * @author pcarr
 */
public class TestServerConfigurationModuleProps {
    private static GpConfig gpConfig;
    private final boolean initIsAdmin=false;
    
    @BeforeClass
    public static void beforeClass() {
        final File configFile=FileUtil.getSourceFile(TestServerConfigurationModuleProps.class, "test_module_properties.yaml");
        final File userGroups=FileUtil.getSourceFile(TestServerConfigurationModuleProps.class, "drm_test_userGroups.xml");
        if (!userGroups.canRead()) {
            fail("test initialization error, can't read userGroups file="+userGroups);
        }
        final UserGroups groupInfo=UserGroups.initFromXml(userGroups);
        
        gpConfig = new GpConfig.Builder()
            .configFile(configFile)
            .groupInfo(groupInfo)
            .build();
        
        // validate the gpConfig
        assertNotNull("Expecting non-null gpConfig", gpConfig);
        final List<Throwable> errors = gpConfig.getInitializationErrors();
        if (errors != null && errors.size() > 0) {
            String errorMessage = "gpConfig initialization error, num="+errors.size();
            Throwable first = errors.get(0);
            if (first != null) {
                errorMessage += " error[0]="+first.getMessage();
            }
            fail(errorMessage);
        }
    }
    
    private TaskInfo taskInfo;
    private JobInfo jobInfo;
    
    @Before
    public void setUp() {
        taskInfo = new TaskInfo();
        taskInfo.setName("ComparativeMarkerSelection");
        taskInfo.giveTaskInfoAttributes().put(GPConstants.LSID, "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:4");
        
        jobInfo = new JobInfo();
        jobInfo.setTaskName("ComparativeMarkerSelection");
        jobInfo.setTaskLSID("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:4");
    }

    private void doTest(final GpContext context, final String expected) {
        doTest(context, "test.prop", expected);
    }

    private void doTest(final GpContext context, final String prop, final String expected) {
        String message="for "+printContext(context)+" expecting "+prop+"="+expected;
        Value value=gpConfig.getValue(context,prop);
        assertEquals(message, expected, value.getValue());
    }
    
    private String printContext(final GpContext context) {
        if (context==null) {
            return "context=null";
        }
        StringBuffer buf=new StringBuffer();
        buf.append("userId="+context.getUserId());
        if (context.getTaskInfo() != null) {
            buf.append(",taskInfo.name="+context.getTaskInfo().getName());
        }
        if (context.getJobInfo() != null) {
            buf.append(",jobInfo.taskName="+context.getJobInfo().getTaskName());
        }
        return buf.toString();
    }

    /**
     * Set a default value in the 'default.properties' section.
     */
    @Test
    public void testDefaultContext() {
        GpContext context = GpContext.getServerContext();
        doTest(context, "DEFAULT_VALUE");
    }

    /**
     * With a null context, use the default value.
     */
    @Test
    public void testNullContext() {
        GpContext context = null;
        doTest(context, "DEFAULT_VALUE");
    }

    /**
     * For a particular user, with no custom settings, use the default value.
     */
    @Test
    public void testJobInfoAndTaskInfoNotSet() {
        //2) user is set, taskInfo and jobInfo not set
        GpContext context = GpContext.getContextForUser("gp_user", initIsAdmin);
        doTest(context, "DEFAULT_VALUE");
    }

    /**
     * Set a custom value in the 'user.properties': <userid> section.
     */
    @Test
    public void testUserWithCustomValue() {
        GpContext context = GpContext.getContextForUser("userD", initIsAdmin);
        doTest(context, "USERD_VALUE");
    }
    
    /**
     * Set a custom value in the 'group.properties': <groupid> section.
     */
    @Test
    public void testGroupWithCustomValue() {
        // user 'Broadie C' is in the 'broadgroup'
        GpContext context = GpContext.getContextForUser("Broadie C", initIsAdmin);
        doTest(context, "BROADGROUP_VALUE");
    }

    /**
     * Set a custom value in the 'module.properties': <lsid> section.
     */
    @Test
    public void testLsidWithCustomValue() {
        GpContext context = GpContext.getContextForUser("gp_user", initIsAdmin);
        context.setTaskInfo(taskInfo);
        doTest(context, "TASK_LSID_VALUE");
    }

    /**
     * Set a custom value in the 'module.properties': <lsid_no_version> section.
     */
    @Test
    public void testLsidNoVersion() {
        GpContext context = GpContext.getContextForUser("gp_user", initIsAdmin);
        taskInfo.giveTaskInfoAttributes().put(GPConstants.LSID, "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:3");
        context.setTaskInfo(taskInfo);
        doTest(context, "TASK_LSID_NO_VERSION_VALUE");
    }

    /**
     * Set a custom value in the 'module.properties': <task_name> section.
     */
    @Test
    public void testTaskName() {
        GpContext context = GpContext.getContextForUser("gp_user", initIsAdmin);
        context.setTaskInfo(taskInfo);
        
        //same taskName but different base lsid
        taskInfo.giveTaskInfoAttributes().put(GPConstants.LSID, "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00045:2");
        context.setTaskInfo(taskInfo);
        doTest(context, "TASK_NAME_VALUE");
    }
    
    /**
     * What about when the JobInfo is set, but not the taskInfo?
     * Get taskName and taskLsid from the context#jobInfo object.
     */
    @Test
    public void testJobInfo() {
        GpContext context = GpContext.getContextForUser("gp_user", initIsAdmin);
        context.setJobInfo(jobInfo);
        doTest(context, "TASK_LSID_VALUE");
    }

    /**
     * When the JobInfo is set, but not the taskInfo, test for lsid no version.
     */
    @Test
    public void testJobInfoLsidNoVersion() {
        GpContext context = GpContext.getContextForUser("gp_user", initIsAdmin);
        jobInfo.setTaskLSID("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:3");
        context.setJobInfo(jobInfo);
        doTest(context, "TASK_LSID_NO_VERSION_VALUE");
    }
    
    /**
     * When the JobInfo is set, but not the taskInfo, test for task name.
     */
    @Test
    public void testJobInfoTaskName() {
        GpContext context = GpContext.getContextForUser("gp_user", initIsAdmin);
        jobInfo.setTaskLSID("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00045:2");
        context.setJobInfo(jobInfo);
        doTest(context, "TASK_NAME_VALUE");
    }

    /**
     * When jobInfo and taskInfo are not set, use the default value.
     */
    @Test
    public void testNullJobInfo() {
        GpContext context = GpContext.getContextForUser("gp_user", initIsAdmin);
        context.setJobInfo(null);
        doTest(context, "DEFAULT_VALUE");
    }
    
    /**
     * What takes precedence, taskInfo or jobInfo? ANSWER, jobInfo
     */
    @Test
    public void testJobInfoAndTaskInfo() {
        GpContext context = GpContext.getContextForUser("gp_user", initIsAdmin);
        context.setTaskInfo(taskInfo);
        jobInfo.setTaskName("ConvertLineEndings");
        jobInfo.setTaskLSID("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:1");
        context.setJobInfo(jobInfo);
        doTest(context, "CONVERTLINEENDINGS_VALUE");
    }
    
    /**
     * what takes precedence, user prop or module prop? ANSWER, user prop.
     */
    @Test
    public void testUserAndModuleProp() {
        GpContext context = GpContext.getContextForUser("userD", initIsAdmin);
        context.setTaskInfo(taskInfo);
        doTest(context, "USERD_VALUE");
    }

    /**
     * what takes precedence, group prop or module prop? ANSWER, group prop.
     */
    @Test
    public void testUserAndGroupProp() {
        GpContext context = GpContext.getContextForUser("adminuser", initIsAdmin);
        context.setTaskInfo(taskInfo);
        doTest(context, "ADMINGROUP_VALUE");

        //secondary test, prove that JobInfo is sufficient, when taskInfo is null
        context.setTaskInfo(null);
        context.setJobInfo(jobInfo);
        doTest(context, "ADMINGROUP_VALUE");
    }
    
    /**
     * what takes precedence, module.prop or group.module.prop? ANSWER, group.module.prop.
     * For userC, in the broadgroup, should inherit module properties from the group.
     */
    @Test
    public void testModulePropInGroup() {
        GpContext context = GpContext.getContextForUser("Broadie C", initIsAdmin);
        context.setTaskInfo(taskInfo);
        doTest(context, "BROADGROUP_LSID_VALUE");
        
        //secondary test, prove that JobInfo is sufficient, when taskInfo is null
        context.setTaskInfo(null);
        context.setJobInfo(jobInfo);
        doTest(context, "BROADGROUP_LSID_VALUE");
    }
    
    /**
     * what takes precedence, user.module.prop or group.module.prop? ANSWER, user.module.prop.
     */
    @Test
    public void testModulePropForUser() {
        GpContext context = GpContext.getContextForUser("userC", initIsAdmin);
        context.setTaskInfo(taskInfo);
        doTest(context, "USERC_TASK_LSID_VALUE");
        
        //secondary test, prove that JobInfo is sufficient, when taskInfo is null
        context.setTaskInfo(null);
        context.setJobInfo(jobInfo);
        doTest(context, "USERC_TASK_LSID_VALUE");
        
        //TODO: tertiary test, prove that JobInfo takes precedence over TaskInfo
    }
    
    /**
     * Make sure the code works for a user who is a member of more than one group.
     */
    @Test
    public void testModulePropForUserInTwoGroups() {
        GpContext context = GpContext.getContextForUser("userA", initIsAdmin);
        context.setTaskInfo(taskInfo);
        doTest(context, "USERA_TASK_LSID_VALUE");
        
        //secondary test, prove that JobInfo is sufficient, when taskInfo is null
        context.setTaskInfo(null);
        context.setJobInfo(jobInfo);
        doTest(context, "USERA_TASK_LSID_VALUE");
        
        //TODO: tertiary test, prove that JobInfo takes precedence over TaskInfo
    }
    
    /**
     * what about when the '*' group is set and the user is not a member of any group?
     */
    @Test
    public void testAllGroups_defaultUser() {
        // a user in no group
        GpContext context=GpContext.getContextForUser("gp_user", initIsAdmin);
        doTest(context, "all.groups.prop", "ALL_GROUPS_VALUE");
    }
    
    /**
     * what about when the '*' group is set and the user is in a group with no custom value.
     * for a particular group?
     */
    @Test
    public void testAllGroups_userInGroup() {        
        // a user in a group, which doesn't set the value
        GpContext context=GpContext.getContextForUser("userC", initIsAdmin);
        doTest(context, "all.groups.prop", "ALL_GROUPS_VALUE");
    }
    
    /**
     * what about when the '*' group is set and the value is customized
     * for a particular group?
     */
    @Test
    public void testAllGroups_override() {        
        // a user in a group, which sets the value
        GpContext context=GpContext.getContextForUser("adminuser", initIsAdmin);
        doTest(context, "all.groups.prop", "ADMINGROUP_VALUE");
    }
    
    
    /**
     * what about when the '*' group is set and the value is customized
     * for a particular group and a particular module?
     */
    @Test
    public void testAllGroups_overrideInModuleProps() {        
        // a user in a group, which overrides the value in the module.properties section
        GpContext context=GpContext.getContextForUser("adminuser", initIsAdmin);
        taskInfo.setName("AllGroupsModule");
        taskInfo.getTaskInfoAttributes().put(GPConstants.LSID, "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00001:1");
        context.setTaskInfo(taskInfo);
        doTest(context, "all.groups.prop", "ADMINGROUP_MODULE_VALUE"); 
    }
    
    /**
     * The executor.default.properties takes precedence over the top level default properties,
     * for the current user's executor.
     */
    @Test
    public void testOverrideInExecutorDefaults() {
        // userF has the 'TestExec' executor
        final String userId="userF";
        final GpContext gpContext=GpContext.getContextForUser(userId, initIsAdmin);
        doTest(gpContext, "test.prop", "BY_EXEC_DEFAULT");
    }
}
