/*
 * DataObjectProxy.java
 *
 * Created on February 17, 2003, 7:12 PM
 */

package org.genepattern.gpge.io;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.io.SummaryInfo;

/**
 * Implementations of this interface will be able to get the data object
 * on demand or simply display some basic or Summaryinfo details without 
 * loading the object into memory.
 * @author  keith
 */
public interface DataObjectProxy extends DataObjector {
    
    /** gets the DataObjector instance from its' source
     *@exception if the data obect cannot be retrived from its' source at this time
     */
    public DataObjector getDataObject() throws java.io.IOException, java.text.ParseException;
    /** gets the Summary information about the object */
    public SummaryInfo getSummaryInfo();
    /** returns a DataModel that defines the type of model this implementation represents  */
    public DataModel getDataModel();
    /** gets the data source's description */
    public String getDataSourceDescription();
    /** returns the DataSource */
    public DataSource getDataSource();
}
