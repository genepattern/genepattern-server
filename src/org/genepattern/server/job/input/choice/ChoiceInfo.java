package org.genepattern.server.job.input.choice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Initialized by parsing the manifest for a given parameter info.
 * @author pcarr
 *
 */
public class ChoiceInfo {
    /**
     * Get the status of the choiceInfo to indicate to the end-user if there were problems initializing 
     * the list of choices for the parameter. Example status messages,
     * 
     *     OK, Initialized from values attribute (old way)
     *     OK, Initialized from choices attribute (new way, not dynamic)
     *     OK, Initialized from remote server (url=, date=)
     *     WARN, Initialized from cache, problem connecting to remote server
     *     ERROR, Error in module manifest, didn't initialize choices.
     *     ERROR, Connection error to remote server (url)
     *     ERROR, Timeout waiting for listing from remote server (url, timeout)
     * 
     * @author pcarr
     *
     */
    public static class Status {
        public static enum Flag {
            OK,
            WARNING,
            ERROR
        }
        
        final private Flag flag;
        final private String message;
        
        public Status(final Flag flag) {
            this(flag,flag.toString());
        }
        public Status(final Flag flag, final String message) {
            this.flag=flag;
            this.message=message;
        }
        
        public Flag getFlag() {
            return flag;
        }
        
        public String getMessage() {
            return message;
        }
    }

    
    //final static ChoiceInfoParser defaultChoiceInfoParser= new DefaultChoiceInfoParser();
    final static ChoiceInfoParser choiceInfoParser= new DynamicChoiceInfoParser();
    public static ChoiceInfoParser getChoiceInfoParser() {
        return choiceInfoParser;
    }
    
    private String ftpDir=null;
    private List<Choice> choices=null;
    private ChoiceInfo.Status status=null;
    
    public void setFtpDir(final String ftpDir) {
        this.ftpDir=ftpDir;
    }
    public String getFtpDir() {
        return ftpDir;
    }
    
    public List<Choice> getChoices() {
        if (choices==null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(choices);
    }
    
    public ChoiceInfo.Status getStatus() {
        return status;
    }
    
    public void add(final Choice choice) {
        if (choices==null) {
            choices=new ArrayList<Choice>();
        }
        choices.add(choice);
    }
    
    public void setStatus(final ChoiceInfo.Status.Flag statusFlag, final String statusMessage) {
        this.status=new ChoiceInfo.Status(statusFlag, statusMessage);
    }

}
