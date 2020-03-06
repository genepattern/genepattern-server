package org.genepattern.server.webservice.server.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class UsageStatsDAO extends BaseDAO {
    private static final Logger log = Logger.getLogger(UsageStatsDAO.class);
    private static String analysisJobTableOrView = "analysis_job_total";
    
    
    public UsageStatsDAO(HibernateSessionManager mgr) {
        super(mgr);
    }
    
    public String generateUserExclusionClause(String userOrEmailList){
        StringBuffer sqlBuff = new StringBuffer("select USER_ID from gp_user where (USER_ID  in "+userOrEmailList +") or (EMAIL in "+userOrEmailList+")" );
        ResultSet rs = null;
        StringBuffer buff = new StringBuffer(" and ( USER_ID not in (");
        try {
            rs = this.executeSQL(sqlBuff.toString());
            while (rs.next()) {
                String userId = rs.getString(1);
                buff.append("\'");
                buff.append(userId);
                buff.append("\'");
                if (! rs.isLast())
                   buff.append(", ");
            }
        } catch (SQLException sqle){
            sqle.printStackTrace();
            return "";
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException e){
                e.printStackTrace();
                return "";
            }
        }
        buff.append(") ) ");
        String str = buff.toString();
        return str;
        
    }
    
    public int getRegistrationCountBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        Integer count = null;
        ResultSet rs = null;
        
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement("select count(USER_ID) from gp_user where (registration_date BETWEEN ? and ?)  "+ userExclusionClause);
             
        pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
          
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
    
    public JSONArray getUserRegistrationsBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        JSONArray users = new JSONArray();
        ResultSet rs = null;
        
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement("select USER_ID, EMAIL from gp_user where (registration_date BETWEEN ? and ?)  "+ userExclusionClause);
        pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
          
        try {
            rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
            while (rs.next()) {
                JSONObject user = new JSONObject();
                user.put("user_id",rs.getString(1));
                user.put("email",rs.getString(2));
               
                users.put(user);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return users;
    }
    
    
    public int getTotalRegistrationCount(String userExclusionClause) throws Exception {
        Integer count = null;
        StringBuffer sqlBuff = new StringBuffer("select count(USER_ID) from gp_user where (USER_ID <> '') "+ userExclusionClause);
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
        StringBuffer sqlBuff = new StringBuffer("select count(*) from "+analysisJobTableOrView+" where (USER_ID <> '') "+ userExclusionClause);
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
      
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement("select count(*) from "+analysisJobTableOrView+" where (date_completed BETWEEN ? and ?)  "+ userExclusionClause);
              
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
    
    
    public int getInternalJobsRunCountBetweenDates(Date startDate, Date endDate, String userExclusionClause, String internalDomain) throws Exception {
        Integer count = null;
        
          @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement(
                "select count(*) from "+analysisJobTableOrView+" where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
                + " and (user_id in ( select user_id from GP_USER where email like '%"+internalDomain+"'))");
              
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
    
    public int getExternalJobsRunCountBetweenDates(Date startDate, Date endDate, String userExclusionClause, String internalDomain) throws Exception {
        Integer count = null;
        
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement(
                "select count(*) from "+analysisJobTableOrView+" where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
                + " and (user_id not in ( select user_id from GP_USER where email like '%"  + internalDomain +"'))");
              
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
    
    
    public JSONArray getModuleRunCountsBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
         JSONArray moduleCounts = new JSONArray();
        
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement(
                "select task_name, count(*) as MC from "+analysisJobTableOrView+" AJ where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
                + " GROUP BY TASK_NAME order by MC desc");
              
        pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
          
        ResultSet rs = null;
        try {
            rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
            while (rs.next()) {
                JSONObject module = new JSONObject();
                module.put("moduleName",rs.getString(1));
                module.put("jobsRun",rs.getInt(2));
               
                moduleCounts.put(module);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return moduleCounts;
    }
   
    
    public JSONArray getModuleRunCountsBetweenDatesByDomain(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        JSONArray moduleCounts = new JSONArray();
        GpConfig gpConfig=ServerConfigurationFactory.instance();
        
        //  select count (*), dd 
        // from ( select  substr(GPU.email, INSTR(GPU.email, '@')+1) as dd, AJ.JOB_NO   
        //        from analysis_job AJ, GP_USER GPU where GPU.USER_ID like 't%' and GPU.USER_ID = AJ.USER_ID  
        //  ) group by dd
        
       String altExclusion = userExclusionClause.toLowerCase().replace("user_id", "gpu.user_id") ;
        
       //@SuppressWarnings("deprecation")
       /* PreparedStatement pstmt = getSession().connection().prepareStatement(
               "select count (*), dd from ( select substr(GPU.email, INSTR(GPU.email, '@')+1) as dd, AJ.JOB_NO "
               + " from analysis_job AJ, GP_USER GPU " 
               + " where (AJ.date_submitted BETWEEN ? and ?)  "  + altExclusion 
               + " and GPU.USER_ID = AJ.USER_ID"
               + ") GROUP BY dd ");
               */
       java.sql.Date startsql = new java.sql.Date(startDate.getTime());
       java.sql.Date endsql = new java.sql.Date(endDate.getTime());
       
       String oracle_frag = " where (AJ.date_completed BETWEEN TO_DATE('"+startsql.toString() +"', 'YYYY-MM-DD') and TO_DATE('"+endsql.toString()+"', 'YYYY-MM-DD') )  "   ;
       String mysql_frag = " where (AJ.date_completed BETWEEN STR_TO_DATE('"+startsql.toString() +"', '%Y-%m-%d') and STR_TO_DATE('"+endsql.toString()+"', '%Y-%m-%d') )  ";
       
       String date_clause = mysql_frag;
       if (gpConfig.getDbVendor().equalsIgnoreCase("MYSQL")){
           date_clause = mysql_frag;
       } else {
           date_clause = oracle_frag;
       }
       
       
       String sql =
               "select dd, count(*) from ( select substr(GPU.email, INSTR(GPU.email, '@')+1) as dd, AJ.JOB_NO "
               + " from "+analysisJobTableOrView+" AJ, GP_USER GPU " 
               + date_clause  + altExclusion 
               + " and GPU.USER_ID = AJ.USER_ID"
               + ") GROUP BY dd ";
 
       //  oracle and HSQL
       //    + " where (AJ.date_submitted BETWEEN TO_DATE('"+startsql.toString() +"', 'YYYY-MM-DD') and TO_DATE('"+endsql.toString()+"', 'YYYY-MM-DD') )  "  + altExclusion 

       //
       
       ResultSet rs = null;
       try {
           // rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
           rs = this.executeSQL(sql);
           while (rs.next()) {
               JSONObject module = new JSONObject();
               module.put("domain",rs.getString(1));
               module.put("jobsRun",rs.getInt(2));
              
               moduleCounts.put(module);
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       return moduleCounts;
   }
    
    
    public JSONArray getModuleErrorCountsBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        JSONArray moduleCounts = new JSONArray();
       
       @SuppressWarnings("deprecation")
       PreparedStatement pstmt = getSession().connection().prepareStatement(
               "select task_name, count(*) as MC from "+analysisJobTableOrView+" AJ where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
               + " AND STATUS_ID=4 GROUP BY TASK_NAME order by MC desc");
             
       pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
         
       ResultSet rs = null;
       try {
           rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
               JSONObject module = new JSONObject();
               module.put("moduleName",rs.getString(1));
               module.put("jobsRun",rs.getInt(2));
              
               moduleCounts.put(module);
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       return moduleCounts;
   }
    
    
    public JSONArray getModuleErrorsBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        JSONArray moduleCounts = new JSONArray();
       
       @SuppressWarnings("deprecation")
       PreparedStatement pstmt = getSession().connection().prepareStatement(
               "select task_name, job_no as MC from "+analysisJobTableOrView+" where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
               + " AND STATUS_ID=4 ");
             
       pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
         
       ResultSet rs = null;
       try {
           rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
               JSONObject module = new JSONObject();
               module.put("moduleName",rs.getString(1));
               module.put("jobsNumber",rs.getInt(2));
              
               moduleCounts.put(module);
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       return moduleCounts;
   }
    
    
    public JSONArray getUserRunCountsBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        JSONArray userCounts = new JSONArray();
       
       @SuppressWarnings("deprecation")
       PreparedStatement pstmt = getSession().connection().prepareStatement(
               "select user_id, count(*) as MC from "+analysisJobTableOrView+" AJ where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
               + " GROUP BY USER_ID order by MC desc");
             
       pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
         
       ResultSet rs = null;
       try {
           rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
               JSONObject user = new JSONObject();
               user.put("user_id",rs.getString(1));
               user.put("jobsRun",rs.getInt(2));
              
               userCounts.put(user);
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       return userCounts;
   }
    
    
    public int getReturnLoginCountBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        Integer count = null;
       
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection()
                .prepareStatement("select count(USER_ID) from gp_user where  (NOT (registration_date BETWEEN ? and ?)) and (last_login_date between ? and ?)" + userExclusionClause);
        
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
       
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection()
                .prepareStatement("select count(USER_ID) from gp_user where  ( (registration_date BETWEEN ? and ?)) and (last_login_date between ? and ?) " + userExclusionClause);
        
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
