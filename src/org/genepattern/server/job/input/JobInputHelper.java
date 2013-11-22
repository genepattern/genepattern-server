package org.genepattern.server.job.input;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.job.input.JobInput.ParamId;
import org.genepattern.server.job.input.batch.BatchInputFileHelper;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApi;
import org.genepattern.server.rest.JobInputApiFactory;
import org.genepattern.server.rest.JobReceipt;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for the job input form, includes methods for preparing both batch jobs and regular jobs.
 * 
 * For batch jobs, you can either explicitly add all batch input values with the #addBatchValue method.
 * Alternatively, you can declare a batch job with the #addBatchDirectory method.
 * This is based on the pre-existing 3.5.0 and earlier method. Which has the following limitations:
 *     1) can only batch on input files, not other types
 *     2) the value for a batch input parameter must be to one and only one directory on the file system:
 *        can be on the server file path or in the user upload tab.
 *     3) when there are multiple batch input parameters, the intersection of all matching parameters determines
 *        how many jobs to run. Matches are based on file basename.
 * 
 * @author pcarr
 *
 */
public class JobInputHelper {
    final static private Logger log = Logger.getLogger(JobInputHelper.class);
    /**
     * Is the input value an external URL?
     * 
     * @param value
     * 
     * @return the URL if it's an external url, otherwise return null.
     */
    public static URL initExternalUrl(final String value) {
        log.debug("intialize external URL for value="+value);
        if (value==null) {
            throw new IllegalArgumentException("value==null");
        }

        if (value.startsWith("<GenePatternURL>")) {
            log.debug("it's a substition for the gp url");
            return null;
        }
        if (value.startsWith(GpFilePath.getGenePatternUrl().toExternalForm())) {
            log.debug("it's a gp url");
            return null;
        }

        URL url=null;
        try {
            url=new URL(value);
        }
        catch (MalformedURLException e) {
            log.debug("it's not a url", e);
            return null;
        }
        
        //special-case for file:/// urls
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            log.debug("it's a file url, assume it's a local path: "+value);
            return null;
        }
        return url;
    }

    private Context userContext=null;
    private final JobInputApi singleJobInputApi;
    private final GetTaskStrategy getTaskStrategy;
    private final BatchInputFileHelper batchInputFileHelper;
    
    public JobInputHelper(final Context userContext, final String lsid) {
        this(userContext, lsid, null);
    }
    public JobInputHelper(final Context userContext, final String lsid, final JobInputApi singleJobInputApi) {
        this(userContext, lsid, singleJobInputApi, null);
    }
    public JobInputHelper(final Context userContext, final String lsid, final JobInputApi singleJobInputApiIn, final GetTaskStrategy getTaskStrategyIn) {
        this.userContext=userContext;
        //inputTemplate.setLsid(lsid);
        if (getTaskStrategyIn == null) {
            getTaskStrategy=new GetTaskStrategyDefault();
        }
        else {
            getTaskStrategy=getTaskStrategyIn;
        }
        if (singleJobInputApiIn == null) {
            this.singleJobInputApi=JobInputApiFactory.createJobInputApi(userContext);
        }
        else {
            this.singleJobInputApi=singleJobInputApiIn;
        }

        final TaskInfo taskInfo = getTaskStrategy.getTaskInfo(lsid);
        this.batchInputFileHelper=new BatchInputFileHelper(userContext, taskInfo);
    }
    
    /**
     * Add a value for a non-batch parameter. 
     * 
     * @param name
     * @param value
     */
    public void addValue(final String name, final String value) {
        this.batchInputFileHelper.addValue(name, value);
    }

    /**
     * Add a value for a non-batch parameter. 
     * 
     * @param paramId
     * @param value
     */
    public void addValue(final ParamId paramId, final String value) {
        this.batchInputFileHelper.addValue(paramId.getFqName(), value);
    }
    
    /**
     * Add a value for a batch parameter.
     * @param name
     * @param value
     */
    public void addBatchValue(final String name, final String value) throws GpServerException {
        addBatchValue(new ParamId(name), value);
    }
    
    /**
     * Add a value for a batch parameter.
     * @param paramId
     * @param value
     */
    public void addBatchValue(final ParamId paramId, final String value) throws GpServerException {
        this.batchInputFileHelper.addBatchValue(paramId.getFqName(), value);
    }

    /*
     * Add a value for a parameter which could be either a single or batch parameter
     * @param pInfo - A ParameterInfo object
     * @param value - The value to set the parameter identified by pInfo
     * @param isBatch  - Whether the parameter is a batch parameter
     */
    public void addSingleOrBatchValue(final ParameterInfo pInfo, String value, boolean isBatch)
    throws GpServerException
    {
        if (isBatch) {
            batchInputFileHelper.addBatchValue(pInfo.getName(), value);
        }
        else {
            batchInputFileHelper.addValue(pInfo.getName(), value);
        }
    }

    /**
     * When you assign an input parameter (e.g. 'input.file') to a batch directory, you are telling the server to
     * automatically generate a batch of jobs, one for each matching file in the batch directory.
     * 
     * A file matches based on the file extension of the file and the list of accepted fileFormats for the parameter,
     * as declared in the module manifest.
     * 
     *
     */
    public void addBatchDirectory(final String name, final String value) throws GpServerException {
        addBatchDirectory(new ParamId(name), value);
    }
    
    public void addBatchDirectory(final ParamId id, final String value) throws GpServerException {
        this.batchInputFileHelper.addBatchValue(id.getFqName(), value);
    }
    
    /**
     * After you initialize all of the values, call this method to create the
     * list of JobInputs, one for each new job to run.
     * 
     * @return
     * @throws GpServerException
     */
    public List<JobInput> prepareBatch() throws GpServerException {
        return batchInputFileHelper.prepareBatch();
    }

    /**
     * Submit your jobs to the GP server.
     * Use the list of JobInput from the prepareBatch() method as input to this method.
     * 
     * @param batchInputs
     * @return
     * @throws GpServerException
     */
    public JobReceipt submitBatch(final List<JobInput> batchInputs) throws GpServerException {
        JobReceipt receipt=new JobReceipt();
        for(JobInput batchInput : batchInputs) {
            String jobId = singleJobInputApi.postJob(userContext, batchInput);
            receipt.addJobId(jobId);
        }
        
        if (receipt.getJobIds().size()>1) {
            //record batch job to DB, optionally assign custom batch id
            final String batchId=recordBatchJob(userContext, receipt);
            receipt.setBatchId(batchId);
        }
        return receipt;
    }

