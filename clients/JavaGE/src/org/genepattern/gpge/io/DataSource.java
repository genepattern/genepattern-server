/*
 * DataSource.java
 *
 * Created on February 18, 2003, 11:26 AM
 */

package org.genepattern.gpge.io;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import org.genepattern.data.DataObjector;
import org.genepattern.io.parsers.DataParser;


/**
 * Classes that implement this interface can access DataObjects from remote
 * (not in memory) locations.
 *
 * @author  keith
 */
public interface DataSource {
    /** gets a copy of the array of DataObjectProxy objects
     * note that this is not dynamically updated but just a static array
     */
    public DataObjectProxy[] getDataProxies();
    /** gets the DataObject from the specified DataObjectProxy
     * @exception IllegalArgumentException if the specified DataObjectProxy is not found from this source
     * @exception IOException if there was a problem reading the data
     * @exception ParseException if there was some problem with the content of the data
     */
    public DataObjector getDataObject(final DataObjectProxy proxy) throws java.io.IOException, ParseException, IllegalArgumentException;
    /** returns a description of the source- i.e. if it reads gct files from a local
     * directory, it reads sdf files from OmniGene, etc
     */
    public String getDescription(DataObjectProxy proxy);
    /** returns the DataParser */
    public DataParser getDataParser();
    /** updates the list of new ones and ones removed */
    public void updateList(List new_data, List removed_data);
    /** returns the type of data that will be read from the raw input stream
     * For example character (ASCII), or binary, or unknown, data.
     */
    public StreamType getStreamType(DataObjectProxy proxy);
    /** returns an InputStream for reading the raw data */
    public InputStream getRawInputStream(final DataObjectProxy proxy) throws IOException;
    // handle listeners
    /** adds the DataSourceUpdateListener to the collection of listeners
     * these listeners will be notified when a new DataObjectProxy
     * object has become available
     */
    public void addDataSourceUpdateListener(final DataSourceUpdateListener listener);
    /** removes the specified DataSourceUpdateListener from the collection of listeners */
    public void removeDataSourceUpdateListener(final DataSourceUpdateListener listener);
}
