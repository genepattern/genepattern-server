/*
 * StreamType.java
 *
 * Created on March 28, 2003, 9:42 AM
 */

package org.genepattern.gpge.io;

/**
 *
 * @author  kohm
 */
public class StreamType extends org.genepattern.util.AbstractKonstant {
    
    /** Creates a new instance of StreamType */
    private StreamType(final String label, final int key) {
        super(label, key);
    }
    
    // fields
    /** 
     * indicates that the DataSource.getRawInputStream() will be delivering Character
     * data (ASCII)
     */
    public static final StreamType TEXT = new StreamType("Text", 0);
    /** 
     * indicates that the DataSource.getRawInputStream() will be delivering binary data
     */
    public static final StreamType BINARY = new StreamType("Binary", 1);
    /** 
     * indicates that the DataSource.getRawInputStream() will be delivering data
     * of an unknown type
     */
    public static final StreamType UNKNOWN = new StreamType("Unknown", -1);
}
