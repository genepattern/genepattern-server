/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

/**
 * Utility class for encoding html strings, copied from RunPipelineDecoratorBase.htmlEncode(str).
 * 
 * @author pcarr
 */
public abstract class HtmlEncoder {
    private static final String entityMap;
    private static final String[] quickEntities;

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
        if (nonHTMLsrc == null) {
            return "";
        }
        StringBuffer res = new StringBuffer();
        int l = nonHTMLsrc.length();
        int idx;
        char c;
        for (int i = 0; i < l; i++) {
            c = nonHTMLsrc.charAt(i);
            idx = entityMap.indexOf(c);
            if (idx == -1) {
                res.append(c);
            } 
            else {
                res.append(quickEntities[idx]);
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

    { "\u0083", "#131" }, { "\u0084", "#132" }, { "\u0085", "#133" },
            { "\u0086", "#134" }, { "\u0087", "#135" }, { "\u0089", "#137" },
            { "\u008A", "#138" }, { "\u008B", "#139" }, { "\u008C", "#140" },
            { "\u0091", "#145" }, { "\u0092", "#146" }, { "\u0093", "#147" },
            { "\u0094", "#148" }, { "\u0095", "#149" }, { "\u0096", "#150" },
            { "\u0097", "#151" }, { "\u0099", "#153" }, { "\u009A", "#154" },
            { "\u009B", "#155" }, { "\u009C", "#156" }, { "\u009F", "#159" },

            { "\u00A0", "nbsp" }, { "\u00A1", "iexcl" }, { "\u00A2", "cent" },
            { "\u00A3", "pound" }, { "\u00A4", "curren" }, { "\u00A5", "yen" },
            { "\u00A6", "brvbar" }, { "\u00A7", "sect" }, { "\u00A8", "uml" },
            { "\u00A9", "copy" }, { "\u00AA", "ordf" }, { "\u00AB", "laquo" },
            { "\u00AC", "not" }, { "\u00AD", "shy" }, { "\u00AE", "reg" },
            { "\u00AF", "macr" }, { "\u00B0", "deg" }, { "\u00B1", "plusmn" },
            { "\u00B2", "sup2" }, { "\u00B3", "sup3" },

            { "\u00B4", "acute" }, { "\u00B5", "micro" }, { "\u00B6", "para" },
            { "\u00B7", "middot" }, { "\u00B8", "cedil" },
            { "\u00B9", "sup1" }, { "\u00BA", "ordm" }, { "\u00BB", "raquo" },
            { "\u00BC", "frac14" }, { "\u00BD", "frac12" },
            { "\u00BE", "frac34" }, { "\u00BF", "iquest" },

            { "\u00C0", "Agrave" }, { "\u00C1", "Aacute" },
            { "\u00C2", "Acirc" }, { "\u00C3", "Atilde" },
            { "\u00C4", "Auml" }, { "\u00C5", "Aring" }, { "\u00C6", "AElig" },
            { "\u00C7", "Ccedil" }, { "\u00C8", "Egrave" },
            { "\u00C9", "Eacute" }, { "\u00CA", "Ecirc" },
            { "\u00CB", "Euml" }, { "\u00CC", "Igrave" },
            { "\u00CD", "Iacute" }, { "\u00CE", "Icirc" },
            { "\u00CF", "Iuml" },

            { "\u00D0", "ETH" }, { "\u00D1", "Ntilde" },
            { "\u00D2", "Ograve" }, { "\u00D3", "Oacute" },
            { "\u00D4", "Ocirc" }, { "\u00D5", "Otilde" },
            { "\u00D6", "Ouml" }, { "\u00D7", "times" },
            { "\u00D8", "Oslash" }, { "\u00D9", "Ugrave" },
            { "\u00DA", "Uacute" }, { "\u00DB", "Ucirc" },
            { "\u00DC", "Uuml" }, { "\u00DD", "Yacute" },
            { "\u00DE", "THORN" }, { "\u00DF", "szlig" },

            { "\u00E0", "agrave" }, { "\u00E1", "aacute" },
            { "\u00E2", "acirc" }, { "\u00E3", "atilde" },
            { "\u00E4", "auml" }, { "\u00E5", "aring" }, { "\u00E6", "aelig" },
            { "\u00E7", "ccedil" }, { "\u00E8", "egrave" },
            { "\u00E9", "eacute" }, { "\u00EA", "ecirc" },
            { "\u00EB", "euml" }, { "\u00EC", "igrave" },
            { "\u00ED", "iacute" }, { "\u00EE", "icirc" },
            { "\u00EF", "iuml" },

            { "\u00F0", "eth" }, { "\u00F1", "ntilde" },
            { "\u00F2", "ograve" }, { "\u00F3", "oacute" },
            { "\u00F4", "ocirc" }, { "\u00F5", "otilde" },
            { "\u00F6", "ouml" }, { "\u00F7", "divid" },
            { "\u00F8", "oslash" }, { "\u00F9", "ugrave" },
            { "\u00FA", "uacute" }, { "\u00FB", "ucirc" },
            { "\u00FC", "uuml" }, { "\u00FD", "yacute" },
            { "\u00FE", "thorn" }, { "\u00FF", "yuml" }, { "\u0080", "euro" } };

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
