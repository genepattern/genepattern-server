package org.genepattern.server.database;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for initializing and launching the in-memory HSQL database.
 * @author pcarr
 *
 */
public class TestHsqlDbUtil {
    private GpConfig gpConfig;
    private GpContext gpContext;
    private File workingDir=new File(System.getProperty("user.dir"));
    private File resourcesDir=new File(workingDir.getParent(), "resources");
    private Value defaultValue;
    private Value defaultValues;
    private String[] defaultExpected=new String[] {
            "-port", "9001", "-database.0", "file:"+resourcesDir+"/GenePatternDB", "-dbname.0", "xdb", "-no_system_exit", "true"};
    
    @Before
    public void setUp() {
        gpConfig=mock(GpConfig.class);
        when(gpConfig.getResourcesDir()).thenReturn(resourcesDir);
        gpContext=GpContext.getServerContext();
        defaultValue=new Value("-port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb");
        defaultValues=new Value(Arrays.asList("-port", "9001", "-database.0", "file:../resources/GenePatternDB", "-dbname.0", "xdb"));
    }
    
    
    /**
     * This is what will be found in the default genepattern.properties file after installing GP <= 3.9.0.
     */
    @Test
    public void initHsqlArgsFromConfig_default() {
        when(gpConfig.getValue(gpContext, "HSQL.args")).thenReturn(defaultValue);
        String[] actual=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);
        assertThat(actual, is(defaultExpected));
    }

    /**
     * In config.yaml,
     *     HSQL.args: [ "-port", "9001", "-database.0", ...,  ]
     */
    @Test
    public void initHsqlArgsFromConfig_asList() {
        when(gpConfig.getValue(gpContext, "HSQL.args")).thenReturn(defaultValues);
        String[] actual=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);        
        assertThat(actual, is(defaultExpected));
    }
    
    /**
     * In config.yaml,
     *     HSQL.args:    <---- set to null or empty string
     *  Or
     *     #HSQL.args    <---- not set at all
     * Then return settings based on path to the resources directory.
     * 
     */
    @Test
    public void intHsqlArgsFromConfig_notset() {
        File resourcesDir=GpConfig.relativize(null, "../resources");
        resourcesDir=new File(GpConfig.normalizePath(resourcesDir.getPath())); 
        when(gpConfig.getResourcesDir()).thenReturn(resourcesDir);
        String[] expected=new String[] {
                "-port", "9001", "-database.0", "file:"+resourcesDir.getPath()+"/GenePatternDB", "-dbname.0", "xdb", "-no_system_exit", "true"};
        String[] actual=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);
        assertThat(actual, is(expected));
    }

    @Test
    public void intHsqlArgsFromConfig_noResourcesDir() {
        String[] actual=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);
        assertThat(actual, is(defaultExpected));
    }

}
