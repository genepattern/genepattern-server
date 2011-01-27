package org.genepattern.server.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 * Job configuration properties for a specific job, default values are loaded based on parsing the job_configuration.yaml file. 
 * The initial version of the API used a java.util.Properties object.
 * This type was created so that the config file can assign a String or a List<String>
 * to a given property value.
 * 
 * @author pcarr
 */
public class CommandProperties {
    public static Logger log = Logger.getLogger(CommandProperties.class);

    public static class Value {
        static public Value parse(Object object) throws ConfigurationException {
            if (object == null) {
              return new Value( (String) null);
            }
            if (object instanceof String) {
                return new Value( (String) object );
            }
            if (object instanceof Number) {
                return new Value(object.toString());
            }
            if (object instanceof Boolean) {
                return new Value(object.toString());
            }
            
            if (object instanceof Collection<?>) {
                List<String> s = new ArrayList<String>();
                for(Object item : ((Collection<?>) object)) {
                    if (item == null) {
                        s.add((String) item);
                    }
                    else if ((item instanceof String)) {
                        s.add((String) item);
                    }
                    else if ((item instanceof Number)) {
                        s.add(item.toString());
                    }
                    else if ((item instanceof Boolean)) {
                        s.add(item.toString());
                    }
                    else {
                        throw new ConfigurationException("Illegal arg, item in Collection<?> is not instanceof String: '"+item.toString()+"'");
                    }
                }
                return new Value( s );
            }
            throw new ConfigurationException("Illegal arg, object is not instanceof String or Collection<?>: '"+object.toString()+"'");
        }
        
        private List<String> values = new ArrayList<String>();
        
        public Value(String value) {
            values.add(value);
        }
        
        public Value(Collection<String> from) {
            values.addAll(from);
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
        
        public int getNumValues() {
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

    public CommandProperties(Properties from) {
        initFromProperties(from);
    }
    
    public CommandProperties(CommandProperties from) {
        props.putAll(from.props);
    }
    
    public void clear() {
        props.clear();
    }
    
    public void put(String key, String value) {
        props.put(key, new Value(value));
    }
    
    public void put(String key, Value value) {
        props.put(key, value);
    }
    
    public void putAll(CommandProperties from) {
        props.putAll(from.props);
    }
    
    public boolean containsKey(String key) {
        return props.containsKey(key);
    }
    
    public int size() {
        return props.size();
    }

    /**
     * Utility method for parsing properties as a boolean.
     * The current implementation uses Boolean.parseBoolean, 
     * which returns true iff the property is set and equalsIgnoreCase 'true'.
     * 
     * @param key
     * @return
     */
    public boolean getBooleanProperty(String key) {
        String val = getProperty(key);
        return Boolean.parseBoolean(val);
    }
    
    /**
     * Utility method for parsing a property as an Integer.
     * 
     * When a non integer value is set in the config file, the default value is returned.
     * Errors are logged, but exceptions are not thrown.
     * 
     * @param key
     * @param defaultValue
     * 
     * @return the int value for the property, or the default value, can return null.
     */
    public Integer getIntegerProperty(String key, Integer defaultValue) {
        String val = getProperty(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e) {
            log.error("Error parsing integer value for property, "+key+"="+val);
            return defaultValue;
        }
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
    
    public Value get(String key) {
        return props.get(key);
    }
    
    public Set<String> keySet() {
        return props.keySet();
    }
    
    public Properties toProperties() {
        return toProperties(true);
    }
    
    public Properties toProperties(boolean ignoreCollections) {
        Properties to = new Properties();
        for(Entry<String,Value> entry : props.entrySet()) {
            if (entry.getValue().getNumValues() == 1) {
                to.setProperty(entry.getKey(), entry.getValue().getValue());
            }
            else {
                if (!ignoreCollections) {
                    //TODO: double-check toString representation of a Value list
                    to.setProperty(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        return to;
    }
}
