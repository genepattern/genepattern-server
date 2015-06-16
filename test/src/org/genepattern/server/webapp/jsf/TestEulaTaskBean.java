/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import org.genepattern.junitutil.JunitAdminClient;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the EulaTaskBean.
 * 
 * @author pcarr
 */
public class TestEulaTaskBean {
    private boolean isAdmin=false;
    private String userId="test";
    private IAdminClient adminClient=new JunitAdminClient(userId, isAdmin);
    private EulaTaskBean bean;
    final String expectedLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:4";
    
    @Before
    public void setUp() {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setUserId("admin");
        taskInfo.setName("PreprocessDataset");
        taskInfo.giveTaskInfoAttributes().put(GPConstants.LSID, "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:4");
        taskInfo.setAccessId(GPConstants.ACCESS_PUBLIC);
        
        try {
            JunitAdminClient.addTask(taskInfo);
        }
        catch (IllegalArgumentException e) {
            //catch this, because it's thrown when we add the same task more than once
        }
        bean = new EulaTaskBean();
        bean.setCurrentUser("test");
        bean.setAdminClient(adminClient);
    }
    
    @Test
    public void testInitFromLsid() {
        bean.setCurrentLsid(expectedLsid);
        Assert.assertEquals("bean.currentLsid", expectedLsid, bean.getCurrentLsid());
        Assert.assertEquals("bean.currentLsidVersion", "4", bean.getCurrentLsidVersion());
        Assert.assertEquals("bean.currentTaskName", "PreprocessDataset", bean.getCurrentTaskName());
    }
    
    @Test
    public void testInitFromLsidNoVersion() {
        bean.setCurrentLsid("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020");
        
        Assert.assertEquals("bean.currentLsid", expectedLsid, bean.getCurrentLsid());
        Assert.assertEquals("bean.currentLsidVersion", "4", bean.getCurrentLsidVersion());
        Assert.assertEquals("bean.currentTaskName", "PreprocessDataset", bean.getCurrentTaskName());
    }
    
    @Test
    public void testInitFromLsidName() {
        bean.setCurrentLsid("PreprocessDataset");
        
        Assert.assertEquals("bean.currentLsid", expectedLsid, bean.getCurrentLsid());
        Assert.assertEquals("bean.currentLsidVersion", "4", bean.getCurrentLsidVersion());
        Assert.assertEquals("bean.currentTaskName", "PreprocessDataset", bean.getCurrentTaskName());
    }
}
