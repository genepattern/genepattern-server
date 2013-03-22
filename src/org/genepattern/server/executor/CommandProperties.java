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
 * Properties for a specific job, default values are loaded based on parsing the config.yaml file. 
 * The initial version of the API used a java.util.Properties object.
 * This type was created so that the config file can assign a String *or* a List<String>
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
        
        private boolean fromCollection=false;
        private List<String> values = new ArrayList<String>();
        
        public Value(final String value) {
            fromCollection=false;
            values.add(value);
        }
        
        public Value(final Collection<String> from) {
            fromCollection=true;
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

        /**
         * Helper method, return true if this object was initialized from a collection 
         * of values, false if it was initialized from a single value. 
         * 
         * This was added to help with parsing the config file. For example,
         * <pre>
               param1: "1"
               param2: ["1"]
         
           param1 was initialized from a single value.
           param2 was initialized from an array of values.
         * </pre>
         * 
         * In both cases, numValues is 1. For param1, isCollection is false.
         * For param2, isCollection is true.
         * 
         * @return
         */
        public boolean isFromCollection() {
            return fromCollection;
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
    
    public Value get(String key, Value defaultValue) {
        if (props.containsKey(key)) {
            return props.get(key);
        }
        return defaultValue;
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
