/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.choice.TestChoiceInfo;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * junit tests for the FileDownloader class.
 * @author pcarr
 *
 */
public class TestFileDownloader {
    private GpConfig gpConfig;

    private String choiceDir="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/";
    private String selectedValue="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt";
    private String choiceDir_dirListing="ftp://gpftp.broadinstitute.org/demo/dir/";
    private String selectedDirValue="ftp://gpftp.broadinstitute.org/demo/dir/A/";

    @Before
    public void setUp() {
        gpConfig=new GpConfig.Builder().build();
    }

    @Test
    public void hasSelectedChoices()  throws JobDispatchException {
        ParameterInfo pInfo=TestChoiceInfo.initFtpParam(choiceDir);
        ParameterInfo pInfoOut = TestChoiceInfo.initFtpParam(choiceDir);
        pInfoOut.setValue(selectedValue);
        ParameterInfo[] moduleInputParams = new ParameterInfo[] { pInfo };
        ParameterInfo[] jobResultParams = new ParameterInfo[] { pInfoOut };
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setParameterInfoArray(moduleInputParams);
        JobInfo jobInfo = Mockito.mock(JobInfo.class);

        Mockito.when(jobInfo.getParameterInfoArray()).thenReturn(jobResultParams);
        GpContext jobContext = new GpContext.Builder()
            .jobInfo(jobInfo)
            .taskInfo(taskInfo)
        .build();
        FileDownloader downloader = FileDownloader.fromJobContext(gpConfig, jobContext);
        
        Assert.assertTrue("Expecting a choice selection", downloader.hasSelectedChoices());
        Assert.assertEquals(selectedValue, 
                downloader.getSelectedChoices().get(0).getValue());
        Assert.assertFalse("Expecting a remote file",
                downloader.getSelectedChoices().get(0).isRemoteDir());
    }
    
    @Test
    public void hasSelectedChoices_dirInput() throws JobDispatchException {
        ParameterInfo pInfo=TestChoiceInfo.initFtpParam(choiceDir_dirListing);
        ParameterInfo pInfoOut = TestChoiceInfo.initFtpParam(choiceDir_dirListing);
        pInfoOut.setValue(selectedDirValue);
        ParameterInfo[] moduleInputParams = new ParameterInfo[] { pInfo };
        ParameterInfo[] jobResultParams = new ParameterInfo[] { pInfoOut };
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setParameterInfoArray(moduleInputParams);
        JobInfo jobInfo = Mockito.mock(JobInfo.class);

        Mockito.when(jobInfo.getParameterInfoArray()).thenReturn(jobResultParams);
        GpContext jobContext = new GpContext.Builder()
            .jobInfo(jobInfo)
            .taskInfo(taskInfo)
        .build();
        FileDownloader downloader = FileDownloader.fromJobContext(gpConfig, jobContext);
        
        Assert.assertTrue("Expecting a choice selection", downloader.hasSelectedChoices());
        Assert.assertEquals("selectedChoices[0].value",
                selectedDirValue, 
                downloader.getSelectedChoices().get(0).getValue());
        Assert.assertTrue("Expecting a remoteDir",
                downloader.getSelectedChoices().get(0).isRemoteDir());
    }

}
