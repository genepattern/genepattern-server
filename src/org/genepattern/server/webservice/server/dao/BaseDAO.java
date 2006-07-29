package org.genepattern.server.webservice.server.dao;

import java.io.*;
import java.sql.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.webservice.*;
import org.hibernate.*;

public class BaseDAO {

    private static Logger log = Logger.getLogger(TaskIntegratorDAO.class);

    public static final int UNPROCESSABLE_TASKID = -1;

    public int PROCESSING_STATUS = 2;

    public static int JOB_WAITING_STATUS = 1;

    protected Session getSession() {
        return HibernateUtil.getSession();
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

    protected static TaskInfo taskInfoFromTaskMaster(TaskMaster tm) {
        return new TaskInfo(tm.getTaskId(), tm.getTaskName(), tm.getDescription(), tm.getParameterInfo(),
                TaskInfoAttributes.decode(tm.getTaskinfoattributes()), tm.getUserId(), tm.getAccessId());

    }

    protected SuiteInfo suiteInfoFromSuite(Suite suite) throws AdminDAOSysException {

        String lsid = suite.getLsid();
        int access_id = suite.getAccessId();
        String name = suite.getName();
        String description = suite.getDescription();
        String owner = suite.getOwner();
        String author = suite.getAuthor();

        ArrayList docs = new ArrayList();
        try {
            String suiteDirStr = DirectoryManager.getSuiteLibDir(name, lsid, owner);
            File suiteDir = new File(suiteDirStr);

            if (suiteDir.exists()) {
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

        ArrayList mods = getSuiteModules(lsid);

        SuiteInfo suiteInfo = new SuiteInfo(lsid, name, description, author, owner, mods, access_id, docs);

        return suiteInfo;

    }

    private ArrayList getSuiteModules(String lsid) throws AdminDAOSysException {
        PreparedStatement st = null;
        ResultSet rs = null;
        ArrayList moduleLSIDs = new ArrayList();

        try {

            st = getSession().connection().prepareStatement("SELECT * FROM suite_modules where lsid =?");
            st.setString(1, lsid);
            rs = st.executeQuery();
            while (rs.next()) {
                String modlsid = rs.getString("module_lsid");
                moduleLSIDs.add(modlsid);
            }
        }
        catch (SQLException e) {
            throw new AdminDAOSysException("A database error occurred", e);
        }
        finally {
            this.cleanupJDBC(rs, st);
        }
        return moduleLSIDs;
    }

 

}
