/*
 * DataSourceManager.java
 *
 * Created on April 11, 2003, 1:07 PM
 */

package org.genepattern.gpge.io;

/**
 * Gets notified of new GroupDataSource objects and old ones that are being
 * removed
 * 
 * @author kohm
 */
public interface DataSourceManager {
	/** adds a new data source */
	public void addDataSource(final GroupDataSource source);

	/** removes an old data source */
	public void removeDataSource(final GroupDataSource source);
}