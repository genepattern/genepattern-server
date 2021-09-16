package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.ExternalFileManager;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.GpFilePathException;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.util.LSID;

import com.google.gson.JsonObject;

public class GlobusTransferMonitor {
    private static GlobusTransferMonitor instance = null;
    private ArrayList<TransferWaitThread> threads = new  ArrayList<TransferWaitThread>();
    private ArrayList<JsonObject> finishedTransfers = new ArrayList<JsonObject>();
    
    private static Logger log = Logger.getLogger(GlobusTransferMonitor.class);
    
    
    public static GlobusTransferMonitor getInstance(){
        if (instance == null) instance = new GlobusTransferMonitor();
        return instance;
    }
    
    protected void threadFinished(TransferWaitThread t){
        threads.remove(t);
        synchronized(this.finishedTransfers){
            finishedTransfers.add(t.getAsJson());
        }
    }
    
    public void addWaitingUser(String user, String taskID, GlobusClient cl, String file, GpContext userContext) {
        TransferWaitThread twt = new TransferWaitThread(user, taskID, cl, file, userContext);
        threads.add(twt);
        twt.start();
        
    }
    
    public void removeWaitingUser(String user, String taskID) {
        for (TransferWaitThread twt: threads){
            if (twt.matchTaskAndUser(user, taskID)) {
                twt.quietStop();
                threads.remove(twt);
                return;
            }
        }
    }
    
    public JsonObject getStatus(String user, String taskID) {
        for (TransferWaitThread twt: threads){
            if (twt.matchTaskAndUser(user, taskID)) {
               
                return twt.getAsJson();
            }
        }
        return null;
    }
    public ArrayList<TransferWaitThread> getRunningForUser(String user) {
        ArrayList<TransferWaitThread> l = new ArrayList<TransferWaitThread> ();
        for (TransferWaitThread twt: threads){
            if (twt.matchUser(user)) {
                l.add(twt);
            }
        }
        return l;
    }
    
    public ArrayList<JsonObject> getStatusForUser(String user) {
        ArrayList<JsonObject> l = new ArrayList<JsonObject> ();
        for (TransferWaitThread twt: threads){
            if (twt.matchUser(user)) {
                l.add(twt.getAsJson());
            }
        }
        
       // loop in reverse since we add at the end
       for (int j = finishedTransfers.size() - 1; j >= 0; j--){    
           JsonObject finishedTransfer = finishedTransfers.get(j);
           if (user.equals(finishedTransfer.get("user").getAsString())){
                l.add(finishedTransfer);
           }
        }
        
        
        return l;
    }
   
    public void clearCompletedForUser(String user) {
       // loop in reverse since we add at the end
        synchronized(this.finishedTransfers){
           ArrayList<JsonObject> newFinishedTransfers = new ArrayList<JsonObject>(); 
           for (int i=0; i<finishedTransfers.size();i++){
               JsonObject transfer = finishedTransfers.get(i);
               if (! user.equals(transfer.get("user").getAsString())){
                   newFinishedTransfers.add(transfer);
               }
               
           }
       
           this.finishedTransfers = newFinishedTransfers;
       }
    }
    
}



class TransferWaitThread extends Thread {
    String user = null;
    String taskId = null;
    GlobusClient globusClient = null;
    String prevStatus = null;
    String status = null;
    JsonObject statusObject = null;
    long lastStatusCheckTime = 0L;
    String error = null;
    String file;
    String awsFileDetails;
    
    GpContext userContext;
    private static Logger log = Logger.getLogger(TransferWaitThread.class);
    
    int initialSleep = 60000;  // one minute

    int maxTries = 2880;  // two days in minutes

    boolean stopQuietly = false;

    public TransferWaitThread(String user, String taskId, GlobusClient client, String file, GpContext userContext) {
        this.user = user;
        this.taskId = taskId;
        this.globusClient = client;
        this.file = file;
        this.userContext = userContext;
    }

    public void quietStop() {
        stopQuietly = true;
    }

    public JsonObject getAsJson(){
        JsonObject transferObject = new JsonObject();
        transferObject.addProperty("id", this.taskId);
        transferObject.addProperty("user", this.user);
        transferObject.addProperty("error", this.error);
        transferObject.addProperty("status", this.status);
        transferObject.addProperty("prevStatus", this.prevStatus);
        transferObject.addProperty("lastStatusCheckTime", this.lastStatusCheckTime);
        transferObject.addProperty("file", this.file);
        transferObject.add("statusObject", this.statusObject);
        
        
        return transferObject;
    }
    
