/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.dao;

import java.io.File;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.SuiteInfo;

import org.hibernate.Session;

public class BaseDAO {
    private static final Logger log = Logger.getLogger(BaseDAO.class);

    public static final int UNPROCESSABLE_TASKID = -1;

    protected final HibernateSessionManager mgr;

    /** @deprecated */
    public BaseDAO() {
        this(null);
    }
    
    /**
     * Constructor.  Conditionally starts a transaction,  if a transaction is already underway
     * the call to beginTransaction does nothing.
     *
     */
    public BaseDAO(final HibernateSessionManager mgrIn) {
        if (mgrIn==null) {
            this.mgr=HibernateUtil.instance();
        }
        else {
            this.mgr=mgrIn;
        }
        this.mgr.beginTransaction();
    }

    
    protected Session getSession() {
        return mgr.getSession();
    }

    protected java.sql.Date now() {
        return new java.sql.Date(Calendar.getInstance().getTimeInMillis());
    }

    protected void cleanupJDBC(ResultSet rs, Statement st) {
        if (rs != null) {
            try {
                rs.close();
            }
            catch (SQLException x) {
            }
        }
        if (st != null) {
            try {
                st.close();
            }
            catch (SQLException x) {
            }
        }

    }

    protected void cleanupJDBC(ResultSet rs, Statement st, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            }
            catch (SQLException x) {
                log.error(x);
            }
        }
        if (st != null) {
            try {
                st.close();
            }
            catch (SQLException x) {
                log.error(x);
            }
        }
        if (conn != null)
            try {
                conn.close();
            }
        catch (SQLException x) {
            log.error(x);
        }

    }

    protected SuiteInfo suiteInfoFromSuite(final Suite suite) throws OmnigeneException {
        String lsid = suite.getLsid();
        int access_id = suite.getAccessId();
        String name = suite.getName();
        String description = suite.getDescription();
        String owner = suite.getUserId();
        String author = suite.getAuthor();

        final ArrayList<String> docs = new ArrayList<String>();
        try {
            boolean alwaysMkdirs=false;
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            final GpContext serverContext=GpContext.getServerContext();
            File suiteDir = DirectoryManager.getSuiteLibDir(gpConfig, serverContext, name, lsid, owner, alwaysMkdirs);
            if (suiteDir != null && suiteDir.exists()) {
                File docFiles[] = suiteDir.listFiles();
                for (int i = 0; i < docFiles.length; i++) {
                    File f = docFiles[i];
                    docs.add(f.getName());
                }
            }
        }
        catch (Exception e) {
            // swallow & no docs
            log.error(e);
        }

        final List<String> mods = suite.getModules();

        SuiteInfo suiteInfo = new SuiteInfo(lsid, name, description, author, owner, mods, access_id, docs);
        suiteInfo.setContact(suite.getContact());

        return suiteInfo;

    }

    /**
     * execute arbitrary SQL on database, returning ResultSet
     * 
     * @param sql
     * @throws OmnigeneException
     * @throws RemoteException
     * @return ResultSet
     */
    public ResultSet executeSQL(String sql) throws OmnigeneException {
        return executeSQL(sql, true);
    }

    public ResultSet executeSQL(String sql, boolean logError) throws OmnigeneException {
    
        Statement stat = null;
        ResultSet resultSet = null;
    
        try {
            Connection conn = getSession().connection();
            stat = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            resultSet = stat.executeQuery(sql);
            CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
            crs.populate(resultSet);
            return crs;
        }
        catch (Exception e) {
            if(logError) log.error("AnalysisHypersonicDAO: executeSQL for " + sql + " failed ", e);
            throw new OmnigeneException(e.getMessage());
        }
    
        finally {
            cleanupJDBC(resultSet, stat);
        }
    
    }

    /**
     * execute arbitrary SQL on database, returning int
     * 
     * @param sql
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int number of rows returned
     */
    public int executeUpdate(String sql) throws SQLException {
        getSession().flush();
        getSession().clear();
    
        Statement updateStatement = null;
    
        try {
            updateStatement = getSession().connection().createStatement();
            return updateStatement.executeUpdate(sql);
        }
        finally {
            cleanupJDBC(null, updateStatement);
        }
    }

}
