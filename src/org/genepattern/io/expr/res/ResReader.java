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

package org.genepattern.io.expr.res;

import java.io.IOException;
import java.io.InputStream;

import org.genepattern.io.AbstractReader;
import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataCreator;
import org.genepattern.io.expr.IExpressionDataReader;
import org.genepattern.io.expr.ReaderUtil;

/**
 * Reads res files.
 * 
 * @author Joshua Gould
 */
public class ResReader extends AbstractReader implements IExpressionDataReader {

    public ResReader() {
        super(new String[] { "res" }, "res");
    }

    public boolean canRead(InputStream in) throws IOException {
        ResParser parser = new ResParser();
        return parser.canDecode(in);
    }

    public Object read(String fileName, IExpressionDataCreator creator)
            throws IOException, ParseException {
        return ReaderUtil.read(new ResParser(), fileName, creator);
    }

    public Object read(InputStream is, IExpressionDataCreator creator)
            throws ParseException, IOException {
        return ReaderUtil.read(new ResParser(), is, creator);
    }
}