package org.genepattern.drm.impl.gpongp;

import java.io.File;
import java.net.URI;
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
import org.genepattern.server.rest.client.GenePatternRestApiV1Client;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.client.GPClient;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
            System.out.println("--------------- --- -- - submitting remote job to " + gpurl +" as " +user);
            GPClient gpClient = new GPClient(gpurl, user, pass);
            
            GenePatternRestApiV1Client gpRestClient = new GenePatternRestApiV1Client(gpurl, user, pass);
            
            
            List<String> localFilePaths = jobContext.getLocalFilePaths();
            JobInfo ji = jobSubmission.getJobInfo();
            ParameterInfo[] pis = ji.getParameterInfoArray();

            final JsonArray paramsJsonArray=new JsonArray();
            
            for (int i=0; i < pis.length; i++){
              String val = pis[i].getValue();
              JsonObject P = null;
              if (val.indexOf("<GenePatternURL>") >= 0){
                  // turn these back into file path references
                  String fileName = val.substring(val.lastIndexOf("/")+1);
                  for ( String filePath: localFilePaths){
                      if (filePath.endsWith(fileName)){
                          val = filePath;
                          //P = new Parameter(pis[i].getName(), new File(val));
                          Object value2 = gpRestClient.uploadFileIfNecessary(true, val);
                          P = gpRestClient.createParameterJsonObject("input.filename", value2);
                          break;
                      }
                  }   
              }  else {
                  P = gpRestClient.createParameterJsonObject(pis[i].getName(), val);
              } 
              paramsJsonArray.add(P);               
           }
            
           
           final JsonObject jobJsonObj=new JsonObject();
           jobJsonObj.addProperty("lsid", ji.getTaskLSID()); 
           jobJsonObj.add("params", paramsJsonArray);
           URI JobStatusUri = gpRestClient.submitJob(jobJsonObj);
           String uriStr = JobStatusUri.getRawPath();
           
           externalJobId = new Integer(uriStr.substring(uriStr.lastIndexOf('/')+1));
           
           System.out.println("Launched remote job ID: "+ externalJobId);
           
        } catch (Exception e) {
            System.out.println("--------------- --- -- - Failed to start remote job");
            e.printStackTrace();
        }
        
        return ""+externalJobId;
    }

    //public static String NOT_STARTED = "Pending";
    //public static String PROCESSING = "Processing";
    //public static String FINISHED = "Finished";
    //public static String ERROR = "Error";
    
    DrmJobState jobInfoStatusToDrmJobState(String jiStatus){
        try {
            // we confuse this server if we report the job state as being queued when it has been dispat hed to the other
            // server.  From the POV of this server its running even if its queued on the other side
            DrmJobState state = DrmJobState.valueOf(jiStatus);
            if (state.is(DrmJobState.IS_QUEUED)){
                return DrmJobState.RUNNING;
            } else {
                return state;
            }
        } catch(Exception e){
            // If only the various parts of GP were consistent
        }
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
            //AnalysisWebServiceProxy analysisProxy = new AnalysisWebServiceProxy(gpurl, user, pass, false);
            //analysisProxy.setTimeout(Integer.MAX_VALUE);
            //JobInfo ji = analysisProxy.checkStatus(new Integer(drmJobId));
            
            System.out.println("Gettin status for local: " + localJobId + " and remote: "+ drmJobId );
            
            GenePatternRestApiV1Client gpRestClient = new GenePatternRestApiV1Client(gpurl, user, pass);
            JsonObject statusJsonObj = gpRestClient.getJobStatus(drmJobId);
            String status = statusJsonObj.getAsJsonObject("status").get("statusFlag").getAsString();
            
            
            
            DrmJobState state = jobInfoStatusToDrmJobState(status);
            DrmJobStatus drmJobStatus=new DrmJobStatus.Builder(drmJobId,state).build();
            if (statusJsonObj.getAsJsonObject("status").get("isFinished").getAsBoolean()){
                System.out.println("JOB IS DONE " + status) ;
                //String resultFiles[] = analysisProxy.getResultFiles(ji.getJobNumber());
                JsonArray outputFiles = statusJsonObj.getAsJsonArray("outputFiles");
                File dir = drmJobRecord.getWorkingDir();
                for (int i=0; i < outputFiles.size();i++){
                    String outFileUrl = outputFiles.get(i).getAsJsonObject().get("link").getAsJsonObject().get("href").getAsString();
                    
                    String name = outFileUrl.substring(outFileUrl.lastIndexOf('/'));
                    
                    // avoid stomping on the special files
                    if (getSpecialRemoteFileNames().keySet().contains(name)){
                        name = getSpecialRemoteFileNames().get(name);
                    }
                    gpRestClient.getOutputFile(outFileUrl, dir, name);
                    
                    
                }
                Boolean delRemote = new Boolean(delete);
                try {
                    if (delRemote) {
                        //analysisProxy = new AnalysisWebServiceProxy(gpurl, user, pass, false);
                        //analysisProxy.deleteJob(new Integer(drmJobId));  
                        gpRestClient.deleteJob(drmJobId);
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
     
       
            GenePatternRestApiV1Client gpRestClient = new GenePatternRestApiV1Client(gpurl, user, pass);
            gpRestClient.terminateJob(drmJobId);
            
            
            try {
                System.out.println("Deleting remote " + drmJobId);
                if (delete) gpRestClient.terminateJob(drmJobId);
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
