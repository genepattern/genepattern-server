/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.database;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.dao.BaseDAO;
import org.hsqldb.Server;

public class HsqlDbUtil {
    private static Logger log = Logger.getLogger(HsqlDbUtil.class);

    /**
     * Initialize the arguments to the HSQL DB startup command.
     * 
     * In GP <= 3.9.0 it was based on this line from the genepattern.properties file.
     * <pre>
     * HSQL.args= -port $HSQL_port$  -database.0 file:../resources/GenePatternDB -dbname.0 xdb
     * </pre>
     * 
     * @return
     */
    public static String[] initHsqlArgs(GpConfig gpConfig, GpContext gpContext) { 
        File dbFilePath=initDbFilePath(gpConfig);
        Value hsqlArgs=gpConfig.getValue(gpContext, "HSQL.args");
        if (hsqlArgs != null && hsqlArgs.getNumValues()==1) {
            String hsqlArg=hsqlArgs.getValue();
            //special-case: replace relative legacy (pre 3.9.0) relative path with absolute path
            hsqlArg = hsqlArg.replace("file:../resources/GenePatternDB", "file:"+dbFilePath);
            return tokenizeHsqlArgs( hsqlArg );
        }
        else if (hsqlArgs != null && hsqlArgs.getNumValues()>1) {
            List<String> values=hsqlArgs.getValues();
            values=appendIfNecessary(values);
            for(int i=0; i<values.size(); ++i) {
                if (values.get(i).equals("file:../resources/GenePatternDB")) {
                    //special-case: replace relative legacy (pre 3.9.0) relative path with absolute path
                    values.set(i, "file:"+dbFilePath);
                }
            }
            String[] rval = values.toArray(new String[values.size()]);
            return rval;
        }
        
        Integer hsqlPort=gpConfig.getGPIntegerProperty(gpContext, "HSQL_port", 9001);
        
        // initialize from the dbFilePath
        return initHsqlArgs(hsqlPort, dbFilePath);
    }
    
    protected static File initDbFilePath(GpConfig gpConfig) {
        File resourcesDir=gpConfig.getResourcesDir();
        if (resourcesDir == null) {
            log.warn("resourcesDir is not set!");
            File workingDir=new File(System.getProperty("user.dir"));
            resourcesDir=new File(workingDir.getParent(), "resources");
        }
        return new File(resourcesDir,"GenePatternDB");
    }
    
    protected static String[] initHsqlArgs(final File hsqlDbFile) {
        return initHsqlArgs(9001, hsqlDbFile);
    }

    protected static String[] initHsqlArgs(final Integer hsqlPort, final File hsqlDbFile) {
        String hsqlArgs=" -port "+hsqlPort+"  -database.0 file:"+hsqlDbFile+" -dbname.0 xdb";
        return tokenizeHsqlArgs( hsqlArgs );
    }
    

    /**
     * Get the current version of GenePattern, (e.g. '3.9.1'). 
     * Automatic schema update is based on the difference between this value (as defined by the GP installation)
     * and the entry in the database.
     * 
     */
    public static String initExpectedSchemaVersion() {
        final String expectedSchemaVersion;
        
        final String gpVersion = ServerConfigurationFactory.instance().getGenePatternVersion();
        //for junit testing, if the property is not in ServerProperties, check System properties
        if ("$GENEPATTERN_VERSION$".equals(gpVersion)) {
            log.info("gpVersion="+gpVersion+" (from ServerProperties)");
            expectedSchemaVersion = System.getProperty("GenePatternVersion", gpVersion);
            log.info("expectedSchemaVersion="+expectedSchemaVersion+" (from System.getProperty)");
        }
        else {
            expectedSchemaVersion=gpVersion;
        }
        return expectedSchemaVersion;
    }
    
    /**
     * Start the database, initializing the DB schema if necessary.
     * 
     * @param hsqlArgs, default value, HSQL.args= -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb 
     * @param expectedSchemaVersion, the version of GP defined in the genepattern.properties file, default value, GenePatternVersion=3.9.1
     * @throws Throwable
     */
    public static void startDatabase(final String hsqlArgs, final String expectedSchemaVersion) throws Throwable {
        String[] argsArray = tokenizeHsqlArgs(hsqlArgs);
        startDatabase(argsArray, expectedSchemaVersion);
    }

