package org.genepattern.server.executor.serverthread;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.JobManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.pipeline.PipelineHandler;
import org.genepattern.server.jobqueue.JobQueue;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * For the PathSeq pipeline, Blast subtraction. 
 * 
 * Starting with initial input fq1 file, run a Blast aligner for each
 * pair of aligner type and database. Use the output of each run as input to the next run.
 * 
 * To use the BlastLoop, create a module with the following command line:
 * org.genepattern.server.executor.serverthread.BlastLoop \
 *     fq1.file=<initial.fq1.file> \
 *     aligner=<aligner.paramlist> \
 *     db=<db.paramlist> \
 *     args=<args.paramlist>
 * 
 * The first entry in the arg list is the 'use output from prev step' param.
 * 
 * @author pcarr
 */
public class BlastLoop extends AbstractServerTask {
    private static Logger log = Logger.getLogger(BlastLoop.class);
    
    //local variables 
    private int parentJobId = -1;
    private BatchParams batchParams = new BatchParams();
    private String inputFileParamName = "";
    private String outputFilenameParamName = "ParsedBlastParser4.output.filename";
    private String finalOutputFilename = "<fq1.file_basename>.final.unmapped.fastq";
    
    private AddToFlag addToFlag = AddToFlag.add_to_child;

    public String call() throws Exception {
        log.debug("starting BwaLoop");
        parseArgs();
        doLoop();
        return JobStatus.PROCESSING;
    }
    
