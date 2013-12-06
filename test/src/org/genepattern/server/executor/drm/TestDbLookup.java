package org.genepattern.server.executor.drm;

import java.io.File;

import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.impl.local.LocalJobRunner;
import org.genepattern.junitutil.DbUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit test cases for creating, updating, and deleting entries from the 'job_runner_job' table.
 * @author pcarr
 *
 */
public class TestDbLookup {
    private static final String jobRunnerClassname=LocalJobRunner.class.getName();
    private static final String jobRunnerName="LocalQueuingSystem-1";
    final Integer gpJobNo=0;
    final String drmJobId="DRM_"+gpJobNo;

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
        final File workingDir=new File("jobResults/"+gpJobNo);
        
        DbLookup dbLookup = new DbLookup(jobRunnerClassname, jobRunnerName);
        dbLookup.insertDrmRecord(workingDir, gpJobNo);
        
        // simulate setting the drmJobId
        
        DrmJobStatus drmJobStatus = new DrmJobStatus.Builder(drmJobId, DrmJobState.QUEUED).build();
        dbLookup.updateDrmRecord(gpJobNo, drmJobStatus);
        
        //List<JobRunnerJob> all=dbLookup.getAll();
        //Assert.assertEquals("all.size", 1, all.size());
    }
    
    @Test
    public void testQuery() {
        DbLookup dbLookup=new DbLookup(jobRunnerClassname, jobRunnerName);
        final String actualDrmJobId=dbLookup.lookupDrmJobId(gpJobNo);
        Assert.assertEquals("lookupDrmJobId("+gpJobNo+")", drmJobId, actualDrmJobId);
        
        //List<String> runningJobIds=dbLookup.getRunningDrmJobIds();
        //Assert.assertEquals("num running jobs", 1, runningJobIds.size());
    }
    
    @Test
    public void testLookupGpJobNo() {
        DbLookup dbLookup=new DbLookup(jobRunnerClassname, jobRunnerName);
        Integer actualGpJobNo=dbLookup.lookupGpJobNo(drmJobId);
        Assert.assertEquals("lookupGpJobNo("+drmJobId+")", gpJobNo, actualGpJobNo);
    }

}
