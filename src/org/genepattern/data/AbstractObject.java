

package org.genepattern.data;
/*
 * WHITEHEAD INSTITUTE
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2001 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 */
import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.genepattern.util.XLogger;



/**
 * Basic class for data structures. Adds a few abstract methods
 *
 * Object is auto assigned a unique programmatic name on construction.
 * The alg used for this is edu.mit.genome.util.UUID
 * and the object name is guranteed to be uniquie in time and space.
 * This is final so, program name of an object never changes.
 * The display name is settable/changeable.
 *
 *
 * Sub-classes must implement:
 * always dynamically (or at least transiently) make the summary so as not to
 * affect any serialization.
 *
 * IMPORTANT see item 54 in block for the initialization stuff.
 *
 * @author Aravind Subramanian
 * @version 1.0
 */

// theres a bit of redundance here - Dataset for instance extends AbstractObject and implements
// ProjectObjetct which in turn extends PersistentObject
public abstract class AbstractObject implements DataObjector {//implements PersistentObject {
    /** Class constructor.
     * Makes a new object with specified name and not mutable.
     * @param name the name of this
     */
    protected AbstractObject(final String name) {
        this(name, false);
    }
    /** Class constructor.
     * Makes a new object with specified name and mutability.
     * @param name the name of this
     * @param ismutable is this a mutable data object
     */
    protected AbstractObject(final String name, final boolean ismutable) {
        this(Id.createId(), name, ismutable);
    }
    /** Class constructor.
     * Makes a new object with specified name.
     * @param id the id
     * @param name the name of this
     * @param ismutable is this a mutable data object
     */
    protected AbstractObject(final Id id, final String name, final boolean ismutable) {
//        if (inited) throw new IllegalStateException("Already initialized. disp name " + name);

        if (id == null) throw new NullPointerException("Parameter id cannot be null");
        if (name == null) throw new NullPointerException("Parameter display name cannot be null");

        this.name = name;
        this.name_ref = fixName(name);
        this.id = id;
        this.is_mutable = ismutable;
        this.log = XLogger.getLogger(this.getClass());
        this.type_objectsetter = new HashMap();
//        this.inited = true;
    }
    
    /** Modifies a name if it contains strange characters.
     * @param name the name of the data object
     * @return String
     */
    public static final String fixName(final String name) {
        final int last = getLastBadCharIndex(name);
      //  System.out.println("name="+name);
        if( last > 0 ) {
            final String fixed = name.substring(last + 1);
          //  System.out.println("Fixed name: \""+fixed+"\"");
            return fixed;
        }
        return name;
        
    }
    /** returns the index of the last bad character (not suitable for File names etc)
     * or -1 if none
     * @param name a name
     * @return String
     */
    public static final int getLastBadCharIndex(final String name) {
        final char[] bad_ones = new char[] {'\\', '/', ':', ';'};
        int last = -1;
        final int limit = bad_ones.length;
        for(int i = 0; i < limit; i++) {
            last = Math.max(last, name.lastIndexOf(bad_ones[i]));
        }
        return last;
    }
    
