package org.genepattern.data.pipeline;

import java.util.SortedMap;
import java.util.TreeMap;

import org.genepattern.webservice.TaskInfo;

public class MissingTasksException extends Exception {
    public static enum Type {
        NO_LSID("The lsid is not set"), /* for example from jobSubmission.getLSID()  */
        NOT_FOUND("The module or pipeline is not installed"),
        PERMISSION_ERROR("The current user does not have permission to run the module or pipeline"),
        OTHER("Unexpected server error checking for the module or pipeline");
        
        private final String message;
        private Type(final String message) {
            this.message=message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    private int idx=0;
    private SortedMap<Integer,String> errors = new TreeMap<Integer,String>();

    public MissingTasksException() {
    }
    public MissingTasksException(final Type type, final JobSubmission jobSubmission) {
        addError(type, jobSubmission);
    }
    public MissingTasksException(final Type type, final TaskInfo taskInfo) {
        addError(type, taskInfo.getName(), taskInfo.getLsid());
    }
    
    public void addError(Type type, JobSubmission jobSubmission) {
        addError(type, jobSubmission.getName(), jobSubmission.getLSID());
    }
    
    public void addError(final Type type, final String taskName, final String taskLsid) {
        final String errorMessage = type.getMessage() +": "+taskName + " (" + taskLsid + ")";
        errors.put(idx, errorMessage);
        ++idx;
    }
    
    public boolean hasErrors() {
        return errors.size()>0;
    }

    public String getMessage() {
        final StringBuffer buf = new StringBuffer();
        for(String message : errors.values()) {
            buf.append(message);
        }
        return buf.toString();
    }
}
