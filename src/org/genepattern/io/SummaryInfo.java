/*
 * SummaryInfo.java
 *
 * Created on February 17, 2003, 7:57 PM
 */

package org.genepattern.io;

import java.util.Map;

import org.genepattern.data.DataModel;

/**
 * Provides some basic information about a DataObject without loading the object
 * into memory.  There will be implementations for each of the data object classes.
 *
 * @author  keith
 */
public interface SummaryInfo {
    /** returns the data model */
    public DataModel getDataModel();
    /** gets the primary information about the DataObject */
    public Map getPrimaryinfo();
    /** get the secondary information about the DataObject */
    public Map getSecondaryInfo();
}
