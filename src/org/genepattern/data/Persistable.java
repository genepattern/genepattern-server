/*
 * Persistable.java
 *
 * Created on October 25, 2002, 11:56 PM
 */

package org.genepattern.data;

/**
 *
 * @author  kohm
 */
public interface Persistable {
    /**
     * this object will encode itself as an XML document by
     * writting to the output stream
     */
    void encodeObject(java.io.OutputStream out) throws java.io.IOException;
    /**
     * this object will initialize itself with data from the XML document
     * available from the input stream 
     */
    void decodeObject(java.io.InputStream in) throws java.io.IOException;
    
}
