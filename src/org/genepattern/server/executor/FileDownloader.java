package org.genepattern.server.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.job.input.choice.Choice;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoFileCache;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for the JobSubmitter, so that we can wait for all external file drop-down selections
 * to be downloaded to the cache before starting the job.
 * 
 * @author pcarr
 *
 */
public class FileDownloader {
    final GetTaskStrategy getTaskStrategy;
    final Integer jobId;
    final JobInfo jobInfo;
    final List<Choice> selectedChoices;
                
    public FileDownloader(final Integer jobId) throws JobDispatchException {
        this(jobId, null);
    }
    public FileDownloader(final Integer jobId, final GetTaskStrategy getTaskStrategyIn) throws JobDispatchException {
        this.jobId=jobId;
        if (getTaskStrategyIn == null) {
            getTaskStrategy=new GetTaskStrategyDefault();
        }
        else {
            getTaskStrategy=getTaskStrategyIn;
        }
        this.jobInfo=initJobInfo();
        this.selectedChoices=initSelectedChoices();
    }

    private JobInfo initJobInfo() throws JobDispatchException {
        JobInfo jobInfo = null;
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            AnalysisDAO dao = new AnalysisDAO();
            jobInfo = dao.getJobInfo(jobId);
        }
        catch (Throwable t) {
            throw new JobDispatchException("Server error: Not able to load jobInfo for jobId: "+jobId, t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return jobInfo;
    }

    private List<Choice> initSelectedChoices() {
        List<Choice> selectedChoices=null;
        final TaskInfo taskInfo=getTaskStrategy.getTaskInfo(jobInfo.getTaskLSID());
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        for(final ParameterInfo pinfo : jobInfo.getParameterInfoArray()) {
            final ParameterInfoRecord pinfoRecord=paramInfoMap.get( pinfo.getName() );
            final ChoiceInfo choiceInfo=ChoiceInfoHelper.initChoiceInfo(pinfoRecord, pinfo);
            final Choice selectedChoice= choiceInfo == null ? null : choiceInfo.getValue(pinfo.getValue());
            final boolean isFileChoiceSelection=
                    pinfoRecord.getFormal().isInputFile()
                    &&
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
        if (selectedChoices==null) {
            return Collections.emptyList();
        }
        return selectedChoices;
    }

    public boolean hasSelectedChoices() {
        return selectedChoices != null && selectedChoices.size()>0;
    }

    public void startDownloadAndWait() throws InterruptedException, ExecutionException {
        if (selectedChoices == null) {
            return;
        }
        
        List<Choice> copy=new ArrayList<Choice>(selectedChoices);
        List<Choice> toRemove=new ArrayList<Choice>();
        while (copy.size()>0) {
            toRemove.clear();
            for(final Choice selectedCopy : copy) {
                try {
                    //this method throws a TimeoutException if the download is not complete
                    final GpFilePath cachedFile=ChoiceInfoFileCache.instance().getCachedGpFilePath(selectedCopy);
                    toRemove.add(selectedCopy);
                }
                catch (TimeoutException e) {
                    //skip, it means the file is still downloading
                }
            }
            for(Choice choiceToRemove : toRemove) {
                copy.remove(choiceToRemove);
            }
        }
    }
}
