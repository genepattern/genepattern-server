package org.genepattern.server.executor.drm.dao;

import java.io.File;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit test cases for creating, updating, and deleting entries from the 'job_runner_job' table.
 * @author pcarr
 *
 */
public class TestRecordJobRunnerJobToDb {
    @BeforeClass
    public static void beforeClass() throws Exception{
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        DbUtil.initDb();
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        DbUtil.shutdownDb();
    }
    
    @Test
    public void testCreate() {
        final Integer gpJobNo=0;
        final String jobRunnerClassname="";
        final String jobRunnerName="";
        final File workingDir=new File("jobResults/"+gpJobNo);
        final JobRunnerJob job = new JobRunnerJob(jobRunnerClassname, jobRunnerName, workingDir, gpJobNo);
        
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().save(job);
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            t.printStackTrace();
            HibernateUtil.rollbackTransaction();
        }
    }

}