    private void parseArgs() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("parsing args...");
            for(int i=0; i<args.length; ++i) {
                String arg = args[i];
                log.debug("\targ["+i+"]='"+arg+"'");
            }
        }
        
        //#) the next arg must be the name of the final fastq output file, e.g.
        SplitArg outputSpec = splitSplit(args[0], '=');
        if (outputSpec == null) {
            throw new Exception("Error parsing command line args[2]='"+args[2]+"': outputSpec == null");
        }
        if (outputSpec.getNames().get(0) == null) {
            throw new Exception("Error parsing command line args[2]='"+args[2]+"': outputSpec.names[0] == null");
        }
        this.outputFilenameParamName =  outputSpec.getNames().get(0);
        if (outputSpec.getValues().get(0) == null) {
            throw new Exception("Error parsing command line args[2]='"+args[2]+"': outputSpec.values[0] == null");
        }
        this.finalOutputFilename = outputSpec.getValues().get(0);
        
        //#) hard-code the addTo parameter (not parsed from the command line)
        addToFlag = AddToFlag.add_to_child; //defalt, add each job as a child of this job
        
        //helper field, in case two or more parameters (by name) have the same filelist value
        Map<String,List<String>> parameterListCache = new HashMap<String,List<String>>();

        log.debug("parsing batch params from command line args...");
        boolean first = true;
        for(int idx=1; idx<args.length; ++idx) {
            String arg = args[idx];
            log.debug("parsing args["+idx+"]='"+args[idx]+"'");
            SplitArg splitArg = splitSplit(arg, '=');
            for(final String name : splitArg.getNames()) {
                for(final String value : splitArg.getValues()) {
                    if (first) {
                        //HACK: the first entry in the arg list is the 'use output from prev step' param
                        inputFileParamName = name;
                        first = false;
                    } 
                    if (isParameterList(value)) {
                        //do something
                        List<String> parsedParameters = parameterListCache.get(value);
                        if (parsedParameters == null) {
                            parsedParameters = parseParameterList(value);
                            parameterListCache.put(value,  parsedParameters);
                        } 
                        for(String parsedParam : parsedParameters) {
                            batchParams.addEntry(name, parsedParam);
                        }
                    }
                    else {
                        batchParams.addEntry(name, value);
                    }
                }
            }            
        }
        log.debug("validating batch params");
        batchParams.validate();
    }
    
    private boolean isParameterList(String value) {
        return value != null && ( value.endsWith("filelist") || value.endsWith("filelist.txt") );
    }

    private List<String> parseParameterList(String value) throws Exception {
        log.debug("initializing file list from file: "+value);
        File parameterList = new File(value);
        List<String> rval = PipelineHandler.parseFileList(parameterList);
        log.debug("...done!");
        return rval;
    }

    private void doLoop() throws Exception {
        log.debug("adding jobs...");
        try {
            HibernateUtil.beginTransaction();
            JobInfo self = PipelineHandler.getJobInfo(jobId);
            
            switch (addToFlag) {
            case add_to_child:
                parentJobId = self.getJobNumber();
                break;
            //big mess, add_to_root and add_to_parent use the same logic, notice there is no 'break'
            case add_to_root:
                log.error("Add to root not implemeneted, adding job to parent");
            case add_to_parent:
                parentJobId = self._getParentJobNumber();
                if (parentJobId < 0) {
                    parentJobId = self.getJobNumber();
                }
                break;
            case add_to_queue:
                parentJobId = -1;
                break;
            }; 
            int numJobs = batchParams.getNumJobsToSubmit();
            for(int stepIdx = 0; stepIdx < numJobs; ++stepIdx) {
                addStep(stepIdx);
            }
            HibernateUtil.commitTransaction();
            CommandManagerFactory.getCommandManager().wakeupJobQueue();
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error adding jobs", t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    /**
     * Add a new step to the pipeline.
     * 
     * currentStepIdx, the counter (starting at 0) for each new job added to the parent pipeline.
     * @throws Exception
     */
    private JobInfo addStep(int currentStepIdx) throws Exception {
        final boolean first = currentStepIdx==0;
        final boolean last = currentStepIdx==(batchParams.getNumJobsToSubmit()-1);
        
        boolean isInTransaction = HibernateUtil.isInTransaction(); 
        HibernateUtil.beginTransaction();
        try {
            AdminDAO adminDao = new AdminDAO();
            
            String tasknameOrLsid = batchParams.get("aligner", currentStepIdx); 
            TaskInfo taskInfo = null;
            try {
                taskInfo = adminDao.getTask(tasknameOrLsid, userId);
            }
            catch (Throwable t) {
                String errorMessage = 
                        "Error creating taskInfo for task='"+tasknameOrLsid+"', userId='"+userId+"': "+t.getLocalizedMessage();
                throw new Exception(errorMessage, t );
            }
            if (taskInfo == null) {
                String errorMessage = 
                        "Missing module '"+tasknameOrLsid+"' for userId='"+userId;
                throw new Exception(errorMessage);
            }
            ParameterInfo[] parameterInfoArray = taskInfo.getParameterInfoArray();
            PipelineHandler.substituteLsidInInputFiles(taskInfo.getLsid(), parameterInfoArray);
            
            for(ParameterInfo param : parameterInfoArray) {
                final boolean matchLast = true;
                String value = batchParams.get(param.getName(), currentStepIdx, matchLast); 
                if (value == null) {
                    value = param.getDefaultValue();
                    if (last) {
                        //special-case: set a unique output filename for the last step in the loop
                        String pname = param.getName();
                        if (pname.endsWith(this.outputFilenameParamName)) {
                            value = this.finalOutputFilename;
                        }
                    }
                }
                boolean useOutputFromPrevStepParam = param.getName().endsWith(inputFileParamName);
                if (useOutputFromPrevStepParam) {
                    if (first) {
                        param.setValue(value);
                        param.getAttributes().remove(PipelineModel.INHERIT_FILENAME);
                        param.getAttributes().remove(PipelineModel.INHERIT_TASKNAME);
                    }
                    else {
                        final int inheritTaskIdx = (currentStepIdx - 1); //use output from previous step
                        final String inheritFilename = "fq1"; //use (first) fq1 output from the previous step
                        param.getAttributes().put(PipelineModel.INHERIT_FILENAME, inheritFilename);
                        param.getAttributes().put(PipelineModel.INHERIT_TASKNAME, ""+inheritTaskIdx);
                        param.getAttributes().remove("MODE");
                    }
                }
                else if (value != null) {
                    param.setValue(value);
                }
                
            }
            JobQueue.Status initialStatus = JobQueue.Status.WAITING;
            if (first) {
                initialStatus = JobQueue.Status.PENDING;
            }
            
            if (parameterInfoArray != null) {
                for (int i = 0; i < parameterInfoArray.length; i++) {
                    if (parameterInfoArray[i].isInputFile()) {
                        String file = parameterInfoArray[i].getValue(); // bug 724
                        if (file != null && file.trim().length() != 0) {
                            String val = file;
                            try {
                                new URL(file);
                            } 
                            catch (MalformedURLException e) {
                                val = new File(file).toURI().toString();
                            }
                            parameterInfoArray[i].setValue(val);
                            parameterInfoArray[i].getAttributes().remove("TYPE");
                            parameterInfoArray[i].getAttributes().remove("MODE");
                        }
                    }
                }
            }
            
            JobInfo jobInfo = JobManager.addJobToQueue(taskInfo, userId, parameterInfoArray, parentJobId, initialStatus);
            return jobInfo;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        } 
    }

}
