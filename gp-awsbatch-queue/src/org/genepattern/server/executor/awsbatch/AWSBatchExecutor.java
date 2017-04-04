package org.genepattern.server.executor.awsbatch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.executor.CommandExecutor2;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.drm.JobExecutor;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;

import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;




public class AWSBatchExecutor extends JobExecutor implements CommandExecutor2 {
    private static final Logger log = Logger.getLogger(AWSBatchExecutor.class);
    
    
    
    
    /**
     * See if we can get everything we need to call AWS properly from here
     * 
     * Class Specific Config
     *     aws queue name
     *     S3 root name
     * 
     * Module specific Config    
     *     tasklib location
     *     input filename typed parameters
     *     
     *  Job Specific config
     *     working dir
     *     input file directory dir
     *        
     *     #
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#
TASKLIB=$TEST_ROOT/R313_cli/tests/affy/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/R313_cli/tests/affy/data
JOB_DEFINITION_NAME="R313_Generic"
JOB_ID=gp_job_AffyST_R313_$1
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/R313_cli/tests/affy/job_12345

     * 
     */
    
    
    public void runCommand(final GpContext jobContext, String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, File stdinFile) throws CommandExecutorException {
        
        ArrayList<String> fileParams = new ArrayList<String>();
        log.error("Tasklib is " +  jobContext.getTaskLibDir().getAbsolutePath() );
        System.out.println(" ===>  Tasklib is " +  jobContext.getTaskLibDir().getAbsolutePath());
        System.out.println(" ===>  working dir  is " +  runDir.getAbsolutePath());
        
        try {
            File InputDir = runDir.getParentFile().createTempFile("forBatch", "aws");
            
            System.out.println(" ===>  INPUT dir  is " +  InputDir.getAbsolutePath());
            if(!(InputDir.delete())) {
                throw new IOException("Could not delete temp file: " + InputDir.getAbsolutePath());
            }
            if(!(InputDir.mkdir())) {
                throw new IOException("Could not create temp directory: " + InputDir.getAbsolutePath());
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        
       
        environmentVariables.put("TASKLIB", jobContext.getTaskLibDir().getAbsolutePath());
        environmentVariables.put("WORKING_DIR", runDir.getAbsolutePath());
         
        super.runCommand( jobContext,commandLine, environmentVariables, runDir, stdoutFile, stderrFile, stdinFile);
    }
    
    
    protected static ParameterInfo getFormalParam(final Map<String,ParameterInfoRecord> paramInfoMap, final String pname) {

        if (paramInfoMap == null || ! paramInfoMap.containsKey(pname)) {

            return null;

        }

        return paramInfoMap.get(pname).getFormal();

    }

    
    protected void logInputFiles(final DrmJobSubmission gpJob) {

        if (log.isDebugEnabled()) {

            final Map<String,ParameterInfoRecord> paramInfoMap =

                    ParameterInfoRecord.initParamInfoMap(gpJob.getJobContext().getTaskInfo());

            // walk through all of the input values

            final JobInput jobInput=gpJob.getJobContext().getJobInput();

            for(final Entry<ParamId, Param> entry : jobInput.getParams().entrySet()) {

                final String pname = entry.getKey().getFqName();

                final Param param = entry.getValue();

                final ParameterInfo formalParam = getFormalParam(paramInfoMap, pname);

                if (formalParam != null && formalParam.isInputFile()) {

                    int i=0;

                    for(final ParamValue paramValue : param.getValues()) {

                        log.debug(""+pname+"["+i+"]: "+paramValue.getValue());

                    }

                }

            }

        }

    }
    
    
    
}
