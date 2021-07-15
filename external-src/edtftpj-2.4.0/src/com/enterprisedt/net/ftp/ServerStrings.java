/**
 *
 *  Copyright (C) 2000-2007  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: ServerStrings.java,v $
 *        Revision 1.2  2011-06-09 07:33:12  bruceb
 *        make public
 *
 *        Revision 1.1  2007/01/12 02:04:23  bruceb
 *        string matchers
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.util.Vector;

/**
 *  Manages strings that match various FTP server replies for
 *  various situations. The strings are not exact copies of server
 *  replies, but rather fragments that match server replies (so that
 *  as many servers as possible can be supported). All fragments are
 *  managed internally in upper case to make matching faster.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
public class ServerStrings {
    
    private Vector strings = new Vector();
    
    /**
     * Add a fragment to be managed
     * 
     * @param string   new message fragment
     */
    public void add(String string) {
        strings.addElement(string.toUpperCase());
    }
    
    /**
     * Get all fragments being managed
     * 
     * @return array of management fragments
     */
    public String[] getAll() {
        String[] result = new String[strings.size()];
        strings.copyInto(result);
        return result;
    }
    
    /**
     * Clear all fragments being managed
     */
    public void clearAll() {
        strings.removeAllElements();
    }
    
    /**
     * Fragment count
     * 
     * @return number of fragments being managed
     */
    public int size() {
        return strings.size();
    }
    
    /**
     * Remove a managed fragment. Only exact matches (ignoring case)
     * are removed
     * 
     * @param string   string to be removed
     * @return true if removed, false if not found
     */
    public boolean remove(String string) {
        String upper = string.toUpperCase();
        for (int i = 0; i < strings.size(); i++) {
            String msg = (String)strings.elementAt(i);
            if (upper.equals(msg)) {
                strings.removeElementAt(i);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns true if any fragment is found in the supplied
     * string.
     * 
     * @param reply   server reply to test for matches
     * @return true for a match, false otherwise
     */
    public boolean matches(String reply) {
        String upper = reply.toUpperCase();
        for (int i = 0; i < strings.size(); i++) {
            String msg = (String)strings.elementAt(i);
            if (upper.indexOf(msg) >= 0)
                return true;
        }
        return false;
    }

  
}
