/*
 * DataSourceUpdateListener.java
 *
 * Created on February 18, 2003, 11:05 AM
 */

package org.genepattern.gpge.io;

/**
 * Classes that implement this interface will be notified of new DataObjects as
 * DataObjectProxy objects or that the DataObjectProxy objects have been
 * removed.
 * 
 * @author keith
 */
public interface DataSourceUpdateListener extends java.util.EventListener {
	/**
	 * called when some data objects proxies are available The parents can be a
	 * zero length array if no parents or defines the parent nodes to be
	 * created. The first parent in the array, index 0, is the top node which
	 * will have a child of parents[1], etc. The last node will have the
	 * DataModel nodes that contain the DataObjectProxy objects.
	 * 
	 * @param proxies
	 *            array of new DataObjectProxy that are to be added
	 * @param parents
	 *            (not null!) The hierarchy of user objects to be turned into
	 *            nodes
	 */
	public void updateAddDataObjects(DataObjectProxy[] proxies, Object[] parents);

	/**
	 * called when some data objects proxies have become unavailable
	 * 
	 * @param proxies
	 *            array of new DataObjectProxy that are to be
	 * @param parents
	 *            (not null!) The hierarchy of user objects whose nodes should
	 *            contain the proxies to be removed
	 * @see updateAddDataObjects(DataObjectProxy[] proxies, Object[] parents)
	 */
	public void updateRemoveDataObjects(DataObjectProxy[] proxies,
			Object[] parents);

	// fields
	/** Convenience variable. Just an empty array for the parents arg */
	static final Object[] NO_PARENTS = new Object[0];
}