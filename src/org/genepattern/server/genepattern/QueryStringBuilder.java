package org.genepattern.server.genepattern;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


/**
 * Build an HTTP query string from a set of zero or more <name>[=<value>] params.
 * Note: the '?' is a character separator and is not part of the query string.
 * @author pcarr
 *
 */
public class QueryStringBuilder {

    private List<GpQueryParam> params;

    /**
     * add a 'field-only' param to the queryString
     * @param name, the un-encoded field name
     * @return
     * @throws UnsupportedEncodingException
     */
    public QueryStringBuilder param(final String name) throws UnsupportedEncodingException {
        return param(name, null);
    }
    
    /**
     * add a <name>=<value> param to the queryString
     * @param name, the un-encoded field name
     * @param value, the un-encoded value
     * @return
     * @throws UnsupportedEncodingException
     */
    public QueryStringBuilder param(final String name, final String value) throws UnsupportedEncodingException {
        if (name==null) {
            // name must not be null
            throw new IllegalArgumentException("name==null");
        }
        if (params==null) {
            params=new ArrayList<GpQueryParam>();
        }
        params.add(new GpQueryParam(name, value));
        return this;
    }

    public String build() {
        //null means, no query string
        if (params==null || params.size()==0) {
            return null;
        }
        boolean first=true;
        final StringBuffer sb=new StringBuffer();
        for(final GpQueryParam param : params) {
            if (first) {
                first=false;
            }
            else {
                sb.append("&");
            }
            sb.append(param.toString());
        }
        return sb.toString();
    }
}