/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/*
 * StringUtils.java
 *
 * Created on March 10, 2003, 12:47 PM
 */

package org.genepattern.util;

import java.io.File;

/**
 * Static methods for String manipulation
 * 
 * @author kohm
 */
public class StringUtils {

    /** Prevent instantation */
    private StringUtils() {
    }

    /**
     * @param in
     * @return true if the string is not null AND it's not the empty string.
     */
    public static final boolean isSet(final String in) {
        if (in == null || in.length()==0) {
            return false;
        }
        return true;
    }

    /**
     * Get the first index of the file separator from the given String.
     * Based on {@link #lastIndexOfFileSeparator(String)}.
     * @param value
     * @return the first index of the file separator.
     * @author pcarr
     */
    public static int indexOfFileSeparator(String value) {
        int  index = value.indexOf(File.separatorChar);
        if (index == -1) {
            char sep = File.separatorChar == '/' ? '\\' : '/';
            index = value.indexOf(sep);
        }
        return index;
    }

    /**
     * Returns the last index of the file separator character.
     * 
     * @param value
     *            The value to get the last index of the file separator.
     * @return The index.
     */
    public static int lastIndexOfFileSeparator(String value) {
        int index = value.lastIndexOf(File.separatorChar);
        if (index == -1) {
            char sep = File.separatorChar == '/' ? '\\' : '/';
            index = value.lastIndexOf(sep);
        }
        return index;
    }

    /**
     * returns a new String where all occurances of the first string are
     * replaced with the second string
     */
    public static final String replaceAll(final String source, final String find, final String replace) {
        final StringBuffer sb = new StringBuffer(source);
        final int diff = replace.length() - find.length();
        final int len = find.length();
        for (int i = source.indexOf(find), offset = 0; i >= 0; i = source.indexOf(find, i + 1)) {
            final int strt = i + offset, end = strt + len;
            sb.replace(strt, end, replace);
            offset += diff;
        }
        return sb.toString();
    }

    /**
     * creates an array of Strings by parsing the input String using the
     * delimiter
     */
    public static final String[] splitStrings(final String text, final char delim) {
        final int num = getNumOccurances(text, delim) + 1;
        if (num == 1) {
            return new String[] { text.trim() };
        }
        // System.out.println("num="+num);
        final String[] strings = new String[num];
        for (int c = 0, last = 0, i = text.indexOf(delim); i >= 0; i = text.indexOf(delim, i)) {
            strings[c++] = text.substring(last, i).trim();
            last = ++i;
        }
        strings[num - 1] = text.substring(text.lastIndexOf(delim) + 1).trim();
        return strings;
    }

    /**
     * creates an array of Strings by parsing the input String using the
     * delimiter
     */
    public static final String[] splitStrings(final String text, final String delim) {
        final int num = getNumOccurances(text, delim);
        if (num == 0) {
            return new String[] { text.trim() };
        }
        final String[] strings = new String[num];
        final int del_len = delim.length() - 1;
        for (int c = 0, last = 0, i = text.indexOf(delim); i >= 0; i = text.indexOf(delim, i)) {
            strings[c++] = text.substring(last, i).trim();
            last = i + del_len;
        }
        return strings;
    }

    /** gets the number of times the delimiter is present in the String */
    public static final int getNumOccurances(final String text, final char delim) {
        int count = 0;
        final int len = text.length() - 1;
        for (int i = text.indexOf(delim); i >= 0 && i < len; i = text.indexOf(delim, ++i)) {
            // System.out.println("i="+i);
            count++;
        }
        return count;
    }

    /** gets the number of times the delimiter is present in the String */
    public static final int getNumOccurances(final String text, final String delim) {
        int count = 0;
        for (int i = text.indexOf(delim); i >= 0; i = text.indexOf(delim, ++i)) {
            count++;
        }
        return count;
    }

    /**
     * counts the number of non-null elements that have a trimed length of at
     * least 1
     */
    public static final int countNonEmpty(final String[] array) {
        final int limit = array.length;
        int cnt = 0;
        for (int i = 0; i < limit; i++) {
            final String string = array[i];
            if (string != null && string.trim().length() > 0)
                cnt++;
        }
        return cnt;
    }

    /**
     * returns the String with the first character capitalized so wisconsin =>
     * Wisconsin
     */
    public static final String capitalize(final String text) {
        final char upper = Character.toUpperCase(text.charAt(0));
        return upper + text.substring(1);
    }

    /** finds the longest string and returns the number of characters */
    public static final int getMaxLineCount(final String text) {
        int longest = -1;
        final char NL = '\n';
        // s = start e = end
        for (int s = 0, e = text.indexOf(NL); e >= 0; s = e, e = text.indexOf(NL, ++e)) {
            final int diff = e - s;
            if (e >= 0 && longest < diff)
                longest = diff;
        }
        if (longest == -1)
            return text.length();
        return longest;
    }

    /**
     * escapes characters that have an HTML entity representation. It uses a
     * quick string -> array mapping to avoid creating thousands of temporary
     * objects.
     * 
     * @param nonHTMLsrc
     *            String containing the text to make HTML-safe
     * @return String containing new copy of string with ENTITIES escaped
     */
    public static final String htmlEncode(String nonHTMLsrc) {
        if (nonHTMLsrc == null)
            return "";
        StringBuffer res = new StringBuffer();
        int l = nonHTMLsrc.length();
        int idx;
        char c;
        for (int i = 0; i < l; i++) {
            c = nonHTMLsrc.charAt(i);
            idx = entityMap.indexOf(c);
            if (idx == -1) {
                res.append(c);
            } else {
                res.append(quickEntities[idx]);
            }
        }
        return res.toString();
    }
    
