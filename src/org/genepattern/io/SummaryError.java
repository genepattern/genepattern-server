/*
 * SummaryError.java
 *
 * Created on February 19, 2003, 4:22 PM
 */

package org.genepattern.io;

import java.util.HashMap;
import java.util.Map;

import org.genepattern.data.DataModel;
import org.genepattern.io.parsers.DataParser;
/**
 * Reports that an error occured while getting summary information.
 * FIXME shouldn't this extend DefaultSummaryInfo?
 *
 * @author  kohm
 */
public class SummaryError implements SummaryInfo{
    
    /** Creates a new instance of SummaryError */
    public SummaryError(final java.io.File file, final Exception ex, final DataParser parser) {
        this(file, ex, parser, null, null);
    }
    /** Creates a new instance of SummaryError */
    public SummaryError(final String file, final Exception ex, final DataParser parser) {
        this(file, ex, parser, null, null);
    }
    /** Creates a new instance of SummaryError */
    public SummaryError(final java.io.File file, final Exception ex, final DataParser parser, final Map primary, final Map secondary) {
        this((Object)file, ex, parser, primary, secondary);
    }
    /** Creates a new instance of SummaryError */
    public SummaryError(final Object file, final Exception ex, final DataParser parser, final Map primary, final Map secondary) {
        final Map map = (primary == null || primary.size() == 0) ? new HashMap(4) :
                new HashMap(primary);
        if( file != null )
            map.put(FILE, file);
        if( ex == null )
            throw new NullPointerException("The Exception cannot be null for reporting the problem!");
        if( parser == null )
            throw new NullPointerException("The data parser cannot be null!");

        map.put(EXCEPTION, ex);
        map.put("Parser=", parser.getClass());
        this.primary = java.util.Collections.unmodifiableMap(map);
        this.secondary = (secondary == null || secondary.size() == 0) ?
            java.util.Collections.EMPTY_MAP : new HashMap(secondary);
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
        return ERROR_MODEL;
    }
    
    // fields
    /** the file lablel */
    private static final String FILE = "File= ";
    /** the exception lablel */
    private static final String EXCEPTION = "Problem=";
    /** the data model */
    private static final DataModel ERROR_MODEL = new DataModel("Error", -1);
    /** the primary information */
    private final Map primary;
    /** the secondary information */
    private final Map secondary;
}
