package org.genepattern.server.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.base.Joiner;


public class Value {
    static public Value parse(final Object object) throws ConfigurationException {
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

        //special-case (added for storing repository details in config file), a map of values
        //e.g. repositoryDetails: { <actual url>: {}, <another url>: {}, ... }
        if (object instanceof Map<?,?>) {
            return new Value( (Map<?,?>) object );
        }

        throw new ConfigurationException("Illegal arg, object is not instanceof String or Collection<?>: '"+object.toString()+"'");
    }

    private boolean fromCollection=false;
    private List<String> values = new ArrayList<String>();
    private Map<?,?> mapValue=null;
    private Integer hashCode=null;

    public Value(final String value) {
        fromCollection=false;
        values.add(value);
    }

    public Value(final Collection<String> from) {
        fromCollection=true;
        if (from!=null) {
            values.addAll(from);
        }
    }

    public Value(final Map<?,?> from) {
        this.mapValue=from;
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

    /**
     * Helper method, to allow for more complicated values (other than String and List<String>).
     * Added to make it easier to configure details for module repositories.
     * 
     * @return
     */
    public boolean isMap() {
        return mapValue != null;
    }

    public Map<?,?> getMap() {
        return Collections.unmodifiableMap(mapValue);
    }
    
    public boolean equals(final Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Value rhs = (Value) obj;
        return new EqualsBuilder()
            .append(fromCollection, rhs.fromCollection)
            .append(values, rhs.values)
            .append(mapValue, rhs.mapValue)
            .isEquals();
    }
    
    public int hashCode() {
        if (hashCode != null) {
            return hashCode;
        }
        hashCode=new HashCodeBuilder()
            .append(values.toArray())
            .hashCode();
        return hashCode;
    }
    
    public String toString() {
        if (values==null) {
            return "";
        }
        if (values.size()==1) {
            return getValue();
        }
        return values.toString();
    }

    /**
     * Call join() with the default ' ' separator character.
     * @return
     */
    public String join() {
        return join(" ");
    }

    /**
     * Print the list of values as a {sep} separated String.
     * @param sep, the separator character, by default ' '.
     * @return
     */
    public String join(String sep) {
        if (sep==null) { sep = " "; }
        if (isMap()) {
            return joinMap(mapValue, sep, "=");
        }
        return join(values, sep);
    }
    
    public static String join(List<String> values, String sep) {
        if (values==null || values.size()==0) {
            return "";
        }
        else if (values.size()==0) {
            return values.get(0);
        }
        return Joiner.on(sep).join(values.toArray());
    }
    
    public static String joinMap(Map<?,?> map, String sep, String keyValueSep) {
        if (map==null || map.size()==0) {
            return "";
        }
        return Joiner.on(sep)
                .withKeyValueSeparator(keyValueSep)  // e.g. '='
                .join(map);
    }
}

