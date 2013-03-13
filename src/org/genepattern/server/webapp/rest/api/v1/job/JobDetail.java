package org.genepattern.server.webapp.rest.api.v1.job;

import java.net.URI;
import java.util.List;

import org.json.JSONObject;

/**
 * The JSON representation for a GenePattern job, in response to a GET request to the REST API.
 * This is yet another name for a JobInfo.
 * 
 * @author pcarr
 *
 */
public class JobDetail extends JSONObject {
    public static class Status {
        private boolean isFinished=false;
        private boolean hasError=false;
        private URI stderrLocation=null;

        public boolean isFinished() {
            return isFinished;
        }
        public void setIsFinished(boolean isFinished) {
            this.isFinished = isFinished;
        }
        public boolean getHasError() {
            return hasError;
        }
        public void setHasError(boolean hasError) {
            this.hasError = hasError;
        }
        public URI getStderrLocation() {
            return stderrLocation;
        }
        public void setStderrLocation(URI stderrLocation) {
            this.stderrLocation = stderrLocation;
        }
    }
    
    public static class TaskDetail {
        String name;
        String lsid;
        String lsidVersion;
        String lsidNoVersion;
        boolean isPipeline;
        String owner;
    }
    
    public static class FileDetail {
        String name; //the filename
        String path; //the relative path to the file
        String size; //the size of the file in bytes
        long timestamp; //the timestamp of the file in the gp file system
    }
    
    //////////////////////
    // factory methods
    //////////////////////
    //static public JSONObject initJobDetail() {
    //}
    
    private URI self;
    private String jobId;
    private String owner;
    private TaskDetail taskInfo;
    
    private Status status=new Status();
    private List<URI> resultFiles;
    private List<JobDetail> children;


}
