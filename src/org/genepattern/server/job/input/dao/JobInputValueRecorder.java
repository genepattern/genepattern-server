package org.genepattern.server.job.input.dao;

import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;

/**
 * Helper class for creating/reading values from the job_input_value table.
 * @author pcarr
 *
 */
public class JobInputValueRecorder {
    private static final Logger log = Logger.getLogger(JobInputValueRecorder.class);

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
            if (log.isDebugEnabled()) {
                log.debug("saved "+numRows+" rows to job_input_value table for gpJobNo="+gpJobNo);
            }
        }
        catch (Throwable t) {
            log.error("error saving entries to job_input_value table for gpJobNo="+gpJobNo, t);
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error saving job_input_values for gpJobNo="+gpJobNo, t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
