package org.genepattern.server.webservice.server.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map.Entry;
import java.util.Set;

public class UsageStatsDAO extends BaseDAO {
    private static final Logger log = Logger.getLogger(UsageStatsDAO.class);
    private static String analysisJobTableOrView = "analysis_job_total";
    
    
    public UsageStatsDAO(HibernateSessionManager mgr) {
        super(mgr);
    }
    
    public String generateUserExclusionClause(String userOrEmailList, String pattern){
        StringBuffer sqlBuff = new StringBuffer("select USER_ID from gp_user where ");
        sqlBuff.append(" ( USER_ID  in ");
        sqlBuff.append(userOrEmailList);
        sqlBuff.append(") or (EMAIL in ");
        sqlBuff.append(userOrEmailList);
        sqlBuff.append(") ");
        if (pattern != null){
            sqlBuff.append(" or email like ") ;
            sqlBuff.append(pattern);
        }
        
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
    
    
    public JSONArray getModuleInstallsBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        JSONArray users = new JSONArray();
        ResultSet rs = null;
        
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement("select TI.USER_ID, TI.LSID, TI.DATE_INSTALLED, TI.SOURCE_TYPE, TM.TASK_NAME from TASK_INSTALL as TI inner join TASK_MASTER as TM on TI.lsid = TM.lsid where (DATE_INSTALLED BETWEEN ? and ?)  ");
        pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
          
        try {
            rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
            while (rs.next()) {
                JSONObject module = new JSONObject();
                module.put("user_id",rs.getString(1));
                module.put("task_name",rs.getString(5));
                module.put("email",rs.getString(2));
                module.put("date_installed",rs.getString(3));
                module.put("source_type",rs.getString(4));
                
                users.put(module);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return users;
    }
    
    
    public int getTotalRegistrationCount(String userExclusionClause) throws Exception {
        Integer count = null;
        StringBuffer sqlBuff = new StringBuffer("select count(USER_ID) from gp_user where (USER_ID <> 'test') "+ userExclusionClause);
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
        StringBuffer sqlBuff1 = new StringBuffer("select count(*) from ANALYSIS_JOB where (USER_ID <> 'test') "+ userExclusionClause);
        StringBuffer sqlBuff2 = new StringBuffer("select count(*) from ANALYSIS_JOB_ARCHIVE where (USER_ID <> 'test') "+ userExclusionClause);
        ResultSet rs = null;
        try {
            rs = this.executeSQL(sqlBuff1.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        try {
            rs = this.executeSQL(sqlBuff2.toString());
            if (rs.next()) {
                count = count + rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        
        return count;
    }
    
    
    public int getJobsRunCountBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        Integer count = null;
        // do the two tables seperately so that indexes are used (does not happen with the view)
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt1 = getSession().connection().prepareStatement("select count(*) from ANALYSIS_JOB where (date_completed BETWEEN ? and ?)  "+ userExclusionClause);
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt2 = getSession().connection().prepareStatement("select count(*) from ANALYSIS_JOB_ARCHIVE where (date_completed BETWEEN ? and ?)  "+ userExclusionClause);
             
        pstmt1.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt1.setDate(2, new java.sql.Date(endDate.getTime()));
        pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
          
        ResultSet rs = null;
        try {
            rs = pstmt1.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        try {
            rs = pstmt2.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = count + rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return count;
    }
    
    
    public int getInternalJobsRunCountBetweenDates(Date startDate, Date endDate, String userExclusionClause, String internalDomain) throws Exception {
        Integer count = null;
        
        // when possible query the tables separately since the view does not permit indexes to be used in MySQL
        
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt1 = getSession().connection().prepareStatement(
                "select count(*) from ANALYSIS_JOB where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
                + " and (user_id in ( select user_id from GP_USER where email like '%"+internalDomain+"'))");
        @SuppressWarnings("deprecation")      
        PreparedStatement pstmt2 = getSession().connection().prepareStatement(
                "select count(*) from ANALYSIS_JOB_ARCHIVE where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
                + " and (user_id in ( select user_id from GP_USER where email like '%"+internalDomain+"'))");
              
          
   
        
        pstmt1.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt1.setDate(2, new java.sql.Date(endDate.getTime()));
        pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
        
        ResultSet rs = null;
        try {
            rs = pstmt1.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        try {
            rs = pstmt2.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = count + rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        
        return count;
    }
    
    public int getAnonymousJobsRunCountBetweenDates(Date startDate, Date endDate) throws Exception {
        Integer count = null;
        
        // when possible query the tables separately since the view does not permit indexes to be used in MySQL
        
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt1 = getSession().connection().prepareStatement(
                "select count(*) from ANALYSIS_JOB where (date_completed BETWEEN ? and ?)  "  
                + " and (user_id in ( select user_id from GP_USER where user_id like 'Anonymous_%' OR user_id like 'Guest_%' OR user_id like 'guest_%'))");
        @SuppressWarnings("deprecation")      
        PreparedStatement pstmt2 = getSession().connection().prepareStatement(
                "select count(*) from ANALYSIS_JOB_ARCHIVE where (date_completed BETWEEN ? and ?)  "  
                + " and (user_id in ( select user_id from GP_USER where user_id like 'Anonymous_%' OR user_id like 'Guest_%' OR user_id like 'guest_%'))");
              
          
   
        
        pstmt1.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt1.setDate(2, new java.sql.Date(endDate.getTime()));
        pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
        
        ResultSet rs = null;
        try {
            rs = pstmt1.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        try {
            rs = pstmt2.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = count + rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        
        return count;
    }
    
    
    public int getExternalJobsRunCountBetweenDates(Date startDate, Date endDate, String userExclusionClause, String internalDomain) throws Exception {
        Integer count = null;
        // when possible query the tables separately since the view does not permit indexes to be used in MySQL
        
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt1 = getSession().connection().prepareStatement(
                "select count(*) from ANALYSIS_JOB where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
                + " and (user_id not in ( select user_id from GP_USER where email like '%"  + internalDomain +"'))");
              
        pstmt1.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt1.setDate(2, new java.sql.Date(endDate.getTime()));
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt2 = getSession().connection().prepareStatement(
                "select count(*) from ANALYSIS_JOB_ARCHIVE where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
                + " and (user_id not in ( select user_id from GP_USER where email like '%"  + internalDomain +"'))");
              
        pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
       
        
        ResultSet rs = null;
        try {
            rs = pstmt1.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        
        try {
            rs = pstmt2.executeQuery(); // this.executeSQL(sqlBuff.toString());
            if (rs.next()) {
                count = count + rs.getInt(1);
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
                "select task_name, count(*), sum(JRJ.cpu_time) as MC from ANALYSIS_JOB AJ, JOB_RUNNER_JOB JRJ where (date_completed BETWEEN ? and ?)  " 
                + " AND AJ.job_no=JRJ.gp_job_no "
                        + userExclusionClause 
                + " GROUP BY TASK_NAME order by MC desc");
              
        pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
        PreparedStatement pstmt2 = getSession().connection().prepareStatement(
                "select task_name, count(*), sum(JRJ.cpu_time) as MC from ANALYSIS_JOB_ARCHIVE AJ, JOB_RUNNER_JOB JRJ where (date_completed BETWEEN ? and ?)  " 
                        + " AND AJ.job_no=JRJ.gp_job_no "
                                + userExclusionClause 
                        + " GROUP BY TASK_NAME order by MC desc");
              
        pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
        HashMap<String, Integer> modJobs = new   HashMap <String, Integer>();
        HashMap<String, Long> modCpu = new   HashMap <String, Long>();
        
        ResultSet rs = null;
        try {
            rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
            while (rs.next()) {
                modJobs.put(rs.getString(1), rs.getInt(2));
                modCpu.put(rs.getString(1), rs.getLong(3));
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        
        
        try {
            rs = pstmt2.executeQuery(); // this.executeSQL(sqlBuff.toString());
            while (rs.next()) {
                String mod = rs.getString(1);
                Integer c = rs.getInt(2);
                Long cpu = rs.getLong(3);
                if (modJobs.containsKey(mod)) {
                    int prev = modJobs.get(mod);  
                    modJobs.put(mod, prev + c);
                } else {        
                    modJobs.put(mod, c);
                } 
                if (modCpu.containsKey(mod)) {
                    long prev = modCpu.get(mod);  
                    modCpu.put(mod, prev + cpu);
                } else {        
                    modCpu.put(mod, cpu);
                } 
            }
        } finally {
            if (rs != null)
                rs.close();
        }
     // Now we have collected the values, sort them and add to the json object
        Set<Entry<String, Integer>> entries = modJobs.entrySet();
        
        Comparator<Entry<String, String>> valueComparator = new Comparator<Entry<String,String>>() { 
                @Override public int compare(Entry<String, String> e1, Entry<String, String> e2) { 
                    String v1 = e1.getValue();
                    String v2 = e2.getValue(); 
                    return v1.compareTo(v2); } 
        };
        List<Entry<String, Integer>> listOfEntries = new ArrayList<Entry<String, Integer>>(entries); // sorting HashMap by values using comparator Collections.sort(listOfEntries, valueComparator);

        for(Entry<String, Integer> entry : listOfEntries){ 
            
            JSONObject user = new JSONObject();
            user.put("moduleName",entry.getKey());
            user.put("jobsRun",entry.getValue());
            Long cpu = modCpu.get(entry.getKey());
            if (cpu == null) cpu = 0L;
            user.put("cpu", cpu);
            
            moduleCounts.put(user);
        }       
        return moduleCounts;
    }
   
    
    public JSONArray getAnonymousModuleRunCountsBetweenDates(Date startDate, Date endDate) throws Exception {
        JSONArray moduleCounts = new JSONArray();
       
       String userInclusionClause =  " and ((AJ.user_id like 'Anonymous_%') OR (AJ.user_id like 'Guest_%') OR (AJ.user_id like 'guest_%')) ";
       System.out.println(userInclusionClause);
       
       @SuppressWarnings("deprecation")
       PreparedStatement pstmt = getSession().connection().prepareStatement(
               "select task_name, count(*), sum(JRJ.cpu_time) as MC from ANALYSIS_JOB AJ, JOB_RUNNER_JOB JRJ where (date_completed BETWEEN ? and ?)  " 
               + " AND AJ.job_no=JRJ.gp_job_no "
                       + userInclusionClause 
               + " GROUP BY TASK_NAME order by MC desc");
             
       pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
       PreparedStatement pstmt2 = getSession().connection().prepareStatement(
               "select task_name, count(*), sum(JRJ.cpu_time) as MC from ANALYSIS_JOB_ARCHIVE AJ, JOB_RUNNER_JOB JRJ where (date_completed BETWEEN ? and ?)  " 
                       + " AND AJ.job_no=JRJ.gp_job_no "
                               + userInclusionClause 
                       + " GROUP BY TASK_NAME order by MC desc");
             
       pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
       HashMap<String, Integer> modJobs = new   HashMap <String, Integer>();
       HashMap<String, Long> modCpu = new   HashMap <String, Long>();
       
       ResultSet rs = null;
       try {
           rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
               modJobs.put(rs.getString(1), rs.getInt(2));
               modCpu.put(rs.getString(1), rs.getLong(3));
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       
       
       try {
           rs = pstmt2.executeQuery(); // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
               String mod = rs.getString(1);
               Integer c = rs.getInt(2);
               Long cpu = rs.getLong(3);
               if (modJobs.containsKey(mod)) {
                   int prev = modJobs.get(mod);  
                   modJobs.put(mod, prev + c);
               } else {        
                   modJobs.put(mod, c);
               } 
               if (modCpu.containsKey(mod)) {
                   long prev = modCpu.get(mod);  
                   modCpu.put(mod, prev + cpu);
               } else {        
                   modCpu.put(mod, cpu);
               } 
           }
       } finally {
           if (rs != null)
               rs.close();
       }
    // Now we have collected the values, sort them and add to the json object
       Set<Entry<String, Integer>> entries = modJobs.entrySet();
       
       Comparator<Entry<String, String>> valueComparator = new Comparator<Entry<String,String>>() { 
               @Override public int compare(Entry<String, String> e1, Entry<String, String> e2) { 
                   String v1 = e1.getValue();
                   String v2 = e2.getValue(); 
                   return v1.compareTo(v2); } 
       };
       List<Entry<String, Integer>> listOfEntries = new ArrayList<Entry<String, Integer>>(entries); // sorting HashMap by values using comparator Collections.sort(listOfEntries, valueComparator);

       for(Entry<String, Integer> entry : listOfEntries){ 
           
           JSONObject user = new JSONObject();
           user.put("moduleName",entry.getKey());
           user.put("jobsRun",entry.getValue());
           Long cpu = modCpu.get(entry.getKey());
           if (cpu == null) cpu = 0L;
           user.put("cpu", cpu);
           
           moduleCounts.put(user);
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
               "select MOD_DOMAIN.dd, count(*) from ( select substr(GPU.email, INSTR(GPU.email, '@')+1) as dd, AJ.JOB_NO "
               + " from ANALYSIS_JOB AJ, GP_USER GPU " 
               + date_clause  + altExclusion 
               + " and GPU.USER_ID = AJ.USER_ID"
               + ") as MOD_DOMAIN GROUP BY dd ";
       String sql2 =
               "select MOD_DOMAIN.dd, count(*) from ( select substr(GPU.email, INSTR(GPU.email, '@')+1) as dd, AJ.JOB_NO "
               + " from ANALYSIS_JOB_ARCHIVE AJ, GP_USER GPU " 
               + date_clause  + altExclusion 
               + " and GPU.USER_ID = AJ.USER_ID"
               + ") as MOD_DOMAIN GROUP BY dd ";
     
       ResultSet rs = null;
     
       HashMap<String, Integer> modJobs = new   HashMap <String, Integer>();
       
       
       try {
           rs = this.executeSQL(sql);; // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
               modJobs.put(rs.getString(1), rs.getInt(2));
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       
       
       try {
           rs = this.executeSQL(sql2);; // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
               String mod = rs.getString(1);
               Integer c = rs.getInt(2);
               if (modJobs.containsKey(mod)) {
                   int prev = modJobs.get(mod);  
                   modJobs.put(mod, prev + c);
               } else {        
                   modJobs.put(mod, c);
               } 
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       
    // Now we have collected the values, sort them and add to the json object
       Set<Entry<String, Integer>> entries = modJobs.entrySet();
       
       Comparator<Entry<String, String>> valueComparator = new Comparator<Entry<String,String>>() { 
               @Override public int compare(Entry<String, String> e1, Entry<String, String> e2) { 
                   String v1 = e1.getValue();
                   String v2 = e2.getValue(); 
                   return v1.compareTo(v2); } 
       };
       List<Entry<String, Integer>> listOfEntries = new ArrayList<Entry<String, Integer>>(entries); // sorting HashMap by values using comparator Collections.sort(listOfEntries, valueComparator);

       for(Entry<String, Integer> entry : listOfEntries){ 
           
           JSONObject user = new JSONObject();
           user.put("domain",entry.getKey());
           user.put("jobsRun",entry.getValue());
          
           moduleCounts.put(user);
       }       
       
       
       
       
       return moduleCounts;
   }
    
    
    public JSONArray getModuleErrorCountsBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        JSONArray moduleCounts = new JSONArray();
       
       @SuppressWarnings("deprecation")
       PreparedStatement pstmt = getSession().connection().prepareStatement(
               "select task_name, count(*) as MC from ANALYSIS_JOB AJ where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
               + " AND STATUS_ID=4 GROUP BY TASK_NAME order by MC desc");
             
       pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
       PreparedStatement pstmt2 = getSession().connection().prepareStatement(
               "select task_name, count(*) as MC from ANALYSIS_JOB_ARCHIVE AJ where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
               + " AND STATUS_ID=4 GROUP BY TASK_NAME order by MC desc");
             
       pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
         
       ResultSet rs = null;
       HashMap<String, Integer> modJobs = new   HashMap <String, Integer>();
       
      
       try {
           rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
               modJobs.put(rs.getString(1), rs.getInt(2));
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       
       
       try {
           rs = pstmt2.executeQuery(); // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
               String mod = rs.getString(1);
               Integer c = rs.getInt(2);
               if (modJobs.containsKey(mod)) {
                   int prev = modJobs.get(mod);  
                   modJobs.put(mod, prev + c);
               } else {        
                   modJobs.put(mod, c);
               } 
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       
    // Now we have collected the values, sort them and add to the json object
       Set<Entry<String, Integer>> entries = modJobs.entrySet();
       
       Comparator<Entry<String, String>> valueComparator = new Comparator<Entry<String,String>>() { 
               @Override public int compare(Entry<String, String> e1, Entry<String, String> e2) { 
                   String v1 = e1.getValue();
                   String v2 = e2.getValue(); 
                   return v1.compareTo(v2); } 
       };
       List<Entry<String, Integer>> listOfEntries = new ArrayList<Entry<String, Integer>>(entries); // sorting HashMap by values using comparator Collections.sort(listOfEntries, valueComparator);

       for(Entry<String, Integer> entry : listOfEntries){ 
           
           JSONObject user = new JSONObject();
           user.put("moduleName",entry.getKey());
           user.put("jobsRun",entry.getValue());
          
           moduleCounts.put(user);
       }       
       
       return moduleCounts;
   }
    
    
    public JSONArray getModuleErrorsBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        JSONArray moduleCounts = new JSONArray();
       
       @SuppressWarnings("deprecation")
       PreparedStatement pstmt = getSession().connection().prepareStatement(
               "select task_name, job_no as MC from ANALYSIS_JOB where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
               + " AND STATUS_ID=4 ");
             
       pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
       PreparedStatement pstmt2 = getSession().connection().prepareStatement(
               "select task_name, job_no as MC from ANALYSIS_JOB_ARCHIVE where (date_completed BETWEEN ? and ?)  "  + userExclusionClause 
               + " AND STATUS_ID=4 ");
             
       pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
        
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
       
       try {
           rs = pstmt2.executeQuery(); // this.executeSQL(sqlBuff.toString());
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
    
    
    public long getTotalJobsCPUBetweenDates(Date startDate, Date endDate,  String userExclusionClause) throws Exception {
        Long count = 0L;
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt = getSession().connection().prepareStatement(
                "select  sum(JRJ.cpu_time) from ANALYSIS_JOB AJ, JOB_RUNNER_JOB JRJ where (date_completed BETWEEN ? and ?)  "
                    + " AND AJ.job_no=JRJ.gp_job_no  "
                    + userExclusionClause );
              
        pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
        
        @SuppressWarnings("deprecation")
        PreparedStatement pstmt2 = getSession().connection().prepareStatement(
                "select  sum(JRJ.cpu_time) from ANALYSIS_JOB_ARCHIVE AJ, JOB_RUNNER_JOB JRJ  where (date_completed BETWEEN ? and ?)  "
                        + " AND AJ.job_no=JRJ.gp_job_no  "
                        + userExclusionClause );
              
        pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
        pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
        
        ResultSet rs = null;
        try {
          
            rs = pstmt.executeQuery(); 
            if (rs.next()){
                count += rs.getLong(1);
            }
        } finally {
            rs.close();
        }
        try {
            rs = pstmt2.executeQuery(); 
            if (rs.next()){
               
                count += rs.getLong(1);
            }
        } finally {
            rs.close();
        }
        
        return count;
    }
    
    
//        select AJ.user_id, sum(JRJ.cpu_time)
//        from ANALYSIS_JOB AJ, JOB_RUNNER_JOB JRJ
//        where AJ.job_no=JRJ.gp_job_no and  AJ.job_no >68000
//        GROUP by AJ.user_id
     
    public JSONArray getUserRunCountsBetweenDates(Date startDate, Date endDate, String userExclusionClause) throws Exception {
        JSONArray userCounts = new JSONArray();
       
       @SuppressWarnings("deprecation")
       PreparedStatement pstmt = getSession().connection().prepareStatement(
               "select user_id, count(*) as MC, sum(JRJ.cpu_time) from ANALYSIS_JOB AJ, JOB_RUNNER_JOB JRJ where (date_completed BETWEEN ? and ?)  "
                   + " AND AJ.job_no=JRJ.gp_job_no  "
                   + userExclusionClause 
               + " GROUP BY AJ.USER_ID order by MC desc");
             
       pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
       
       
       
       @SuppressWarnings("deprecation")
       PreparedStatement pstmt2 = getSession().connection().prepareStatement(
               "select user_id, count(*) as MC, sum(JRJ.cpu_time) from ANALYSIS_JOB_ARCHIVE AJ, JOB_RUNNER_JOB JRJ  where (date_completed BETWEEN ? and ?)  "
                       + " AND AJ.job_no=JRJ.gp_job_no  "
                       + userExclusionClause 
               + " GROUP BY USER_ID order by MC desc");
             
       pstmt2.setDate(1, new java.sql.Date(startDate.getTime()));
       pstmt2.setDate(2, new java.sql.Date(endDate.getTime()));
       HashMap<String, Integer> userJobs = new   HashMap <String, Integer>();
       HashMap<String, Long> userCpu = new   HashMap <String, Long>();
       
       ResultSet rs = null;
       try {
           rs = pstmt.executeQuery(); // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
             
               String u = rs.getString(1);
               Integer c = rs.getInt(2);
               Long cpu = rs.getLong(3);
               
               userJobs.put(u,c);
               userCpu.put(u, cpu);
           }
       } finally {
           if (rs != null)
               rs.close();
       }
       
       try {
           rs = pstmt2.executeQuery(); // this.executeSQL(sqlBuff.toString());
           while (rs.next()) {
           
               String userId = rs.getString(1);
               Integer c = rs.getInt(2);
               Long cpu = rs.getLong(3);
               
               if (userJobs.containsKey(userId)) {
                   int prev = userJobs.get(userId);  
                   userJobs.put(userId, prev + c);
               } else {
                   userJobs.put(userId, c);
               }
               if (userCpu.containsKey(userId)) {
                   long prev = userJobs.get(userId);  
                   userCpu.put(userId, prev + cpu);
               } else {
                   userCpu.put(userId, cpu);
               }
           
           }
       } finally {
           if (rs != null)
               rs.close();
       }

       
       
       
       // Now we have collected the values, sort them and add to the json object
       Set<Entry<String, Integer>> entries = userJobs.entrySet();
       
       Comparator<Entry<String, String>> valueComparator = new Comparator<Entry<String,String>>() { 
               @Override public int compare(Entry<String, String> e1, Entry<String, String> e2) { 
                   String v1 = e1.getValue();
                   String v2 = e2.getValue(); 
                   return v1.compareTo(v2); } 
       };
       List<Entry<String, Integer>> listOfEntries = new ArrayList<Entry<String, Integer>>(entries); // sorting HashMap by values using comparator Collections.sort(listOfEntries, valueComparator);

       for(Entry<String, Integer> entry : listOfEntries){ 
           
           JSONObject user = new JSONObject();
           user.put("user_id",entry.getKey());
           user.put("jobsRun",entry.getValue());
           Long cpu = userCpu.get(entry.getKey());
           if (cpu == null) cpu = 0L;
           user.put("cpu", cpu);
           
           userCounts.put(user);
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
