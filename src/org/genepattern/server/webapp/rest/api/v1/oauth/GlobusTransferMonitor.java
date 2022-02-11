package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
        JsonObject finishedTransferJson = t.getAsJson();
        String user = finishedTransferJson.get("user").getAsString();
        String fileName = finishedTransferJson.get("file").getAsString();
        synchronized(this.finishedTransfers){
            // if a user has a finished transfer with the same filename, drop it from the list, last one wins
            for (int i=0; i<finishedTransfers.size();i++){
                JsonObject transfer = finishedTransfers.get(i);
                if (user.equals(transfer.get("user").getAsString())){
                    if (fileName.equals(transfer.get("file").getAsString())){
                        finishedTransfers.remove(transfer);
                    } 
                    else {
                        // also remove anything for this user that is over 60 minutes old
                        // since the list will grow infinitely otherwise and if they left
                        // the page while it was running it would not be cleared using 
                        // the usual mechanisms...
                        long lastTimeStamp = transfer.get("timestamp").getAsLong();
                        long timeNow = System.currentTimeMillis();
                        
                        if ((timeNow - lastTimeStamp) > (10*60*1000)){
                            finishedTransfers.remove(transfer);
                        }
                    }
                }
                
            }
            
            finishedTransfers.add(finishedTransferJson);
        }
    }
    
    public void addFailedTransferStart(String submissionId, String user, String file, String destDir, String error){
        JsonObject failedTransferStart = new JsonObject();
        UUID uuid = UUID.randomUUID();
        
        failedTransferStart.addProperty("submissionid", submissionId);
        failedTransferStart.addProperty("user", user);
        failedTransferStart.addProperty("error", error);
        failedTransferStart.addProperty("status", "FAILED");
        failedTransferStart.addProperty("file", file);
        failedTransferStart.addProperty("destDir", destDir);
        failedTransferStart.addProperty("size", -1);
        failedTransferStart.addProperty("timestamp", System.currentTimeMillis());
        
        //  mimic a globus error status object that the UI expects to be able to display
        JsonObject statusObj = new JsonObject();
        statusObj.addProperty("is_ok", false);
        statusObj.addProperty("nice_status_short_description", error);
        statusObj.addProperty("bytes_transferred", 0);
        
        failedTransferStart.add("statusObject", statusObj);
        
        
        
        finishedTransfers.add(failedTransferStart);
        
    }
    
    public void addWaitingOutbound(String submissionId, String user, GlobusClient cl, String file, GpContext userContext, String destDir, long fileSize, String destEndpointId, String label, boolean isDirectory) {
        TransferOutWaitThread twt = new TransferOutWaitThread(submissionId, user, cl, file, userContext, destDir, fileSize, destEndpointId, label, isDirectory);
        threads.add(twt);
        twt.start();
        
    }
    
    
    public void addWaitingInbound(String submissionId, String user, String taskID, GlobusClient cl, String file, GpContext userContext, String destDir, long fileSize, boolean recursive) {
        TransferInWaitThread twt = new TransferInWaitThread(submissionId, user, taskID, cl, file, userContext, destDir, fileSize, recursive);
        threads.add(twt);
        twt.start();
        
    }
    
    public JsonObject removeWaitingUser(String user, String submissionId) {
        for (TransferWaitThread twt: threads){
            if (twt.matchTaskAndUser(user, submissionId)) {
                JsonObject ret = twt.quietStop();
                threads.remove(twt);
                return  ret;
            }
        }
        return null;
    }
    
    public JsonObject getStatus(String user, String submissionId) {
        for (TransferWaitThread twt: threads){
            if (twt.matchTaskAndUser(user, submissionId)) {
               
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
    
    public void clearCompletedTask(String user, String submissionId) {
        // loop in reverse since we add at the end
         synchronized(this.finishedTransfers){
            for (int i=0; i<finishedTransfers.size();i++){
                JsonObject transfer = finishedTransfers.get(i);
                if (user.equals(transfer.get("user").getAsString()) 
                        && submissionId.equals(transfer.get("submissionid").getAsString())){
                    finishedTransfers.remove(transfer);
                }
            }
        }
     }
    
}


class TransferWaitThread extends Thread {
    String submissionId;
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
    String destDir = null;
    long fileSize = 0L;
    String direction = null;
    String label = null;
    boolean recursive = false;
    
    GpContext userContext;
    protected static Logger log = Logger.getLogger(TransferInWaitThread.class);
    
    // XXX 6 sec for test
    int initialSleep = 6000;  // one minute

    int maxTries = 2880;  // two days in minutes

    boolean stopQuietly = false;
    
    public JsonObject quietStop() {
        stopQuietly = true;
        
        try {
        
        if (statusObject.get("status").getAsString().equals("ACTIVE"))
            return globusClient.cancelTransfer(user, statusObject);
        } catch (Exception e){
            e.printStackTrace();
            JsonObject err = new JsonObject();
            
            err.addProperty("error", e.getMessage());
            return err;
        }
        return null;
    }

    public JsonObject getAsJson(){
        JsonObject transferObject = new JsonObject();
        transferObject.addProperty("taskid", this.taskId);
        transferObject.addProperty("submissionid", this.submissionId);
        transferObject.addProperty("user", this.user);
        transferObject.addProperty("error", this.error);
        transferObject.addProperty("status", this.status);
        transferObject.addProperty("prevStatus", this.prevStatus);
        transferObject.addProperty("lastStatusCheckTime", this.lastStatusCheckTime);
        transferObject.addProperty("file", this.file);
        transferObject.add("statusObject", this.statusObject);
        transferObject.addProperty("destDir", this.destDir);
        transferObject.addProperty("size", this.fileSize);
        transferObject.addProperty("timestamp", System.currentTimeMillis());
        transferObject.addProperty("direction", direction);
        transferObject.addProperty("recursive", recursive);
        
        return transferObject;
    }
    
    protected void cleanUpACL() {
        ArrayList<TransferWaitThread> running = GlobusTransferMonitor.getInstance().getRunningForUser(this.user);
        if (running.size() == 0){
        String token = globusClient.getTokenFromUserPrefs(user, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
            String ACLRuleId = this.globusClient.getUserACLId(token);
            if (ACLRuleId != null){
                this.globusClient.teardownOpenACLForTransfer(token, ACLRuleId);
            }
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
    protected static int incrementSleep(int init, int maxTries, int count) {
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

    public boolean matchTaskAndUser(String user, String submissionId) {
        if (!user.equals(this.user))  return false;
        try {
            return (this.submissionId.equals(submissionId));
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
            //proc.waitFor(3, TimeUnit.MINUTES);
            proc.waitFor();
            
            
            success = (proc.exitValue() == 0);
            if (success) {
                String s;
                BufferedReader stdOut = null;
                try {
                    stdOut = new BufferedReader(new  InputStreamReader(proc.getInputStream()));
                    int lineNumber = 0;
                    while ((s = stdOut.readLine()) != null) {
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
    
    private static void logStderr(Process proc, String action) throws IOException {
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
    private static void logStdout(Process proc, String action) throws IOException {
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
    
    public static boolean s3MoveFile(GpContext userContext, String fromFileS3Url, File toFile, boolean recursive) throws IOException {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = getBucketName(gpConfig, userContext);
        String bucketRoot = getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, "aws-cli", "aws-cli.sh");
         
        String execArgs[];
        if (recursive){
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "mv", "--recursive", fromFileS3Url, "s3://"+bucket+ "/"+bucketRoot+toFile.getAbsolutePath()};             
                
        } else {
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "mv", fromFileS3Url, "s3://"+bucket+ "/"+bucketRoot+toFile.getAbsolutePath()};             
        }
        
        boolean success = false;
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            // proc.waitFor(3, TimeUnit.MINUTES);
            proc.waitFor();
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

class TransferInWaitThread extends TransferWaitThread {
  
    public TransferInWaitThread(String submissionId, String user, String taskId, GlobusClient client, String file, GpContext userContext, String dest, long size, boolean recursive) {
        this.submissionId = submissionId;
        this.user = user;
        this.taskId = taskId;
        this.globusClient = client;
        this.file = file;
        this.userContext = userContext;
        this.destDir = dest;
        this.fileSize = size;
        this.direction = "inbound";
        this.recursive = recursive;
    }

   
    
    public void run() {
    try {
         this.status = "Undetermined";
        this.prevStatus = "Undetermined";
       
        int count = 0;
        int sleep = initialSleep; // wouldn't be here if it was fast
        
        sleep = 10000; // for debugging
        
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
                
                status = globusClient.checkTransferStatus(statusObject);
                //Boolean isOk = statusObject.get("is_ok").getAsBoolean();
               
                Boolean isOk = true;
                try {
                    isOk = statusObject.get("is_ok").getAsBoolean();
                } catch (Exception e){
                    // do noting as is_ok = null in the json causes errors which is a PITA
                    isOk = true;
                }
                
                if (!isOk){
                    // globus has many ways of returning errors
                    status = "ERROR";
                    error = statusObject.get("nice_status_short_description").getAsString();
                }
                
                lastStatusCheckTime = System.currentTimeMillis();
            } catch (Exception e){
                error = e.getMessage();
                status = "ERROR";
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

   
    private void finalizeLocalFileTransfer(HibernateSessionManager hib, GpConfig gpConfig) throws Exception, GpFilePathException, DbException {
        String myEndpointRoot = gpConfig.getGPProperty(this.userContext, OAuthConstants.OAUTH_LOCAL_ENDPOINT_ROOT, "/Users/liefeld/Desktop/GlobusEndpoint/");
        File newFile = new File(myEndpointRoot + user +"/globus/"+file);
        if (newFile.exists()) {
            // file path like /gp/users/jliefeld@ucsd.edu/test2.txt
            GpFilePath uploadFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, getFinalFilePath(file), (LSID)null);
            newFile.renameTo(uploadFilePath.getServerFile());
            JobInputFileUtil fileUtil = new JobInputFileUtil(gpConfig, this.userContext);
            
            hib.beginTransaction();
            fileUtil.updateUploadsDb(hib, uploadFilePath);
            hib.commitTransaction();
        } else {
            throw new Exception("File from Globus is missing.  Not at " + newFile.getAbsolutePath());
        }
    }
    
    private String getFinalFilePath(String file){
        String dirPath = "/";
        if (destDir !=null){
            String encodedUser = URLEncoder.encode(user);
            String userDir = "/gp/users/"+encodedUser;
            int idx = destDir.indexOf(userDir);
            if (idx > 0){
                // we have a valid looking destination
                dirPath = destDir.substring(idx + userDir.length());
            }
        }
        
        try {
            // handle special characters
            String f2 = URIUtil.encodePath(file);
            return "/gp/users/"+user+ dirPath + f2;
        }
        catch (URIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return "/gp/users/"+user+ dirPath + file;
      //  return "/gp/users/"+user+ dirPath +  URLEncoder.encode(file);
        
    }
    
    private void finalizeS3FileTransfer(HibernateSessionManager hib, GpConfig gpConfig) throws Exception, GpFilePathException, DbException {
        String myS3EndpointRoot = gpConfig.getGPProperty(this.userContext, OAuthConstants.OAUTH_S3_ENDPOINT_ROOT, "/Users/liefeld/Desktop/GlobusEndpoint/");
        ExternalFileManager efManager = DataManager.getExternalFileManager(this.userContext);
        
      
        
        if (verifyS3FileExists(this.userContext, myS3EndpointRoot + user +"/globus/"+file)) {
            // file path like /gp/users/jliefeld@ucsd.edu/test2.txt
            // need to look at desired destDir
            String encodedUser = URLEncoder.encode(user);
            int idx = destDir.indexOf("/gp/users/"+encodedUser);
            String dirPath = destDir.substring(idx);
            dirPath.replace(encodedUser, user);
            //destDir.substring(destDir.indexOf("/users/"+encodedUser) + encodedUser.length() + 7);
            
            GpFilePath uploadFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, getFinalFilePath( file), (LSID)null);
      
            // move the file within S3 to the desired location   
            s3MoveFile(this.userContext, myS3EndpointRoot + user +"/globus/"+file, uploadFilePath.getServerFile(), this.recursive);
            
            JobInputFileUtil fileUtil = new JobInputFileUtil(gpConfig, this.userContext);
            BigInteger size = statusObject.get("bytes_transferred").getAsBigInteger();
            
            uploadFilePath.setFileLength(new Long(size.longValue()));
            uploadFilePath.setLastModified(new Date());
            if (recursive) {
                // make the parent directory because it needs to be therre to mimic the S3 file structure
                uploadFilePath.getServerFile().mkdirs();
                hib.beginTransaction();
                fileUtil.updateUploadsDb(hib, uploadFilePath, false);
                hib.commitTransaction();
                
                // now we need to get the directory listing from S3 and make the local sub directories and
                // add the files one by one
             
                addAllChildren(fileUtil, hib, uploadFilePath);
                
                
            } else {
                hib.beginTransaction();
                fileUtil.updateUploadsDb(hib, uploadFilePath, false);
                hib.commitTransaction();
            }
            
            
            
        } else {
            throw new Exception("File from Globus is missing.  Not at " + myS3EndpointRoot + user +"/globus/"+file);
        }
    }
    
    protected void addAllChildren(JobInputFileUtil fileUtil, HibernateSessionManager hib, GpFilePath uploadFilePath) throws IOException, GpFilePathException, DbException{
        final GpContext serverContext = GpContext.getServerContext();
        final ExternalFileManager externalFileManager = DataManager.getExternalFileManager(serverContext);
      
        ArrayList<GpFilePath> children = externalFileManager.listFiles(this.userContext, uploadFilePath.getServerFile());
        
        for (GpFilePath child: children){
            if (child.getServerFile().isDirectory()){
                addAllChildren(fileUtil, hib, child);
            } else {
                hib.beginTransaction();
                fileUtil.updateUploadsDb(hib, child, false);
                hib.commitTransaction();
            }
        }
    }
    

   
}

/**
 * variant of the TransferInWaitThread that is modified for outbound transfers.  In this case we start without a taskId and have
 * to copy the file to the endpoint and start the transfer
 * 
 * @author liefeld
 *
 */
class TransferOutWaitThread extends TransferWaitThread{
    String destinationEndpointId = null;
    String fileUrl = null;
    String S3TempLocationURL = null;
    
    public TransferOutWaitThread(String submissionId, String user,  GlobusClient client, String file, GpContext userContext, String dest, long size, String destEndpointId, String label, boolean isDirectory) {
        this.fileUrl = file;
        try {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            GpFilePath uploadFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, file, (LSID)null);
            // String fileName = uploadFilePath.getName();
            this.file = uploadFilePath.getRelativePath();
        } catch (Exception e){
            this.error = e.getMessage();
            this.file = file;
        }
        this.submissionId = submissionId;
        this.user = user;
        this.taskId = null;
        this.globusClient = client;
        
        this.userContext = userContext;
        this.destDir = dest;
        this.fileSize = size;
        this.destinationEndpointId = destEndpointId;
        this.direction = "outbound";
        this.label = label;
        this.recursive = isDirectory;
    }
    
    public void run() {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        // first copy the files and start the transfer.  Then loop and check status like the transfer in version
        
        try {
            GpFilePath uploadFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, this.fileUrl, (LSID)null);
            String fileName = uploadFilePath.getName();
            
                 
            // make a copy of the GP file on GenePattern's globus endpoint so that it can be transferred
            String myEndpointRoot = gpConfig.getGPProperty(userContext, OAuthConstants.OAUTH_LOCAL_ENDPOINT_ROOT, "/Users/liefeld/Desktop/GlobusEndpoint/");
            File newFile = new File(myEndpointRoot + user +"/globus/"+fileName);
            String myEndpointType = gpConfig.getGPProperty(userContext, OAuthConstants.OAUTH_LOCAL_ENDPOINT_TYPE);
            
            if (myEndpointType.equalsIgnoreCase(OAuthConstants.OAUTH_ENDPOINT_TYPE_LOCALFILE)){
                Files.copy(uploadFilePath.getServerFile().toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
            } else if (myEndpointType.equalsIgnoreCase(OAuthConstants.OAUTH_ENDPOINT_TYPE_S3)){
                // need to tell the externalFileManager to copy it for us
                String fromFileS3Url = "s3://"+getBucketName(gpConfig, userContext)+ "/"+getBucketRoot(gpConfig, userContext)+uploadFilePath.getServerFile().getAbsolutePath();
                String myS3EndpointRoot = gpConfig.getGPProperty(userContext, OAuthConstants.OAUTH_S3_ENDPOINT_ROOT, "/Users/liefeld/Desktop/GlobusEndpoint/");
                String toS3Url = myS3EndpointRoot + user +"/globus/"+fileName;
                // XXX JTL need to delete this copy later
                S3TempLocationURL = toS3Url;
                globusClient.s3CopyFile(userContext, fromFileS3Url,  toS3Url, recursive);
            }
            
            String transferToken = globusClient.getTokenFromUserPrefs(user, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
            
            // so to transfer in we need to know our endpoint ID
            String myEndpointId = gpConfig.getGPProperty(userContext, OAuthConstants.OAUTH_LOCAL_ENDPOINT_ID, "eb7230ac-d467-11eb-9b44-47c0f9282fb8");
            
    
            JsonObject transferObject = new JsonObject();
            transferObject.addProperty("DATA_TYPE", "transfer");
            transferObject.addProperty("submission_id", submissionId);
            transferObject.addProperty("source_endpoint", myEndpointId);
            transferObject.addProperty("destination_endpoint", destinationEndpointId);
            transferObject.addProperty("verify_checksum", true);
            transferObject.addProperty("label", label);
            if (recursive){
                // for directories we need to add filter rules, we will exclude any hidden files
                // {
                //    "DATA_TYPE": "filter_rule",
               //     "method": "exclude",
               //     "name": ".*"
               //   }
                JsonArray filterRules = new JsonArray();
                
                JsonObject filterRule = new JsonObject();
                filterRule.addProperty("DATA_TYPE", "filter_rule");
                filterRule.addProperty("method", "exclude");
                filterRule.addProperty("name", ".*");
                filterRules.add(filterRule);
                
                transferObject.add("filter_rules", filterRules);
            }
            
            JsonObject transferItem = new JsonObject();
            transferItem.addProperty("DATA_TYPE", "transfer_item");
            transferItem.addProperty("recursive", recursive);
            transferItem.addProperty("destination_path", destDir + fileName);
            transferItem.addProperty("source_path", "/~/GenePatternLocal/"+ user +"/globus/"+fileName);
            
            
           
            // TODO MUST COPY THE FILE TO MY GLOBUS ENDPOINT FIRST
            
            JsonArray transferItems = new JsonArray();
            transferItems.add(transferItem);
            
            transferObject.add("DATA", transferItems);
            
            //System.out.println("     "+transferObject);
            URL url = new URL(globusClient.transferAPIBaseUrl+"/transfer");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();        
            connection.setRequestMethod("POST");
            
            // only need users token here as long as the user permissions were set earlier (using the GenePattern client token)
            connection.setRequestProperty("Authorization","Bearer "+ transferToken);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            
            connection.setDoOutput(true);
            
            try(OutputStream os = connection.getOutputStream()) {
                byte[] input = transferObject.toString().getBytes("utf-8");
                os.write(input, 0, input.length);           
            }
            System.out.println("TRANSFER OUT OBJECT \n" + transferObject.toString());
            String taskId = null;
            JsonElement transferResponse = null;
            
            transferResponse =  getJsonResponse(connection);
            this.taskId = transferResponse.getAsJsonObject().get("task_id").getAsString();
            
       
        } catch (Exception e){
            this.error = e.getMessage();
            this.status = "ERROR";
           e.printStackTrace();
           try {
               globusClient.s3DeleteTempFile(userContext, S3TempLocationURL, recursive);
           } catch (Exception ee){
               ee.printStackTrace();
           }
           GlobusTransferMonitor.getInstance().threadFinished(this);
           cleanUpACL(); // get rid of the ACL if no transfers are in progress
           return;
        } 
        
        
        
        
        
        try {
             this.status = "Undetermined";
            this.prevStatus = "Undetermined";
           
            int count = 0;
            int sleep = initialSleep; // wouldn't be here if it was fast
            
            sleep = 10000; // for debugging
            
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
                    
                    status = globusClient.checkTransferStatus(statusObject);
                    lastStatusCheckTime = System.currentTimeMillis();
                    
                   
                    Boolean isOk = true;
                    try {
                        isOk = statusObject.get("is_ok").getAsBoolean();
                    } catch (Exception e){
                        // when is_ok == nul gson gets discombobulated
                        isOk = true;
                    }
                    if (!isOk){
                        // globus has many ways of returning errors
                        status = "ERROR";
                        error = statusObject.get("nice_status_short_description").getAsString();
                    }
                    
                    
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
                // nothing to do but shut down.  For transfers out the extra work is at the beginning
                // not at the end
            } else {
                this.error = "Transfer failed.  Final status was " + this.status;
            }
            
            
        } catch (Exception e) {
            // problem getting status. Send an email indicating this and
            // end the thread
            e.printStackTrace();

            GlobusTransferMonitor em = GlobusTransferMonitor.getInstance();
            
        } finally {
            // delete the temp copy of the file/dir
            try {
                globusClient.s3DeleteTempFile(userContext, S3TempLocationURL, recursive);
            } catch (Exception e){
                e.printStackTrace();
            }
            GlobusTransferMonitor.getInstance().threadFinished(this);
            cleanUpACL(); // get rid of the ACL if no transfers are in progress
        }
    }

    public JsonElement getJsonResponse(HttpURLConnection con) throws UnsupportedEncodingException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
              response.append(responseLine.trim());
        }
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(response.toString());
        return je;
    }
    
    
}

