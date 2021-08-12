package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.util.HttpNotificationManager;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.LSID;
import org.genepattern.webservice.JobInfo;

public class GlobusTransferMonitor {
    private static GlobusTransferMonitor instance = null;
    private ArrayList<TransferWaitThread> threads = new  ArrayList<TransferWaitThread>();
    private ArrayList<HashMap<String,String>> finishedTransfers = new ArrayList<HashMap<String,String>>();
    
    public static GlobusTransferMonitor getInstance(){
        if (instance == null) instance = new GlobusTransferMonitor();
        return instance;
    }
    
    protected void threadFinished(TransferWaitThread t){
        threads.remove(t);
        finishedTransfers.add(t.getStatusMap());
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
    
    public HashMap<String,String> getStatus(String user, String taskID) {
        for (TransferWaitThread twt: threads){
            if (twt.matchTaskAndUser(user, taskID)) {
               
                return twt.getStatusMap();
            }
        }
        return null;
    }
    
    public ArrayList<HashMap<String,String>> getStatusForUser(String user) {
        ArrayList<HashMap<String,String>> l = new ArrayList<HashMap<String,String>> ();
        for (TransferWaitThread twt: threads){
            if (twt.matchUser(user)) {
                l.add(twt.getStatusMap());
            }
        }
        
       // loop in reverse since we add at the end
       for (int j = finishedTransfers.size() - 1; j >= 0; j--){    
           HashMap<String,String> finishedTransfer = finishedTransfers.get(j);
           if (user.equals(finishedTransfer.get("user"))){
                l.add(finishedTransfer);
           }
        }
        
        
        return l;
    }
    
    
}



class TransferWaitThread extends Thread {
    String user = null;
    String taskId = null;
    GlobusClient globusClient = null;
    String prevStatus = null;
    String status = null;
    long lastStatusCheckTime = 0L;
    String error = null;
    String file;
    GpContext userContext;
    
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

    public HashMap<String,String> getStatusMap(){
        HashMap<String,String> map = new HashMap();
        map.put("status", status);
        map.put("prevStatus", status);
        map.put("lastStatusCheckTime", ""+ lastStatusCheckTime);
        map.put("error", error);
        map.put("id", this.taskId);
        map.put("file", this.file);
        map.put("error", this.error);
        map.put("user", this.user);
         
        return map;
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
                System.out.println("\tTRANSFER TOKEN (get status):"+token );
                status = globusClient.checkTransferStatus(taskId, token);
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
    }
    }

    public void finalizeTransfer() {
        HibernateSessionManager hib =  HibernateUtil.instance();
        try {
            GpConfig gpConfig=ServerConfigurationFactory.instance();
           
            String myEndpointRoot = gpConfig.getGPProperty(this.userContext, OAuthConstants.OAUTH_LOCAL_ENPOINT_ROOT, "/Users/liefeld/Desktop/GlobusEndpoint/");
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
            String token = globusClient.getTokenFromUserPrefs(user, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
            
            String ACLRuleId = this.globusClient.getUserACLId(token);
            if (ACLRuleId != null){
                this.globusClient.teardownOpenACLForTransfer(token, ACLRuleId);
            }
            
        } catch (Exception e){
            this.error = e.getMessage();
        } finally {
            if (hib.isInTransaction()) hib.rollbackTransaction();
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
    
}
