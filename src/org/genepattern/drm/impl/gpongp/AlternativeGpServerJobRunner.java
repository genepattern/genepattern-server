package org.genepattern.drm.impl.gpongp;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.impl.local.LocalJobRunner;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.JobInfoWrapper.InputFile;
import org.genepattern.server.JobInfoWrapper.ParameterInfoWrapper;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.jobresult.JobResult;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.client.GPClient;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class AlternativeGpServerJobRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(AlternativeGpServerJobRunner.class);
    GpConfig config;
    
    public AlternativeGpServerJobRunner(){
    

    
    }
    
    @Override
    public void stop() {
        // TODO Auto-generated method stub
        
        
        log.error("AlternativeGpServerJobRunner STOP NOT IMPLEMENTED XXX");
    }

    public JobInfoWrapper getJobInfoWrapper(int jobNumber) {
        GpContext context = GpContext.getContextForUser(UIBeanHelper.getUserId());
        if (!ServerConfigurationFactory.instance().getGPBooleanProperty(context, "display.input.results", false)) { return null; }

        String userId = UIBeanHelper.getUserId();
        HttpServletRequest request = UIBeanHelper.getRequest();
        String contextPath = request.getContextPath();
        String cookie = request.getHeader("Cookie");
        JobInfoManager jobInfoManager = new JobInfoManager();
        return jobInfoManager.getJobInfo(cookie, contextPath, userId, jobNumber);
    }
    
    
    @Override
    public String startJob(DrmJobSubmission jobSubmission) throws CommandExecutorException {
        Integer externalJobId = -1;
        // XXX HACK for now just refresh this with each run
        config = jobSubmission.getGpConfig();
        GpContext jobContext = jobSubmission.getJobContext();
        String user = config.getGPProperty(jobContext, "remote.user");
        String pass = config.getGPProperty(jobContext, "remote.password");
        String gpurl = config.getGPProperty(jobContext, "remote.genepattern.url");
        try {
            GPClient gpClient = new GPClient(gpurl, user, pass);
            List<String> localFilePaths = jobContext.getLocalFilePaths();
            JobInfo ji = jobSubmission.getJobInfo();
            ParameterInfo[] pis = ji.getParameterInfoArray();
            ArrayList<Parameter> remoteParamList = new ArrayList<Parameter>();
            for (int i=0; i < pis.length; i++){
                String val = pis[i].getValue();
                if (val.indexOf("<GenePatternURL>") >= 0){
                    // turn these back into file path references
                    int idx = val.indexOf("<GenePatternURL>")+ 16;
                    String fileName = val.substring(val.lastIndexOf("/")+1);
                    for ( String filePath: localFilePaths){
                        if (filePath.endsWith(fileName)){
                            val = filePath;
                            break;
                        }
                    }   
                }  
                Parameter P = new Parameter(pis[i].getName(), val);
                remoteParamList.add(P);               
            }
            externalJobId = gpClient.runAnalysisNoWait(ji.getTaskLSID(), 
                    remoteParamList.toArray(new Parameter[0])); 
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return ""+externalJobId;
    }

    //public static String NOT_STARTED = "Pending";
    //public static String PROCESSING = "Processing";
    //public static String FINISHED = "Finished";
    //public static String ERROR = "Error";
    
    DrmJobState jobInfoStatusToDrmJobState(String jiStatus){
        
        if (jiStatus.equalsIgnoreCase(JobStatus.NOT_STARTED)){
            return DrmJobState.IS_QUEUED;
        } else if (jiStatus.equalsIgnoreCase(JobStatus.PROCESSING)){
            return DrmJobState.RUNNING;
        } if (jiStatus.equalsIgnoreCase(JobStatus.FINISHED)){
            return DrmJobState.DONE;
        } if (jiStatus.equalsIgnoreCase(JobStatus.ERROR)){
            return DrmJobState.FAILED;
        } else {
            System.out.println("UNFAMILIAR STATE " + jiStatus);
            return DrmJobState.UNDETERMINED;      
        }
    }
    
    static Map<String,String> specialRemoteFileNameMap = null;
    private static Map<String, String> getSpecialRemoteFileNames(){
        if (specialRemoteFileNameMap == null){
            specialRemoteFileNameMap = new HashMap<String,String>();
            specialRemoteFileNameMap.put("stdout.txt", "remote_stdout.txt");
            specialRemoteFileNameMap.put("stderr.txt", "remote_stderr.txt");
            specialRemoteFileNameMap.put("gp_execution_log.txt", ".remote_gp_execution_log.txt");
        }
        return specialRemoteFileNameMap;
    }
    
    
    
    @Override
    public DrmJobStatus getStatus(DrmJobRecord drmJobRecord) {
        // TODO Auto-generated method stub
        final String drmJobId=drmJobRecord.getExtJobId();
        final Integer localJobId = drmJobRecord.getGpJobNo();
        try {
            final HibernateSessionManager mgr=org.genepattern.server.database.HibernateUtil.instance();   
            GpContext context = GpContext.createContextForJob(mgr, new Integer(localJobId));
            String user = config.getGPProperty(context, "remote.user");
            String pass = config.getGPProperty(context, "remote.password");
            String gpurl = config.getGPProperty(context, "remote.genepattern.url");
            String delete = config.getGPProperty(context, "delete.on.remote.on.completion");
            AnalysisWebServiceProxy analysisProxy = new AnalysisWebServiceProxy(gpurl, user, pass, false);
            analysisProxy.setTimeout(Integer.MAX_VALUE);
            JobInfo ji = analysisProxy.checkStatus(new Integer(drmJobId));
            
            String status = ji.getStatus();
            
            DrmJobState state = jobInfoStatusToDrmJobState(status);
            DrmJobStatus drmJobStatus=new DrmJobStatus.Builder(drmJobId,state).build();
            if (drmJobStatus.getJobState().is(DrmJobState.TERMINATED)){
                System.out.println("JOB IS DONE " + status) ;
                String resultFiles[] = analysisProxy.getResultFiles(ji.getJobNumber());
                for (int i=0; i < resultFiles.length;i++){
                    
                    File dir = drmJobRecord.getWorkingDir();
                    int idx = resultFiles[i].indexOf(".att_");
                    String name = resultFiles[i].substring(idx+5);
                    
                    // avoid stomping on the special files
                    if (getSpecialRemoteFileNames().keySet().contains(name)){
                        name = getSpecialRemoteFileNames().get(name);
                    }
                    File newFile = new File(dir, name);
                    //System.out.println("Moving output file to "+ newFile.getAbsolutePath());
                    File origFile = new File(resultFiles[i]);
                    boolean fileMoveSuccess = origFile.renameTo(newFile);
                }
                Boolean delRemote = new Boolean(delete);
                try {
                    if (delRemote) {
                        analysisProxy = new AnalysisWebServiceProxy(gpurl, user, pass, false);
                        analysisProxy.deleteJob(new Integer(drmJobId));  
                    }
                    
                } catch (Exception e){
                    e.printStackTrace();
                    log.error(e);
                }
            }
            return drmJobStatus;
        }
        catch (NumberFormatException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (Throwable e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
          
        return new DrmJobStatus.Builder(drmJobId, DrmJobState.FAILED).build();
    }

    @Override
    public boolean cancelJob(DrmJobRecord jobRecord) throws Exception {
        // final Future<DrmJobStatus> task=runningTasks.get(jobRecord.getExtJobId());
        log.error("AlternativeGpServerJobRunner CANCEL NOT IMPLEMENTED XXX");
        final String drmJobId=jobRecord.getExtJobId();
        try {
            final HibernateSessionManager mgr=org.genepattern.server.database.HibernateUtil.instance(); 
            final Integer localJobId = jobRecord.getGpJobNo();
            
            Integer jobId = new Integer(localJobId);
            
            GpContext context = GpContext.createContextForJob(mgr, jobId);
            
            String user = config.getGPProperty(context, "remote.user");
            String pass = config.getGPProperty(context, "remote.password");
            String gpurl = config.getGPProperty(context, "remote.genepattern.url");
            Boolean delete = config.getGPBooleanProperty(context, "delete.on.remote.on.completion");
     
       
            AnalysisWebServiceProxy analysisProxy = new AnalysisWebServiceProxy(gpurl, user, pass, true);
            analysisProxy.setTimeout(Integer.MAX_VALUE);
            analysisProxy.terminateJob(jobId);
            
            try {
                System.out.println("Deleting remote " + drmJobId);
                if (delete) analysisProxy.deleteJob(new Integer(drmJobId));
            } catch (Exception e){
                log.error(e);
            }
            
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        
    }

}