    /** deserializes the object from the FeaturesetProperties attributes
     * @param prop the featureset properties object
     */
    protected final void deserializeFromProperties(final FeaturesetProperties prop) {
        final Map attrs = prop.getAttributes();
        final Class clss = this.getClass();
        final java.lang.reflect.Field fields[] = clss.getDeclaredFields();//.getFields();
        final int num = fields.length;
        try {
            System.out.println(clss.getName()+" has "+num+" fields:");
            for (int i = 0; i < num; i++) {
                final Field field = fields[i];
                final String name = field.getName();
                final Object value = getValue(attrs, name);//attrs.get(name);
                //System.out.println("setting '"+name+"' to '"+value+"'");
                if( value == null )
                    continue; // try the next field
                field.setAccessible(true);
                final Class type = field.getType();
                
                if( type.isPrimitive() && value instanceof String ) {
                    final AbstractPrimitiveParser parser = (AbstractPrimitiveParser)TYPE_PRIMITIVE_PARSER.get(type);
                    if( parser == null )
                        throw new UnsupportedOperationException("primitive type parser for "+type+" is not yet implemented!");
                    parser.parseToVar(field, (String)value, this);
                } else {
                    final AbstractObjectParser ob_parser = (AbstractObjectParser)type_objectsetter.get(type);
                    if( ob_parser == null ) {
                        if( type == value.getClass() )
                            field.set(this, value);
                        else
                            parseToVar(type, field, value);
                    } else {
                        ob_parser.setVar(field, value, this);
                    }
                    
                }
            }
            
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException("Exception occured while accessing the fields\n"+ex);
        }
    }
    /** gets the value by trying the various possible forms of the name as a key*/
    private Object getValue(final Map attrs, final String name) {
        Object value = attrs.get(name);
        if( value == null) {
            value = attrs.get(name + '=');
            if( value == null )
                value = attrs.get(name + ':');
        }
        return value;
    }
    /** parses the String value to the type specified */
    private void parseToVar(final Class type, final Field field, final Object value) throws IllegalAccessException{
        if( value instanceof String ) {
        //if( value.getClass() == String.class ) {
            final AbstractPrimitiveParser parser = (AbstractPrimitiveParser)TYPE_PRIMITIVE_ARRAY_PARSER.get(type);
            if( parser == null )
                throw new UnsupportedOperationException("primitive array type parser for "+type+" is not yet implemented!");
            parser.parseToVar(field, (String)value, this);
        } else if(value instanceof String[]) {
            final AbstractArrayParser parser = (AbstractArrayParser)TYPE_PRIMITIVE_ARRAY_PARSER.get(type);
            if( parser == null )
                throw new UnsupportedOperationException("primitive array type parser for "+type+" is not yet implemented!");
            parser.parseToVar(field, (String[])value, this);
        } else {
            System.out.println("Not implemented yet: converting a "
                +value.getClass()+" to a type="+type+" field="+field+" value="+value);
            //throw new ClassCastException("Not implemented yet: converting a "
            //    +value.getClass()+" to a "+type);
        }
    }
    /** Programmatic name of this object
     * @return Programmatic name of this object
     */
    public Id getId() {
        //checkInit(); // uncloometn me if the ko serialization idea didnt oan out IMP IMp IMp
        return id;
    }

//    /**
//     * Must be called by all public instance methods of this abstract class.
//     * @throws IllegalStateException if the class has not been initialized properly
//     */
//    private void checkInit() {
//        if (! inited) throw new IllegalStateException("Uninitialized: " + name);
//    }

    /** gets the name of this data object
     * @return Display name of this object
     */
    public String getName() {
        //checkInit(); // uncloometn me if the ko serialization idea didnt oan out IMP IMp IMp
        return name;
    }

    /** returns the summary of this object
     * @return String
     */    
    public String getSummary() {
        if (log.isDebugEnabled()) log.debug("Using summary from AbstractObject");
        //checkInit(); // intentionally dont call! some summary helps when debugging
        StringBuffer sb = new StringBuffer("-------------------------------\n");
        sb.append("Id=").append(getId()).append("\n");
        sb.append("Name=").append(getName()).append("\n");
        sb.append("--------------------------------");
        return sb.toString();
    }

    /**
     * Convenience method (for developers) to pretty print out the
     * contents of the IData.
     * Mostly for use when debugging.<br>
     * Sub-classes must override for custom output.
     */
    public void printf() {
//        log.info(getSummary());
    }

    public String getMetaLine() {
        return "";
    }

