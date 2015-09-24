package org.genepattern.junitutil;

import java.util.HashMap;

import org.genepattern.server.job.input.NumValues;
import org.genepattern.server.job.input.NumValuesParserImpl;
import org.genepattern.server.job.input.ParamListHelper.ListMode;
import org.genepattern.webservice.ParameterInfo;

@SuppressWarnings("unchecked")
public class ParameterInfoBuilder {
    private ParameterInfo pinfo=new ParameterInfo();

    public ParameterInfoBuilder() {
        reset();
    }
    
    public void reset() {
        pinfo=new ParameterInfo();   
        pinfo.setAttributes(new HashMap<String,String>());
    }
    
    public ParameterInfo build() {
        return pinfo;
    }

    public ParameterInfoBuilder name(final String name) {
        pinfo.setName(name);
        return this;
    }
    
    public ParameterInfoBuilder description(final String description) {
        pinfo.setDescription(description);
        return this;
    }
    
    public ParameterInfoBuilder property(final String key, final String value) {
        pinfo.getAttributes().put(key, value);
        return this;
    }

    public ParameterInfoBuilder defaultValue(final String defaultValue) {
        pinfo.getAttributes().put("default_value", defaultValue);
        return this;
    }
    
    public ParameterInfoBuilder flag(final String flag) {
        pinfo.getAttributes().put("flag", flag);
        return this;
    }
    
    public ParameterInfoBuilder prefixWhenSpecified(final String prefix_when_specified) {
        pinfo.getAttributes().put("prefix_when_specified", prefix_when_specified);
        return this;
    }
    
    public ParameterInfoBuilder optional(boolean optional) {
        if (optional) {
            pinfo.getAttributes().put("optional", "on");
        }
        else {
            pinfo.getAttributes().put("optional", "");
        }
        return this;
    }

    /**
     * For example, 0+, 0..1, 1+, 4
     * @param numValuesSpec
     * @return
     * @throws Exception
     */
    public ParameterInfoBuilder numValues(final String numValuesSpec) throws Exception {
        NumValues numValues=new NumValuesParserImpl().parseNumValues(numValuesSpec);
        return numValues(numValues);
    }

    public ParameterInfoBuilder numValues(final NumValues numValues) {
        pinfo.getAttributes().put("numValues", numValuesToString(numValues));
        return this;
    }
    
    protected static String numValuesToString(final NumValues numValues) {
        if (numValues==null) {
            return "";
        }
        
        // min must be non-null so ...
        if (numValues.getMin()==numValues.getMax()) {
            return ""+numValues.getMin();
        }
        if (numValues.getMax()==null) {
            return numValues.getMin()+"+";
        }
        return numValues.getMin()+".."+numValues.getMax();
    }
    
    public ParameterInfoBuilder listMode(ListMode listMode) {
        pinfo.getAttributes().put("listMode", listMode.name());
        return this;
    }
    
    public ParameterInfoBuilder listModeSep(final String listModeSep) {
        pinfo.getAttributes().put("listModeSep", listModeSep);
        return this;
    }
    
}
