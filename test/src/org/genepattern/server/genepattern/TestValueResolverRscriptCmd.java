package org.genepattern.server.genepattern;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.junit.Before;
import org.junit.Test;

/**
 * test cases for the '<run-rscript>' substitution variable,
 * based on the 'helloWorld_R3.1' demo module.
 * 
 *     commandLine=<R3.1_Rscript> <libdir>mainMethod.R
 * 
 * @author pcarr
 */
public class TestValueResolverRscriptCmd {
    private static final String gp_home="/opt/gp_home";
    private static final String tasklibdir=gp_home+"/taskLib";
    private static final String patches=gp_home+"/patches";
    private static final String wrapper_scripts=gp_home+"/resources/wrapper_scripts";

    private static final String taskName="helloWorld_R3.1";
    private static final String libdir=tasklibdir+"/"+taskName+".1/";
    private static final String cmdLine="<R3.1_Rscript> <libdir>mainMethod.R";
    
    /* 
     * for each test ... 
     * create mock values similar to those initialized at runtime in 
     * GenePatternAnalysisTask#onJob before submitting a job to the queue
     */
    private GpContext jobContext;
    private Map<String,ParameterInfoRecord> parameterInfoMap = Collections.emptyMap();
    private Map<String,String> propsMap;
    
    @Before
    public void setUp() {
        jobContext=mock(GpContext.class);
        propsMap = new HashMap<String,String>();
        propsMap.put("name", taskName);
        propsMap.put("libdir", libdir);
        propsMap.put("patches", patches);
    }
    
    // boilerplate ...
    private GpConfig.Builder builder() {
        final GpConfig.Builder b=new GpConfig.Builder()
                .addProperty("wrapper-scripts", wrapper_scripts)
                .addProperty("R3.1_Rscript", "<run-rscript> -v 3.0 --")
                .addProperty("run-rscript", "<wrapper-scripts>/run-rscript.sh -c <env-custom> -l <libdir> -p <patches> -a <env-arch>");
        return b;
    }
    
    /**
     * test substitutions with 'env-custom' and 'env-arch' customizations
     */
    @Test
    public void substitute_commandLine() {
        final GpConfig gpConfig=builder()
            .addProperty("env-custom", "env-custom-rhel6.sh")
            .addProperty("env-arch", "rhel6")
        .build();
        final List<String> expected=Arrays.asList( wrapper_scripts+"/run-rscript.sh",
                "-c", "env-custom-rhel6.sh",
                "-l", libdir, "-p", patches, 
                "-a", "rhel6", 
                "-v", "3.0", "--", libdir+"mainMethod.R");
        final List<String> actual=ValueResolver.resolveValue(gpConfig, jobContext, cmdLine, propsMap, parameterInfoMap);
        assertThat( actual , is(expected) );
    }
    
    /**
     * 'env-custom' and 'env-arch' not set
     */
    @Test
    public void substitute_commandLine_env_not_set() {
        final GpConfig gpConfig=builder().build();
        final List<String> expected=Arrays.asList( wrapper_scripts+"/run-rscript.sh",
                "-c", // <---- '-c' with no env-custom arg
                "-l", libdir, "-p", patches, 
                "-a", // <---- '-a' with no env-arch arg
                "-v", "3.0", "--", libdir+"mainMethod.R");
        final List<String> actual=ValueResolver.resolveValue(gpConfig, jobContext, cmdLine, propsMap, parameterInfoMap);
        assertThat( actual , is(expected) );
    }

    /**
     * 'env-custom' and 'env-arch' set to empty string
     */
    @Test
    public void substitute_commandLine_env_empty() {
        final GpConfig gpConfig=builder()
            .addProperty("env-custom", "")
            .addProperty("env-arch", "")
        .build();
        final List<String> expected=Arrays.asList( wrapper_scripts+"/run-rscript.sh",
                "-c", // <---- '-c' with no env-custom arg
                "-l", libdir, "-p", patches, 
                "-a", // <---- '-a' with no env-arch arg
                "-v", "3.0", "--", libdir+"mainMethod.R");
        final List<String> actual=ValueResolver.resolveValue(gpConfig, jobContext, cmdLine, propsMap, parameterInfoMap);
        assertThat( actual , is(expected) );
    }

}
