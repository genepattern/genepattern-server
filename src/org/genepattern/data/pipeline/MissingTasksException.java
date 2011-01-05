package org.genepattern.data.pipeline;

import java.util.SortedMap;
import java.util.TreeMap;

public class MissingTasksException extends Exception {
    private int idx=0;
    private SortedMap<Integer,String> errors = new TreeMap<Integer,String>();

    public MissingTasksException() {
    }

    public void addError(JobSubmission jobSubmission) {
        String errorMessage = "No such module " + jobSubmission.getName() + " (" + jobSubmission.getLSID() + ")";
        errors.put(idx, errorMessage);
        ++idx;
    }
    
    public String getMessage() {
        final StringBuffer buf = new StringBuffer();
        for(String message : errors.values()) {
            buf.append(message);
        }
        return buf.toString();
    }
}
