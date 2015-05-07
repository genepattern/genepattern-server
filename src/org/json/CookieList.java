package org.json;

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

import java.util.Iterator;

/**
 * Convert a web browser cookie list string to a JSONObject and back.
 * @author JSON.org
 * @version 2
 */
public class CookieList {

    /**
     * Convert a cookie list into a JSONObject. A cookie list is a sequence
     * of name/value pairs. The names are separated from the values by '='.
     * The pairs are separated by ';'. The names and the values
     * will be unescaped, possibly converting '+' and '%' sequences.
     *
     * To add a cookie to a cooklist,
     * cookielistJSONObject.put(cookieJSONObject.getString("name"),
     *     cookieJSONObject.getString("value"));
     * @param string  A cookie list string
     * @return A JSONObject
     * @throws JSONException
     */
    public static JSONObject toJSONObject(String string) throws JSONException {
        JSONObject o = new JSONObject();
        JSONTokener x = new JSONTokener(string);
        while (x.more()) {
            String name = Cookie.unescape(x.nextTo('='));
            x.next('=');
            o.put(name, Cookie.unescape(x.nextTo(';')));
            x.next();
        }
        return o;
    }


    /**
     * Convert a JSONObject into a cookie list. A cookie list is a sequence
     * of name/value pairs. The names are separated from the values by '='.
     * The pairs are separated by ';'. The characters '%', '+', '=', and ';'
     * in the names and values are replaced by "%hh".
     * @param o A JSONObject
     * @return A cookie list string
     * @throws JSONException
     */
    public static String toString(JSONObject o) throws JSONException {
        boolean      b = false;
        Iterator     keys = o.keys();
        String       s;
        StringBuffer sb = new StringBuffer();
        while (keys.hasNext()) {
            s = keys.next().toString();
            if (!o.isNull(s)) {
                if (b) {
                    sb.append(';');
                }
                sb.append(Cookie.escape(s));
                sb.append("=");
                sb.append(Cookie.escape(o.getString(s)));
                b = true;
            }
        }
        return sb.toString();
    }
}
