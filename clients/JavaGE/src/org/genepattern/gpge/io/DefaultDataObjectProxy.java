/*
 * DefaultDataObjectProxy.java
 *
 * Created on February 19, 2003, 1:25 PM
 */

package org.genepattern.gpge.io;

import java.io.IOException;

import org.genepattern.data.AbstractObject;
import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.io.SummaryInfo;


/**
 * Default implementation of the DataObjectProxy interface.
 * This class is a Proxy for a DataObject.  It can return the summary information
 * about the object.  If getDataObject() is called the actual DataObject is
 * loaded from the DataSource.
 *
 * @author  kohm
 */
public class DefaultDataObjectProxy extends AbstractObject implements DataObjectProxy {
    
    /** Creates a new instance of DefaultDataObjectProxy */
    public DefaultDataObjectProxy(final String name, final DataSource source, final SummaryInfo summary ) {
        super(name);
        this.source = source;
        this.summary = summary;
    }
    
    /** gets the DataObjector instance from its' source
     * @exception IOException if the data object cannot be retrived from its' source
     * @exception IllegalArgumentException if the data source no longer has the DataObject
     */
    public DataObjector getDataObject() throws java.io.IOException, java.text.ParseException, IllegalArgumentException {
        if( data_object == null )
            data_object = source.getDataObject(this);
        return data_object;
    }
     
    /** gets the Summary information about the object  */
    public SummaryInfo getSummaryInfo() {
        return summary;
    }
    /** returns a DataModel that defines the type of model this implementation represents  */
    public DataModel getDataModel() {
        return summary.getDataModel(); 
    }
    /** gets the data source's description */
    public String getDataSourceDescription() {
        return source.getDescription(this);
    }
    /** returns the DataSource  */
    public DataSource getDataSource() {
        return source;
    }
    // methods that well behaving object should override
    
    /** this is a reminder that data objects must override equals(Object)  */
    public boolean equals(Object obj) {
        if( !(obj instanceof DefaultDataObjectProxy) )
            return false;
        final DefaultDataObjectProxy other = (DefaultDataObjectProxy)obj;
        return ( getName().equals(other.getName()) && getDataModel().equals(other.getDataModel())
            && getSummaryInfo().equals(other.getSummaryInfo()) );
    }
    /** this is a reminer that classes that override equals must also
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
     *
     */
    public int hashCode() {
        if( hash_code == 0 ) {
            int result = 17;
            result *= 37 + getName().hashCode();
            result *= 37 + getSummaryInfo().hashCode();
            if( isMutable() )
                return result;
            else
                this.hash_code = result;
        }
        return this.hash_code;
    }
    
    /** this is a reminder that data objects must override toString()  */
    public String toString() {
        return name_ref;
    }
    
    // fields
    /** the hash code */
    private int hash_code = 0;
    /** the data source - where to get the DataObject */
    private final DataSource source;
    /** the data object's summary information */
    private final SummaryInfo summary;
    /** the data object or null if it hasn't been retrieved yet */
    private DataObjector data_object; // fixme this should be a weak reference
}