    // subclasses that have meta data should override
    public boolean initMetaData(String metaline) {
        return false;
    }
    /** returns false if this is an object that cannot have it's internal state changed
     * @return true if this is a mutable object
     */
    public final boolean isMutable() { 
        return is_mutable;
    }
    /** helper method for subclasses caclulating hash code
     *
     * usage: result = 37 * result + calcObjsHash(some_object);
     * @param obj an object
     * @return int
     */
    protected static final int calcObjsHash(final Object obj) {
        if( obj == null )
            return 0;
        if( obj.getClass().isArray() )
            return calcArrayHash(obj);
        return obj.hashCode();
    }
    /** helper method for subclasses caclulating hash code with arrays of int
     *
     * usage: result = 37 * result + calcObjsHash(int_array);
     * @param array array of objects
     * @return int
     */
    protected static final int calcObjsHash(final Object[] array) {
        if( array == null )
            return 0;
        int result = 0;
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            final Object obj = array[i];
            if( obj.getClass().isArray() )
                result += calcArrayHash(obj);
            else
                result += array[i].hashCode();
        }
        return result;
    }
    /**
     * helper method for subclasses caclulating hash code of arrays
     * 
     * usage: result = 37 * result + calcObjsHash(int_array);
     */
    private static final int calcArrayHash(final Object array) {
        if( array == null )
            return 0;
        final Class clss = array.getClass();
        if( !clss.isArray() )
            throw new IllegalArgumentException("Not an array: "+clss);
        
        
        final int len = Array.getLength(array);
        if( len == 0 )
            return 0;
        
        if(array instanceof byte[] )
            return calcObjsHash((byte[])array);
        else if( array instanceof boolean [])
            return calcObjsHash((boolean [])array);
        else if( array instanceof char[])
            return calcObjsHash((char[])array);
        else if( array instanceof short[])
            return calcObjsHash((short[])array);
        else if( array instanceof int[])
            return calcObjsHash((int[])array);
        else if( array instanceof long[])
            return calcObjsHash((long[])array);
        else if( array instanceof float[])
            return calcObjsHash((float[])array);
        else if( array instanceof double[])
            return calcObjsHash((double[])array);
        else if( array instanceof Object[])
            return calcObjsHash((Object[])array);
        else 
            throw new IllegalArgumentException("Unknown array: "+array.getClass());
    }
    /** helper method for subclasses caclulating hash code with arrays of boolean
     *
     * usage: result = 37 * result + calcObjsHash(int_array);
     * @param array an array of booleans
     * @return int
     */
    protected static final int calcObjsHash(final boolean[] array) {
        if( array == null )
            return 0;
        int result = 0;
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            result += (array[i]) ? 1 : 0;
        }
        return result;
    }
    /** helper method for subclasses caclulating hash code with arrays of byte
     *
     * usage: result = 37 * result + calcObjsHash(int_array);
     * @param array an array of bytes
     * @return int
     */
    protected static final int calcObjsHash(final byte[] array) {
        if( array == null )
            return 0;
        int result = 0;
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            result += array[i];
        }
        return result;
    }
    /** helper method for subclasses caclulating hash code with arrays of char
     *
     * usage: result = 37 * result + calcObjsHash(int_array);
     * @param array an array of characters
     * @return int
     */
    protected static final int calcObjsHash(final char[] array) {
        if( array == null )
            return 0;
        int result = 0;
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            result += array[i];
        }
        return result;
    }
    /** helper method for subclasses caclulating hash code with arrays of short
     *
     * usage: result = 37 * result + calcObjsHash(int_array);
     * @param array an array of shorts
     * @return int
     */
    protected static final int calcObjsHash(final short[] array) {
        if( array == null )
            return 0;
        int result = 0;
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            result += array[i];
        }
        return result;
    }
    /** helper method for subclasses caclulating hash code with arrays of int
     *
     * usage: result = 37 * result + calcObjsHash(int_array);
     * @param array an array of ints
     * @return int
     */
    protected static final int calcObjsHash(final int[] array) {
        if( array == null )
            return 0;
        int result = 0;
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            result += array[i];
        }
        return result;
    }
    /** helper method for subclasses caclulating hash code with arrays of loong
     *
     * usage: result = 37 * result + calcObjsHash(int_array);
     * @param array an array of longs
     * @return int
     */
    protected static final int calcObjsHash(final long[] array) {
        if( array == null )
            return 0;
        int result = 0;
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            final long l = array[i];
            result += (int)(l ^ (l >>> 32));
        }
        return result;
    }
    /** helper method for subclasses caclulating hash code with arrays of float
     *
     * usage: result = 37 * result + calcObjsHash(int_array);
     * @param array an array of floats
     * @return int
     */
    protected static final int calcObjsHash(final float[] array) {
        if( array == null )
            return 0;
        int result = 0;
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            result += Float.floatToIntBits(array[i]);
        }
        return result;
    }
    /** helper method for subclasses caclulating hash code with arrays of double
     *
     * usage: result = 37 * result + calcObjsHash(int_array);
     * @param array an array of doubles
     * @return int
     */
    protected static final int calcObjsHash(final double[] array) {
        if( array == null )
            return 0;
        int result = 0;
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            final long l = Double.doubleToLongBits(array[i]);
            result += (int)(l ^ (l >>> 32));
        }
        return result;
    }
    // helper methods
    /** helper method that copies String arrays and creates one if needed
     * @param source the array to copy
     * @param array the array to copy to or null
     * @return String[]
     */
    public static final String[] arrayCopy(final String[] source, final String[] array) {
        final int len = source.length;
        final String[] destination = (array == null) ? new String[len] : array;
        System.arraycopy(source, 0, destination, 0, len);
        return destination;
    }

    // abstract methods

    /** this is a reminder that data objects must override toString()
     * @return String
     */
    abstract public String toString();
    /** this is a reminder that data objects must override equals(Object)
     * @param obj the other object
     * @return true if these two objects are equal
     */
    abstract public boolean equals(Object obj);
    /** this is a reminder that classes that override equals must also
     * create a working hash algorithm.
     * for example:
     *
     * given:
     * boolean b
     *  compute (b ? 0 : 1)
     * byte, char, short, or int i
     *  compute (int)i
     * long l
     *  compute (int)(l ^ (l >>> 32))
     * float f
     *  compute Float.floatToIntBits(f)
     * double d
     *  compute Double.doubleToLongBits(d) then compute as long
     *
     * Object just get it's hash or if null then 0
     *
     * Arrays compute for each element
     *
     * i.e.:
     * int result = 17; // prime number
     * result = 37 * result + (int)character;
     * result = 37 * result + Float.floatToIntBits(f);
     * etc..
     * return result;
     * @return int
     */
    abstract public int hashCode();
    
    //Fields
    /** the type to primitive parser/setter Map */
    protected static final Map TYPE_PRIMITIVE_PARSER;
    /** the type to primitive array parser/setter Map */
    protected static final Map TYPE_PRIMITIVE_ARRAY_PARSER;
    /** the type to object setter Map */
    protected final Map type_objectsetter;
    /** Name of this object - can change */
    private String name;
    /** the toString rep of this */
    protected String name_ref;
    /** Programmatic id of this object - invariant */
    private final Id id;
    /** is this instance nutable */
    private final boolean is_mutable;
    /** For logging support */
    protected transient XLogger log;

