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
 * Interface for expression data readers.
 * 
 * @author Joshua Gould
 */
public interface IExpressionDataReader {
    public Object read(String pathname, IExpressionDataCreator creator)
            throws org.genepattern.io.ParseException, IOException;

    public Object read(InputStream is, IExpressionDataCreator creator)
            throws org.genepattern.io.ParseException, IOException;

    public String getFormatName();

    public List getFileSuffixes();

    public boolean canRead(InputStream is) throws IOException;
}