    public static final String htmlEncodeLongString(String nonHTMLsrc) {
        if (nonHTMLsrc == null)
            return "";
        StringBuffer res = new StringBuffer();
        int l = nonHTMLsrc.length();
        int idx;
        char c;
        for (int i = 0; i < l; i++) {
            c = nonHTMLsrc.charAt(i);
            idx = entityMap.indexOf(c);
            if (idx == -1) {
                res.append(c);
            } else {
                res.append(quickEntities[idx]);
            }
            if (i!=0 && i%150==0) {
            	res.append("\n");
            }
        }
        return res.toString();
    }

    /**
     * static lookup table for htmlEncode method
     * 
     * @see #htmlEncode(String)
     * 
     */
    private static final String[][] ENTITIES = {
    /* We probably don't want to filter regular ASCII chars so we leave them out */
    { "&", "amp" }, { "<", "lt" }, { ">", "gt" }, { "\"", "quot" },

    { "\u0083", "#131" }, { "\u0084", "#132" }, { "\u0085", "#133" }, { "\u0086", "#134" }, { "\u0087", "#135" },
            { "\u0089", "#137" }, { "\u008A", "#138" }, { "\u008B", "#139" }, { "\u008C", "#140" },
            { "\u0091", "#145" }, { "\u0092", "#146" }, { "\u0093", "#147" }, { "\u0094", "#148" },
            { "\u0095", "#149" }, { "\u0096", "#150" }, { "\u0097", "#151" }, { "\u0099", "#153" },
            { "\u009A", "#154" }, { "\u009B", "#155" }, { "\u009C", "#156" }, { "\u009F", "#159" },

            { "\u00A0", "nbsp" }, { "\u00A1", "iexcl" }, { "\u00A2", "cent" }, { "\u00A3", "pound" },
            { "\u00A4", "curren" }, { "\u00A5", "yen" }, { "\u00A6", "brvbar" }, { "\u00A7", "sect" },
            { "\u00A8", "uml" }, { "\u00A9", "copy" }, { "\u00AA", "ordf" }, { "\u00AB", "laquo" },
            { "\u00AC", "not" }, { "\u00AD", "shy" }, { "\u00AE", "reg" }, { "\u00AF", "macr" }, { "\u00B0", "deg" },
            { "\u00B1", "plusmn" }, { "\u00B2", "sup2" }, { "\u00B3", "sup3" },

            { "\u00B4", "acute" }, { "\u00B5", "micro" }, { "\u00B6", "para" }, { "\u00B7", "middot" },
            { "\u00B8", "cedil" }, { "\u00B9", "sup1" }, { "\u00BA", "ordm" }, { "\u00BB", "raquo" },
            { "\u00BC", "frac14" }, { "\u00BD", "frac12" }, { "\u00BE", "frac34" }, { "\u00BF", "iquest" },

            { "\u00C0", "Agrave" }, { "\u00C1", "Aacute" }, { "\u00C2", "Acirc" }, { "\u00C3", "Atilde" },
            { "\u00C4", "Auml" }, { "\u00C5", "Aring" }, { "\u00C6", "AElig" }, { "\u00C7", "Ccedil" },
            { "\u00C8", "Egrave" }, { "\u00C9", "Eacute" }, { "\u00CA", "Ecirc" }, { "\u00CB", "Euml" },
            { "\u00CC", "Igrave" }, { "\u00CD", "Iacute" }, { "\u00CE", "Icirc" }, { "\u00CF", "Iuml" },

            { "\u00D0", "ETH" }, { "\u00D1", "Ntilde" }, { "\u00D2", "Ograve" }, { "\u00D3", "Oacute" },
            { "\u00D4", "Ocirc" }, { "\u00D5", "Otilde" }, { "\u00D6", "Ouml" }, { "\u00D7", "times" },
            { "\u00D8", "Oslash" }, { "\u00D9", "Ugrave" }, { "\u00DA", "Uacute" }, { "\u00DB", "Ucirc" },
            { "\u00DC", "Uuml" }, { "\u00DD", "Yacute" }, { "\u00DE", "THORN" }, { "\u00DF", "szlig" },

            { "\u00E0", "agrave" }, { "\u00E1", "aacute" }, { "\u00E2", "acirc" }, { "\u00E3", "atilde" },
            { "\u00E4", "auml" }, { "\u00E5", "aring" }, { "\u00E6", "aelig" }, { "\u00E7", "ccedil" },
            { "\u00E8", "egrave" }, { "\u00E9", "eacute" }, { "\u00EA", "ecirc" }, { "\u00EB", "euml" },
            { "\u00EC", "igrave" }, { "\u00ED", "iacute" }, { "\u00EE", "icirc" }, { "\u00EF", "iuml" },

            { "\u00F0", "eth" }, { "\u00F1", "ntilde" }, { "\u00F2", "ograve" }, { "\u00F3", "oacute" },
            { "\u00F4", "ocirc" }, { "\u00F5", "otilde" }, { "\u00F6", "ouml" }, { "\u00F7", "divid" },
            { "\u00F8", "oslash" }, { "\u00F9", "ugrave" }, { "\u00FA", "uacute" }, { "\u00FB", "ucirc" },
            { "\u00FC", "uuml" }, { "\u00FD", "yacute" }, { "\u00FE", "thorn" }, { "\u00FF", "yuml" },
            { "\u0080", "euro" } };

    private static String entityMap;

    private static String[] quickEntities;
    static {
        // Initialize some local mappings to speed it all up
        int l = ENTITIES.length;
        StringBuffer temp = new StringBuffer();

        quickEntities = new String[l];
        for (int i = 0; i < l; i++) {
            temp.append(ENTITIES[i][0]);
            quickEntities[i] = "&" + ENTITIES[i][1] + ";";
        }
        entityMap = temp.toString();
    }

}
