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

import java.io.Serializable;
import java.util.ArrayList;

//import org.genepattern.io.DataFormat;
//import org.genepattern.io.DataStore;
//import org.genepattern.io.DataStoreException;
//import org.genepattern.io.Serializer;
//import edu.mit.genome.gp.node.TemplateNode;
//import edu.mit.genome.gp.*;

/**
 * A Template Object.
 * 
 * A Template is made up of a collection of TemplateItem's. A TemplateItem can
 * belong to one or more Klass, but must belong to at least one Klass.
 * 
 * <bold>Implementation Notes </bold> <br>
 * Different from genecluster CKlassAssignments - The klass labels in this
 * template can be accessed through <code>getSept(i).getName()</code> rather
 * than directly through this class.
 * 
 * 
 * @author Michael Angelo
 * @author Aravind Subramanian (re-engineered for GenePattern)
 * @author Keith Ohm
 * @version %I%, %G%
 *  
 */

public interface Template extends DataObjector, Serializable {
	// fields
	/** the DataModel that should be returned from getDataModel() */
	public static final DataModel DATA_MODEL = new DataModel("Class Vector");

	/** the DataModel that should be returned from klass.getDataModel() */
	public static final DataModel KLASS_DATA_MODEL = new DataModel(
			"Template.Klass");

	/** the DataModel that should be returned from item.getDataModel() */
	public static final DataModel ITEM_DATA_MODEL = new DataModel(
			"Template.Item");

	//    /** Generated serialized version UID */
	//    private static final long serialVersionUID = 3328207228376112777L;

	//    protected static transient XLogger log =
	// XLogger.getLogger(Template.klass);

	// method signature
	//    /** The node delegate. Don't bother with this!
	//     * @return Class
	//     */
	//    public java.lang.Class getNodeDelegate();

	/**
	 * Label of the klass at specified location
	 * 
	 * @return String
	 * @param i
	 *            the index to the label
	 */
	public String getKlassLabel(int i);

	/**
	 * name of this object
	 * 
	 * @return String
	 */
	public String getName();

	/**
	 * Does this Template hold numeric TemplateIems?
	 * 
	 * @return true if this has numeric <CODE>Item</CODE> s
	 */
	public boolean isNumeric();

	/**
	 * gets the klass at the specified index
	 * 
	 * @param i
	 *            the index to the klass
	 * @return Klass
	 */
	public Template.Klass getKlass(int i);

	/**
	 * returns an array of Klass objects
	 * 
	 * @return Klass[]
	 */
	public Klass[] getKlasses();

	/**
	 * gets the <CODE>Item</CODE> at the specified index
	 * 
	 * @param i
	 *            the index to the item
	 * @return Item
	 */
	public Template.Item getItem(int i);

	/**
	 * returns an array of items
	 * 
	 * @return Item[]
	 */
	public Item[] getItems();

	/**
	 * gets the number of <CODE>Item</CODE>s.
	 * 
	 * @return int
	 */
	public int getItemCount();

	/**
	 * gets the number of <CODE>Klass</CODE> objects.
	 * 
	 * @return int
	 */
	public int getKlassCount();

	/**
	 * if this is numeric then gets all the identifications of <CODE>Item
	 * </CODE> objects as a <CODE>FloatVector</CODE>
	 * 
	 * @return FloatVector
	 */
	public FloatVector toVector();

	/**
	 * Only valid if there is a collection of Klass's associated with this
	 * Template.
	 * 
	 * The method cycles (in order of addition) through all its member
	 * TemplateItems. In the first progress run, a list of unique TemplateItem
	 * Id's. is generated. The size of this list must correspond to the size of
	 * the Klass collection.
	 * 
	 * Each Id is then assigned to one Klass (again, in order).
	 * 
	 * The member TemplateItems are then again cycled through, and assigned to a
	 * Klass (based on the Id - Klass map produced in the previous run).
	 * 
	 * Thus, the result, is that each Klass is now associated with a collection
	 * of TemplateItem's.
	 * 
	 * The Klass does NOT have to be the same as the TemplateItems id.
	 *  
	 */
	public void assignItems();

	/**
	 * PersistentObject impl.
	 * 
	 * @return true if persistent object
	 */
	//    public DataFormat getExternalizedFormat();
	public boolean isRestorable();

