/*
 * Panel.java
 *
 * Created on November 13, 2002, 2:32 AM
 */

package org.genepattern.data;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.data.annotation.AnnotationFactory;
import org.genepattern.data.annotation.Annotator;
import org.genepattern.util.ArrayUtils;

/**
 * This represents either a row or a column.
 * Specifically the names and descriptions (annotations).
 *
 * @author  kohm
 */
public class NamesPanelMutable extends AbstractNamesPanel implements Cloneable{
    
//    /** static initilizer */
//    static {
//        /* Thoughts:
//         * This class can be persisted through XMLEncoder because the property
//         * names, prop_names, match the variables.  Also the order of the
//         * variables (represented by the Strings) match one of the public constructors
//         *    -- kwo
//         */
//        //create instance of abstract class
//        final java.beans.Encoder encoder = new java.beans.Encoder() {};
//        final String[] prop_names = new String [] {"names", "annotation"};
//        // defines how to create an immutable bean - via its' constructor
//        encoder.setPersistenceDelegate(NamesPanelMutable.class,
//                        new java.beans.DefaultPersistenceDelegate(prop_names));       
//    }
    
    /** Creates a new instance of NamesPanel */
    public NamesPanelMutable(final List names, final AnnotationFactory annotation) {
        super(annotation, true/*is mutable*/);
//        if(names == null)
//            throw new NullPointerException("The names List cannot be null!");
        this.names = names;
    }
    /** Creates a new instance of NamesPanel from the AbstractNamesPanel*/
    public NamesPanelMutable(final AbstractNamesPanel panel) {
        super((AnnotationFactory)panel.internalGetAnnotationFactory().clone(), true);
        this.names = new ArrayList(panel.getListOfNames());
    }
    /** Creates a new instance of NamesPanel that is a subset of this one */
    public NamesPanel createSubSet(final int[] indices) {
        final int cnt = indices.length;
        final ArrayList new_names  = new ArrayList(cnt);
        for(int i = 0; i < cnt; i++) {
            new_names.add(names.get(indices[i]));
        }
        return new NamesPanelMutable(new_names, (AnnotationFactory)internalGetAnnotationFactory().clone());
    }
    
    /** removes entries as specified in the array of indices */
    public final void removeEntries(final int[] inds) {
        final int[] indices = (int[])inds.clone();
        final int cnt = indices.length;
        java.util.Arrays.sort(indices);
        // start with the largest indicies first so the smaller ones are always valid
        for(int i = indices.length - 1; i >= 0; i--) { // rev. loop  
            this.names.remove(indices[i]); 
        }
    }
    /** removes entries as specified in the array of indices */
    public final void removeEntries(final IntRanges ranges) {
        // start with the largest indicies first so the smaller ones are always valid
        for(IntRanges.ReverseIntIterator iter = ranges.getReverseIntIterator(); iter.hasMore(); ) { 
            this.names.remove(iter.next()); 
        }
    }
    
    // implemented abstract methods
    
    /** the number of names */
    public final int getCount() {
        return names.size();
    }
    public String getName(int rown) {
        //checkInit();
        return (String)names.get(rown);
    }
    /** returns a list of the names the original List */
    public List getListOfNames() {
        //checkInit();
        return names;
    }

    public int getIndex(final String name) {
        //checkInit();
        return names.indexOf(name);
    }
    /** gets the names as an array */
    public String[] getNames() {
        //checkInit();
        return (String[]) names.toArray(new String[getCount()]);
    }

    // Cloneable method signature
    
    /** creates a shallow clone of this object */
    public Object clone() {
        return new NamesPanelMutable((ArrayList)((ArrayList)names).clone(),
                              (AnnotationFactory)internalGetAnnotationFactory().clone());
    }
    
    // setter methods
    
    /** sets the List of names */
    public final void setNames(List names) {
        this.names = names;
    }
    /** adds a name to the list */
    public final void add(String name) {
        names.add(name);
    }
    
    
    // fields 
    
    /** the names */
    private List names;
}
