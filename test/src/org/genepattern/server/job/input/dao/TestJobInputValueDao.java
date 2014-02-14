package org.genepattern.server.job.input.dao;

import java.util.List;
import java.util.Map.Entry;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJobInputValueDao {
    final String gpUrl="http://127.0.0.1:8080/gp/";
    final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:1";
    
    @BeforeClass
    static public void beforeClass() throws Exception {
        DbUtil.initDb();
    }

    @AfterClass
    static public void afterClass() throws Exception {
        DbUtil.shutdownDb();
    }
    
    public void saveJobInput(final Integer gpJobNo, final JobInput jobInput) throws Exception {
        final boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            int numRows=0;
            for(final Param param : jobInput.getParams().values()) {
                int idx=1; //Note, db index starts at 1 so that max(idx)==num values
                for(final Entry<GroupId,ParamValue> entry : param.getValuesAsEntries()) {
                    final GroupId groupId=entry.getKey();
                    final ParamValue paramValue=entry.getValue();
                    final JobInputValue j=JobInputValue.create(gpJobNo, param.getParamId(), idx, groupId, paramValue);
                    HibernateUtil.getSession().saveOrUpdate(j);
                    ++idx;
                    ++numRows;
                }
            }
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            //TODO: log error
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error saving job_input_values for gpJobNo="+gpJobNo, t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public JobInput fetchJobInput(final int gpJobNo) throws Exception {
        boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            final String hql = "from "+JobInputValue.class.getName()+" vv where vv.gpJobNo = :gpJobNo order by vv.pname, vv.idx";
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            query.setInteger("gpJobNo", gpJobNo);
            final List<JobInputValue> records = query.list();
            
            final JobInput jobInput=new JobInput();
            for(final JobInputValue v : records) {
                final GroupId groupId=new GroupId(v.getGroupName());
                jobInput.addValue(v.getPname(), v.getPvalue(), groupId);
            }
            return jobInput;
        }
        catch (Throwable t) {
            //TODO: log error
            throw new Exception("Error getting records from job_input_value table, for gpJobNo="+gpJobNo, t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    @Test
    public void testAddJobInputValue() throws Exception { 
        int jobNo=1;
        JobInput jobInput=new JobInput();
        jobInput.setLsid(cleLsid);
        jobInput.addValue("input.filename", gpUrl+"users/test_user/all_aml_test_01.cls", new GroupId("test"));
        jobInput.addValue("input.filename", gpUrl+"users/test_user/all_aml_test_02.cls", new GroupId("test"));
        jobInput.addValue("input.filename", gpUrl+"users/test_user/all_aml_test_01.gct", new GroupId("test"));
        jobInput.addValue("input.filename", gpUrl+"users/test_user/all_aml_test_02.gct", new GroupId("test"));
        jobInput.addValue("input.filename", gpUrl+"users/test_user/all_aml_train_01.cls", new GroupId("train"));
        jobInput.addValue("input.filename", gpUrl+"users/test_user/all_aml_train_02.cls", new GroupId("train"));
        jobInput.addValue("input.filename", gpUrl+"users/test_user/all_aml_train_01.gct", new GroupId("train"));
        jobInput.addValue("input.filename", gpUrl+"users/test_user/all_aml_train_02.gct", new GroupId("train"));
        
        // example job config params
        jobInput.addValue("drm.queue", "broad");
        jobInput.addValue("drm.memory", "8gb");
        saveJobInput(jobNo, jobInput);
        
        JobInput jobInputOut = fetchJobInput(jobNo);
        Assert.assertEquals("numParams", 3, jobInputOut.getParams().size());
        Assert.assertEquals("input.filename[0]", gpUrl+"users/test_user/all_aml_test_01.cls", jobInputOut.getParamValues("input.filename").get(0).getValue());
        Assert.assertEquals("input.filename[1]", gpUrl+"users/test_user/all_aml_test_02.cls", jobInputOut.getParamValues("input.filename").get(1).getValue());
        Assert.assertEquals("input.filename[2]", gpUrl+"users/test_user/all_aml_test_01.gct", jobInputOut.getParamValues("input.filename").get(2).getValue());
        Assert.assertEquals("input.filename[3]", gpUrl+"users/test_user/all_aml_test_02.gct", jobInputOut.getParamValues("input.filename").get(3).getValue());
        Assert.assertEquals("input.filename[4]", gpUrl+"users/test_user/all_aml_train_01.cls", jobInputOut.getParamValues("input.filename").get(4).getValue());
        Assert.assertEquals("input.filename[5]", gpUrl+"users/test_user/all_aml_train_02.cls", jobInputOut.getParamValues("input.filename").get(5).getValue());
        Assert.assertEquals("input.filename[6]", gpUrl+"users/test_user/all_aml_train_01.gct", jobInputOut.getParamValues("input.filename").get(6).getValue());
        Assert.assertEquals("input.filename[7]", gpUrl+"users/test_user/all_aml_train_02.gct", jobInputOut.getParamValues("input.filename").get(7).getValue());
        
        Assert.assertEquals("drm.queue", "broad", jobInputOut.getParam("drm.queue").getValues().get(0).getValue());
        Assert.assertEquals("drm.memory", "8gb", jobInputOut.getParam("drm.memory").getValues().get(0).getValue());
    }

}