	//    public PersistentProxy load(DataStore store) throws DataStoreException {
	//        Serializable ser = new Serializer().load(store);
	//
	//        if (ser instanceof Template) {
	//            Template template = (Template)ser;
	//            //return template;
	//
	//            return new PersistentProxy(template);
	//        }
	//        else throw new DataStoreException("DataStore does not contain a Template
	// object. It contains: " + ser.getKlass());
	//
	//    }
	//
	//    /**
	//     * PersistentObject impl.
	//     * Stored as serializable data.
	//     */
	//    public void save(DataStore store) throws DataStoreException {
	//        new Serializer().save(this, store);
	//    }

	/**
	 * Convenience method for use when debugging. Pretty prints out the content
	 * of this Template.
	 */
	public void printf();

	/**
	 * returns a summary of this data object
	 * 
	 * @return summary info on this Template.
	 * @see Template#getAsString() to get this in Cls format.
	 */
	public String getSummary();

	/**
	 * Munge this Template object into a String. Just to keep things familiar
	 * use the cls format. Example: <br <
	 * 
	 * <pre>
	 * 
	 *  53 3 1
	 *  # Breast Bladder Renal
	 *  0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 2
	 *  
	 * </pre>
	 * 
	 * @param line_seperator
	 *            the characters that will be used to separate lines
	 * @return String
	 */
	public String getAsString(final String line_seperator);

	/**
	 * gets the item's klass' index (Needed in KlassEstimate Funcs)
	 * 
	 * @param i
	 *            the index of the item
	 * @return int
	 */
	public int getKlassIndexForItem(int i);

