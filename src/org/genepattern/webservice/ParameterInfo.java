/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.webservice;

/**
 * Used to hold information about Task Parameter
 * 
 * @author Rajesh Kuttan, Hui Gong
 * @version $Revision 1.3$
 */

import static org.genepattern.util.GPConstants.PARAM_INFO_NAME_OFFSET;
import static org.genepattern.util.GPConstants.PARAM_INFO_PREFIX;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.genepattern.util.GPConstants;

public class ParameterInfo implements Serializable {

    private String name = "", value = "", description = "";

    private HashMap attributes;

    // note additional keys for the attributes map are defined in
    // GPConstants.PARAM_INFO_XX
    // keys for the attributes HashMap
    public static final String TYPE = "TYPE";

    public static final String MODE = "MODE";

    // used as a value for key "TYPE"
    public static final String FILE_TYPE = "FILE";

    // used as value for key "MODE"
    public static final String INPUT_MODE = "IN";

    public static final String OUTPUT_MODE = "OUT";

    // the CACHED_INPUT_MODE and URL_INPUT_MODE parameters are not used by the
    // server
    // they are here so that the JavaGE client can set the input mode to
    // something other than IN
    public static final String CACHED_INPUT_MODE = "CACHED_IN";

    public static final String URL_INPUT_MODE = "URL_IN";

    private static final String LABEL = "LABEL";

    /** Creates new ParameterInfo */
    public ParameterInfo() {
    }

