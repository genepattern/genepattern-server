package org.genepattern.server.job.input.choice;

import java.util.ArrayList;
import java.util.List;

/**
 * Initialized by parsing the manifest for a given parameter info.
 * @author pcarr
 *
 */
public class ChoiceInfo {
    //final static ChoiceInfoParser defaultChoiceInfoParser= new DefaultChoiceInfoParser();
    final static ChoiceInfoParser choiceInfoParser= new DynamicChoiceInfoParser();
    public static ChoiceInfoParser getChoiceInfoParser() {
        return choiceInfoParser;
    }
    
    private String ftpDir=null;
    private List<Choice> choices=null;
    
    public void setFtpDir(final String ftpDir) {
        this.ftpDir=ftpDir;
    }
    public String getFtpDir() {
        return ftpDir;
    }
    
    public List<Choice> getChoices() {
        return choices;
    }
    
    public void add(final Choice choice) {
        if (choices==null) {
            choices=new ArrayList<Choice>();
        }
        choices.add(choice);
    }

}
