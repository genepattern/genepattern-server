package org.genepattern.server.job.input.choice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.webservice.ParameterInfo;

import com.google.inject.internal.Objects;

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
     * Advanced configuration option for the FTP mode to use when listing the contents of
     * a choiceDir from a remote FTP server. Passive mode is used by default. This property is
     * only required if you need to use active mode.
     * 
     * E.g.
     *     p4_choiceDirFtpPassiveMode=false
     * 
     */
    public static final String PROP_CHOICE_DIR_FTP_PASSIVE_MODE="choiceDirFtpPassiveMode";
    
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
    public static final String CHOICE_EMPTY_DISPLAY_VALUE_DEFAULT="";

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
     *     NOT_INITIALIZED, the server did not initialize the list of choices from the remote directory,
     * 
     * @author pcarr
     *
     */
    public static class Status {
        public static enum Flag {
            OK,
            WARNING,
            ERROR,
            NOT_INITIALIZED
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

    public static enum Type {
        NotSet,  //no declared drop-down menu
        StaticText, //a static text drop-down menu
        StaticFile, //a static file drop-down menu
        DynamicFile; //a dynamic file drop-down menu
    }
    
    public static Type initType(final ParameterInfo param) {
        //the new way (>= 3.7.0), check for remote ftp directory
        final String choiceDir = (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR);
        final String declaredChoicesStr= (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE);
        if (param.isInputFile()) {
            if (choiceDir != null) {
                return Type.DynamicFile;
            }
            if (declaredChoicesStr != null) {
                return Type.StaticFile;
            }
        }
        //the new way (>= 3.7.0), check for 'choice' attribute in manifest
        if (declaredChoicesStr != null) {
            return Type.StaticText;
        }
        //the old way (<= 3.6.1, based on 'values' attribute in manifest)
        final String choicesString=param.getValue();
        final List<Choice> choiceList=ChoiceInfoHelper.initChoicesFromManifestEntry(choicesString);
        if (choiceList != null && choiceList.size() > 0) {
            return Type.StaticText;
        }
        return Type.NotSet;
    }

    public static final boolean hasDynamicChoiceInfo(final ParameterInfo param)
    {
        final String choiceDir = (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR);
        if (choiceDir != null) {
            return true;
        }

        return false;
    }

    public static final boolean hasStaticChoiceInfo(final ParameterInfo param)
    {
        final String declaredChoicesStr= (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE);
        if (declaredChoicesStr != null) {
            return true;
        }
        Map<String,String> legacy=param.getChoices();
        if (legacy != null && legacy.size()>0) {
            return true;
        }
        return false;
    }

    public static final boolean hasChoiceInfo(final ParameterInfo param) {
        final String choiceDir = (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR);
        if (choiceDir != null) {
            return true;
        }
        final String declaredChoicesStr= (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE);
        if (declaredChoicesStr != null) {
            return true;
        }
        Map<String,String> legacy=param.getChoices();
        if (legacy != null && legacy.size()>0) {
            return true;
        }
        return false;
    }
    
    /**
     * Get the display value for a drop-down parameter. This is called when building up the job status page or execution log,
     * starting with the actual parameter value, do a reverse-lookup of the display name for the parameter.
     * 
     * This only returns a value for static drop-down parameters.
     * 
     * @param actualValue, the value that is passed along as an arg on the module command line
     * @param formalParam, the formal ParameterInfo (does not necessary have the actual value)
     * @return the displayValue or null if this does not match the existing drop-down menu item
     * 
     */
    public static final String getDisplayValueForActualValue(final String actualValue, final ParameterInfo formalParam) {
        if (ChoiceInfo.hasDynamicChoiceInfo(formalParam)) {
            if (log.isDebugEnabled()) { log.debug("skipping dynamic drop-down, pname="+formalParam.getName()); }
            return null;
        }
        if (!ChoiceInfo.hasStaticChoiceInfo(formalParam)) {
            if (log.isDebugEnabled()) { log.debug("skipping static drop-down, pname="+formalParam.getName()); }
            return null;
        }
        int numMatches=0;
        String displayValue=null;
        List<Choice> choices=ChoiceInfo.getStaticChoices(formalParam);
        for(final Choice choice : choices) {
            if (Objects.equal(choice.getValue(), actualValue)) {
                ++numMatches;
                displayValue=choice.getLabel();
            }
        }
        if (numMatches==1) {
            return displayValue;
        }
        else if (numMatches > 1) {
            log.error("numMatches="+numMatches+" for param.name="+formalParam.getName()+", param.value="+actualValue);
            return null;
        }
        else {
            return null;
        }
    }
    
    /**
     * Get the list of choices declared directly in the manifest with the newer 'choices' attribute.
     * 
     * @param param
     * @return
     */
    public static final List<Choice> getDeclaredChoices(final ParameterInfo param) {
        final String declaredChoicesStr= (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE);
        if (declaredChoicesStr != null) {
            log.debug("Initializing "+ChoiceInfo.PROP_CHOICE+" entry from manifest for parm="+param.getName());
            //choices=ParameterInfo._initChoicesFromString(declaredChoicesStr);
            return ChoiceInfoHelper.initChoicesFromManifestEntry(declaredChoicesStr);
        }
        return Collections.emptyList();
    }
    
    /**
     * Get the list of choices declared directly in the manifest with either the newer 'choices' attribute
     * or the older (<=3.6.1) manifest format.
     * 
     * @param param
     * @return
     */
    public static final List<Choice> getStaticChoices(final ParameterInfo param) {
        //the new way (>= 3.7.0), check for 'choice' attribute in manifest
        final String declaredChoicesStr= (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE);
        if (declaredChoicesStr != null) {
            log.debug("Initializing "+ChoiceInfo.PROP_CHOICE+" entry from manifest for parm="+param.getName());
            return ChoiceInfoHelper.initChoicesFromManifestEntry(declaredChoicesStr);
        }
        else {
            //the old way (<= 3.6.1, based on 'values' attribute in manifest)
            log.debug("Initializing choices from value attribute");
            final String choicesString=param.getValue();
            return ChoiceInfoHelper.initChoicesFromManifestEntry(choicesString);
        }
    }

    /**
     * Called from the DirFilter(ParameterInfo param) constructor to extract the
     * 'choiceDirFilter' value from the parameter.
     * 
     * @param param
     * @return
     */
    public static String getChoiceDirFilter(final ParameterInfo param) {
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        if (param.getAttributes()==null) {
            throw new IllegalArgumentException("param.attributes==null");
        }
        final String choiceDirFilter;
        if ( param.getAttributes().containsKey(ChoiceInfo.PROP_CHOICE_DIR_FILTER) )
            //trim if necessary
            choiceDirFilter = ((String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR_FILTER)).trim();
        else {
            choiceDirFilter = "";
        }
        return choiceDirFilter;
    }
    
    /**
     * Check the manifest for an optional ftp passive mode flag.
     * By default and when not set return true.
     * Return false only if choiceDirFtpPassiveMode=false
     * 
     * @param param
     * @return
     */
    public static boolean getFtpPassiveMode(final ParameterInfo param) {
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        if (param.getAttributes()==null) {
            throw new IllegalArgumentException("param.attributes==null");
        }
        //final String choiceDirFilter;
        if ( param.getAttributes().containsKey(ChoiceInfo.PROP_CHOICE_DIR_FTP_PASSIVE_MODE) ) {
            // return false only if choiceDirFtpPassMode=false
            final String passiveModeStr = ((String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR_FTP_PASSIVE_MODE)).trim().toLowerCase();
            if (passiveModeStr.equals("false")) {
                return false;
            }
        }
        // by default, return true
        return true;
    }

    /**
     * @param initDynamicDropdown, if true initialize dynamic drop-down by doing a remote 'ls' call
     *     (or loading from a cached copy if it's available).
     * @return
     * @deprecated - prefer to pass in a valid gpContext
     */
    public static ChoiceInfoParser getChoiceInfoParser(final boolean initDynamicDropdown) {
        GpConfig gpConfig=ServerConfigurationFactory.instance();
        GpContext gpContext=GpContext.getServerContext();
        return getChoiceInfoParser(gpConfig, gpContext, initDynamicDropdown);
    }
    
    public static ChoiceInfoParser getChoiceInfoParser(final GpContext gpContext) {
        GpConfig gpConfig=ServerConfigurationFactory.instance();
        return getChoiceInfoParser(gpConfig, gpContext);
    }
    
    public static ChoiceInfoParser getChoiceInfoParser(final GpConfig gpConfig, final GpContext gpContext) {
        final boolean initDynamicDropdown=true;
        return getChoiceInfoParser(gpConfig, gpContext, initDynamicDropdown);
    }
    
    /**
     * 
     * @param gpConfig, a valid GP server configuration
     * @param gpContext, a valid GP context, expecting to have taskInfo set.
     * @param initDynamicDropdown, if true initialize dynamic drop-down by doing a remote 'ls' call
     *     (or loading from a cached copy if it's available).
     * @return
     */
    public static ChoiceInfoParser getChoiceInfoParser(final GpConfig gpConfig, final GpContext gpContext, final boolean initDynamicDropdown) {
        return new DynamicChoiceInfoParser(gpConfig, gpContext, initDynamicDropdown);
    }

    private final String paramName;
    
    private String choiceDir=null;
    private List<Choice> choices=null;
    private ChoiceInfo.Status status=null;
    private Choice selected=null;
    private boolean allowCustomValue=true;
    
    public ChoiceInfo(final String paramName) {
        this.paramName=paramName;
    }
    
    /**
     * Helper method to set the choiceInfo.allowCustomValue flag based on the
     *     properties in the manifest file.
     * 
     * @param choiceInfo
     * @param param
     */
    public void initAllowCustomValue(final ParameterInfo param) {
        final String allowCustomValueStr;
        if (param==null) {
            allowCustomValueStr=null;
        }
        else {
            allowCustomValueStr= (String) param.getAttributes().get( ChoiceInfo.PROP_CHOICE_ALLOW_CUSTOM_VALUE );
        }
        if (ChoiceInfoHelper.isSet(allowCustomValueStr)) {
            // if it's 'on' then it's true
            if ("on".equalsIgnoreCase(allowCustomValueStr.trim())) {
                setAllowCustomValue(true);
            }
            else {
                //otherwise, false
                setAllowCustomValue(false);
            }
        }
        else if (param.isInputFile()) {
            //by default, a file choice parameter allows a custom value
            setAllowCustomValue(true);
        }
        else {
            //by default, a text choice parameter does not allow a custom value
            setAllowCustomValue(false);
        }
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
    
    public void setStatus(final ChoiceInfo.Status.Flag statusFlag, final String statusMessage) {
        this.status=new ChoiceInfo.Status(statusFlag, statusMessage);
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
    
    /**
     * If there is a default_value which matches one of the drop-down items return
     * the matching Choice, or null if none set or no match.
     * 
     * @param param
     * @return
     */
    private Choice getDeclaredDefaultChoice(final ParameterInfo param) {
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        final String defaultValue=param.getDefaultValue();
        final boolean hasDefaultValue = ChoiceInfoHelper.isSet(defaultValue);
        if (!hasDefaultValue) {
            return null;
        }
        // if we're here, it means there is a default value
        Choice defaultChoice = getFirstMatchingValue(defaultValue);
        if (defaultChoice==null) {
            //try to match by name
            defaultChoice = getFirstMatchingLabel(defaultValue);
            if (defaultChoice != null) {
                log.debug("No match by value, but found match by displayName="+defaultValue);
            }
        }
        return defaultChoice;
    }

    /**
     * When to add an empty choice to the list?
     * If it's a File drop-down AND there is not an existing empty item on the list
     * 
     * use-cases:
     * - a text drop-down, never append an empty choice
     * - a file drop-down which already has an empty actual value, never append an empty choice
     * - an optional file drop-down with no default value, yes
     * - an optional file drop-down with a default value, yes
     * - a required file drop-down with no default value, yes
     * - a required file drop-down with a default value, maybe ... but we will go with yes
     * 
     * @param param
     */
    private Choice getChoiceToAppend(final Type type, final ParameterInfo param) {
        if (type==Type.NotSet || type==Type.StaticText) {
            return null;
        }
        
        //1) check for an existing empty valued item 
        //final Choice emptyMatchingValue=getFirstMatchingValue("");
        //final Choice emptyMatchingLabel=getFirstMatchingLabel("");
        final boolean appendEmptyChoice =
                getFirstMatchingValue("") == null &&
                getFirstMatchingLabel("") == null;
        if (appendEmptyChoice) {
            //check manifest for displayValue for the first item on the list
            String emptyDisplayValue= (String) param.getAttributes().get(PROP_CHOICE_EMPTY_DISPLAY_VALUE);
            if (emptyDisplayValue==null) {
                emptyDisplayValue=CHOICE_EMPTY_DISPLAY_VALUE_DEFAULT;
            }
            final Choice choiceToAppend=new Choice(emptyDisplayValue, "");
            return choiceToAppend;
        } 
        return null;
    }

    public void initDefaultValue(final ParameterInfo param) {
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        //special-case: choices not initialized
        if (choices==null) {
            log.warn("choices==null, for param.name="+param.getName());
            return;
        }
        //special-case: no choices
        if (choices.size()==0) {
            log.warn("choices.size==0, for param.name="+param.getName());
            return;
        }

        final Type type=initType(param);
        Choice choiceToAppend=getChoiceToAppend(type,param);
        if (choiceToAppend != null) {
            choices.add(0, choiceToAppend);
        }
        Choice defaultChoice=getDeclaredDefaultChoice(param);
        if (defaultChoice == null) {
            defaultChoice=choices.get(0);
        }
        selected=defaultChoice;
        log.debug("Initial selection is "+selected);

    }
    
    /**
     * If necessary add an empty item to the top of the choice list and select the default value.
     * Must call this method after the list of choices is initialized.
     * 
     * If there is no default value AND if there is not an existing empty-valued item on the list
     * append a new 'Choose...' option as the 1st item on the list.
     * 
     * The author the module can override the default ('Choose...') display value in one of two ways.
     * 1) add an empty valued item to the list of choices, fill in the custom display value, or
     * 2) set the choiceEmptyDisplayValue property in the manifest, e.g.
     *     p4_choiceEmptyDisplayValue=Make a selection...
     * 
     * @param defaultValue
     */
    public void _initDefaultValue(final ParameterInfo param) {
        if (param.isInputFile()) {
            _initDefaultValue_FileType(param);
        }
        _initDefaultValue_TextType(param); 
    }

    public void _initDefaultValue_FileType(final ParameterInfo param) {
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        //special-case: no choices
        if (choices==null || choices.size()==0) {
            return;
        }
        
        //1) check for a default value in the list
        final Choice defaultChoice=getDeclaredDefaultChoice(param);
        if (defaultChoice != null) {
            selected=defaultChoice;
            log.debug("Initial selection is "+selected);
            
        }
        //otherwise select the first item from the list
        selected=choices.get(0);
        log.debug("Initial selection is "+selected);
    }
    
    /**
     * Legacy mode for default choice
     */
    public void _initDefaultValue_TextType(final ParameterInfo param) {
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        //special-case: no choices
        if (choices==null || choices.size()==0) {
            return;
        }
        
        Choice defaultChoice=getDeclaredDefaultChoice(param);
        if (defaultChoice == null) {
            defaultChoice=choices.get(0);
        }
        selected=defaultChoice;
        log.debug("Initial selection is "+selected);
    } 

    public Choice getValue(final String lvalue) {
        if (lvalue==null) {
            throw new IllegalArgumentException("value==null");
        }
        if (choices==null) {
            return null;
        }
        
        Choice altMatch=null; //double check for trailing slash ('/') 
        for(Choice choice : choices) {
            final String rvalue=choice.getValue();
            if (lvalue.equals(rvalue)) {
                return choice;
            }
            if (altMatch == null) {
                if (Choice.equalsIgnoreTrailingSlash(lvalue,rvalue, false)) {
                    //special-case, matched without trailing slash
                    altMatch=choice;
                }
            }
        }
        
        if (altMatch != null) {
            return altMatch;
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