    /**
     * Start the database, initializing the DB schema if necessary.
     * 
     * @param hsqlArgs, default value, HSQL.args= -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb 
     * @param expectedSchemaVersion, the version of GP defined in the genepattern.properties file, default value, GenePatternVersion=3.9.1
     * @throws Throwable
     */
    public static void startDatabase(final String[] hsqlArgs, final String expectedSchemaVersion) throws Throwable {
        log.debug("Starting HSQL Database...");
        Server.main(hsqlArgs);
        
        try {
            // 1) ...
            // HibernateUtil.init();
            HibernateUtil.beginTransaction();
            try {
                // 2) ...
                updateSchema(expectedSchemaVersion);
                HibernateUtil.commitTransaction();
            }
            catch (Throwable t) {
                // ... 2) can't update the schema
                try {
                    HibernateUtil.rollbackTransaction();
                }
                catch (Throwable ex2) {
                    log.error("Error rolling back transaction: "+ex2.getLocalizedMessage(), ex2);
                }
                throw new Throwable("Error checking or updating db schema: "+t.getLocalizedMessage(), t);
            }
        }
        catch (Throwable t) {
            // ... 1) can't even begin a transaction
            throw new Throwable("Database connection error: "+t.getLocalizedMessage(), t);
        }
        finally {
            //in both cases, attempt to close the session
            try {
                HibernateUtil.closeCurrentSession();
            }
            catch (Exception e) {
                log.error("Exception thrown closing database connection: "+e.getLocalizedMessage(), e);
            }
        }
    }

    protected static List<String> appendIfNecessary(final List<String> argsList) {
        //prevent HSQLDB from calling System.exit when errors occur,
        //    which is the default behavior when starting the DB using Server.main
        if (!argsList.contains("-no_system_exit")) {
            List<String> updatedArgs=new ArrayList<String>(argsList);
            updatedArgs.add("-no_system_exit");
            updatedArgs.add("true");
            return updatedArgs;
        }
        return argsList;
    }
    
    protected static String[] tokenizeHsqlArgs(final String hsqlArgs) {
        StringTokenizer strTok = new StringTokenizer(hsqlArgs);
        List<String> argsList = new ArrayList<String>();
        //int i=0;
        while (strTok.hasMoreTokens()){
            String tok = strTok.nextToken();
            argsList.add(tok);
        }
        argsList=appendIfNecessary(argsList);
        String[] argsArray = new String[argsList.size()];
        argsArray = argsList.toArray(argsArray);
        return argsArray;
    }