	//I N N E R C L A S S E S
	/**
	 * A Klass is the replacement for Michael Angelo's ClusterData. This class
	 * probably needs a better name but ClusterData was confusing too, though
	 * maybe eventually revert to it. Anyhow, Roget's says a "sept" is a
	 * division of a family, especially a division of a clan. I wanted to avoid
	 * using the word "Class" as its loaded with other meanings in java.
	 * 
	 * Note the name of this class has been changed to Klass
	 */
	public static class Klass implements org.genepattern.data.DataObjector,
			Serializable {

		/**
		 * Klass constructor
		 * 
		 * @param name
		 *            the name of this klass
		 */
		public Klass(String name) {
			this(name, false/* not mutable */);
		}

		/**
		 * Core constructor for Klass and its' subclasses
		 * 
		 * @param name
		 *            the name of this klass
		 * @param mutable
		 *            is this a mutable klass
		 */
		protected Klass(String name, boolean mutable) {
			this.name = name.trim();
			this.is_mutable = mutable;
			items = new ArrayList();
		}

		//     // @todo check me - inserted for for db4 use
		//    public Klass() { }

		/**
		 * returns a DataModel that defines the type of model this
		 * implementation represents
		 * 
		 * @return DataModel
		 */
		public org.genepattern.data.DataModel getDataModel() {
			return KLASS_DATA_MODEL;
		}

		/**
		 * The id for this sept
		 * 
		 * @return String
		 */
		public String getName() {
			return name;
		}

		/**
		 * gets the item
		 * 
		 * @param i
		 *            the index to the item
		 * @return Item
		 */
		public Template.Item getItem(int i) {
			return (Template.Item) items.get(i);
		}

		/**
		 * Number of Template.Items that are members
		 * 
		 * @return int
		 */
		public int getSize() {
			return items.size();
		}

		//    public void printf() {
		//        log.info("[Klass] name=" + getName() + " numitems=" + getSize() );
		//    }

		/**
		 * gets the smallest position of all the <CODE>Item</CODE> in this
		 * <CODE>Klass</CODE>.
		 * 
		 * @return int
		 */
		public int getMinItemPosition() {
			int min = this.getItem(0).getPosition(); // something in this klass
			final int len = this.getSize();
			for (int i = 1; i < len; i++) {
				final int pos = this.getItem(i).getPosition();
				if (pos < min)
					min = pos;
			}
			return min;
		}

		/**
		 * gets the smallest position of all the <CODE>Item</CODE> in this
		 * <CODE>Klass</CODE>.
		 * 
		 * @return int
		 */
		public int getMaxItemPosition() {
			int max = 0;
			final int len = this.getSize();
			for (int i = 0; i < len; i++) {
				final int pos = this.getItem(i).getPosition();
				if (pos > max)
					max = pos;
			}
			return max;
		}

		/**
		 * returns true if this has the item Note this could be an expesive
		 * operation if there are many items
		 * 
		 * @param item
		 *            the item to check for
		 * @return true if have the <CODE>Item</CODE>
		 */
		public boolean hasItem(Item item) {
			return items.contains(item);
		}

		/**
		 * returns a String representation of this
		 * 
		 * @return String
		 */
		public String toString() {
			final int limit = items.size();
			final char space = ' ';
			StringBuffer buf = new StringBuffer(getName());
			buf.append(" items[");
			buf.append("pos:");
			for (int i = 0; i < limit; i++) {
				Item item = (Item) items.get(i);
				buf.append(space);
				buf.append(item.getPosition());
			}
			buf.append(']');
			return buf.toString();
			//return getName();
			//return "[Klass: "+this.name+" num items="+items.size()+"]";
		}

		//        /**
		//         * this is called to indicate that the state has changed
		//         * Does nothing if the class is immutable
		//         */
		//        protected final void reset() {
		//            if( isMutable() ){
		//                hashcode = 0;
		//            }
		//        }
		/**
		 * returns false if this is an object that cannot have it's internal
		 * state changed
		 * 
		 * @return true if this is a mutable data object
		 */
		public boolean isMutable() {
			return is_mutable;
		}

		/**
		 * this is a reminder that data objects must override equals(Object)
		 * 
		 * @param obj
		 *            the other object
		 * @return true if these two objects are equal
		 */
		public boolean equals(Object obj) {
			//System.out.println("checking Klass "+this+" vs "+obj);
			if (this == obj)
				return true;
			if (!(obj instanceof Klass))
				return false;
			final Klass klass = (Klass) obj;
			//System.out.println("Klass equals "+items.equals(klass.items)+"
			// "+name+" "+klass.name);
			return (((this.name == klass.name) || (name != null && this.name
					.equals(klass.name))) && items.equals(klass.items));
		}

		/**
		 * this is a reminer that classes that override equals must also create
		 * a working hash algorithm. for example:
		 * 
		 * given: boolean b compute (b ? 0 : 1) byte, char, short, or int i
		 * compute (int)i long l compute (int)(l ^ (l >>> 32)) float f compute
		 * Float.floatToIntBits(f) double d compute Double.doubleToLongBits(d)
		 * then compute as long
		 * 
		 * Object just get it's hash or if null then 0
		 * 
		 * Arrays compute for each element
		 * 
		 * i.e.: int result = 17; // prime number result = 37 * result +
		 * (int)character; result = 37 * result + Float.floatToIntBits(f); etc..
		 * return result;
		 * 
		 * @return int
		 */
		public int hashCode() {
			if (hashcode == 0 || isMutable()) {
				int result = 17;
				result = 37 * result + ((name == null) ? 0 : name.hashCode());
				if (items == null)
					result = 37 * result + 0;
				else {
					final int cnt = items.size();
					for (int i = 0; i < cnt; i++) {
						result = 37 * result + items.get(i).hashCode();
					}
				}

				if (isMutable())
					return result;
				else
					hashcode = result;
			}
			return hashcode;

		}

		//fields

		/** Generated serialized version UID */
		private static final long serialVersionUID = 3328777223001142727L;

		/** The name of this Template Klass */
		protected String name;

		/** list of Template.Items */
		protected ArrayList items;

		/** returns true if this class is mutable */
		protected final boolean is_mutable;

		/** returns the hash code or 0 if not calculated */
		private int hashcode = 0;

	} // End Template.Klass

