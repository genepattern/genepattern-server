/*
 * WHITEHEAD INSTITUTE
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2001 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 */

package org.genepattern.data;

/**
 * enum construct
 */
public class Dir {

    public static final Dir FORWARD = new Dir("Forward (top of list to bottom)");

    public static final Dir REVERSE = new Dir("Reverse (bottom of list to top)");

    public static final Dir TWOTAILED = new Dir("Two Tailed");

    public static Dir[] DIR_OPTIONS = new Dir[]{FORWARD, REVERSE, TWOTAILED};

    private String type;

    /**
     * Privatized class constructor.
     */
    private Dir(String type) {
        this.type = type;
    }

    public String toString() {
        return type;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Dir) {
            if (((Dir)obj).type.equals(this.type)) return true;
        }
        return false;
    }

    /**
     *  a lookup metod for dir
     */
    public static Dir lookupDir(Object obj) {
        if (obj == null) throw new NullPointerException("Null dir not allowed");
        if (obj instanceof Dir) return (Dir)obj;
        if (obj instanceof String) {
            if (obj.toString().equals(FORWARD.toString())) return FORWARD;
            else if (obj.toString().equals(REVERSE.toString())) return REVERSE;
            else if (obj.toString().equals(TWOTAILED.toString())) return TWOTAILED;
        }

        throw new IllegalArgumentException("Unable to resolve dir: " + obj);

    }

} // End Dir
