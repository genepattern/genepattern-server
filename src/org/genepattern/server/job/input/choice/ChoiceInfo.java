package org.genepattern.server.job.input.choice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.webservice.ParameterInfo;

/**
 * JavaBean representation of a module drop-down menu for a given input parameter.
 * Initialized by parsing the parameter info attributes from the manifest file.
 * @author pcarr
 *
 */
public class ChoiceInfo {
    final static private Logger log = Logger.getLogger(ChoiceInfo.class);

    /**
     * ParameterInfo attribute for the module manifest. Set the list of choices for a drop-down menu for this parameter.
     * This is in the same format as the 'value' parameter for GP <=3.6.1 'Choice' parameter.
     * 
     * E.g.
     *     p4_choices=ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf/Arabidopsis_thaliana_Ensembl_TAIR10.gtf=Arabidopsis_thaliana_Ensembl_TAIR10.gtf;
     */
    public static final String PROP_CHOICE="choices";
    /**
     * ParameterInfo attribute for the module manifest.
     * Create a drop-down menu by listing the contents of the remote directory. 
     * This is created dynamically, when the module input form is created, instead of hard-coding
     * a list of values in the manifest file.
     * 
     * E.g.
     *     p4_choiceDir=ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf
     */
    public static final String PROP_CHOICE_DIR="choiceDir";
    /**
     * ParameterInfo attribute for the module manifest. 
     * When creating a drop-down menu from a remote directory, optionally filter the list of
     * values based on a glob pattern. This filter applies only to the remote 'choiceDir' setting.
     * 
     * By default, 'readme.*' and '*.md5' files are ignored. Override the default setting by adding a
     * comma separated list of glob (e.g. '*.gtf') or anti-glob (e.g. '!*.md5') patterns.
     *     p4_choiceDirFilter=*.gtf
     *     p4_choiceDirFilter=!*.md5
     * 
     * By default, directory values are ignored, override the default setting by adding an optional 'type=<all | dir | file>'.
     *     p4_choiceDirFilter=type=all
     *     p4_choiceDirFilter=type=file
     *     p4_choiceDirFilter=type=dir
     * 
     * These two can be combined, but you must only set one type= value.
     *     p4_choiceDirFilter=type=dir&human*
     */
    public static final String PROP_CHOICE_DIR_FILTER="choiceDirFilter";
    /**
     * ParameterInfo attribute for the module manifest, optionally declare whether a custom value is allowed for the given
     * parameter. This only applies for parameters with a drop-down menu.
     */
    public static final String PROP_CHOICE_ALLOW_CUSTOM_VALUE="choiceAllowCustom";
    /**
     * ParameterInfo attribute for the module manifest, optionally declare the display value for the first item on the drop-down menu.
     * E.g. 'Choose...'.
     */
    public static final String PROP_CHOICE_EMPTY_DISPLAY_VALUE="choiceEmptyDisplayValue";
    public static final String CHOICE_EMPTY_DISPLAY_VALUE_DEFAULT="Choose ...";

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
        
