package org.json;

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/**
 * The HTTPTokener extends the JSONTokener to provide additional methods
 * for the parsing of HTTP headers.
 * @author JSON.org
 * @version 2
 */
public class HTTPTokener extends JSONTokener {

    /**
     * Construct an XMLTokener from a string.
     * @param s A source string.
     */
    public HTTPTokener(String s) {
        super(s);
    }


    /**
     * Get the next token or string. This is used in parsing HTTP headers.
     * @throws JSONException
     * @return A String.
     */
    public String nextToken() throws JSONException {
        char c;
        char q;
        StringBuffer sb = new StringBuffer();
        do {
            c = next();
        } while (Character.isWhitespace(c));
        if (c == '"' || c == '\'') {
            q = c;
            for (;;) {
                c = next();
                if (c < ' ') {
                    throw syntaxError("Unterminated string.");
                }
                if (c == q) {
                    return sb.toString();
                }
                sb.append(c);
            }
        } 
        for (;;) {
            if (c == 0 || Character.isWhitespace(c)) {
                return sb.toString();
            }
            sb.append(c);
            c = next();
        }
    }
}
