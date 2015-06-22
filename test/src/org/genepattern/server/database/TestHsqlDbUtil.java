/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

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
    private File schemaDir=new File("website/WEB-INF/schema");
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

    @Test
    public void listSchemaFiles_nullDbSchemaVersion() {
        final String schemaPrefix="analysis_hypersonic-";
        final String dbSchemaVersion=null;
        List<File> schemaFiles = HsqlDbUtil.listSchemaFiles(schemaDir, schemaPrefix, "3.9.3", dbSchemaVersion);
        assertEquals("num schema files, new install of 3.9.3", 40, schemaFiles.size());
    }

    @Test
    public void listSchemaFiles_emptyDbSchemaVersion() {
        final String schemaPrefix="analysis_hypersonic-";
        final String dbSchemaVersion="";
        List<File> schemaFiles = HsqlDbUtil.listSchemaFiles(schemaDir, schemaPrefix, "3.9.3", dbSchemaVersion);
        assertEquals("num schema files, new install of 3.9.3", 40, schemaFiles.size());
    }
    
    @Test
    public void listSchemaFiles_update() {
        final String schemaPrefix="analysis_hypersonic-";
        List<File> schemaFiles = HsqlDbUtil.listSchemaFiles(schemaDir, schemaPrefix, "3.9.2", "3.9.1");
        assertEquals("num schema files, updated install of 3.9.2", 1, schemaFiles.size());
    }
    
    @Test
    public void listSchemaFiles_default() {
        final String schemaPrefix="analysis_hypersonic-";
        List<File> schemaFiles = HsqlDbUtil.listSchemaFiles(schemaDir, schemaPrefix, null, null);
        assertEquals("num schema files, latest version", 40, schemaFiles.size());
    }
    
    @Test
    public void getLatestSchemaVersionFromSchemaDir() {
        HsqlDbUtil.DbSchemaFilter f=new HsqlDbUtil.DbSchemaFilter("analysis_hypersonic-");
        int c=f.compare(new File("analysis_hypersonic-3.9.3"), new File("analysis_hypersonic-3.9.3-a"));
        assertEquals("'3.9.3'.compare('3.9.3-a')", true, c<0);
        //Strings.
        //assertEquals(
        
        //HsqlDbUtil.getLatestSchemaVersionFromSchemaDir(schemaDir, "");
    }
    
    @Ignore @Test
    public void initDbSchemaMysql() throws Throwable {
        
        Properties p=new Properties();
        ConfigUtil.loadPropertiesInto(p, new File(resourcesDir, "database_default.properties"));

        //loadProperties(mysqlProperties, new File("resources/database_default.properties"));
        p.setProperty("database.vendor", "MySQL");
        p.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        p.setProperty("hibernate.connection.url", "jdbc:mysql://127.0.0.1:3306/gpdev");
        p.setProperty("hibernate.connection.username", "gpdev");
        p.setProperty("hibernate.connection.password", "gpdev");
        p.setProperty("hibernate.default_schema", "genepattern");
        p.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");

        
        GpConfig gpConfig=Mockito.mock(GpConfig.class);
        Mockito.when(gpConfig.getDbProperties()).thenReturn(p);
        HibernateSessionManager mgr=HibernateUtil.initFromConfig(gpConfig, gpContext);
        
        
        // TODO: pass in the mgr in the call to updateSchema
        //HibernateUtil.setInstance(mgr);
        //HsqlDbUtil.updateSchema(resourcesDir, "analysis_mysql", "3.9.2");
    }
    
    

}
