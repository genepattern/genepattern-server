package org.genepattern.analysis;

/**
 * Used to hold information about Task Parameter
 *
 * @author Rajesh Kuttan, Hui Gong
 * @version $Revision 1.3$
 */

import java.io.Serializable;
import java.util.HashMap;

public class ParameterInfo implements Serializable {

    private String name="",value="",description="";
    private HashMap attributes;
    //keys for the attributes HashMap
    public static final String TYPE="TYPE";
    public static final String MODE="MODE";
    public static final String DISPLAY_TYPE="DISPLAY_TYPE";
    public static final String LABEL="LABEL";
    public static final String DEFAULT="DEFAULT";
    public static final String RANGE="RANGE";
    public static final String NOTE="NOTE";
    //used as a value for key "TYPE"
    public static final String FILE_TYPE="FILE";
    //used as value for key "MODE"
    public static final String INPUT_MODE="IN";
    public static final String OUTPUT_MODE="OUT";

    // the CACHED_INPUT_MODE and URL_INPUT_MODE parameters are not used by the server
    // they are here so that the JavaGE client can set the input mode to something other than IN
    public static final String CACHED_INPUT_MODE = "CACHED_IN";
    public static final String URL_INPUT_MODE = "URL_IN";

    /** Creates new ParameterInfo */
    public ParameterInfo() {
    }

    public ParameterInfo(String name,String value,String description) {
        this.name=name;
        this.value=value;
        this.description=description;
    }

    public void setName(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        this.value=value;
    }

    public String getValue() {
        return value;
    }

    public void setDescription(String description) {
        this.description=description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Sets attributes for the ParameterInfo
     * @param attributes a HashMap containing detail information about this parameter
     */
    public void setAttributes(HashMap attributes){
        this.attributes = attributes;
    }

    /**
     * Gets HashMap attributes
     * @return HashMap attributes containing keys as described in the public static field,
     * and values for description of detail information about the parameter
     */
    public HashMap getAttributes(){
        return this.attributes;
    }

    /**
     * Checks to see if this parameter is used to specify uploaded file from client.
     * @return true if this is a uploaded file from client
     */
    public boolean isInputFile(){
        if(attributes!=null && attributes.containsKey(TYPE) && attributes.containsKey(MODE)){
            String type = (String)this.attributes.get(TYPE);
            String mode = (String)this.attributes.get(MODE);
            if(type.equals(FILE_TYPE) && mode.equals(INPUT_MODE)){
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if this parameter is used to specify output file from server
     * @return true if this is used to specify result file from server
     */
    public boolean isOutputFile(){
        if(this.attributes!=null && this.attributes.containsKey(ParameterInfo.TYPE) && this.attributes.containsKey(ParameterInfo.MODE)){
            String type = (String)this.attributes.get(ParameterInfo.TYPE);
            String mode = (String)this.attributes.get(ParameterInfo.MODE);
            if(type.equals(ParameterInfo.FILE_TYPE)&&mode.equals(ParameterInfo.OUTPUT_MODE)){
                return true;
            }
        }
        return false;
    }

    /**
     * Sets this as an input file parameter.
     */
    public void setAsInputFile(){
        if(this.attributes==null) this.attributes = new HashMap();
        this.attributes.put(ParameterInfo.TYPE, ParameterInfo.FILE_TYPE);
        this.attributes.put(ParameterInfo.MODE, ParameterInfo.INPUT_MODE);
    }

    /**
     * Sets this as an output file parameter.
     */
    public void setAsOutputFile(){
        if(this.attributes==null) this.attributes = new HashMap();
        this.attributes.put(ParameterInfo.TYPE, ParameterInfo.FILE_TYPE);
        this.attributes.put(ParameterInfo.MODE, ParameterInfo.OUTPUT_MODE);
    }

    /**
     * Gets the parameter displaying label.
     * @return displaying label as a String
     */
    public String getLabel(){
        String label="";
        if(this.attributes!=null && this.attributes.containsKey(ParameterInfo.LABEL)){
            label = (String)this.attributes.get(ParameterInfo.LABEL);
        }
        return label;
    }

    /**
     * Sets the parameter displaying label
     * @param label used as displaying purpose
     */
    public void setLabel(String label){
        if(this.attributes==null) this.attributes = new HashMap();
        this.attributes.put(ParameterInfo.LABEL, label);
    }
    
    /** returns a String representation of this */
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("name=");
        buf.append(this.name);
        
        buf.append(" value=");
        buf.append(value);
        
        buf.append(" Description");
        buf.append(description);
        
        buf.append(" Attribute:");
        buf.append(attributes);
        return buf.toString();
    }
    
    public boolean hasChoices(String delimiter) {
	return value != null && value.length() > 0 && getChoices(delimiter).length > 1;
    }

    public String[] getChoices(String delimiter) {
    	if (value == null) return new String[0];
	String[] choices = value.split(delimiter, -1);
	return choices;
    }
}
