package org.genepattern.server.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.dao.BaseDAO;

public class HsqlDbUtil {

    private static Logger log = Logger.getLogger(HsqlDbUtil.class);

    /**
     * 
     */
    public static void startDatabase() {
        // @todo - get from properites file
        String port = System.getProperty("HSQL.port", "9001");
        String dbFile = System.getProperty("HSQL.dbfile", "../resources/GenePatternDB");
        String dbUrl = "file:" + dbFile;
        String dbName = System.getProperty("HSQL.dbName", "xdb");
        String[] args = new String[] { "-port", port, "-database.0", dbUrl, "-dbname.0", dbName };
        org.hsqldb.Server.main(args);

        HibernateUtil.getSession().beginTransaction();
        updateSchema();
        HibernateUtil.getSession().getTransaction().commit();
    }

    public static void shutdownDatabase() {
        
        try {
            HibernateUtil.beginTransaction();
            log.info("Checkpointing database");
            AnalysisDAO dao = new AnalysisDAO();
            dao.executeUpdate("CHECKPOINT");
            log.info("Checkpointed.");
            dao.executeUpdate("SHUTDOWN");
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            t.printStackTrace();
            log.info("checkpoint database in StartupServlet.destroy", t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }


    }

    private static void updateSchema() {

        try {
            String resourceDir = new File(System.getProperty("resources")).getCanonicalPath();
            log.debug("resourcesDir=" + new File(resourceDir).getCanonicalPath());

            if (!checkSchema(resourceDir)) {

                createSchema(resourceDir);

                if (!checkSchema(resourceDir)) {
                    System.err.println("schema didn't check after creating");

                    throw new IOException("unable to successfully create task_master table. Other tables also suspect.");
                }
            }
        }
        catch (IOException e) {
            log.error(e);
        }


    }

    /**
     * 
     * @param resourceDir
     * @param props
     * @return
     */
    private static boolean checkSchema(String resourceDir) {
        log.debug("checking schema");
        boolean upToDate = false;
        String dbSchemaVersion = "";
        String requiredSchemaVersion = System.getProperty("GenePatternVersion");
        // check schemaVersion

        String sql = "select value from props where key='schemaVersion'";

        try {
            BaseDAO dao = new BaseDAO();
            ResultSet resultSet = dao.executeSQL(sql, false);
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
            // 
            log.info("Database tables not found.  Create new database");
            dbSchemaVersion = "";
        }

        System.setProperty("dbSchemaVersion", dbSchemaVersion);
        System.out.println("schema up-to-date: " + upToDate + ": " + requiredSchemaVersion + " required, "
                + dbSchemaVersion + " current");
        return upToDate;
    }

    /**
     * 
     * @param resourceDir
     * @param props
     * @throws IOException
     */
    private static void createSchema(String resourceDir) throws IOException {
        final String schemaPrefix = System.getProperty("HSQL.schema", "analysis_hypersonic-");
        File[] schemaFiles = new File(resourceDir).listFiles(new FilenameFilter() {
            // INNER CLASS !!!
            public boolean accept(File dir, String name) {
                return name.endsWith(".sql") && name.startsWith(schemaPrefix);
            }
        });
        Arrays.sort(schemaFiles, new Comparator() {
            public int compare(Object o1, Object o2) {
                File f1 = (File) o1;
                File f2 = (File) o2;
                String name1 = f1.getName();
                String version1 = name1.substring(schemaPrefix.length(), name1.length()
                        - ".sql".length());
                String name2 = f2.getName();
                String version2 = name2.substring(schemaPrefix.length(), name2.length()
                        - ".sql".length());
                return version1.compareToIgnoreCase(version2);
            }
        });
        String expectedSchemaVersion = System.getProperty("GenePatternVersion");
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
    }

    protected static void processSchemaFile(File schemaFile) throws IOException {
        log.info("updating database from schema " + schemaFile.getCanonicalPath());
        String all = readFile(schemaFile);
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
            try {
                log.debug("-> " + sql);
                (new BaseDAO()).executeUpdate(sql);
            }
            catch (Exception se) {
                log.error(se);
            }
        }
    }

    static String readFile(File file) throws IOException {
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
        }
        catch (IOException ioe) {
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
        return b.toString();
    }

    public static void main(String args[]) {

        startDatabase();
        try {
            System.in.read();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
