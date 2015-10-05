package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.ParamListHelper.ListMode;
import org.genepattern.server.job.input.TestLoadModuleHelper;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by nazaire on 9/28/15.
 */
public class TestJavascriptHandler
{
    private TaskInfo taskInfo;
    final String gpUrl="http://127.0.0.1:8080/gp/";
    private GpConfig gpConfig;

    private static final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00045:7.11";

    @Before
    public void setUp() throws IOException {
        File rootJobDir= FileUtil.getDataFile("jobResults");
        gpConfig=new GpConfig.Builder()
                .genePatternURL(new URL(gpUrl))
                .addProperty(GpConfig.PROP_JOBS, rootJobDir.getAbsolutePath())
                .build();


        final TaskInfoAttributes tia = new TaskInfoAttributes();
        tia.put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_JAVASCRIPT);
        tia.put("commandLine", "cmsviewer.html ? <comparative.marker.selection.filename> <dataset.filename>");
        taskInfo=mock(TaskInfo.class);
        when(taskInfo.getLsid()).thenReturn(lsid);
        when(taskInfo.getTaskInfoAttributes()).thenReturn(tia);
        when(taskInfo.getAttributes()).thenReturn(tia);
    }

    @Test
    public void testUrlEncoding() throws Exception
    {
        final String odfFileUrl = "http://www.broadinstitute.org/cancer/software/genepattern/data/protocols/all_aml_test.preprocessed.comp.marker.odf";

        final String dataFileUrl = "http://www.broadinstitute.org/cancer/software/genepattern/data/protocols/all_aml_test.preprocessed.gct";

        Map<String, List<String>> substitutedValues=new LinkedHashMap<String,List<String>>();

        substitutedValues.put("comparative.marker.selection.filename", Arrays.asList(odfFileUrl));
        substitutedValues.put("dataset.filename", Arrays.asList(dataFileUrl));

        GpConfig gpConfig=new GpConfig.Builder().build();

        String launchUrl = JavascriptHandler.generateLaunchUrl(gpConfig, taskInfo, substitutedValues);

        assertEquals(
            // expected
               gpUrl + "tasklib/" + lsid + "/cmsviewer.html?comparative.marker.selection.filename=" + URLEncoder.encode(odfFileUrl, "UTF-8") + "&dataset.filename=" + URLEncoder.encode(dataFileUrl, "UTF-8"),
                    // actual
                    launchUrl);
    }
}
