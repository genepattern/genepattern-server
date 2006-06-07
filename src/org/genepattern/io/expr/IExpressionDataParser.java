/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.io.expr;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Interface for reading expression data documents using callbacks.
 * 
 * @author Joshua Gould
 */
public interface IExpressionDataParser {

    /**
     * Sets the handler that will receive events
     * 
     * @param h
     *            The new handler value
     */
    public void setHandler(IExpressionDataHandler h);

    /**
     * Returns <code>true</code> if this parser can claims to be able to
     * decode the given stream upon brief examination, <code>false</code>
     * otherwise
     * 
     * @param is
     *            The input stream
     * @return Whether this parser can decode the given stream
     * @exception IOException
     *                If an error occurs while reading from the stream
     */
    public boolean canDecode(InputStream is) throws IOException;

    /**
     * Parse a document. The application can use this method to instruct the
     * parser to begin parsing a document from any valid input source.
     * Applications may not invoke this method while a parse is in progress
     * (they should create a new parser instead). Once a parse is complete, an
     * application may reuse the same parser object, possibly with a different
     * input source. During the parse, the parser will provide information about
     * the document through the registered event handler. This method is
     * synchronous: it will not return until parsing has ended. If a client
     * application wants to terminate parsing early, it should throw an
     * exception.
     * 
     * @param is
     *            The input stream
     * @throws org.genepattern.io.ParseException -
     *             Any parse exception, possibly wrapping another exception.
     * @throws IOException -
     *             An IO exception from the parser, possibly from a byte stream
     *             or character stream supplied by the application.
     */
    public void parse(InputStream is) throws org.genepattern.io.ParseException,
            IOException;

    /**
     * Gets the format name of the type of documents that this parser can decode
     * 
     * @return the format name
     */
    public String getFormatName();

    /**
     * Gets the standard file suffixes that this parser can decode
     * 
     * @return the file suffixes
     */
    public List getFileSuffixes();
}