    public ParameterInfo(String name, String value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Sets attributes for the ParameterInfo
     * 
     * @param attributes
     *            a HashMap containing detail information about this parameter
     */
    public void setAttributes(HashMap attributes) {
        this.attributes = attributes;
    }

    /**
     * Gets HashMap attributes
     * 
     * @return HashMap attributes containing keys as described in the public
     *         static field, and values for description of detail information
     *         about the parameter
     */
    public HashMap getAttributes() {
        return this.attributes;
    }

    /**
     * Checks to see if this parameter is used to specify uploaded file from
     * client.
     * 
     * @return true if this is a uploaded file from client
     */
    public boolean isInputFile() {
        if (attributes != null && attributes.containsKey(TYPE) && attributes.containsKey(MODE)) {
            String type = (String) this.attributes.get(TYPE);
            String mode = (String) this.attributes.get(MODE);
            if (type.equals(FILE_TYPE) && mode.equals(INPUT_MODE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if this parameter is used to specify output file from
     * server
     * 
     * @return true if this is used to specify result file from server
     */
    public boolean isOutputFile() {
        if (this.attributes != null && this.attributes.containsKey(ParameterInfo.TYPE)
                && this.attributes.containsKey(ParameterInfo.MODE)) {
            String type = (String) this.attributes.get(ParameterInfo.TYPE);
            String mode = (String) this.attributes.get(ParameterInfo.MODE);
            if (type.equals(ParameterInfo.FILE_TYPE) && mode.equals(ParameterInfo.OUTPUT_MODE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if this parameter is used to specify a password from
     * client.
     * 
     * @return true if this is a uploaded file from client
     */
    public boolean isPassword() {
        if (attributes != null && (attributes.containsKey(TYPE) || attributes.containsKey(TYPE.toLowerCase()))) {
            String type = (String) this.attributes.get(TYPE);
            if (type == null)
                type = (String) this.attributes.get(TYPE.toLowerCase());
            if (type.equals(GPConstants.PARAM_INFO_PASSWORD)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean _isDirectory() {
        if (attributes != null && (attributes.containsKey(TYPE) || attributes.containsKey(TYPE.toLowerCase()))) {
            String type = (String) this.attributes.get(TYPE);
            if (type == null)
                type = (String) this.attributes.get(TYPE.toLowerCase());
            if (type.equals(GPConstants.PARAM_INFO_TYPE_DIR)) {
                return true;
            }
        }
        return false;
    }

    //support for STDERR and STDOUT result files
    private static final String IS_STDOUT_KEY = "IS_STDOUT";
    private static final String IS_STDERR_KEY = "IS_STDERR";
    public boolean _isStdoutFile() {
        return isOutputFile() && attributes != null && attributes.containsKey(IS_STDOUT_KEY);
    }

    public boolean _isStderrFile() {
        return isOutputFile() && attributes != null && attributes.containsKey(IS_STDERR_KEY);
    }
	
    public void _setAsStdoutFile() {
        setAsOutputFile();
        this.attributes.put(IS_STDOUT_KEY, "true");
    }

    public void _setAsStderrFile() {
        setAsOutputFile();
        this.attributes.put(IS_STDERR_KEY, "true");
    }
	
    //helper methods for configuration properties
    //private static String CONFIG_PARAM_PREFIX = ".gp.config.";
    //public boolean _isConfigurationParameter() {
    //    return name.startsWith(CONFIG_PARAM_PREFIX);
    //}
    //public String _getConfigurationParameterName() {
    //    if (name.startsWith(CONFIG_PARAM_PREFIX)) {
    //        return name.substring( CONFIG_PARAM_PREFIX.length() );
    //    }
    //    //TODO: log error
    //    return name;
    //}

    /**
     * Sets this as an input file parameter.
     */
    public void setAsInputFile() {
        if (this.attributes == null)
            this.attributes = new HashMap();
        this.attributes.put(ParameterInfo.TYPE, ParameterInfo.FILE_TYPE);
        this.attributes.put(ParameterInfo.MODE, ParameterInfo.INPUT_MODE);
    }

    /**
     * Sets this as an output file parameter.
     */
    public void setAsOutputFile() {
        if (this.attributes == null)
            this.attributes = new HashMap();
        this.attributes.put(ParameterInfo.TYPE, ParameterInfo.FILE_TYPE);
        this.attributes.put(ParameterInfo.MODE, ParameterInfo.OUTPUT_MODE);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.name);
        if (!isOptional()) {
            buf.append(" *");
        }
        Map<String, String> choices = getChoices();
        if (choices.size() > 0) {
            buf.append(" [");
            boolean first = true;
            for (Iterator<String> keys = choices.keySet().iterator(); keys.hasNext();) {
                if (!first) {
                    buf.append(", ");
                }
                buf.append(keys.next());
                first = false;
            }
            buf.append("]");
        }

        return buf.toString();
    }

    public boolean hasChoices(String delimiter) {
        return value != null && value.length() > 0 && getChoices(delimiter).length > 1;
    }

    public String[] getChoices(String delimiter) {
        if (value == null)
            return new String[0];
        String[] choices = value.split(delimiter, -1);
        return choices;
    }

    // where a chioce lhs=rhs is present, get the rhs of the choice based
    // on the value of the parameter
    public String getUIValue(ParameterInfo formalParam) {
        if (formalParam == null) {
            formalParam = this;
        }
        if (!formalParam.hasChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER))
            return getValue();
        String uiValue = getValue();

        String[] choices = formalParam.getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
        for (int i = 0; i < choices.length; i++) {
            String[] subchoices = choices[i].split(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
            if (subchoices.length > 1) {

                String lhs = subchoices[0];
                String rhs = subchoices[1];
                if (lhs.equals(getValue()))
                    return rhs;
                if (rhs.equals(getValue()))
                    return rhs;
                // if not a match, go on to the next choice option
            }
        }
        return uiValue;
    }

    /**
     * Returns <tt>true</tt> if this parameter is optional, <tt>false</tt>
     * otherwise.
     * 
     * @return <tt>true</tt> if this parameter is optional, <tt>false</tt>
     *         otherwise.
     */
    public boolean isOptional() {
        if (getAttributes() == null) {
            return false;
        }
        String optional = (String) getAttributes().get(GPConstants.PARAM_INFO_OPTIONAL[0]);
        return (optional != null && optional.length() > 0);

    }
    
    public String _getOptionalPrefix() {
        String optionalPrefix = "";
        String key = PARAM_INFO_PREFIX[PARAM_INFO_NAME_OFFSET];
        if (attributes != null && attributes.containsKey(key)) {
            Object value = attributes.get(key);
            if (value != null) {
                optionalPrefix = (String) value;
            }
        }
        return optionalPrefix;
    }

    /**
     * Returns the default value for this parameter or <tt>null</tt> if no
     * default value has been set.
     * 
     * @return The default value.
     */
    public String getDefaultValue() {
        if (getAttributes() == null) {
            return null;
        }
        return (String) getAttributes().get((String) TaskExecutor.PARAM_INFO_DEFAULT_VALUE[0]);
    }

    /**
     * Returns a map of the choices for this parameter or an empty map if no
     * choices exist. The keys in the map are the choice values that are human
     * readable while the values are the corresponding choice values that are
     * passed on the command line.
     * 
     * @return The choices.
     */
    public Map<String, String> getChoices() {
        String choicesString = getValue();
        Map<String, String> uiValueToCommandLineValueMap = new HashMap<String, String>();
        if (choicesString != null && !choicesString.equals("")) {
            String[] choicesArray = choicesString.split(";");
            if (choicesArray != null) {
                for (int j = 0; j < choicesArray.length; j++) {
                    String[] choiceValueAndChoiceUIValue = choicesArray[j].split("=");
                    if (choiceValueAndChoiceUIValue.length == 2) {
                        uiValueToCommandLineValueMap
                                .put(choiceValueAndChoiceUIValue[1], choiceValueAndChoiceUIValue[0]);
                    } else if (choiceValueAndChoiceUIValue.length == 1) {
                        uiValueToCommandLineValueMap
                                .put(choiceValueAndChoiceUIValue[0], choiceValueAndChoiceUIValue[0]);
                    }
                }
            }
        }
        return uiValueToCommandLineValueMap;
    }

    /**
     * Gets the parameter displaying label.
     * 
     * @return displaying label as a String
     */
    public String getLabel() {
        String label = "";
        if (this.attributes != null && this.attributes.containsKey(ParameterInfo.LABEL)) {
            label = (String) this.attributes.get(ParameterInfo.LABEL);
        }
        return label;
    }

    /**
     * Sets the parameter displaying label
     * 
     * @param label
     *            used as displaying purpose
     */
    public void setLabel(String label) {
        if (this.attributes == null)
            this.attributes = new HashMap();
        this.attributes.put(ParameterInfo.LABEL, label);
    }
    
    //support for deep copy
    static public ParameterInfo _deepCopy(final ParameterInfo orig) {
        ParameterInfo copy = new ParameterInfo(orig.getName(), orig.getValue(), orig.getDescription());
        HashMap origAttributes = orig.getAttributes();
        HashMap copyAttributes = new HashMap();
        //this works as a deep copy because we know that the attributes are a Map<String,String>
        copyAttributes.putAll(orig.getAttributes());
        copy.setAttributes(copyAttributes);
        return copy;
    }

}
