/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Internal representation of a field=value pair in a query string.
 * Prefixed with the 'Gp' so that we don't confuse it with the JAX-RS QueryParam annotation.
 * @author pcarr
 *
 */
public class GpQueryParam {
    private final String encodedStr;

    public GpQueryParam(final String field) throws UnsupportedEncodingException {
        this(field, (String)null);
    }

    public GpQueryParam(final String field, final String value) throws UnsupportedEncodingException {
        final String encodedName=URLEncoder.encode(field, "UTF-8");
        if (value!=null) {
            final String encodedValue=URLEncoder.encode(value, "UTF-8");
            encodedStr=encodedName+"="+encodedValue;
        }
        else {
            encodedStr=encodedName;
        }
    }

    /**
     * Gets the URLencoded value for the entire query parameter, 
     *     <encodeName>=<encodedValue>
     */
    public String toString() {
        return encodedStr;
    }
}
