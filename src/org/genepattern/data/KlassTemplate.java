
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

package org.genepattern.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.genepattern.data.FloatVector;
import org.genepattern.util.ArrayUtils;
import org.genepattern.util.XLogger;

//import org.genepattern.io.DataFormat;
//import org.genepattern.io.DataStore;
//import org.genepattern.io.DataStoreException;
//import org.genepattern.io.Serializer;
//import edu.mit.genome.gp.node.TemplateNode;



/**
 * A Template Object.
 *
 * A Template is made up of a collection of TemplateItem's.
 * A TemplateItem can belong to one or more Klass, but must belong to at least one Klass.
 *
 * <bold> Implementation Notes </bold><br>
 * Different from genecluster Template - The class labels in this template can
 * be accessed through <code>getSept(i).getName()</code> rather than directly
 * through this class.
 *
 *
 * @author Michael Angelo
 * @author Aravind Subramanian (re-engineered for GenePattern)
 * @version %I%, %G%
 *
 */

public class KlassTemplate extends AbstractObject implements Template, Serializable {

//    /**
//     * Klass constructor.
//     * Creates a new empty Template.
//     */
//    public final MutableTemplate(String name) {
//        super(name);
//        klasses = new ArrayList();
//        items = new ArrayList();
//    }
    /**
     * Klass constructor.
     * Creates a new KlassTemplate where the ids values correspond to the labels.
     * The smallest id value is associated with the first klass which is created 
     * from the first element in the array of klass labels.  The second smallest 
     * value would be associated with the second klass created from the second 
     * klass label etc.
     * 
     */
    public KlassTemplate(final String name, final String[] klass_labels, final int[] ids) {
        this(name, klass_labels.length, ids.length, false);
        this.should_verify = true;
        this.is_numeric = true;
        final int num_sep = klass_labels.length, num_items = ids.length;
        
        for (int i = 0; i < num_sep; i++) {
            klasses.add(new Klass(klass_labels[i]));
        }
        final SampleLabel any = SampleLabel.ANY_SAMPLE;
        for (int i = 0; i < num_items; i++) {
            items.add(new Item(any, String.valueOf(ids[i]), i));
        }
        //this.assignItems(); cannot just assign items
        // find the unique ids in sorted order
        final int[] unique_ints = ArrayUtils.unique(ids);
        final int uniq_cnt = unique_ints.length;
        if(uniq_cnt != num_sep)
            throw new IllegalStateException("Size mismatch: num klasses="
                + num_sep + " uniq ids=" + num_items);
            
        // add the items to the Klass 
        // the index of the Klass that is the same index as the id value in
        // the unique_ints array
        for(int i = 0; i < num_items; i++) {
            final int id = ids[i];
            final int index = Arrays.binarySearch(unique_ints, id);
            ((Klass)klasses.get(index)).items.add(items.get(i));
        }
        //this.assignItems();
    }
    /** Constructs a new KlassTemplate */
    public KlassTemplate(final String name, final Klass[] klass_array, final Item[] item_array, final boolean should_verify) {
        this(name, Arrays.asList(checkKlasses(klass_array, item_array)), Arrays.asList(item_array));
        this.should_verify = should_verify;
        this.assignItems();
    }
    /** Constructs a new KlassTemplate from only the item_array*/
    public KlassTemplate(final String name, final Item[] item_array, final boolean should_verify) {
        this(name, Arrays.asList(createKlasses(item_array)), Arrays.asList(item_array));
        this.should_verify = should_verify;
        this.assignItems();
    }
    /**
     * Constructs a new KlassTemplate
     * Assumes that the sept List has Klass objects and 
     * The items List contains Item object
     * Does not call assignItems()
     */
    protected KlassTemplate(final String name, final List septs, final List items) {
        this(name, septs.size(), items.size(), false);
        this.klasses.addAll(septs);
        this.items.addAll(items);
    }
    /**
     * Helper constuctor
     * Does not call assignItems()
     */
    protected KlassTemplate(final String name, final int num_classes, final int num_items, final boolean mutable) {
        super(name);
//        this.name = name;
        klasses = new ArrayList(num_classes);
        items   = new ArrayList(num_items);
        this.mutable = mutable;
    }
    
//    public final java.lang.Class getNodeDelegate() {
//        return TemplateNode.class;
//    }