//    public static String getBaseFilename(final GpFilePath file) {
//        if (file==null) {
//            log.error("file==null");
//            return "";
//        }
//        String filename=null;       
//        if (file.getName()!=null) {
//            filename=file.getName();
//        }
//        else {
//            try {
//                URL url=file.getUrl();
//                if (url != null) {
//                    String path=url.getPath();
//                    if (path != null) {
//                        filename=new File(path).getName();
//                    }
//                }
//            }
//            catch (Exception e) {
//                log.error(e);
//            }
//        }
//        if (filename==null) {
//            log.error("error getting filename for file="+file);
//            return "";
//        }
//        int periodIndex = filename.lastIndexOf('.');
//        if (periodIndex > 0) {
//            return filename.substring(0, periodIndex);
//        }
//        else {
//            return filename;
//        }
//    }
//    
    private String recordBatchJob(final Context userContext, final JobReceipt jobReceipt) throws GpServerException {
        //legacy implementation, based on code in SubmitJobServlet
        String batchId="";
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            BatchJob batchJob = new BatchJob(userContext.getUserId());

            for(final String jobId : jobReceipt.getJobIds()) {
                int jobNumber = Integer.parseInt(jobId);
                batchJob.getBatchJobs().add(new AnalysisJobDAO().findById(jobNumber));
            }
            new BatchJobDAO().save(batchJob);
            batchId = ""+batchJob.getJobNo();
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new GpServerException("Error recording batch jobs to DB", t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return batchId;
    }

}
