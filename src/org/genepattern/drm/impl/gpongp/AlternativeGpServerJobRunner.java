package org.genepattern.drm.impl.gpongp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.FileUtils;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.CpuTime;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.DataManager;
import org.genepattern.server.InputFilePermissionsHelper;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.ExternalFileManager;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.rest.client.GenePatternRestApiV1Client;
import org.genepattern.server.rest.client.TaskObj;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AlternativeGpServerJobRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(AlternativeGpServerJobRunner.class);
    GpConfig config;
    HashMap<Integer,Integer> outputFileRetryCount;
    HashMap<String,Boolean> failedFileDeletes;
    
    
    ThreadPoolExecutor executor;
    
    public AlternativeGpServerJobRunner(){
        outputFileRetryCount = new HashMap<Integer,Integer>();
        failedFileDeletes = new HashMap<String,Boolean>();
        // 3 threads standing, one each for two output files, stdout, stderr
        executor =  (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        executor.setKeepAliveTime(60, TimeUnit.SECONDS);
        executor.setMaximumPoolSize(30);
       
        
    }
    
    @Override
    public void stop() {
        // TODO Auto-generated method stub
        
        
        log.error("AlternativeGpServerJobRunner STOP NOT IMPLEMENTED XXX");
    }

    public JobInfoWrapper getJobInfoWrapper(int jobNumber) {
        String userId = UIBeanHelper.getUserId();
        String contextPath = config.getGpPath();
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        String cookie = "";
        JobInfoManager m = new JobInfoManager();
        return m.getJobInfo(cookie, contextPath, userId, jobNumber);
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
        ExternalFileManager externalFileManager = DataManager.getExternalFileManager(jobContext);
        
        try {
            System.out.println("--------------- --- -- - submitting remote job to " + gpurl +" as " +user);
             
            GenePatternRestApiV1Client gpRestClient = new GenePatternRestApiV1Client(gpurl, user, pass);           
            JobInfo ji = jobSubmission.getJobInfo();
            
            // verify the module exists on the remote GP server
            try {
                System.out.println(" -- validating LSID on remote " + ji.getTaskLSID());
                TaskObj to = gpRestClient.getTaskObj(ji.getTaskLSID());
                System.out.println("Found Task obj " + to);
            } catch (Exception e){ 
                log.error(e);
                e.printStackTrace();
                throw new CommandExecutorException("Module "+ ji.getTaskName() + " with LSID: "+ 
                            ji.getTaskLSID()+" is not on the remote system designated to execute it."+
                            "  Contact your admin to inform them.");
            }
            
            
            
            ParameterInfo[] pis = ji.getParameterInfoArray();

            final JsonArray paramsJsonArray=new JsonArray();
            
            for (int i=0; i < pis.length; i++){
              String val = pis[i].getValue();
              JsonObject P = null;
              if (val.indexOf("<GenePatternURL>") >= 0){
                  File serverFile = null;
                  Object value2 = null;
                  GpFilePath gpfp = GpFileObjFactory.getRequestedGpFileObj(config, val);
                  serverFile = gpfp.getServerFile();
                  //
                  // When an external file manager is in use, its still possible that a file is local (legacy files and a few
                  // otehr cases) so we make sure its not local first.
                  //
                  if (serverFile.exists()) {
                      value2 = gpRestClient.uploadFileIfNecessary(true, serverFile.getAbsolutePath());
                  } else if ((externalFileManager != null) ) {
                      value2 = new URL(externalFileManager.getDownloadURL(jobContext, serverFile));
                  } 

                  P = gpRestClient.createParameterJsonObject(pis[i].getName(), value2);
             
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
           gpRestClient.addComment(externalJobId, "Launched from " + config.getGpUrl() + " for user " + ji.getUserId()+ " original job id:  " + ji.getJobNumber());
           
           
        } catch (Exception e) {
            System.out.println("--------------- --- -- - Failed to start remote job");
            log.error(e);
            e.printStackTrace();
            throw new CommandExecutorException(e.getMessage());
        }
        
        return ""+externalJobId;
    }
        
        
        
        // get a file for a url that starts as getFile.jsp?task=***&file=***
        // copied from GenePatternAnalysisTask.localInputUrlToFile()
        private File getFileForUrl(String urlPath, GpContext jobContext){
            int lastIdx = urlPath.lastIndexOf("?");
            
            // request parameters are: task=lsid & job=<job_number> & file=filename
            String params = urlPath.substring(lastIdx + 1);
            int idx1 = params.indexOf("task=");
            int endIdx1 = params.indexOf('&', idx1);
            if (endIdx1 == -1) {
                endIdx1 = params.length();
            }
            int idx2 = params.indexOf("file=");
            int endIdx2 = params.indexOf('&', idx2);
            if (endIdx2 == -1) {
                endIdx2 = params.length();
            }
            String lsid = params.substring(idx1 + 5, endIdx1);
            try {
                lsid = URLDecoder.decode(lsid, "UTF-8");
            } 
            catch (UnsupportedEncodingException e) {
                log.error("Error", e);
            }
            String filename = params.substring(idx2 + 5, endIdx2);
            if (filename == null) {
                return null;
            }
            try {
                filename = URLDecoder.decode(filename, "UTF-8");
            } 
            catch (UnsupportedEncodingException e) {
                log.error("Error", e);
            }
            int jobNumber = -1;
            int idx3 = params.indexOf("job=");
            if (idx3 >= 0) {
                int endIdx3 = params.indexOf('&', idx3);
                if (endIdx3 == -1) {
                    endIdx3 = params.length();
                }
                String jobNumberParam = params.substring(idx3 + 4, endIdx3);
                if (jobNumberParam != null) {
                    try {
                        jobNumber = Integer.parseInt(jobNumberParam);
                    }
                    catch (NumberFormatException e) {
                        log.error("Invalid request parameter, job="+jobNumberParam, e);
                    }
                }
            }
            if (lsid == null || lsid.trim().equals("")) { 
                // input file look in temp for pipelines run without saving
                File parentTempdir = ServerConfigurationFactory.instance().getTempDir(jobContext);
                File in = new File(parentTempdir, filename);
                if (in.exists() && jobNumber >= 0) {
                    // check whether the current user has access to the job
                    if (jobContext.canReadJob()) {
                        return in;
                    }
                    throw new IllegalArgumentException("You are not permitted to access the requested file: "+in.getName());
                }
                else if (in.exists()) {
                    InputFilePermissionsHelper perm = new InputFilePermissionsHelper(jobContext.getUserId(), filename);
                    if (perm.isCanRead()) return in;
                }                      

                //special case: look for file among the user uploaded files
                try {
                    File userUploadDir = config.getUserUploadDir(jobContext);
                    in = new File(userUploadDir, filename);
                    boolean foundUserUpload = in.canRead();
                    if (foundUserUpload) {
                        return in;
                    }
                }
                catch (Throwable t) {
                    log.error("Unexpected error getting useruploadDir", t);
                }

                //special case: Axis
                in = new File(ServerConfigurationFactory.instance().getSoapAttDir(jobContext), filename);
                if (in.exists()) {
                    //TODO: permissions check for SOAP upload, see *similar* code in getFile.jsp
                    return in;
                }
                return null;
            }
            // check that user can access requested module
            TaskInfo taskInfo = null;
            try {
                LocalAdminClient adminClient = new LocalAdminClient(jobContext.getUserId());
                taskInfo = adminClient.getTask(lsid);
            }
            catch (WebServiceException e) {
                String errorMessage = "Unable to find file: "+filename+ " ("+urlPath+"). Because of a database connection error: "+e.getLocalizedMessage();
                log.error(errorMessage, e);
                throw new IllegalArgumentException(errorMessage,e);
            } 
            if (taskInfo == null) {
                String errorMessage = "You are not permitted to access the requested file: "+filename+ " ("+urlPath+")";
                log.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            File file = null;
            String taskLibDir = DirectoryManager.getTaskLibDir(taskInfo);
            if (taskLibDir == null) {
                String errorMessage = "You are not permitted to access the requested file: "+filename+ " ("+urlPath+")";
                log.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            file = new File(taskLibDir, filename);
            if (file.exists()) {
                return file;
            }
            else {
                //new code circa 3.6.0 release (just before 3.6.1)
                String errorMessage = "Requested file does not exist: "+filename+ " ("+urlPath+")";
                log.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        
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
    
    //   "2020-08-13T11:33:47-07:00"
    static SimpleDateFormat format = new SimpleDateFormat("MMMM d, yyyy");

    private java.util.Date getDate(String dateStr){
        try {
            return DateUtil.parseDate(dateStr);
        } catch (Exception e){
            // we  expect it to be called with a null at the beginning of every job
            // e.printStackTrace();
            return null;
        }
        
    }
    
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-M-dd", Locale.ENGLISH);
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm:ss", Locale.ENGLISH);
    
    // try to get cpu time or extimate it if unavailable
    protected long getCpuTime(JsonObject statusObj){
        long cpuTime = 0L;
        try {
            
            cpuTime = statusObj.getAsJsonObject("status").get("cpuTimeMillis").getAsLong();
            if (cpuTime != 0) return cpuTime;
            
            long end = getDate(statusObj.get("dateCompleted").getAsString()).getTime();
            long start = getDate(statusObj.get("dateSubmitted").getAsString()).getTime();
            cpuTime = end - start;
            
            long nCpu = 1;
            JsonArray requirements = statusObj.get("status").getAsJsonObject().get("resourceRequirements").getAsJsonArray();
            for (int i=0; i< requirements.size(); i++){
                JsonObject req = requirements.get(i).getAsJsonObject();
                if ("job.cpuCount".equalsIgnoreCase(req.get("key").getAsString())){
                    nCpu = req.get("value").getAsLong();
                    break;
                }
            }
            return nCpu * cpuTime;
            
        } catch(Exception e){
            // swallow it, not always gonna be able to get this detail
            return cpuTime;
        } 
       
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
            
            log.debug("Getting status for local: " + localJobId + " and remote: "+ drmJobId );
            
            
            final GenePatternRestApiV1Client gpRestClient = new GenePatternRestApiV1Client(gpurl, user, pass);
            JsonObject statusJsonObj = gpRestClient.getJobStatus(drmJobId);
            
            log.debug(statusJsonObj.toString());
            String status = statusJsonObj.getAsJsonObject("status").get("statusFlag").getAsString();
            log.debug("              -----> " + status);
            
            
            
            String startTime = null;
            try {
                startTime = statusJsonObj.getAsJsonObject("status").get("startTime").getAsString();
            } catch (Exception e){
         
            }
            
            String submitTime = null;
            try {
                submitTime = statusJsonObj.get("dateSubmitted").getAsString();
            } catch (Exception e){}
            
            DrmJobState state = jobInfoStatusToDrmJobState(status);
            DrmJobStatus.Builder statusBuilder = new DrmJobStatus.Builder(drmJobId,state); 
            log.debug("  remote job state is " + state);
            if ((startTime != null) &&(!startTime.isEmpty())) statusBuilder.startTime(getDate(startTime));
            if ((submitTime != null) && (!submitTime.isEmpty())) statusBuilder.submitTime(getDate(submitTime));
             
            if (statusJsonObj.getAsJsonObject("status").get("isFinished").getAsBoolean()){
                log.debug("JOB IS DONE " + status + "  " + localJobId) ;
                statusBuilder.cpuTime(new CpuTime(getCpuTime(statusJsonObj)));
                
                //String resultFiles[] = analysisProxy.getResultFiles(ji.getJobNumber());
                JSONArray outFilesJSON = new JSONArray();
                JsonArray outputFiles = statusJsonObj.getAsJsonArray("outputFiles");
                log.debug("Job output files are " + outputFiles.toString());
                final File dir = drmJobRecord.getWorkingDir();
                
                if (outputFiles.size() == 0){
                    // We get a race condition sometimes when the job is done but the files have not yet been registered
                    // remotely.  Lets just punt once here and say its still running and check when it comes back
                    log.debug("NO OUTPUT FILES ");
                    
                    Integer prevTries = outputFileRetryCount.get(localJobId);
                    if (prevTries == null) prevTries = 1;
                    else prevTries++;
                    outputFileRetryCount.put(localJobId, prevTries);
                    if (prevTries < 5){
                        log.debug("  ---  fake return as RUNNING to wait for files ==== ");
                        return new DrmJobStatus.Builder(drmJobId,DrmJobState.RUNNING).build();
                       
                    } else {
                        // clear the dictionary and report it as finished with no files
                        outputFileRetryCount.remove(localJobId);
                    }
                }
                final GpContext serverContext = GpContext.getServerContext();
                final ExternalFileManager externalFileManager = DataManager.getExternalFileManager(serverContext);
                int numOutputFilesRetrieved = 0;
                for (int i=0; i < outputFiles.size();i++){
                    final String outFileUrl = outputFiles.get(i).getAsJsonObject().get("link").getAsJsonObject().get("href").getAsString();
                    String outFileNameAndPath = outputFiles.get(i).getAsJsonObject().get("path").getAsString();
                    String origName = outFileUrl.substring(outFileUrl.lastIndexOf('/')+1);
                    // so we get the actual filename by looking at the end of the URL because the Name part of the link and 
                    // figure out any directories by pulling the name off the path to see what is left
                    String pathBits[] = outFileNameAndPath.split("/");
                    for (int ii=0; ii< pathBits.length-1; ii++){
                        System.out.println("subDir = " + pathBits[ii]);
                        File outFileDir = new File(dir, pathBits[ii]);
                        outFileDir.mkdirs();
                    }
                    String name = origName;
                    
                    // avoid stomping on the special files
                    if (getSpecialRemoteFileNames().keySet().contains(origName)){
                        name = getSpecialRemoteFileNames().get(origName);
                        outFileNameAndPath = outFileNameAndPath.replace(origName, name);
                    } 
                   
                    
                    log.debug("Saving remote result file " + outFileNameAndPath + " to " + dir.getAbsolutePath());
                    File outFile = new File(dir, outFileNameAndPath);
                    final String finalName = outFileNameAndPath;
                    
                    System.out.println("Creating marker with >>" + dir+"<< and >>" + name);
                    final File downloadMarker = new File(dir, ".marker." + name+".marker");
                    
                    if ((!outFile.exists())  && (!downloadMarker.exists()) ){
                        //
                        // Here we use a separate thread to download the files because for jobs with lots of
                        // files or large files, the download can take longer than the polling frequency and if we just
                        // do it synchronously we get stuck downloading over and over again forever.  So instead we download
                        // in a new thread and actually call it done when all the files are present, returning the RUNNING
                        // state until that is so.
                        //
                        
                        if (!downloadMarker.exists()){
                            downloadMarker.createNewFile();
                            Future<Boolean> f=executor.submit(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                      
                                        File downloadedFile = gpRestClient.getOutputFile(outFileUrl, dir, finalName);
                                        // delay briefly because the file found later needs to be flushed
                                        downloadMarker.delete();
                                        if  (downloadMarker.exists()){
                                            // delete failed, try harder
                                            try {
                                                Thread.currentThread().sleep(200);
                                                FileDeleteStrategy.FORCE.delete(downloadMarker);
                                                if (downloadMarker.exists()) failedFileDeletes.put(finalName, downloadMarker.exists());
                                            } catch (IOException e){
                                                failedFileDeletes.put(finalName, true);
                                            }
                                        }
                                        return downloadedFile.exists();
                                }
                            });
                         }
    
                    }  else if (outFile.exists() && (!downloadMarker.exists())){
                        numOutputFilesRetrieved +=1;
                    }  else if (outFile.exists() && (failedFileDeletes.get(finalName) == true)){
                        numOutputFilesRetrieved +=1;
                        failedFileDeletes.remove(finalName);
                        try {
                            FileDeleteStrategy.FORCE.delete(downloadMarker);
                        } catch (Exception e){
                            // one last try, belt and suspenders
                        }
                    }
                }
                log.debug("    ---- Found "+numOutputFilesRetrieved+ " of " + outputFiles.size());
                
                if (numOutputFilesRetrieved == outputFiles.size()){
                    if (externalFileManager != null){
                        for (int i=0; i < outputFiles.size();i++){
                            String outFileUrl = outputFiles.get(i).getAsJsonObject().get("link").getAsJsonObject().get("href").getAsString();
                            String name = outFileUrl.substring(outFileUrl.lastIndexOf('/'));
                            String outFileNameAndPath = outputFiles.get(i).getAsJsonObject().get("path").getAsString();
                            String origName = outFileUrl.substring(outFileUrl.lastIndexOf('/')+1);
                            
                            // avoid stomping on the special files
                            if (getSpecialRemoteFileNames().keySet().contains(origName)){
                                name = getSpecialRemoteFileNames().get(origName);
                                outFileNameAndPath = outFileNameAndPath.replace(origName, name);
                            } 
                            final File outFile = new File(dir, outFileNameAndPath);
                            
                            //System.out.println(" 4. Download complete " + outFile.getAbsolutePath() + "  len= " +  outFile.length());
                            
                            
                            Date date = new Date(outFile.lastModified());
                            JSONObject oneFileJSON = new JSONObject();
                            oneFileJSON.put("filename", outFile.getAbsolutePath());
                            oneFileJSON.put("size", outFile.length());
                            oneFileJSON.put("date", dateFormatter.format(date));
                            oneFileJSON.put("time", timeFormatter.format(date));
                            outFilesJSON.put(oneFileJSON);
                            
                            Future<Boolean> f=executor.submit(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    
                                    return externalFileManager.syncLocalFileToRemote(serverContext, outFile, true);
                                }
                            });
                           
                        }
                        // all files retrieved so lets mark it done
                        // for externally managed files, write the JSON file so they are added to the DB
                        File outListFile = new File(drmJobRecord.getWorkingDir(), ExternalFileManager.nonRetrievedFilesFileName);
                        BufferedWriter writer = new BufferedWriter(new FileWriter (outListFile));
                        writer.append(outFilesJSON.toString());
                        writer.close();
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
                    
                    
                } else {
                    // leave it in RUNNING state while the files are downloading until they are all present
                    
                    // statusBuilder = new DrmJobStatus.Builder(drmJobId,DrmJobState.RUNNING); 
                    log.error("Job: "+localJobId+"  Remote "+drmJobId+" job state is " + state + " but only " +numOutputFilesRetrieved +" retrieved of "+outputFiles.size()+".");
                    
                    //statusBuilder.startTime(getDate(startTime));
                    //statusBuilder.submitTime(getDate(submitTime));
                    
                }
            }
            
            return statusBuilder.build();
        }
        catch (NumberFormatException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            log.error(e1);
        }
        catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            log.error(e1);
        }
        catch (Throwable e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            log.error(e1);
        }
          
        return new DrmJobStatus.Builder(drmJobId, DrmJobState.FAILED).build();
    }

  
    
    
    
    
    @Override
    public boolean cancelJob(DrmJobRecord jobRecord) throws Exception {
       
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
