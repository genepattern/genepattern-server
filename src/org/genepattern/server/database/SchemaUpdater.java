package org.genepattern.server.database;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.domain.PropsTable;
import org.genepattern.webservice.OmnigeneException;

import com.google.common.base.Strings;

/**
 * Automatically update the GenePattern database schema, usually called on server startup.
 * 
 * This is a re-factor of code from the HsqlDbUtil class circa GP <= 3.9.2 which loads 
 * matching database DDL scripts (analysis_<db_type>-<gp-version>.sql) from the resources directory.
 * 
 *  The main difference is that a HibernateSessionManager is passed as an argument to the helper methods
 *  to make it easier to test. 
 * 
 * @author pcarr
 *
 */
public class SchemaUpdater {
    private static final Logger log = Logger.getLogger(SchemaUpdater.class);

    protected static void updateSchema(final HibernateSessionManager mgr, final File resourceDir, final String schemaPrefix, final String expectedSchemaVersion) 
    throws DbException
    {
        try {
            mgr.beginTransaction();
        }
        catch (Throwable t) {
            // ... 1) can't even begin a transaction
            throw new DbException("Database connection error: "+t.getLocalizedMessage(), t);
        }
        try {
            innerUpdateSchema(mgr, resourceDir, schemaPrefix, expectedSchemaVersion);
            mgr.commitTransaction();
        }
        catch (DbException e) {
            mgr.rollbackTransaction();
            throw e;
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new DbException("Unexpected error initializing the databse: "+t.getLocalizedMessage(), t);
        }
        finally {
            try {
                mgr.closeCurrentSession();
            }
            catch (Exception e) {
                log.error("Exception thrown closing database connection: "+e.getLocalizedMessage(), e);
            }
        }
    }
    
    private static void innerUpdateSchema(final HibernateSessionManager sessionMgr, final File resourceDir, final String schemaPrefix, final String expectedSchemaVersion) 
    throws DbException 
    {
        final String dbSchemaVersion=getDbSchemaVersion(sessionMgr);
        boolean upToDate = false;
        if (!Strings.isNullOrEmpty(dbSchemaVersion)) {
            upToDate = (expectedSchemaVersion.compareTo(dbSchemaVersion) <= 0);
        }
        log.info("schema up-to-date: " + upToDate + ": " + expectedSchemaVersion + " required, " + dbSchemaVersion + " current");
        if (upToDate) {
            return;
        }

        // run all new DDL scripts to bring the DB up to date with the GP version
        log.info("Updating schema...");
        createSchema(sessionMgr, resourceDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);

        // validate that the DB version is up to date
        final String updatedSchemaVersion=getDbSchemaVersion(sessionMgr);
        if (!Strings.isNullOrEmpty(updatedSchemaVersion)) {
            upToDate = (expectedSchemaVersion.compareTo(updatedSchemaVersion) <= 0);
        }
        if (!upToDate) {
            log.error("schema didn't have correct version after creating");
            throw new DbException("schema didn't have correct version after creating, expected="+expectedSchemaVersion+", actual="+updatedSchemaVersion);
        }
        log.info("Updating schema...Done!");
    }

    /**
     * Query the database for the current schema version recorded in the database.
     * @return
     */
    protected static String getDbSchemaVersion(final HibernateSessionManager sessionMgr) throws DbException {
        String dbSchemaVersion="";
        final boolean isInTransaction=sessionMgr.isInTransaction();
        try {
            if (!isInTransaction) {
                sessionMgr.beginTransaction();
            }

            boolean e=tableExists(sessionMgr, "PROPS");
            if (!e) {
                e=tableExists(sessionMgr, "props");
            }
            if (!e) {
                log.info("No 'props' table in database. Create new database.");
                dbSchemaVersion="";
            }
            else {
                dbSchemaVersion=PropsTable.selectValue(sessionMgr, "schemaVersion");
            }
        }
        catch (Throwable t) {
            throw new DbException("Unexpected error getting dbSchemaVersion: "+t.getLocalizedMessage(), t);
        }
        finally {
            if (!isInTransaction) {
                sessionMgr.closeCurrentSession();
            }
        }
        log.info("Current dbSchemaVersion: "+dbSchemaVersion);
        return dbSchemaVersion;
    }

    protected static boolean tableExists(final HibernateSessionManager sessionMgr, String tableName) {
        final boolean isInTransaction=sessionMgr.isInTransaction();
        try {
            sessionMgr.beginTransaction();
            DatabaseMetaData md =
                    sessionMgr.getSession().connection().getMetaData();
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
                sessionMgr.closeCurrentSession();
            }
        }
        return false;
    }

    private static void createSchema(final HibernateSessionManager sessionMgr, final File resourceDir, final String schemaPrefix, final String expectedSchemaVersion, final String dbSchemaVersion) 
    throws DbException
    {
        List<File> schemaFiles=HsqlDbUtil.listSchemaFiles(resourceDir, schemaPrefix, expectedSchemaVersion, dbSchemaVersion);
        for(final File schemaFile : schemaFiles) {
            processSchemaFile(sessionMgr, schemaFile);
        }
    }

    protected static void processSchemaFile(final HibernateSessionManager sessionMgr, final File schemaFile) throws DbException {
        log.info("updating database from schema " + schemaFile.getPath());
        String all = null;
        try {
            all = HsqlDbUtil.readFile(schemaFile);
        }
        catch (IOException e) {
            log.error("Error reading schema file=" + schemaFile.getPath(), e);
            throw new DbException("Error reading schema file="+schemaFile.getPath(), e);
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
                //BaseDAO dao = new BaseDAO();
                executeUpdate(sessionMgr, sql);
            }
            catch (Throwable t) {
                log.error("Error processing SQL in schemaFile="+schemaFile);
                log.error("sql: "+sql, t);
                throw new DbException("Error processing SQL in schemaFile="+schemaFile+
                        "\n\t"+t.getLocalizedMessage()+
                        "\n\t"+sql);
            }
        }
        log.debug("updating database from schema ... Done!");
    }
    
    /**
     * execute arbitrary SQL on database, returning int
     * 
     * @param sql
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int number of rows returned
     */
    private static int executeUpdate(final HibernateSessionManager sessionMgr, final String sql) throws SQLException 
    {
        sessionMgr.getSession().flush();
        sessionMgr.getSession().clear();
    
        Statement updateStatement = null;
    
        try {
            updateStatement = sessionMgr.getSession().connection().createStatement();
            return updateStatement.executeUpdate(sql);
        }
        finally {
            if (updateStatement != null) {
                updateStatement.close();
            }
        }
    }

}