    public void run() {
    try {
         this.status = "Undetermined";
        this.prevStatus = "Undetermined";
       
        int count = 0;
        int sleep = initialSleep; // wouldn't be here if it was fast
        
        sleep = 1000; // for debugging
        
        while ((status.equalsIgnoreCase("ACTIVE") || (status.equalsIgnoreCase("Undetermined")))  && !stopQuietly) {
            count++;
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ie) {
            }
            prevStatus = status;
            
            try {
                // TODO should we refresh the token before each use?
                String token = globusClient.getTokenFromUserPrefs(user, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
                statusObject = globusClient.getTransferDetails(taskId, token);
                
                //System.out.println("\tTRANSFER TOKEN (get status):"+token );
                //String appAccessToken = globusClient.getApplicationCredentials() ;
                //statusObject = globusClient.getTransferDetails(taskId, appAccessToken);
                
                status = globusClient.checkTransferStatus(statusObject);
                
                lastStatusCheckTime = System.currentTimeMillis();
            } catch (Exception e){
                error = e.getMessage();
            }
            
            sleep = incrementSleep(initialSleep, maxTries, count);
            if (count > maxTries){
                status = "TIMED OUT";
                break;
            }
            
        }
        // this transfer is all done. Move the file to the user's directory      
        if (this.status.equals("SUCCEEDED")) {
            this.finalizeTransfer();
        } else {
            this.error = "Transfer failed.  Final status was " + this.status;
        }
        
        
    } catch (Exception e) {
        // problem getting status. Send an email indicating this and
        // end the thread
        e.printStackTrace();

        GlobusTransferMonitor em = GlobusTransferMonitor.getInstance();
        
    } finally {
        GlobusTransferMonitor.getInstance().threadFinished(this);
        cleanUpACL(); // get rid of the ACL if no transfers are in progress
    }
    }

    public void finalizeTransfer() {
        HibernateSessionManager hib =  HibernateUtil.instance();
        try {
            GpConfig gpConfig=ServerConfigurationFactory.instance();
            String myEndpointType = gpConfig.getGPProperty(this.userContext, OAuthConstants.OAUTH_LOCAL_ENDPOINT_TYPE);
            
            if (myEndpointType.equalsIgnoreCase(OAuthConstants.OAUTH_ENDPOINT_TYPE_LOCALFILE)){
                finalizeLocalFileTransfer(hib, gpConfig);
            } else if (myEndpointType.equalsIgnoreCase(OAuthConstants.OAUTH_ENDPOINT_TYPE_S3)){
                finalizeS3FileTransfer(hib, gpConfig);
            }
            
           
            
        } catch (Exception e){
            log.error(e);
            e.printStackTrace();
            this.error = "Error after transfer completed" + e.getMessage();
            this.status = "ERROR";
        } finally {
            if (hib.isInTransaction()) hib.rollbackTransaction();
        }
    }

    private void cleanUpACL() {
        ArrayList<TransferWaitThread> running = GlobusTransferMonitor.getInstance().getRunningForUser(this.user);
        if (running.size() == 0){
        String token = globusClient.getTokenFromUserPrefs(user, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
            String ACLRuleId = this.globusClient.getUserACLId(token);
            if (ACLRuleId != null){
                this.globusClient.teardownOpenACLForTransfer(token, ACLRuleId);
            }
        }
    }

    private void finalizeLocalFileTransfer(HibernateSessionManager hib, GpConfig gpConfig) throws Exception, GpFilePathException, DbException {
        String myEndpointRoot = gpConfig.getGPProperty(this.userContext, OAuthConstants.OAUTH_LOCAL_ENDPOINT_ROOT, "/Users/liefeld/Desktop/GlobusEndpoint/");
        File newFile = new File(myEndpointRoot + user +"/globus/"+file);
        if (newFile.exists()) {
            // file path like /gp/users/jliefeld@ucsd.edu/test2.txt
            GpFilePath uploadFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, "/gp/users/"+user+"/"+newFile.getName(), (LSID)null);
            newFile.renameTo(uploadFilePath.getServerFile());
            JobInputFileUtil fileUtil = new JobInputFileUtil(gpConfig, this.userContext);
            
            hib.beginTransaction();
            fileUtil.updateUploadsDb(hib, uploadFilePath);
            hib.commitTransaction();
        } else {
            throw new Exception("File from Globus is missing.  Not at " + newFile.getAbsolutePath());
        }
    }
    
