package org.genepattern.server.webservice.server.dao;

import java.io.*;
import java.sql.*;
import java.util.Calendar;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.StatelessSession;

public class BaseDAO {

    private static Logger log = Logger.getLogger(TaskIntegratorHSQLDAO.class);

    public static final int UNPROCESSABLE_TASKID = -1;

    public int PROCESSING_STATUS = 2;

    public static int JOB_WAITING_STATUS = 1;

    private static String dbURL;

    private static String dbUsername;

    private static String dbPassword;

    public static String filenameFromURL(String url) {
        int idx = url.lastIndexOf("/");
        if (idx >= 0)
            return url.substring(idx + 1);
        else
            return url;
    }

    public java.sql.Date now() {
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

    /**
     * @deprecated
     * @param rs
     * @param st
     * @param conn
     */
    protected void close(ResultSet rs, Statement st, Connection conn) {
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
        try {
            if (conn != null) {
                conn.close();
            }
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected Session getSession() {
        return HibernateUtil.getSession();
    }

    /**
     * @deprecated
     * @return
     */
    protected Connection getConnection() {
        return getSession().connection();
    }

}
