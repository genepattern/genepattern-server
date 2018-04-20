package org.genepattern.server.webservice.server.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import org.genepattern.server.database.HibernateSessionManager;

public class UsageStatsDAO extends BaseDAO {
    private static final Logger log = Logger.getLogger(UsageStatsDAO.class);

    public UsageStatsDAO(HibernateSessionManager mgr) {
        super(mgr);
    }
    
    public int getRegistrationCountBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        Integer count = null;
        SimpleDateFormat df = new SimpleDateFormat("");
       
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement("select count (USER_ID) from gp_user where registration_date BETWEEN ? and ?  ");
              
        pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
          
        ResultSet rs = null;
        try {
            rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return count;
    }
    
    public int getTotalRegistrationCount(String userExclusionClause) throws Exception {
        Integer count = null;
        StringBuffer sqlBuff = new StringBuffer("select count (USER_ID) from gp_user");
        ResultSet rs = null;
        try {
            rs = this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return count;
    }
    
    public int getTotalJobsRunCount(String userExclusionClause) throws Exception {
        Integer count = null;
        StringBuffer sqlBuff = new StringBuffer("select count(*) from analysis_job");
        ResultSet rs = null;
        try {
            rs = this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return count;
    }
    
    
    public int getJobsRunCountBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        Integer count = null;
        SimpleDateFormat df = new SimpleDateFormat("");
       
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement("select count (*) from analysis_job where date_submitted BETWEEN ? and ?  ");
              
        pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
          
        ResultSet rs = null;
        try {
            rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return count;
    }
    
    
    public int getReturnLoginCountBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        Integer count = null;
        SimpleDateFormat df = new SimpleDateFormat("");
       
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection()
                .prepareStatement("select count(USER_ID) from gp_user where  (NOT (registration_date BETWEEN ? and ?)) and (last_login_date between ? and ?) ");
        
        java.sql.Date startDateSql = new java.sql.Date(startDate.getTime());
        java.sql.Date endDateSql = new java.sql.Date(endDate.getTime());
        pstmt.setDate(1, startDateSql);
        pstmt.setDate(2, endDateSql);
        pstmt.setDate(3, startDateSql);
        pstmt.setDate(4, endDateSql);
           
        ResultSet rs = null;
        try {
            rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return count;
    }
    
    public int getNewLoginCountBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        Integer count = null;
        SimpleDateFormat df = new SimpleDateFormat("");
       
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection()
                .prepareStatement("select count(USER_ID) from gp_user where  ( (registration_date BETWEEN ? and ?)) and (last_login_date between ? and ?) ");
        
        java.sql.Date startDateSql = new java.sql.Date(startDate.getTime());
        java.sql.Date endDateSql = new java.sql.Date(endDate.getTime());
        pstmt.setDate(1, startDateSql);
        pstmt.setDate(2, endDateSql);
        pstmt.setDate(3, startDateSql);
        pstmt.setDate(4, endDateSql);
           
        ResultSet rs = null;
        try {
            rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return count;
    }

    
    public void test1(Date startDate, Date endDate) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("");
       
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection()
                .prepareStatement("select USER_ID, last_login_date from gp_user where  ( (registration_date BETWEEN ? and ?)) and (last_login_date between ? and ?) ");
               
    
        java.sql.Date startDateSql = new java.sql.Date(startDate.getTime());
        java.sql.Date endDateSql = new java.sql.Date(endDate.getTime());
        pstmt.setDate(1, startDateSql);
        pstmt.setDate(2, endDateSql);
        pstmt.setDate(3, startDateSql);
        
        
        pstmt.setDate(4, endDateSql);
        ResultSet rs = null;
        try {
            rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
            while (rs.next()) {
                System.err.println( rs.getString(1) + " == " + rs.getString(2) + "\n  ");
                
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return ;
    }
    
   
    
}
