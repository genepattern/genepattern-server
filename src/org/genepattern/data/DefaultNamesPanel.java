/*
 * Panel.java
 *
 * Created on November 13, 2002, 2:32 AM
 */

package org.genepattern.data;

import java.util.Arrays;
import java.util.List;

import org.genepattern.data.annotation.AnnotationFactory;
import org.genepattern.util.ArrayUtils;

/**
 * This represents either a row or a column.
 * Specifically the names and descriptions (annotations).
 *
 * @author  kohm
 */
public class DefaultNamesPanel extends AbstractNamesPanel implements Cloneable{
    
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
//        encoder.setPersistenceDelegate(DefaultNamesPanel.class,
//                        new java.beans.DefaultPersistenceDelegate(prop_names));       
//    }
    
    /** Creates a new instance of NamesPanel */
    public DefaultNamesPanel(final String[] names, final AnnotationFactory annotation) {
        super(annotation, false/*not mutable*/);
        if(names == null)
            throw new NullPointerException("The names array cannot be null!");
        this.names = names;
    }
    /** Creates a new instance of NamesPanel from another */
    public DefaultNamesPanel(final AbstractNamesPanel panel) {
        super((AnnotationFactory)panel.internalGetAnnotationFactory().clone(),  false);
        final String[] names = panel.getNames();
        this.names = (panel.isMutable()) ? (String[])names.clone() : names;
    }
//    /** Creates a new instance of NamesPanel that is a subset of the other one */
//    public NamesPanel(final NamesPanel other, final int[] indices) {
//        final int cnt = indices.length;
//        names  = new String[cnt];
//        for(int i = 0; i < cnt; i++) {
//            names[i] = other.getName(indices[i]);
//        }
//        annotation = (AnnotationFactory)other.getAnnotationFactory().clone();
//    }
    
    /** Creates a new instance of NamesPanel that is a subset of this one */
    public NamesPanel createSubSet(final int[] indices) {
        final int cnt = indices.length;
        final String[] new_names  = new String[cnt];
        for(int i = 0; i < cnt; i++) {
            new_names[i] = names[indices[i]];
        }
        return new DefaultNamesPanel(new_names, (AnnotationFactory)internalGetAnnotationFactory().clone());
    }
    
    // implemented abstract methods
    
    /** the number of names */
    public final int getCount() {
        return names.length;
    }
    public String getName(int rown) {
        //checkInit();
        return names[rown];
    }

    public List getListOfNames() {
        //checkInit();
        return Arrays.asList(names);
    }

    public int getIndex(final String name) {
        //checkInit();
        return ArrayUtils.getIndex(name, names);
    }

    public String[] getNames() {
        //checkInit();
        return (String[])names.clone();
    }

    // Cloneable method signature
    
    /** creates a shallow clone of this object */
    public Object clone() {
        return new DefaultNamesPanel((String[])names.clone(),
                              (AnnotationFactory)internalGetAnnotationFactory().clone());
    }
    
    // fields 
    
    /** the names */
    private String[] names;
}
