/*
 * DataObjectWrapper.java
 *
 * Created on July 8, 2003, 4:28 PM
 */

package org.genepattern.io;

import org.genepattern.data.AbstractObject;
import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;

/**
 * Wraps any object to look like a real DataObject
 * @author  kohm
 */
public class DataObjectWrapper implements DataObjector {
    
    /** Creates a new instance of DataObjectWrapper */
    public DataObjectWrapper(final String name, final Object object) {
        this.name = AbstractObject.fixName(name);
        this.object = object;
        this.data_model = DataModel.findModel(object.getClass().getName());
    }
    
    /** returns the Object */
    public Object getObject() {
        return object;
    }
    
    /** returns a DataModel that defines the type of model this implementation represents  */
    public DataModel getDataModel() {
        return data_model;
    }    
    
    /** returns the name of this data object  */
    public String getName() {
        return name;
    }
    
    /** returns false if this is an object that cannot have it's internal state changed  */
    public boolean isMutable() {
        throw new UnsupportedOperationException();
    }
    /** this is a reminder that data objects must override toString() */
    public String toString() {
        return object.toString();
    }
    /** this is a reminder that data objects must override equals(Object) */
    public boolean equals(Object obj) {
        if( obj instanceof DataObjectWrapper )
            return object.equals(((DataObjectWrapper)obj).object);
        return false;
    }
    /**
     * this is a reminer that classes that override equals must also 
     * create a working hash algorithm
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
     */
    public int hashCode() {
        return object.hashCode();
    }
    
    // fields
    /** the object */
    private final Object object;
    /** the name of this object */
    private final String name;
    /** the DataModel this object represents */
    private DataModel data_model;
}