    public static void shutdownDatabase() {
        try {
            HibernateUtil.beginTransaction();
            log.info("Checkpointing database");
            AnalysisDAO dao = new AnalysisDAO();
            dao.executeUpdate("CHECKPOINT");
            log.info("Checkpointed.");
            dao.executeUpdate("SHUTDOWN");
        }  
        catch (Throwable t) {
            t.printStackTrace();
            log.info("checkpoint database in StartupServlet.destroy", t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    private static void updateSchema(final String expectedSchemaVersion) 
    throws Exception 
    {
        log.debug("Updating schema...");
        if (!checkSchema(expectedSchemaVersion)) {
            createSchema(expectedSchemaVersion);
            if (!checkSchema(expectedSchemaVersion)) {
                log.error("schema didn't have correct version after creating");
                //throw new IOException("Unable to successfully update database tables.");
            }
        }
        log.debug("Updating schema...Done!");
    }

    /**
     * 
     * @param resourceDir
     * @param props
     * @return
     */
    private static boolean checkSchema(final String requiredSchemaVersion) throws Exception {
        boolean upToDate = false;
        String dbSchemaVersion = "";

        BaseDAO dao = new BaseDAO();

        // check schemaVersion
        try {
            ResultSet resultSet = dao.executeSQL("select value from props where key='schemaVersion'", false);
            if (resultSet.next()) {
                dbSchemaVersion = resultSet.getString(1);
                upToDate = (requiredSchemaVersion.compareTo(dbSchemaVersion) <= 0);
            }
            else {
                dbSchemaVersion = "";
                upToDate = false;
            }
        }
        catch (Exception e) {
            log.info("Database tables not found.  Create new database");
            dbSchemaVersion = "";
        }

        System.setProperty("dbSchemaVersion", dbSchemaVersion);
        log.info("schema up-to-date: " + upToDate + ": " + requiredSchemaVersion + " required, " + dbSchemaVersion + " current");
        return upToDate;
    }

    /**
     * @return the resources directory, or null if there are configuration errors
     */
    private static File getResourceDir() {
        String resourceDirProp = System.getProperty("resources");
        if (resourceDirProp == null || resourceDirProp.trim().length() == 0) {
            log.error("Missing required System property, 'resources': "+resourceDirProp);
            //TODO: throw exception
            return null;
        }
        File resourceDir = new File(resourceDirProp);
        if (!resourceDir.canRead()) {
            log.error("Configuration error: Can't read resources directory: "+resourceDir.getPath());
            //TODO: throw exception
            return null;
        }
        return resourceDir;
    }
    
    /**
     * 
     * @param resourceDir
     * @param props
     * @throws IOException
     */
    private static void createSchema(final String expectedSchemaVersion) {
        File resourceDir = getResourceDir();
        final String schemaPrefix = System.getProperty("HSQL.schema", "analysis_hypersonic-");
        FilenameFilter schemaFilenameFilter = new FilenameFilter() {
            // INNER CLASS !!!
            public boolean accept(File dir, String name) {
                return name.endsWith(".sql") && name.startsWith(schemaPrefix);
            }
        };
        File[] schemaFiles = resourceDir.listFiles(schemaFilenameFilter);
        Arrays.sort(schemaFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                String name1 = f1.getName();
                String version1 = name1.substring(schemaPrefix.length(), name1.length() - ".sql".length());
                String name2 = f2.getName();
                String version2 = name2.substring(schemaPrefix.length(), name2.length() - ".sql".length());
                return version1.compareToIgnoreCase(version2);
            }
        });
//        //for junit testing, if the property is not in ServerProperties, check System properties
//        if ("$GENEPATTERN_VERSION$".equals(expectedSchemaVersion)) {
//            log.info("expectedSchemaVersion="+expectedSchemaVersion+" (from ServerProperties)");
//            expectedSchemaVersion = System.getProperty("GenePatternVersion", expectedSchemaVersion);
//            log.info("expectedSchemaVersion="+expectedSchemaVersion+" (from System.getProperty)");
//        }
        String dbSchemaVersion = (String) System.getProperty("dbSchemaVersion");
        for (int f = 0; f < schemaFiles.length; f++) {
            File schemaFile = schemaFiles[f];
            String name = schemaFile.getName();
            String version = name.substring(schemaPrefix.length(), name.length() - ".sql".length());
            if (version.compareTo(expectedSchemaVersion) <= 0 && version.compareTo(dbSchemaVersion) > 0) {
                log.info("processing" + name + " (" + version + ")");
                processSchemaFile(schemaFile);
            }
            else {
                log.info("skipping " + name + " (" + version + ")");
            }
        }
        log.debug("createSchema ... Done!");
    }

    private static void processSchemaFile(File schemaFile) {
        log.info("updating database from schema " + schemaFile.getPath());
        String all = null;
        try {
            all = readFile(schemaFile);
        }
        catch (IOException e) {
            log.error("database not updated from schema, error reading schema " + schemaFile.getPath(), e);
            return;
        }
        while (!all.equals("")) {
            all = all.trim();
            int i = all.indexOf('\n');
            if (i == -1) i = all.length() - 1;
            if (all.startsWith("--")) {
                all = all.substring(i + 1);
                continue;
            }

            i = all.indexOf(';');
            String sql;
            if (i != -1) {
                sql = all.substring(0, i);
                all = all.substring(i + 1);
            }
            else {
                sql = all;
                all = "";
            }
            sql = sql.trim();
            log.info("apply SQL-> " + sql);
            BaseDAO dao = new BaseDAO();
            dao.executeUpdate(sql);
        }
        log.debug("updating database from schema ... Done!");
    }

    /**
     * @param file
     * @return the contents of the file as a String
     * @throws IOException from reader.read
     */
    private static String readFile(File file) throws IOException 
    {
        FileReader reader = null;
        StringBuffer b = new StringBuffer();
        try {
            reader = new FileReader(file);
            char buffer[] = new char[1024];
            while (true) {
                int i = reader.read(buffer, 0, buffer.length);
                if (i == -1) {
                    break;
                }
                b.append(buffer, 0, i);
            }
            return b.toString();
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException ioe) {
                }
            }
        }
    }

}
