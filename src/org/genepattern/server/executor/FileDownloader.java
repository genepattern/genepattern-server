package org.genepattern.server.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.job.input.cache.FileCache;
import org.genepattern.server.job.input.choice.Choice;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for the JobSubmitter, for downloading external file drop-down selections
 * to the cache before starting a job.
 * 
 * @author pcarr
 *
 */
public class FileDownloader {
    private static final Logger log = Logger.getLogger(FileDownloader.class);

    private final List<Choice> selectedChoices;
    
    /**
     * Initialize a JobInfo instance for the given jobId.
     * @param jobId
     * @return
     * @throws JobDispatchException
     */
    public static final JobInfo initJobInfo(final Integer jobId) throws JobDispatchException {
        JobInfo jobInfo = null;
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        log.debug("job #"+jobId+", isInTransaction="+isInTransaction);
        try {
            AnalysisDAO dao = new AnalysisDAO();
            jobInfo = dao.getJobInfo(jobId);
        }
        catch (Throwable t) {
            final String message="Server error: Not able to load jobInfo for jobId: "+jobId;
            log.debug(message, t);
            throw new JobDispatchException("Server error: Not able to load jobInfo for jobId: "+jobId, t);
        }
        finally {
            if (!isInTransaction) {
                log.debug("job #"+jobId+", closing DB session");
                HibernateUtil.closeCurrentSession();
            }
        }
        return jobInfo;
    }
    
    /**
     * Create a new downloader for the given job based on the jobId.
     * 
     * @param jobId
     * @return
     * @throws JobDispatchException
     */
    public static final FileDownloader fromJobInfo(final JobInfo jobInfo) throws JobDispatchException {
        return new FileDownloader(jobInfo);
    }
                
    private FileDownloader(final JobInfo jobInfo) {
        this(jobInfo, null);
    }
    private FileDownloader(final JobInfo jobInfo, final GetTaskStrategy getTaskStrategyIn) {
        final GetTaskStrategy getTaskStrategy;
        if (getTaskStrategyIn == null) {
            getTaskStrategy=new GetTaskStrategyDefault();
        }
        else {
            getTaskStrategy=getTaskStrategyIn;
        }
        final TaskInfo taskInfo=getTaskStrategy.getTaskInfo(jobInfo.getTaskLSID());
        this.selectedChoices=initSelectedChoices(taskInfo, jobInfo);        
    }


    /**
     * Initialize a list of selected Choices for the given job. For each input parameter, if it has a file drop-down
     * (aka Choice) and the runtime value was selected from the drop-down, then add it to the list.
     * 
     * @param taskInfo
     * @param jobInfo
     * @return an empty list of the job has no input values from a file drop-down selection.
     */
    private static List<Choice> initSelectedChoices(final TaskInfo taskInfo, final JobInfo jobInfo) {
        List<Choice> selectedChoices=null;
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        for(final ParameterInfo pinfo : jobInfo.getParameterInfoArray()) {
            final ParameterInfoRecord pinfoRecord=paramInfoMap.get( pinfo.getName() );
            if (pinfoRecord==null) {
                //skip, probably here because it's a completed job
                log.debug("skipping param="+pinfo.getName());
            }
            else {
                final ChoiceInfo choiceInfo=ChoiceInfoHelper.initChoiceInfo(pinfoRecord, pinfo);
                final Choice selectedChoice= choiceInfo == null ? null : choiceInfo.getValue(pinfo.getValue());
                final boolean isFileChoiceSelection=
                        pinfoRecord.getFormal().isInputFile() &&
                        selectedChoice != null && 
                        selectedChoice.getValue() != null && 
                        selectedChoice.getValue().length() > 0;
                if (isFileChoiceSelection) {
                    //lazy-init the list
                    if (selectedChoices==null) {
                        selectedChoices=new ArrayList<Choice>();
                    }
                    selectedChoices.add(selectedChoice);
                }
            }
        }
        if (selectedChoices==null) {
            return Collections.emptyList();
        }
        return selectedChoices;
    }

    /**
     * Does the job have at least one input selected from a file drop-down?
     * @return
     */
    public boolean hasSelectedChoices() {
        return selectedChoices != null && selectedChoices.size()>0;
    }

    /**
     * Call this method before running the job, it takes care of downloading any input files selected from a 
     * drop-down menu. The main purpose of this method is to wait, if necessary, for each of the files to download 
     * into the cache before proceeding.
     * 
     * @see ChoiceInfoCache
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void startDownloadAndWait() throws InterruptedException, ExecutionException {
        if (selectedChoices == null) {
            return;
        }
        if (selectedChoices.size()==0) {
            return;
        }
        
        // loop through all the choices and start downloading ...
        for(final Choice selectedChoice : selectedChoices) {
            try {
                final String selectedValue=selectedChoice.getValue();
                final boolean isDir=selectedChoice.isRemoteDir();
                final Future<?> f = FileCache.instance().getFutureObj(selectedValue, isDir);
                f.get(100, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e) {
                //skip, it means the file is still downloading
            }
            Thread.yield();
        }
        // now loop through all of the choices and wait for each download to complete
        for(final Choice selectedChoice : selectedChoices) {
            final String selectedValue=selectedChoice.getValue();
            final boolean isDir=selectedChoice.isRemoteDir();
            final Future<?> f = FileCache.instance().getFutureObj(selectedValue, isDir);
            f.get();
        }    
    }
    
}
