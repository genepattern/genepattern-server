/*
 * This software and its documentation are copyright 2002 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 * 
 * This software is made available for use on a case by case basis, and
 * only with specific written permission from The Whitehead Institute.
 * It may not be redistributed nor posted to any bulletin board, included
 * in any shareware distributions, or the like, without specific written
 * permission from The Whitehead Institute.  This code may be customized
 * by individual users, although such versions may not be redistributed
 * without specific written permission from The Whitehead Institute.
 * 
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 *
 */
/*
 * Encoder.java
 *
 * Created on December 18, 2002, 8:03 PM
 */

package org.genepattern.io.encoder;

import java.io.IOException;
import java.io.OutputStream;

import org.genepattern.data.DataObjector;
/** Interface that defines the methods for encoding a data object.
 * @author kohm
 */
public interface Encoder {
    /** encodes the data to the output stream
     * @param data the data object
     * @param out the output stream
     * @throws IOException if a problem arises durring an I/O operation
     */
    public void write (final DataObjector data, final OutputStream out) throws IOException;
    /** gets the file extension for the specified object or null if wrong type
     * @param data the data object
     * @return String the prefered file extension for this file format
     */
    public String getFileExtension(final DataObjector data);
    /** returns true if this can handle encoding the specified DataObjector
     * @param data the data object
     * @return true if this can encode the data object
     */
    public boolean canEncode(final DataObjector data);
}
