/*
 * Panel.java
 *
 * Created on November 13, 2002, 2:32 AM
 */

package org.genepattern.data;

import org.genepattern.data.annotation.AnnotationFactory;
import org.genepattern.data.annotation.Annotator;

/**
 * This represents either a row or a column.
 * Specifically the names and descriptions (annotations).
 *
 * @author  kohm
 */
public abstract class AbstractNamesPanel implements NamesPanel{
    
    /** Creates a new instance of NamesPanel */
    public AbstractNamesPanel(final AnnotationFactory annotation, final boolean mutable) {
        if(annotation == null)
            throw new NullPointerException("AnnotationFactory cannot be null!");
        this.annotation = annotation;
        this.is_mutable = mutable;
    }
    
    // abstract methods
    
    /** creates a clone of this instance */
    abstract public Object clone();
    /** the number of names */
    abstract public int getCount();
    /** gets the name at the specified index */
    abstract public String getName(final int index);
    /** gets the names as a List */
    abstract public java.util.List getListOfNames();
    /** gets the index of the name or -1 if not found */
    abstract public int getIndex(final String name);
    /** gets the names as an array */
    abstract public String[] getNames();
    /** Creates a new instance of NamesPanel that is a subset of this one  */
    abstract public NamesPanel createSubSet(int[] indices);

    // implemented methods
    
    /** gets the annotation or description at the specified index */
    public final String getAnnotation(final int index) {
        return getAnnotator().getDescription(getName(index));
    }
    
    /** gets the Annotator  */
    public final Annotator getAnnotator() {
        return annotation.getCurrent();
    }
    
    /** returns true if this object can have its' internal state changed through setters*/
    public final boolean isMutable() {
        return this.is_mutable;
    }
    /** returns a DataModel that defines the type of model this implementation represents */
    public org.genepattern.data.DataModel getDataModel() {
        return DATA_MODEL;
    }

    // internal use methods
    
    /** get the AnnotationFactory */
    protected final AnnotationFactory internalGetAnnotationFactory() {
//        if(!is_mutable)
//            throw new UnsupportedOperationException("Cannot access sensitive object from immutable class!");
        return annotation;
    }
    /** sets the Annotation factory */
    protected final void internalSetAnnotationFactory(final AnnotationFactory af) {
        if(!is_mutable)
            throw new UnsupportedOperationException("Cannot change an immutable object!");
        this.annotation = af;
    }
    /** returns the name */
    public String getName() {
        return toString();
    }
// overwritten Object methods
    /**
     * returns the name of this object
     */
    public String toString(Object obj) {
        return "[NamesPanel ("+getCount()+" elements) "+annotation+"]";
        //return getName();
    }
    /** determines if the value of this object is equal to another Dataset */
    public boolean equals(Object obj) {
        if( obj == this )
            return true;
        if( !(obj instanceof NamesPanel) )
            return false;
        final AbstractNamesPanel panel = (AbstractNamesPanel)obj;
        final int count = this.getCount();
        final String[] names = getNames();
        final String[] others = panel.getNames();
        if( count == panel.getCount() && names != null && others != null ) {
            for(int i = 0; i < count; i++) {
                final String name = names[i], other = others[i];
                if( !(name == null && other == null || (name != null && name.equals(other))) )
                    return false;
            }
            return true;
        }
        return false;
    }
    /**
     * calculates the hash code for a dataset based on the significant fields
     * This is only calculated once for immutable Datasets but has to be recaclulated
     * each time it is called for mutatable ones.  Could try to optimize this by only
     * recalculating the hascode after there was a change.  However,  this 
     * would not be fool-proof and would be somewhat more compicated thus will be
     * avoided...
     */
    public int hashCode() {
        final boolean mutable = isMutable();
        if( mutable || hashcode == 0 ) { // compute it
            final String[] names = getNames();
            
            int result = 17;
            if (names != null) {
                final int count = names.length;
                for(int i = 0; i < count; i++) {
                    result = 37 * result + names[i].hashCode();
                }
            } else 
                result = 37 * result + 0; // null names
            
            if( mutable ) // if mutable then recalculate each time ( i.e. don't set hashcode)
                return result;
            else // immutable object only need to calc this once!!
                hashcode = result;
        }
        return hashcode;
    }
    
    // fields
    /** the AnnotationFactory */
    private AnnotationFactory annotation;
    /** is this object immutable */
    private final boolean is_mutable;
    /** the hash code value or 0 if not calculated */
    private int hashcode = 0;
}
