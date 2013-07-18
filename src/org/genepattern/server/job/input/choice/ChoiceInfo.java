package org.genepattern.server.job.input.choice;

import java.util.ArrayList;
import java.util.List;

/**
 * Initialized by parsing the manifest for a given parameter info.
 * @author pcarr
 *
 */
public class ChoiceInfo {
    final static ChoiceInfoParser defaultChoiceInfoParser= new DefaultChoiceInfoParser();
    public static ChoiceInfoParser getChoiceInfoParser() {
        return defaultChoiceInfoParser;
    }
    
    Object source;
    private List<Choice> choices=null;
    
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
