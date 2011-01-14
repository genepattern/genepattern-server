package org.genepattern.server.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.Analysis.JobSortOrder;
import org.genepattern.webservice.JobInfo;
import org.hibernate.Query;
import org.hibernate.SQLQuery;

public class BatchJobDAO extends BaseDAO {

	private static final Logger log = Logger.getLogger(BatchJobDAO.class);

	public BatchJob findById(Integer id) {
		log.debug("getting BatchJob instance with id: " + id);
		try {
			return (BatchJob) HibernateUtil.getSession().get(
					"org.genepattern.server.domain.BatchJob", id);
		} catch (RuntimeException re) {
			log.error("get failed", re);
			throw re;
		}
	}	
	
	public List<BatchJob> findByUserId(String userId){
		 Query query =  HibernateUtil.getSession().getNamedQuery("getBatchJobsForUser");
	     query.setString("userId", userId);	     
	     return query.list();	
	}

	public JobInfo[] getBatchJobs(String userId, String batchFilter, int firstJob,
			int numJobs, JobSortOrder jobSortOrder,
			boolean jobSortAscending) {
		 Query baseQueryText =   HibernateUtil.getSession().getNamedQuery("getJobsInBatch");
		 
		 StringBuffer orderedQuery = new StringBuffer(baseQueryText.getQueryString());
		 switch (jobSortOrder) {
	        case JOB_NUMBER:
	            orderedQuery.append(" ORDER BY JOB_NO");
	            break;
	        case JOB_STATUS:
	            orderedQuery.append(" ORDER BY STATUS_ID");
	            break;
	        case SUBMITTED_DATE:
	            orderedQuery.append(" ORDER BY DATE_SUBMITTED");
	            break;
	        case COMPLETED_DATE:
	            orderedQuery.append(" ORDER BY DATE_COMPLETED");
	            break;
	        case USER:
	            orderedQuery.append(" ORDER BY USER_ID");
	            break;
	        case MODULE_NAME:
	            orderedQuery.append(" ORDER BY TASK_NAME");
	            break;
	        }
	     orderedQuery.append(jobSortAscending ? " ASC" : " DESC");	     
	     SQLQuery query = HibernateUtil.getSession().createSQLQuery(orderedQuery.toString());
		 
	     query.setInteger("batchId", Integer.parseInt(undecorate(batchFilter)));
	     query.addEntity(AnalysisJob.class);
	     query.setMaxResults(numJobs);
	     query.setFirstResult(firstJob);
	     
	     
	     List<AnalysisJob> queryResults = query.list();
	     List<JobInfo> results = new ArrayList<JobInfo>();	       
	     for (AnalysisJob aJob : queryResults) {
	        JobInfo ji = new JobInfo(aJob);
	        results.add(ji);
	     }
	     return results.toArray(new JobInfo[] {});	
		
	}
	
	public Integer getNumBatchJobs(String batchFilter){		
		 Query query =  HibernateUtil.getSession().getNamedQuery("getNumJobsInBatch");
	     query.setInteger("batchId", Integer.parseInt(undecorate(batchFilter)));
	     return (Integer) query.list().get(0);
	}
	
	public String undecorate(String batchFilter){
		if (batchFilter.startsWith(BatchJob.BATCH_KEY)){
			return batchFilter.substring(BatchJob.BATCH_KEY.length());
		}else{
			return batchFilter;
		}
	}

}
