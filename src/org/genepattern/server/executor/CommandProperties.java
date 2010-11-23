package org.genepattern.server.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Job configuration properties for a specific job, default values are loaded based on parsing the job_configuration.yaml file. 
 * The initial version of the API used a java.util.Properties object.
 * This type was created so that the config file can assign a String or a List<String>
 * to a given property value.
 * 
 * @author pcarr
 */
public class CommandProperties {
    public static class Value {
        private List<String> values = new ArrayList<String>();
        
        public Value(String value) {
            values.add(value);
        }
        
        public String getValue() {
            if (values.size() == 0) {
                return null;
            }
            return values.get(0);
        }
        
        public List<String> getValues() {
            return Collections.unmodifiableList(values);
        }
        
        public int numValues() {
            return values.size();
        }
    }
    
    /**
     * Utility method which initializes the Map<String,Value> from a Properties instance. 
     * It automatically converts null values to empty strings.
     * 
     * @param props
     */
    private void initFromProperties(Properties _props) {
        for(Map.Entry<Object,Object> entry : _props.entrySet()) {
            Value value = new Value(""+entry.getValue());
            props.put(""+entry.getKey(), value);
        }
    }
    
    private Map<String,Value> props  = new HashMap<String, Value>();
    
    public CommandProperties() {
    }
    public CommandProperties(Properties props) {
        initFromProperties(props);
    }
    
    public boolean containsKey(String key) {
        return props.containsKey(key);
    }
    
    public int size() {
        return props.size();
    }

    /**
     * @param key
     * @return the value as a string, or null if the property is not found.
     */
    public String getProperty(String key) {
        Value val = props.get(key);
        if (val == null) {
            return null;
        }
        return val.getValue();
    }
    
    public String getProperty(String key, String defaultValue) {
        Value val = props.get(key);
        if (val == null) {
            return defaultValue;
        }
        return val.getValue();
    }
    
    public String get(String key) {
        return getProperty(key);
    }
}