    /**
     * Label of the klass at specified location
     */
    public final String getKlassLabel(final int i) {
        return ((Klass)klasses.get(i)).getName();
    }

    /**
     * Does this Template hold TemplateItems with numeric Ids?
     */
    public final boolean isNumeric() {
        return is_numeric;
    }

    public final Template.Klass getKlass(final int i) {
        return (Template.Klass)klasses.get(i);
    }

    public final Template.Item getItem(final int i) {
        return (Template.Item)items.get(i);
    }

    public final int getItemCount() {
        return items.size();
    }

    public final int getKlassCount() {
        return klasses.size();
    }

    public final FloatVector toVector() {
        if (! isNumeric()) throw new IllegalArgumentException("Cant convert non-numeric Template to a FloatVector");
        FloatVector v = new FloatVector(this.getItemCount());
        for (int i=0; i < getItemCount(); i++) {
            v.setElement(i, getItem(i).floatValue());
        }
        return v;
    }

     /**
     * it is possible to split
     * not sure if needed??
     */
     /*
    public final static FloatVector[] asVectors(Template template) {
        if (! template.isNumeric()) throw new IllegalArgumentException("Cannot convert a non-numeric Template to vectors");

        FloatVector[] vectors = new FloatVector[template.getKlassCount()];
        for (int i =0; i < template.getKlassCount(); i++) {
            Template.Klass cl = (Template.Klass)template.getKlass(i);
            vectors[i] = new FloatVector(cl.getSize());
            for (int j=0; j < cl.getSize(); j++) {
                vectors[i].setElement(j, new Float(cl.getItem(j).toString()).floatValue());
            }
        }

        return vectors;
    }
    */

    /**
     * Only valid if there is a collection of Klass's associated with this Template.
     *
     * The method cycles (in order of addition) through all its member TemplateItems.
     * In the first progress run, a list of unique TemplateItem Id's.
     * is generated. The size of this list must correspond to the
     * size of the Klass collection.
     *
     * Each Id is then assigned to one Klass (again, in order).
     *
     * The member TemplateItems are then again cycled through, and assigned to
     * a Klass (based on the Id - Klass map produced in the previous run).
     *
     * Thus, the result, is that each Klass is now associated with a collection of TemplateItem's.
     *
     * The Klass does NOT have to be the same as the TemplateItems id.
     *
     */
    public final void assignItems() {
        if (klasses == null)
            throw new java.lang.IllegalStateException("Cannot call method: no assoc'd Septs");
        if (items == null)
            throw new java.lang.IllegalStateException("Cannot call method: no assoc'd Items");
        
        final int num_classes = klasses.size();
        // find all unique ids
        final ArrayList ids = new ArrayList(num_classes);
        final int num_items = getItemCount();
        for (int i=0; i < num_items; i++) {
            Template.Item item = (Template.Item)items.get(i);
            if (!ids.contains(item.getId()))
                ids.add(item.getId());
            item.pos = i;
        }
        
        final int num_ids = ids.size();
        if (should_verify && num_ids != num_classes) {
            System.err.println("Error: Size mismatch:\n"+this.getAsString("\n"));
            throw new IllegalStateException("Size mismatch: num klasses=" + num_classes + " uniq ids=" + num_ids);
        }

        // ok, all looks good
        final HashMap ht = new HashMap((num_ids * 4) / 3 + 4);
        for (int i=0; i < num_ids; i++) {
            ht.put(ids.get(i), klasses.get(i));
        }

        // finally, the actual assignment
         for (int i=0; i < num_items; i++) {
            final Template.Item item = (Template.Item)items.get(i);
            final Klass klass = (Klass)ht.get(item.getId());
            klass.items.add(item);
        }
        
        //check for numeric Item Ids
        if( !this.is_numeric ) {
            try {
                for (int i=0; i < num_items; i++) {
                    final Template.Item item = (Template.Item)items.get(i);
                    item.intValue();
                }
                this.is_numeric = true;
            } catch (NumberFormatException ex) {
                this.is_numeric = false;
            }
        }
    }

