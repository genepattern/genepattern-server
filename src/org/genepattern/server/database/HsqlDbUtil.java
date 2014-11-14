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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.domain.Props;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.dao.BaseDAO;
import org.hsqldb.Server;

import com.google.common.base.Strings;

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
     * Start the database, initializing the DB schema if necessary.
     * 
     * @param hsqlArgs, default value, HSQL.args= -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb 
     * @param expectedSchemaVersion, the version of GP defined in the genepattern.properties file, default value, GenePatternVersion=3.9.1
     * @throws Throwable
     */
    public static void startDatabase(final String hsqlArgs) throws Throwable {
        String[] argsArray = tokenizeHsqlArgs(hsqlArgs);
        startDatabase(argsArray);
    }

    /**
     * Start the database, initializing the DB schema if necessary.
     * 
     * @param hsqlArgs, default value, HSQL.args= -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb 
     * @param expectedSchemaVersion, the version of GP defined in the genepattern.properties file, default value, GenePatternVersion=3.9.1
     * @throws Throwable
     */
    public static void startDatabase(final String[] hsqlArgs) throws Throwable {
        log.debug("Starting HSQL Database...");
        Server.main(hsqlArgs);
    }
    
    /**
     * On server startup, start a Hibernate transaction and run all necessary DDL scripts 
     * to update the database schema to match the current GenePattern version.
     * 
     * @param expectedSchemaVersion
     * @throws Throwable
     */
    public static void updateSchema(final File resourceDir, final String schemaPrefix, final String expectedSchemaVersion) throws Throwable {
        try {
            // 1) ...
            HibernateUtil.beginTransaction();
            try {
                // 2) ...
                innerUpdateSchema(resourceDir, schemaPrefix, expectedSchemaVersion);
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

    private static void innerUpdateSchema(final File resourceDir, final String schemaPrefix, final String expectedSchemaVersion) 
    throws Exception 
    {
        final String dbSchemaVersion=getDbSchemaVersion();
        boolean upToDate = false;
        if (!Strings.isNullOrEmpty(dbSchemaVersion)) {
            upToDate = (expectedSchemaVersion.compareTo(dbSchemaVersion) <= 0);
        }
        log.info("schema up-to-date: " + upToDate + ": " + expectedSchemaVersion + " required, " + dbSchemaVersion + " current");
        if (upToDate) {
            return;
        }

        // run all new DDL scripts to bring the DB up to date with the GP version
        log.debug("Updating schema...");
        createSchema(resourceDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);

        // validate that the DB version is up to date
        final String updatedSchemaVersion=getDbSchemaVersion();
        if (!Strings.isNullOrEmpty(updatedSchemaVersion)) {
            upToDate = (expectedSchemaVersion.compareTo(updatedSchemaVersion) <= 0);
        }
        if (!upToDate) {
            log.error("schema didn't have correct version after creating");
        }
        log.debug("Updating schema...Done!");
    }

    protected static boolean tableExists(String tableName) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            DatabaseMetaData md = HibernateUtil.getSession().connection().getMetaData();
            ResultSet rs=md.getTables(null, null, tableName, null);
            if (rs.next()) {
                return true;
            }
        }
        catch (Throwable t) {
            log.error("Unexpected error checking if tableExists for tableName="+tableName, t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return false;
    }
    
    /**
     * Query the database for the current schema version recorded in the database.
     * @return
     */
    protected static String getDbSchemaVersion() {
        boolean e=tableExists("PROPS");
        if (!e) {
            return "";
        }
        
        String dbSchemaVersion;
        try {
            dbSchemaVersion=Props.selectValue("schemaVersion");
        }
        catch (Throwable t) {
            log.info("Database tables not found.  Create new database");
            dbSchemaVersion="";
        }
        log.info("Current dbSchemaVersion: "+dbSchemaVersion);
        return dbSchemaVersion;
    }

    /**
     * 
     * @throws exception
     */
    private static void createSchema(final File resourceDir, final String schemaPrefix, final String expectedSchemaVersion, final String dbSchemaVersion) {
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
            try {
                BaseDAO dao = new BaseDAO();
                dao.executeUpdate(sql);
            }
            catch (Throwable t) {
                log.error("Error processing SQL in schemaFile="+schemaFile);
                log.error("sql: "+sql, t);
            }
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
