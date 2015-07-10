/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.genepattern.server.DbException;
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
    private File resourcesDir=new File(workingDir, "resources");
    private Value defaultValue;
    private Value defaultValues;
    private String[] defaultExpected=new String[] {
            "-port", "9001", "-database.0", "file:"+resourcesDir+"/GenePatternDB", "-dbname.0", "xdb", "-no_system_exit", "true"};
    
    @Before
    public void setUp() {
        gpConfig=new GpConfig.Builder()
            .resourcesDir(resourcesDir)
            .addProperty("HSQL_port", "9001")
        .build();

        gpContext=GpContext.getServerContext();
        defaultValue=new Value("-port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb");
        defaultValues=new Value(Arrays.asList("-port", "9001", "-database.0", "file:../resources/GenePatternDB", "-dbname.0", "xdb"));
    }
    
    /**
     * This is what will be found in the default genepattern.properties file after installing GP <= 3.9.0.
     */
    @Test
    public void initHsqlArgsFromConfig_default() throws DbException {
        gpConfig=new GpConfig.Builder()
            .resourcesDir(resourcesDir)
            .addProperty("HSQL_port", "9001")
            .addProperty("HSQL.args", defaultValue.getValue())
        .build();

        String[] actual=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);
        assertThat(actual, is(defaultExpected));
    }
    
    /**
     * When 'HSQL.args is not set but HSQL_port is set.
     */
    @Test
    public void initHsqlArgs_customPort() throws DbException {
        gpConfig=new GpConfig.Builder()
            .resourcesDir(resourcesDir)
            .addProperty("HSQL_port", "9005")
        .build();

        String[] actual=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);
        String[] expected=new String[] {
            "-port", "9005", "-database.0", "file:"+resourcesDir+"/GenePatternDB", "-dbname.0", "xdb", "-no_system_exit", "true"};
        assertThat(actual, is(expected));
    }

    /**
     * When HSQL.args is not set but the path to the 'resources' dir is set to a custom location.
     */
    @Test
    public void initHsqlArgs_customResourcesDir() throws DbException {
        File customResourcesDir=new File("resources").getAbsoluteFile();
        gpConfig=new GpConfig.Builder()
            .resourcesDir(customResourcesDir)
            .addProperty("HSQL_port", "9001")
        .build();

        String[] actual=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);
        String[] expected=new String[] {
            "-port", "9001", "-database.0", "file:"+customResourcesDir+"/GenePatternDB", "-dbname.0", "xdb", "-no_system_exit", "true"};
        assertThat(actual, is(expected));
    }

    /**
     * In config.yaml,
     *     HSQL.args: [ "-port", "9001", "-database.0", ...,  ]
     */
    @Test
    public void initHsqlArgsFromConfig_asList() throws DbException {
        gpConfig=mock(GpConfig.class);
        when(gpConfig.getResourcesDir()).thenReturn(resourcesDir);
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
    public void intHsqlArgsFromConfig_notset() throws DbException {
        File resourcesDir=GpConfig.relativize(null, "../resources");
        resourcesDir=new File(GpConfig.normalizePath(resourcesDir.getPath())); 
        gpConfig=new GpConfig.Builder()
            .resourcesDir(resourcesDir)
            .addProperty("HSQL_port", "9001")
        .build();

        String[] expected=new String[] {
                "-port", "9001", "-database.0", "file:"+resourcesDir.getPath()+"/GenePatternDB", "-dbname.0", "xdb", "-no_system_exit", "true"};
        String[] actual=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);
        assertThat(actual, is(expected));
    }

    @Test
    public void intHsqlArgsFromConfig_defaultResourcesDir() throws DbException {
        String[] actual=HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);
        assertThat(actual, is(defaultExpected));
    }
    
    @Test(expected=DbException.class)
    public void intHsqlArgsFromConfig_resourcesDirConfigError() throws DbException {
        gpConfig=mock(GpConfig.class);
        when(gpConfig.getResourcesDir()).thenReturn(null);
        HsqlDbUtil.initHsqlArgs(gpConfig, gpContext);
    }

}