    /**
     * PersistentObject impl.
     */
//    public final DataFormat getExternalizedFormat() {
//        return DataFormat.SERIALIZED;
//    }

    public final boolean isRestorable() {
        return true;
    }

//    public final PersistentProxy load(DataStore store) throws DataStoreException {
//        Serializable ser = new Serializer().load(store);
//
//        if (ser instanceof Template) {
//            Template template = (Template)ser;
//            //return template;
//
//            return new PersistentProxy(template);
//        }
//        else throw new DataStoreException("DataStore does not contain a Template object. It contains: " + ser.getKlass());
//
//    }

    /**
     * PersistentObject impl.
     * Stored as serializable data.
     */
//    public final void save(DataStore store) throws DataStoreException {
//        new Serializer().save(this, store);
//    }

     /**
     * Convenience method for use when debugging.
     * Pretty prints out the content of this Template.
     */
    public final void printf() {
//        log.info(this.getSummary());
    }

    /**
     * @return summary info on this Template.
     * @see KlassTemplate#getAsString(String) to get this in Cls format.
     * @return String the summary information
     */
    public final String getSummary() {
        StringBuffer s = new StringBuffer();
        s.append("Template id=" + getId() + " name=" + getName() + "\n");
        s.append("Number of klasses=" + getKlassCount() + "\n");
        s.append("Number of items=" + getItemCount() + "\n");
        return s.toString();
    }

    /**
     *
     * Munge this Template object into a String. Just to keep things familiar
     * use the cls format.
     * Example:<br<
     * <pre>
     * 53 3 1
     * # Breast Bladder Renal
     * 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 2
     *</pre>
     *
     */
    public final String getAsString(final String line_seperator) {
        StringBuffer s = new StringBuffer();
        s.append(this.getItemCount());
        s.append(" ");
        s.append(this.getKlassCount());
        s.append(" ");
        s.append("1"); // dont know what this is?? but never seem anything other than 1
        s.append(line_seperator);
        s.append("#");
        for (int i=0; i < this.getKlassCount(); i++) {
            s.append(' ');
            s.append(this.getKlassLabel(i));
        }
        s.append(line_seperator);

        for (int i=0; i < this.getItemCount(); i++) {
            s.append(' ');
            s.append(this.getItem(i).getId());
        }
        s.append(line_seperator);

        return s.toString();

    }
    /** prints the contents */
    public final void dump() {
        System.out.println(getSummary());
        System.out.println("Klasses:");
        final int num_septs = klasses.size();
        for(int i = 0; i< num_septs; i++) {
            System.out.println(klasses.get(i));
        }
        System.out.println("Items:");
        final int num = items.size();
        for(int i = 0; i< num; i++) {
            System.out.println( ((Template.Item)items.get(i)).toString(this));
        }
    }

//    /** @maint must keep order in synch with writeObject */
//    private void readObject(ObjectInputStream os) throws IOException, ClassNotFoundException {
//        os.defaultReadObject();
//
//        //Manually deserialize and initialize superclass state
//        Id id = (Id)os.readObject();
//        String name = (String)os.readObject();
//        initialize(id, name);
//    }
//    /** @maint must keep order in synch with readObject */
//    private void writeObject(ObjectOutputStream s) throws IOException {
//        s.defaultWriteObject();
//        s.writeObject(getId());
//        s.writeObject(getName());
//    }
    public final String toString() {
        return getName();
    }

    /** gets the item's klasses' index (Needed in KlassEstimate Funcs)  */
    public final int getKlassIndexForItem(final int j) {
        final Item item = getItem(j);
        if( isNumeric() ) {
            try {
                return item.intValue();
            } catch (NumberFormatException ex) {
                // falls through to try the next loop
            }
        }
        // otherwise defaults to the slow way...
        final int limit = klasses.size();
        for(int i =  0; i < limit; i++) {
            if(((Template.Klass)klasses.get(i)).hasItem(item))
                return i;
        }
        throw new IllegalStateException("Could not find a class that contains item["+j+"] "+item);
        //return -1; // actually an error condition
    }
    