        public String toString() {
            return flag.name()+": "+message;
        }
    }

    
    final static ChoiceInfoParser choiceInfoParser= new DynamicChoiceInfoParser();
    public static ChoiceInfoParser getChoiceInfoParser() {
        return choiceInfoParser;
    }
    
    final private String paramName;
    
    private String choiceDir=null;
    private List<Choice> choices=null;
    private ChoiceInfo.Status status=null;
    private Choice selected=null;
    private boolean allowCustomValue=true;
    
    public ChoiceInfo(final String paramName) {
        this.paramName=paramName;
    }
    
    public String getParamName() {
        return paramName;
    }
    
    public Choice getSelected() {
        return selected;
    }
    
    public void setChoiceDir(final String choiceDir) {
        this.choiceDir=choiceDir;
    }
    public String getChoiceDir() {
        return choiceDir;
    }
    
    public List<Choice> getChoices() {
        if (choices==null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(choices);
    }
    
    public void setAllowCustomValue(final boolean b) {
        this.allowCustomValue=b;
    }
    public boolean isAllowCustomValue() {
        return allowCustomValue;
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
    
    /**
     * If necessary add an empty item to the top of the choice list and select the default value.
     * Must call this method after the list of choices is initialized.
     * 
     * If there is an empty valued item on the list don't create a new 'Choose...' entry.
     * Otherwise,
     *     If the param is optional, always add a 'Choose...' option as the 1st item on the list
     *     If the param is required, include a 'Choose...' option only if there is no default value
     * 
     * @param defaultValue
     */
    public void initDefaultValue(final ParameterInfo param) {
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        final String defaultValue=param.getDefaultValue();
        final boolean hasDefaultValue = ChoiceInfoHelper.isSet(defaultValue);
        
        //1) check for an existing empty valued item 
        Choice emptyChoice = getFirstMatchingValue("");
        if (emptyChoice == null) {
            // otherwise, check for an existing item with an empty displayValue
            emptyChoice = getFirstMatchingLabel("");
        }
        if (emptyChoice == null) {
            final boolean appendEmptyChoice=!hasDefaultValue;
            //Note: this is a workaround for GP-4615, a more natural rule would be to include the 
            //    'Choose...' menu for all optional parameters as well as required parameters which don't have a default value
            //    e.g.
            //    final boolean appendEmptyChoice=( param.isOptional() || (!param.isOptional() && !hasDefaultValue) );
            if (appendEmptyChoice) {
                //check manifest for displayValue for the first item on the list
                String emptyDisplayValue= (String) param.getAttributes().get(PROP_CHOICE_EMPTY_DISPLAY_VALUE);
                if (emptyDisplayValue==null) {
                    emptyDisplayValue=CHOICE_EMPTY_DISPLAY_VALUE_DEFAULT;
                }
                emptyChoice=new Choice(emptyDisplayValue, "");
                if (choices==null) {
                    log.error("Can't set default value on an empty list");
                }
                else {
                    //insert at top of the list
                    choices.add(0, emptyChoice);
                } 
            }
        }
        
        //2) select either the default value or the empty value if there is no default value
        Choice defaultChoice;
        if (!hasDefaultValue) {
            defaultChoice=emptyChoice;
        }
        else {
            // if we're here, it means there is a default value
            defaultChoice = getFirstMatchingValue(defaultValue);
            if (defaultChoice==null) {
                //try to match by name
                defaultChoice = getFirstMatchingLabel(defaultValue);
                if (defaultChoice != null) {
                    log.debug("No match by value, but found match by displayName="+defaultValue);
                }
            }
            if (defaultChoice==null) {
                log.debug("No match in choice for defaultValue="+defaultValue);
                defaultChoice=emptyChoice;
            }
        }
        selected=defaultChoice;
        log.debug("Initial selection is "+selected);
    }
    
    public Choice getValue(final String value) {
        if (value==null) {
            throw new IllegalArgumentException("value==null");
        }
        if (choices==null) {
            return null;
        }
        for(Choice choice : choices) {
            if (value.equals(choice.getValue())) {
                return choice;
            }
        }
        return null; 
    }
    
    private Choice getFirstMatchingValue(final String value) {
        if (value==null) {
            throw new IllegalArgumentException("value==null");
        }
        if (choices==null) {
            return null;
        }
        for(Choice choice : choices) {
            if (value.equals(choice.getValue())) {
                return choice;
            }
        }
        return null;
    }

    private Choice getFirstMatchingLabel(final String label) {
        if (label==null) {
            throw new IllegalArgumentException("label==null");
        }
        if (choices==null) {
            return null;
        }
        for(Choice choice : choices) {
            if (label.equals(choice.getLabel())) {
                return choice;
            }
        }
        return null;
    }

}
