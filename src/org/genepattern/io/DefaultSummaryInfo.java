/*
 * DefaultSummaryInfo.java
 *
 * Created on February 19, 2003, 4:26 PM
 */

package org.genepattern.io;

import java.util.Map;
import java.util.HashMap;

import org.genepattern.data.DataModel;

import org.genepattern.data.*;

/**
 * Default inplementation of SummaryInfo.
 * @author  kohm
 */
public class DefaultSummaryInfo implements SummaryInfo {
    
    /** Creates a new instance of DefaultSummaryInfo */
    public DefaultSummaryInfo(final Map primary, final Map secondary, final DataModel model) {
        this.primary   = new HashMap(primary);
        this.secondary = (secondary != null) ? new HashMap(secondary) : java.util.Collections.EMPTY_MAP;
        this.model = model;
    }
    
    /** gets the primary information about the DataObject  */
    public Map getPrimaryinfo() {
        return primary;
    }
    
    /** get the secondary information about the DataObject  */
    public Map getSecondaryInfo() {
        return secondary;
    }
    
    /** returns the data model  */
    public DataModel getDataModel() {
        return model;
    }
    
    // fields
    /** the primary information */
    private final Map primary;
    /** the secondary information */
    private final Map secondary;
    /** the data model */
    private final DataModel model;
}