    /** returns an array of Item objects  */
    public Item[] getItems() {
        return (Item[])items.toArray(new Item[items.size()]);
    }
    /** returns an array of Klass objects  */
    public Klass[] getKlasses() {
        return (Klass[])klasses.toArray(new Klass[klasses.size()]);
    }
    /** returns a DataModel that defines the type of model this implementation represents */
    public org.genepattern.data.DataModel getDataModel() {
        return DATA_MODEL;
    }
    /** this is a reminder that data objects must override equals(Object) */
    public boolean equals(final Object obj) {
        if(obj == this)
            return true;
        if( !(obj instanceof Template) )
            return false;
        final Template temp = (Template)obj;
//        System.out.println("in KlassTemplate.equals():");
//        System.out.println("item counts "+getItemCount()+" == "+temp.getItemCount());
//        System.out.println("klass count "+getKlassCount()+" == "+temp.getKlassCount());
//        System.out.println("are the klasses equal "+Arrays.equals(getKlasses(), temp.getKlasses()));
//        System.out.println("are the items equal "+Arrays.equals(getItems(), temp.getItems()));
        return ( this.getItemCount() == temp.getItemCount() && 
        this.getKlassCount() == temp.getKlassCount() &&
        Arrays.equals(getKlasses(), temp.getKlasses()) &&
        Arrays.equals(getItems(), temp.getItems()) );
    }
    /**
     * this is a reminer that classes that override equals must also 
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
     */
    public int hashCode() {
        if( hashcode == 0 || isMutable() ){
            int result = 17;
            if (this.items == null)
                result = 37 * result + 0;
            else {
                final int icnt = items.size();
                for(int i =0; i< icnt; i++) {
                    result = 37 * result + items.get(i).hashCode();
                }
            }
            if (this.klasses == null)
                result = 37 * result + 0;
            else {
                final int kcnt = klasses.size();
                for(int i =0; i< kcnt; i++) {
                    result = 37 * result + klasses.get(i).hashCode();
                }
            }
            if(isMutable())
                return result;
            else
                hashcode = result;
        }
        return hashcode;
    }
    
    // utility methods 
    /** checks if the klasses array is null and creates one if needed */
    private static final Template.Klass[] checkKlasses(final Template.Klass[] klasses, final Template.Item[] items) {
        if(klasses == null)
            return createKlasses(items);
        return klasses;
    }
    /** creates Klass objects from the Item objects  */
    public static final Klass[] createKlasses(final Template.Item[] items) {
        final int items_cnt = items.length;
        boolean numeric = true;
        // make sure the returned Klass array is in the order the new Ids are found
        final ArrayList list = new ArrayList(items_cnt);
        final HashSet set = new HashSet(items_cnt * 4 / 3 + 1);
        for(int i = 0; i< items_cnt; i++) {
            final String id = items[i].getId();
            if( set.add(id) ) {
                list.add(id);
                if(numeric) {
                    try {
                        Float.parseFloat(id);
                    } catch (NumberFormatException ex) {
                        numeric = false;
                    }
                }
            }
        }
        //this.is_numeric = numeric;
        final int klass_cnt = list.size();
        final Klass[] klasses = new Klass[klass_cnt];
        if(numeric) {
            final String text = "class_";
            for(int i = 0; i < klass_cnt; i++) {
                klasses[i] = new Klass(text+list.get(i));
            }
        } else {
            for(int i = 0; i < klass_cnt; i++) {
                klasses[i] = new Klass((String)list.get(i));
            }
        }
        return klasses;
        //return (Klass[])list.toArray(new Klass[list.size()]);
    }
    
    //fields 
    /** the hash code value or 0 if not calculated */
    private int hashcode = 0;

    /** The klasses that Template.Item s belong to */
    protected ArrayList klasses;

    /** List of TemplateItems */
    protected ArrayList items;
    /** true if this DataObject is mutable */
    protected final boolean mutable;
    /** Flag for whether this Template holds numeric items or not */
    protected boolean is_numeric = false;
    
//    /** the name of this instance */
//    protected String name;

    /** Generated serialized version UID */
    private static final long serialVersionUID = 3328207228376112777L;

    protected static transient XLogger log = XLogger.getLogger(Template.class);
    /**
     * When assigning items should it verify that there are at least one
     * Item for every Klass
     */
    protected boolean should_verify = true;
} // End Template



