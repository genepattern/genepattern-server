package org.genepattern.server.webservice.server.dao;

import java.sql.*;

/**
 * Utility class for transferring contents from HSQL to ORACLE.
 * @author jrobinso
 *
 */
public class TransferUtil
{
    static String fromURL = "jdbc:hsqldb:file:/java/cvs/genepattern/resources/GenePatternDB";
    static String fromUsername = "sa";
    static String fromPassword = "";
 
    static String toUrl = "jdbc:hsqldb:file:/java/cvs/genepattern/resources/TestDB";
    static String toUsername = "sa";
    static String toPassword = "";

    //static String toUsername = "gpportal";
    //static String toPassword = "gpportal";
    static Connection toConnection = null;
    static Connection fromConnection = null;


    public static void main(String[] args) {
 
        try {
            intitializeDrivers();

            fromConnection = getConnection(fromURL, fromUsername, fromPassword);
            toConnection = getConnection(toUrl, toUsername, toPassword);
            
            transferAnalysisJob(fromConnection, toConnection);
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            try {
                if (toConnection != null) {
                	toConnection.close();
                }
                if(fromConnection != null) {
                	fromConnection.close();
                }
            }
            catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
 
    public static void transferAnalysisJob(Connection hsqlConnection, Connection oracleConnection) {
        String selectSql = "select * from analysis_job";
        String insertSql = 
            "insert into analysis_job (job_no, task_id, status_id, date_submitted, date_completed,                  " + 
            " parameter_info, user_id, isindexed, access_id, job_name, lsid, task_lsid, task_name, parent, deleted) " + 
            " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
        ResultSet rs = null;
        PreparedStatement readStatement = null;
        PreparedStatement in = null;
        try {
            readStatement = hsqlConnection.prepareStatement("select * from analysis_job");
            rs = readStatement.executeQuery();
            while (rs.next()) {
                in = oracleConnection.prepareStatement(insertSql);
                in.setInt(1, rs.getInt("JOB_NO"));

                Number taskId = (Number) rs.getObject("TASK_ID");
                if (taskId == null) {
                    in.setNull(2, Types.INTEGER);
                }
                else {
                    in.setInt(2, taskId.intValue());
                }

                Number statusId = (Number) rs.getObject("STATUS_ID");
                if (statusId == null) {
                    in.setNull(3, Types.INTEGER);
                }
                else {
                    in.setInt(3, statusId.intValue()); 
                }
 
                Timestamp dateSubmitted = rs.getTimestamp("DATE_SUBMITTED");
                if (dateSubmitted == null) {
                    in.setNull(4, Types.TIMESTAMP);
                }
                else {
                    in.setTimestamp(4, dateSubmitted);
                }
                            
                Timestamp dateCompleted = rs.getTimestamp("DATE_COMPLETED");
                if (dateCompleted == null) {
                    in.setNull(5, Types.TIMESTAMP);
                }
                else {
                    in.setTimestamp(5, dateCompleted);
                }
                
                String parameterInfo = rs.getString("PARAMETER_INFO");
                if (parameterInfo == null) {
                    in.setNull(6, Types.VARCHAR);
                }
                else {
                    in.setString(6, parameterInfo);
                }
               
                String userId = rs.getString("USER_ID");
                if (userId == null) {
                    in.setNull(7, Types.VARCHAR);
                }
                else {
                    in.setString(7, userId);
                }
                
                in.setBoolean(8, rs.getBoolean("ISINDEXED"));
                               
                Number accessId = (Number) rs.getObject("ACCESS_ID");
                if (accessId == null) {
                    in.setNull(9, Types.INTEGER);
                }
                else {
                    in.setInt(9, accessId.intValue()); 
                }

                String jobName = rs.getString("JOB_NAME");
                if (jobName == null) {
                    in.setNull(10, Types.VARCHAR);
                }
                else {
                    in.setString(10, jobName);
                }

                String lsid = rs.getString("LSID");
                if (lsid == null) {
                    in.setNull(11, Types.VARCHAR);
                }
                else {
                    in.setString(11, lsid);
                }

                String taskLsid = rs.getString("TASK_LSID");
                if (taskLsid == null) {
                    in.setNull(12, Types.VARCHAR);
                }
                else {
                    in.setString(12, taskLsid);
                }

                String taskName = rs.getString("TASK_NAME");
                if (taskName == null) {
                    in.setNull(13, Types.VARCHAR);
                }
                else {
                    in.setString(13, taskName);
                }
 
                Number parent = (Number) rs.getObject("PARENT");
                if (parent == null) {
                    in.setNull(14, Types.INTEGER);
                }
                else {
                    in.setInt(14, parent.intValue()); 
                }

                in.setBoolean(15, rs.getBoolean("DELETED"));
                
                in.executeUpdate();
                in.close();
                               
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            closeResources(rs, readStatement);
            closeResources(null, in);
        }

    }

    private static void closeResources(ResultSet rs, Statement statement) {
        try {
            if (rs != null)
                rs.close();
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            if (statement != null)
                statement.close();
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    private static Connection getConnection(String dbUrl, String username, String password)
        throws SQLException {


        return DriverManager.getConnection(dbUrl, username, password);

    }


    private static void intitializeDrivers() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        }
        catch (Exception e) {
            System.out.println("ERROR: failed to load HSQLDB JDBC driver.");
            e.printStackTrace();
        }
    }


}
