package org.genepattern.server.database.hsqldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;

public class TestHsqldbConfig {
    @Test 
    public void _08a_check_analysis_job_archive() throws ExecutionException, DbException, SQLException {
        final HibernateSessionManager mgr=DbUtil.getTestDbSession();
        final ResultSet rs=DbUtil.executeSqlQuery(mgr, "SELECT * from ANALYSIS_JOB_ARCHIVE");
        // expected columns
        final String[] expectedColumns=new String[] {
            "JOB_NO", "TASK_ID", "STATUS_ID", "DATE_SUBMITTED", "DATE_COMPLETED", 
            "PARAMETER_INFO", "USER_ID", "ISINDEXED",  "ACCESS_ID", "JOB_NAME", "LSID", "TASK_LSID",  "TASK_NAME", "PARENT", "DELETED" };

        final int colCount=rs.getMetaData().getColumnCount();
        final String[] actualColumns=new String[colCount];
        for(int colIdx=0; colIdx<colCount; ++colIdx) {
            actualColumns[colIdx]=rs.getMetaData().getColumnName(1+colIdx);
        }
        assertThat("column names", actualColumns, is(expectedColumns));
    }
    
    @Test 
    public void _08b_check_job_runner_job_archive() throws ExecutionException, DbException, SQLException {
        final HibernateSessionManager mgr=DbUtil.getTestDbSession();
        final ResultSet rs=DbUtil.executeSqlQuery(mgr, "SELECT * from JOB_RUNNER_JOB_ARCHIVE");
        // expected columns
        final String[] expectedColumns=new String[] {
            "GP_JOB_NO", "LSID", "JR_CLASSNAME", "JR_NAME", "EXT_JOB_ID", "QUEUE_ID", "JOB_STATE", 
            "SUBMIT_TIME", "START_TIME", "END_TIME", "CPU_TIME", "MAX_MEM", "MAX_SWAP", "MAX_PROCESSES", "MAX_THREADS", 
            "STATUS_DATE", "STATUS_MESSAGE", "EXIT_CODE", "TERMINATING_SIGNAL", 
            "REQ_MEM", "REQ_CPU_COUNT", "REQ_NODE_COUNT", "REQ_WALLTIME", "REQ_QUEUE", 
            "WORKING_DIR", "STDIN_FILE", "STDOUT_FILE", "LOG_FILE", "STDERR_FILE" };

        final int colCount=rs.getMetaData().getColumnCount();
        final String[] actualColumns=new String[colCount];
        for(int colIdx=0; colIdx<colCount; ++colIdx) {
            actualColumns[colIdx]=rs.getMetaData().getColumnName(1+colIdx);
        }
        assertThat("column names", actualColumns, is(expectedColumns));
    }
    
    @Test 
    public void _08c_check_on_analysis_job_del_trigger() throws ExecutionException, DbException, SQLException {
        final HibernateSessionManager mgr=DbUtil.getTestDbSession();
        
        final ResultSet rs=DbUtil.executeSqlQuery(mgr, "SELECT * from INFORMATION_SCHEMA.TRIGGERS where trigger_name='ON_ANALYSIS_JOB_DEL'");
        boolean hasNext=rs.next();
        if (!hasNext) {
            fail("missing expected trigger, 'ON_ANALYSIS_JOB_DEL'");
        }
        assertEquals("trigger_name", "ON_ANALYSIS_JOB_DEL", rs.getString("TRIGGER_NAME"));
        assertEquals("event_manipulation", "DELETE", rs.getString("EVENT_MANIPULATION"));
    }

    @Test
    public void _08d_check_analysis_job_total_view() throws DbException, ExecutionException, SQLException {
        final HibernateSessionManager mgr=DbUtil.getTestDbSession();
        DbUtil.assertAnalysisJobTotalView(mgr);
    }

}