    private void finalizeS3FileTransfer(HibernateSessionManager hib, GpConfig gpConfig) throws Exception, GpFilePathException, DbException {
        String myS3EndpointRoot = gpConfig.getGPProperty(this.userContext, OAuthConstants.OAUTH_S3_ENDPOINT_ROOT, "/Users/liefeld/Desktop/GlobusEndpoint/");
        ExternalFileManager efManager = DataManager.getExternalFileManager(this.userContext);
        
       
        if (verifyS3FileExists(this.userContext, myS3EndpointRoot + user +"/globus/"+file)) {
            // file path like /gp/users/jliefeld@ucsd.edu/test2.txt
            GpFilePath uploadFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, "/gp/users/"+user+"/"+file, (LSID)null);
      
            // move the file within S3 to the desired location   
            s3CopyFile(this.userContext, myS3EndpointRoot + user +"/globus/"+file, uploadFilePath.getServerFile());
            
            JobInputFileUtil fileUtil = new JobInputFileUtil(gpConfig, this.userContext);
            BigInteger size = statusObject.get("bytes_transferred").getAsBigInteger();
            
            uploadFilePath.setFileLength(new Long(size.longValue()));
            uploadFilePath.setLastModified(new Date());
            hib.beginTransaction();
            fileUtil.updateUploadsDb(hib, uploadFilePath);
            hib.commitTransaction();
            
            // now we use the details in the globus status object to get the size and we need to set ther date
            
            
        } else {
            throw new Exception("File from Globus is missing.  Not at " + myS3EndpointRoot + user +"/globus/"+file);
        }
    }
    
    

    public String getUser() {
        return user;
    }

    /**
     * make the sleep time go up as it takes longer to exec. eg for 100 tries of 1000ms (1 sec) first 20 are 1 sec each
     * next 20 are 2 sec each next 20 are 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
     * 
     * @param init
     *                Description of the Parameter
     * @param maxTries
     *                Description of the Parameter
     * @param count
     *                Description of the Parameter
     * @return Description of the Return Value
     */
    private static int incrementSleep(int init, int maxTries, int count) {
    if (count < (maxTries * 0.2)) {
        return init;
    }
    if (count < (maxTries * 0.4)) {
        return init * 2;
    }
    if (count < (maxTries * 0.6)) {
        return init * 4;
    }
    if (count < (maxTries * 0.8)) {
        return init * 8;
    }
    return init * 16;
    }

    public boolean matchTaskAndUser(String user, String taskId) {
        if (!user.equals(this.user))  return false;
        try {
            return (this.taskId.equals(taskId));
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean matchUser(String user) {
        if (!user.equals(this.user))  return false;
        return true;
    }
    
    
    public  boolean verifyS3FileExists(GpContext userContext, String s3PathAndFile  ) throws IOException {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
     
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, "aws-cli", "aws-cli.sh");        
        String execArgs[] = new String[] {awsfilepath+awsfilename, "s3", "ls",  s3PathAndFile};

        boolean success = false;
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(execArgs);
            // proc.waitFor(3, TimeUnit.MINUTES);
            proc.wait();
            success = (proc.exitValue() == 0);
            if (success) {
                String s;
                BufferedReader stdOut = null;
                try {
                    stdOut = new BufferedReader(new  InputStreamReader(proc.getInputStream()));
                    int lineNumber = 0;
                    while ((s = stdOut.readLine()) != null) {
                        System.out.println(s);
                        if (lineNumber == 1){
                            // this is the line containing size etc we will need for the finalize step
                            awsFileDetails = s;
                        }
                        lineNumber++;
                    }
                } finally {
                    if (stdOut != null) stdOut.close();
                }
            } else {
                logStderr(proc, "sync S3 file"); 
            }
          //  } 
        } catch (Exception e){
            log.debug(e);
        } finally {
            if (proc != null) proc.destroy();
        }
        return success;
    }
    
    private void logStderr(Process proc, String action) throws IOException {
        String s;
        BufferedReader stdError = null;
        try {
            stdError = new BufferedReader(new  InputStreamReader(proc.getErrorStream()));
            log.debug("Here is the standard error of "+action+" from S3 (if any):\n");
            while ((s = stdError.readLine()) != null) {
                log.debug(s);
            }
        } finally {
            if (stdError != null) stdError.close();
        }
    }
    private void logStdout(Process proc, String action) throws IOException {
        String s;
        BufferedReader stdOut = null;
        try {
            stdOut = new BufferedReader(new  InputStreamReader(proc.getInputStream()));
            log.debug("Here is the standard out of "+action+" from S3 (if any):\n");
            while ((s = stdOut.readLine()) != null) {
                log.debug(s);
            }
        } finally {
            if (stdOut != null) stdOut.close();
        }
    }
    
    public boolean s3CopyFile(GpContext userContext, String fromFileS3Url, File toFile) throws IOException {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = getBucketName(gpConfig, userContext);
        String bucketRoot = getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, "aws-cli", "aws-cli.sh");
         
        String execArgs[];
        
        execArgs = new String[] {awsfilepath+awsfilename, "s3", "cp", fromFileS3Url, "s3://"+bucket+ "/"+bucketRoot+toFile.getAbsolutePath()};             
        
        
        boolean success = false;
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            // proc.waitFor(3, TimeUnit.MINUTES);
            proc.wait();
            success = (proc.exitValue() == 0);
            if (!success){
                logStdout(proc, "copy s3 file"); 
                logStderr(proc, "copy s3 file"); 
            }
            
        } catch (Exception e){
            log.debug(e);
            return false;
            
        } finally {
            proc.destroy();
        }
        return success;
    
    }
    static String getBucketName(final GpConfig gpConfig, GpContext userContext) {
        String aws_s3_root = gpConfig.getGPProperty(userContext, "aws-s3-root");
        
        // pull the bucket name out of something like "s3://moduleiotest/gp-dev-ami"
        int endIdx = aws_s3_root.indexOf("/", 5);
        return aws_s3_root.substring(5,endIdx);
    }
     static String getBucketRoot(final GpConfig gpConfig, GpContext userContext) {
        String aws_s3_root = gpConfig.getGPProperty(userContext, "aws-s3-root");
       
        // pull the bucket root path out of something like "s3://moduleiotest/gp-dev-ami"
        int endIdx = aws_s3_root.indexOf("/", 5);
        return aws_s3_root.substring(endIdx+1);
    }
}
