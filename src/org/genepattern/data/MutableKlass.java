package org.genepattern.data;

/**
 * 
 */
public class MutableKlass extends Template.Klass {

	/** Generated serialized version UID */
	private static final long serialVersionUID = 3639907228371141927L;

	/**
	 * Class constructor
	 */
	public MutableKlass(String aName) {
		super(aName, true/* mutable */);
	}

	//    // @todo check me - inserted for for db4 use
	//    public MutableKlass() { }

	public void setName(String aName) {
		//        reset();
		name = aName;
	}

	public void add(Template.Item item) {
		//        reset();
		items.add(item);
	}

} // End MutableClass