//    private boolean inited;
    /** static initializer */
    static {
        //create the instances of the AbstractPrimitiveParser objects
        final Map map = new HashMap();
        
        Class clss = Integer.TYPE;
        AbstractPrimitiveParser parser = new AbstractPrimitiveParser() {
            protected void parseToVar(final Field field, final String value, final Object owner) throws IllegalAccessException{
		    field.setInt(owner, Integer.parseInt(value.trim()));
		}
        };
        map.put(clss, parser);
        
        clss = Double.TYPE;
        parser = new AbstractPrimitiveParser() {
            protected void parseToVar(final Field field, final String value, final Object owner) throws IllegalAccessException{
                field.setDouble(owner, Double.parseDouble(value));
            }
        };
        map.put(clss, parser);
        
        clss = Float.TYPE;
        parser = new AbstractPrimitiveParser() {
            protected void parseToVar(final Field field, final String value, final Object owner) throws IllegalAccessException{
                field.setFloat(owner, Float.parseFloat(value));
            }
        };
        map.put(clss, parser);
        
        clss = Boolean.TYPE;
        parser = new AbstractPrimitiveParser() {
            protected void parseToVar(final Field field, final String value, final Object owner) throws IllegalAccessException{
                field.setBoolean(owner, Boolean.getBoolean(value));
            }
        };
        map.put(clss, parser);
        
        clss = Character.TYPE;
        parser = new AbstractPrimitiveParser() {
            protected void parseToVar(final Field field, final String value, final Object owner) throws IllegalAccessException{
                if(value.length() == 1)
                    field.setChar(owner, (value.charAt(0)));
                else {
                    throw new IllegalArgumentException("Cannot convert a multi character String \""
                        +value+"\" to a single character!");
                }
            }
        };
        map.put(clss, parser);
        
        clss = Byte.TYPE;
        parser = new AbstractPrimitiveParser() {
            protected void parseToVar(final Field field, final String value, final Object owner) throws IllegalAccessException{
                field.setByte(owner, Byte.parseByte(value));
            }
        };
        map.put(clss, parser);
        
        clss = Short.TYPE;
        parser = new AbstractPrimitiveParser() {
            protected void parseToVar(final Field field, final String value, final Object owner) throws IllegalAccessException{
                field.setShort(owner, Short.parseShort(value));
            }
        };
        map.put(clss, parser);
        
        clss = Long.TYPE;
        parser = new AbstractPrimitiveParser() {
            protected void parseToVar(final Field field, final String value, final Object owner) throws IllegalAccessException{
                field.setLong(owner, Long.parseLong(value));
            }
        };
        map.put(clss, parser);
        
        TYPE_PRIMITIVE_PARSER = java.util.Collections.unmodifiableMap(map);
        // end primitive parser/setter
        
        // start parser setters for primitive arrays
        
        final Map clss_array = new HashMap();
        //AbstractObjectParser obj_parser;
        
        clss = int[].class;
        parser = new AbstractArrayParser() {
            /** implementations should create a primitive array of the specified size */
            protected Object createArray(final int size) {
                return new int[size];
            }
            /** implementations should parse the value to a number and set the
             * value at the indicated index
             */
            protected void setValue(final String token, final Object array, final int i) {
                ((int[])array)[i] = Integer.parseInt(token.trim());
            }
        };
        clss_array.put(clss, parser);
        
        clss = float[].class;
        parser = new AbstractArrayParser() {
            /** implementations should create a primitive array of the specified size */
            protected Object createArray(final int size) {
                return new float [size];
            }
            /** implementations should parse the value to a number and set the
             * value at the indicated index
             */
            protected void setValue(final String token, final Object array, final int i) {
                ((float [])array)[i] = Float.parseFloat(token.trim());
            }
        };
        clss_array.put(clss, parser);
        
        clss = double[].class;
        parser = new AbstractArrayParser() {
            /** implementations should create a primitive array of the specified size */
            protected Object createArray(final int size) {
                return new double[size];
            }
            /** implementations should parse the value to a number and set the
             * value at the indicated index
             */
            protected void setValue(final String token, final Object array, final int i) {
                ((double[])array)[i] = Double.parseDouble(token.trim());
            }
        };
        clss_array.put(clss, parser);
        //FIXME there are more primitive arrays to implement parsers for
//        clss = int[].class;
//        parser = new AbstractArrayParser() {
//            /** implementations should create a primitive array of the specified size */
//            protected Object createArray(final int size) {
//                return new int[size];
//            }
//            /** implementations should parse the value to a number and set the
//             * value at the indicated index
//             */
//            protected void setValue(final String token, final Object array, final int i) {
//                ((int[])array)[i] = Integer.parseInt(token.trim());
//            }
//        };
//        clss_array.put(clss, parser);
        
        TYPE_PRIMITIVE_ARRAY_PARSER = java.util.Collections.unmodifiableMap(clss_array);
        // end primitive array parsers 
        
    }

    // I N N E R   C L A S S E S
    /** Defines a method to parse a string to a value and set the variable  */
    abstract static class AbstractPrimitiveParser {
        abstract protected void parseToVar(final Field field, final String string_value, final Object owner) throws IllegalAccessException;
        //abstract protected void parseToVar(final Field field, final String[] string_value, final Object owner) throws IllegalAccessException;
    }
    /** Defines a method to set field to a value */
    protected abstract static class AbstractObjectParser {
        /** sets the field to the specified value
         * @param field the field to set
         * @param value the value to set the field with
         * @param owner the object/instance of the field
         * @throws IllegalAccessException if there is a problem accessing the specified field
         */        
        abstract protected void setVar(final Field field, final Object value, final Object owner) throws IllegalAccessException;
    }
    /** Defines a method to parse a string to an array of values and set the variable
     * to the array
     */
    protected abstract static class AbstractArrayParser extends AbstractPrimitiveParser {
        /** Parses the string value into its' array elements and sets the field
         * to the resulting array.
         * @param field the variable to set
         * @param value the text that represents an array of values
         * @param owner the instance whos field should be set
         * @throws IllegalAccessException if it is not possible to set the field
         */        
        protected void parseToVar(final Field field, final String value, final Object owner) throws IllegalAccessException{
            final StringTokenizer tok = new StringTokenizer(value);
            final int cnt = tok.countTokens();
            int i = 0;
            final Object array = createArray(cnt);
            try {
                for( ; tok.hasMoreTokens(); i++) {
                    setValue(tok.nextToken(), array, i);
                }
            } catch (NumberFormatException ex) {
                throw new NumberFormatException("While parsing values for "
                    +field.getName()+", token "+i+" is not a number!\n"+ex);
            }
            field.set(owner, array);
        }
        /** Parses the individual string values into their true types and populates the 
         * array elements and sets the field
         * to the resulting array.
         * @param field the variable to set
         * @param string_values the array of strings that represents an array
         *  of possibly other values
         * @param owner the instance whos field should be set
         * @throws IllegalAccessException if it is not possible to set the field
         */
        protected void parseToVar(final Field field, final String[] string_values, final Object owner) throws IllegalAccessException{
            final int cnt = string_values.length;
            int i = 0;
            final Object array = createArray(cnt);
            try {
                for(; i < cnt; i++) {
                    setValue(string_values[i], array, i);
                }
            } catch (NumberFormatException ex) {
                throw new NumberFormatException("While parsing values for "
                    +field.getName()+", token "+i+" is not a number!\n"+ex);
            }
            field.set(owner, array);
        }
        /** implementations should create a primitive array of the specified size
         * @param size the size of the array to create
         * @return Object, the created array
         */
        abstract protected Object createArray(final int size);
        /** implementations should parse the value to some type (a number, for example)
         *  and set the indicated index in the array
         * @param token the text to parse to some other type
         * @param array the array to set the element at a particular index
         * @param i the index in the array
         */
        abstract protected void setValue(final String token, final Object array, final int i);
    }
    
} // End AbstractObject




  /** Short summary of this object */
    //private String fSumm = "AbstractObject_summ_not_set";
    /**
     * @return a summary of the contents of this object.
     */
    //public String getSummary() {
       // return fSumm;
   // }

    /**
     * Sub-classes must call to set the data summary.
     */
    //protected void setSummary(String summ) {
       // fSumm = summ;
    //}
