/*
 * DataSelectListener.java
 *
 * Created on February 18, 2003, 1:42 PM
 */

package org.genepattern.gpge.ui.maindisplay;

import org.genepattern.data.DataModel;
import org.genepattern.gpge.io.DataObjectProxy;
import org.genepattern.gpge.io.GroupDataSource;

/**
 * When DataObject and DataModel Nodes are selected a DataSelectListener gets
 * notified.
 * 
 * @author keith
 */
public interface DataSelectListener {
	/** notifies the listener that a DataObjectProxy Node has been selected */
	public void dataProxySelected(final DataObjectProxy proxy, final Object node);

	/** notifies the listener that a DataModel Node has been selected */
	public void dataModelSelected(final DataModel model, final Object node);

	/** the listener is notified that a GroupDataSource has been selected */
	public void dataSourceSelected(final GroupDataSource source,
			final Object node);

	/** indicates that some unknown node was selected */
	public void unknownSelected(Object source);
}