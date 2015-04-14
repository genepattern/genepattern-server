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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
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
    
    public static void updateSchema(final File resourceDir, final String schemaPrefix, final String expectedSchemaVersion) throws Throwable {
        SchemaUpdater.updateSchema(HibernateUtil.instance(), resourceDir, schemaPrefix, expectedSchemaVersion);
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
        shutdownDatabase(HibernateUtil.instance());
    }

    public static void shutdownDatabase(final HibernateSessionManager mgr) {
        try {
            log.info("Shutting down HSQL database ...");
            mgr.beginTransaction();
            log.info("Checkpointing database ...");
            HibernateUtil.executeSQL(mgr, "CHECKPOINT");
            log.info("Checkpointed.");
            HibernateUtil.executeSQL(mgr, "SHUTDOWN");
        }
        catch (DbException e) {
            log.error("Error shutting down database: "+e.getLocalizedMessage(), e);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    /**
     * Get the list of schema files to process for the given schemaPrefix, e
     * @return
     */
    protected static List<File> listSchemaFiles(final File resourceDir, final String schemaPrefix, final String expectedSchemaVersion, final String dbSchemaVersion) {
        log.debug("listing schema files ... ");
        List<File> rval=new ArrayList<File>();
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
            if (expectedSchemaVersion==null || (version.compareTo(expectedSchemaVersion) <= 0 && version.compareTo(dbSchemaVersion!=null?dbSchemaVersion:"") > 0)) {
                log.debug("adding " + name + " (" + version + ")");
                rval.add(schemaFile);
            }
            else {
                log.debug("skipping " + name + " (" + version + ")");
            }
        }
        log.debug("listing schema files ... Done!");
        return rval;
    }

    /**
     * @param file
     * @return the contents of the file as a String
     * @throws IOException from reader.read
     */
    protected static String readFile(File file) throws IOException 
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
