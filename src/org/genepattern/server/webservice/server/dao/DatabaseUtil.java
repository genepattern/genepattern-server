package org.genepattern.server.webservice.server.dao;

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
import org.genepattern.server.util.HibernateUtil;


public class DatabaseUtil {

    private static Logger log = Logger.getLogger(DatabaseUtil.class);

    public static void startDatabase() {
        // @todo - get from properites file
        String[] args = new String[] { "-port", "9001", "-database.0", "file:../resources/GenePatternDB", "-dbname.0",
                "xdb" };
        org.hsqldb.Server.main(args);
        HibernateUtil.getSession().beginTransaction();
        updateSchema();
        HibernateUtil.getSession().getTransaction().commit();
    }

    public static void shutdownDatabase() {
        BaseDAO dao = new BaseDAO();
        dao.executeUpdate("SHUTDOWN COMPACT");

    }

    private static void updateSchema() {
        /*       try {
            String resourceDir = new File(System.getProperty("resources")).getCanonicalPath();
            log.debug("resourcesDir=" + new File(resourceDir).getCanonicalPath());

            Properties props = loadProps(resourceDir);
            log.debug(props);

            System.setProperty("genepattern.properties", resourceDir);
            System.setProperty("omnigene.conf", resourceDir);

            if (!checkSchema(resourceDir, props)) {

                createSchema(resourceDir, props);

                if (!checkSchema(resourceDir, props)) {
                    System.err.println("schema didn't check after creating");

                    throw new IOException(
                            "unable to successfully create task_master table.  Other tables also suspect.");

                }
            }
        } catch (IOException e) {
            log.error(e);
        }*/

    }

    private static Properties loadProps(String propsDir) throws IOException {
        File propFile = new File(propsDir, "genepattern.properties");
        FileInputStream fis = null;
        Properties props = new Properties();
        try {
            fis = new FileInputStream(propFile);
            props.load(fis);
        } catch (IOException ioe) {
            throw new IOException("InstallTasks.loadProps: " + propFile.getCanonicalPath() + " cannot be loaded.  "
                    + ioe.getMessage());
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException ioe) {
            }
        }
        return props;
    }

    /**
     * 
     * @param resourceDir
     * @param props
     * @return
     */
    private static boolean checkSchema(String resourceDir, Properties props) {
        log.debug("checking schema");
        boolean upToDate = false;
        String dbSchemaVersion = "";
        String requiredSchemaVersion = props.getProperty("GenePatternVersion");
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
        } catch (Exception e) {
            // 
            log.info("Database tables not found.  Create new database");
            dbSchemaVersion = "";
        }

        props.setProperty("dbSchemaVersion", dbSchemaVersion);
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
    private static void createSchema(String resourceDir, final Properties props) throws IOException {
        File[] schemaFiles = new File(resourceDir).listFiles(new FilenameFilter() {
            // INNER CLASS !!!
            public boolean accept(File dir, String name) {
                return name.endsWith(".sql") && name.startsWith(props.getProperty("DB.schema"));
            }
        });
        Arrays.sort(schemaFiles, new Comparator() {
            public int compare(Object o1, Object o2) {
                File f1 = (File) o1;
                File f2 = (File) o2;
                String name1 = f1.getName();
                String version1 = name1.substring(props.getProperty("DB.schema").length(), name1.length()
                        - ".sql".length());
                String name2 = f2.getName();
                String version2 = name2.substring(props.getProperty("DB.schema").length(), name2.length()
                        - ".sql".length());
                return version1.compareToIgnoreCase(version2);
            }
        });
        String expectedSchemaVersion = props.getProperty("GenePatternVersion");
        String dbSchemaVersion = (String) props.remove("dbSchemaVersion");
        for (int f = 0; f < schemaFiles.length; f++) {
            File schemaFile = schemaFiles[f];
            String name = schemaFile.getName();
            String version = name.substring(props.getProperty("DB.schema").length(), name.length() - ".sql".length());
            if (version.compareTo(expectedSchemaVersion) <= 0 && version.compareTo(dbSchemaVersion) > 0) {
                log.info("processing" + name + " (" + version + ")" );
                processSchemaFile(schemaFile);
            }
            else {
                log.info("skipping " + name + " (" + version + ")");
            }
        }
    }

    protected static void processSchemaFile(File schemaFile) throws  IOException {
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
            } catch (Exception se) {
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
        } catch (IOException ioe) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
            }
        }
        return b.toString();
    }

    public static void main(String args[]) {

        startDatabase();
        try {
            System.in.read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