	/** This represents the sample or feature. */
	public static class Item implements org.genepattern.data.DataObjector,
			Serializable {
		/**
		 * Item constructor
		 * 
		 * @param sample
		 *            the sample object
		 * @param id
		 *            the id
		 */
		public Item(final SampleLabel sample, final String id) {
			this(sample, id, -1);
		}

		/**
		 * Item constructor
		 * 
		 * @param sample
		 *            the sample object
		 * @param id
		 *            the id
		 * @param pos
		 *            the position
		 */
		public Item(final SampleLabel sample, String id, int pos) {
			this(sample, id, pos, false/* not mutable */);
		}

		/**
		 * Main constructor for Item
		 * 
		 * @param sample
		 *            the sample object
		 * @param id
		 *            the id
		 * @param pos
		 *            the positiion
		 * @param mutable
		 *            is this mutable
		 */
		protected Item(final SampleLabel sample, String id, int pos,
				boolean mutable) {
			this.sample = sample;
			this.id = id.trim();
			this.pos = pos;
			this.is_mutable = mutable;
		}

		/**
		 * returns the SampleLabel
		 * 
		 * @return SampleLabel
		 */
		public SampleLabel getSampleLabel() {
			return sample;
		}

		/**
		 * returns the id
		 * 
		 * @return String
		 */
		public String getId() {
			return id;
		}

		//    // @todo remove -- tmo for db4o testing
		//    public void setId(String n) {
		//        this.id = n;
		//    }

		/**
		 * gets the position of this Item relative to the others
		 * 
		 * @return int
		 */
		public int getPosition() {
			return pos;
		}

		/**
		 * works iff numeric
		 * 
		 * @return float
		 */
		public float floatValue() {
			return Float.parseFloat(id);
		}

		/**
		 * works iff numeric
		 * 
		 * @return int
		 */
		public int intValue() {
			return Integer.parseInt(id);
		}

		/**
		 * returns a DataModel that defines the type of model this
		 * implementation represents
		 * 
		 * @return DataModel
		 */
		public org.genepattern.data.DataModel getDataModel() {
			return ITEM_DATA_MODEL;
		}

		/**
		 * returns a String representation of this instance
		 * 
		 * @return String
		 */
		public String toString() {
			return "Sample " + sample + " ID " + id + " position " + pos;
		}

		/**
		 * returns the string representation of this Item
		 * 
		 * @param temp
		 *            the template that is belongs to
		 * @return String
		 */
		protected final String toString(final Template temp) {
			String nn;
			try {
				int i = Integer.parseInt(id);
				nn = " (" + temp.getKlass(i).name + ")";
			} catch (NumberFormatException ex) {
				nn = "";
			}
			return "Sample " + sample + " ID " + id + nn + " position " + pos;
		}

		/**
		 * returns false if this is an object that cannot have it's internal
		 * state changed
		 * 
		 * @return boolean
		 */
		public boolean isMutable() {
			return is_mutable;
		}

		/**
		 * this is a reminder that data objects must override equals(Object)
		 * 
		 * @param obj
		 *            the other object
		 * @return boolean
		 */
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Item))
				return false;
			final Item item = (Item) obj;
			return (this.pos == item.pos && this.sample.equals(item.sample) && ((this.id == null && item.id == null) || this.id != null
					&& this.id.equals(item.id)));
		}

		/**
		 * this is a reminer that classes that override equals must also create
		 * a working hash algorithm. for example:
		 * 
		 * given: boolean b compute (b ? 0 : 1) byte, char, short, or int i
		 * compute (int)i long l compute (int)(l ^ (l >>> 32)) float f compute
		 * Float.floatToIntBits(f) double d compute Double.doubleToLongBits(d)
		 * then compute as long
		 * 
		 * Object just get it's hash or if null then 0
		 * 
		 * Arrays compute for each element
		 * 
		 * i.e.: int result = 17; // prime number result = 37 * result +
		 * (int)character; result = 37 * result + Float.floatToIntBits(f); etc..
		 * return result;
		 * 
		 * @return int
		 */
		public int hashCode() {
			final boolean is_mut = isMutable();
			if (hashcode == 0 || is_mut) {
				int result = 17;
				result = 37 * result + pos;
				result = 37 * result + ((id == null) ? 0 : id.hashCode());
				result = 37 * result
						+ ((sample == null) ? 0 : sample.hashCode());
				if (is_mut)
					return result;
				else
					hashcode = result;
			}
			return hashcode;
		}

		/**
		 * returns the name of this data object
		 * 
		 * @return String
		 */
		public String getName() {
			return id;
		}

		//    public void printf() {
		//        StringBuffer buf = new StringBuffer("Item id: ").append(id).append("
		// pos: ").append(pos).append('\n');
		//        log.info(buf.toString());
		//    }

		// fields

		/** Generated serialized version UID */
		private static final long serialVersionUID = 4320029628631142727L;

		/** the sample label */
		protected final SampleLabel sample;

		/** Identifier for this Item */
		protected final String id;

		/**
		 * Position of this item in the Template vector -1 means its not been
		 * initialized
		 */
		protected int pos = -1; // @change me back to -1 after db4o test!!!!!!!

		/** is this class mutable */
		private final boolean is_mutable;

		/** the hash code value or 0 if not calculated */
		private int hashcode = 0;

	} // End Item

} // End Template

