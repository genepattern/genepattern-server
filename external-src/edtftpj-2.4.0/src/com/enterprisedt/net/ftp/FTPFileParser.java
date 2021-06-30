/**
 *
 *  edtFTPj
 * 
 *  Copyright (C) 2000-2004 Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  Bug fixes, suggestions and comments should be should posted on 
 *  http://www.enterprisedt.com/forums/index.php
 *
 *  Change Log:
 *
 *    $Log: FTPFileParser.java,v $
 *    Revision 1.10  2008-07-15 05:41:33  bruceb
 *    refactor parsing code
 *
 *    Revision 1.9  2007-12-18 07:53:20  bruceb
 *    trimStart() changes
 *
 *    Revision 1.8  2007-10-12 05:20:44  bruceb
 *    permit ignoring date parser errors
 *
 *    Revision 1.7  2007-01-15 23:03:22  bruceb
 *    more splitter methods
 *
 *    Revision 1.6  2005/07/22 10:25:12  bruceb
 *    upped MAX_FIELDS
 *
 *    Revision 1.5  2005/06/03 11:26:25  bruceb
 *    comment change
 *
 *    Revision 1.4  2004/10/18 15:57:51  bruceb
 *    setLocale added
 *
 *    Revision 1.3  2004/07/23 08:29:57  bruceb
 *    updated comment
 *
 *    Revision 1.2  2004/06/25 11:48:30  bruceb
 *    changed MAX_FIELDS to 20
 *
 *    Revision 1.1  2004/04/17 23:42:07  bruceb
 *    file parsing part II
 *
 *    Revision 1.1  2004/04/17 18:37:23  bruceb
 *    new parse functionality
 *
 */

package com.enterprisedt.net.ftp;

import java.text.ParseException;
import java.util.Locale;

/**
 *  Root class of all file parsers
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.10 $
 */
abstract public class FTPFileParser {
    
    /**
     * Maximum number of fields in raw string
     */
    private final static int MAX_FIELDS = 100;
    
    /**
     * Ignore date parsing errors
     */
    protected boolean ignoreDateParseErrors = false;
    
    /**
     * Parse server supplied string
     * 
     * @param raw   raw string to parse
     */
    abstract public FTPFile parse(String raw) throws ParseException;
    
    /**
     * Set the locale for date parsing of listings
     * 
     * @param locale    locale to set
     */
    abstract public void setLocale(Locale locale);
    
    /**
     * Ignore date parse errors
     * 
     * @param ignore
     */
    public void setIgnoreDateParseErrors(boolean ignore) {
        this.ignoreDateParseErrors = ignore;
    }
    
    /**
     * Valid format for this parser
     * 
     * @param listing   listing to test
     * @return true if valid
     */
    public boolean isValidFormat(String[] listing) {
        return false;
    }
    
    /**
     * Does this parser parse multiple lines to get one listing?
     * 
     * @return  
     */
    public boolean isMultiLine() {
        return false;
    }
    
    /**
     * Trim the start of the supplied string
     * 
     * @param str   string to trim
     * @return string trimmed of whitespace at the start
     */
    protected String trimStart(String str) {
        StringBuffer buf = new StringBuffer();
        boolean found = false;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!found & Character.isWhitespace(ch))
                continue;
            found = true;
            buf.append(ch);
        }
        return buf.toString();
    }
      
    /**
     * Splits string consisting of fields separated by
     * whitespace into an array of strings. Yes, we could
     * use String.split() but this would restrict us to 1.4+
     * 
     * @param str   string to split
     * @return array of fields
     */
    protected String[] split(String str) {
        return split(str, new WhitespaceSplitter());
    }
    
    /**
     * Splits string consisting of fields separated by
     * whitespace into an array of strings. Yes, we could
     * use String.split() but this would restrict us to 1.4+
     * 
     * @param str   string to split
     * @return array of fields
     */
    protected String[] split(String str, char token) {
        return split(str, new CharSplitter(token));
    }
    
    /**
     * Splits string consisting of fields separated by
     * whitespace into an array of strings. Yes, we could
     * use String.split() but this would restrict us to 1.4+
     * 
     * @param str   string to split
     * @return array of fields
     */
    protected String[] split(String str, Splitter splitter) {
        String[] fields = new String[MAX_FIELDS];
        int pos = 0;
        StringBuffer field = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!splitter.isSeparator(ch))
                field.append(ch);
            else {
                if (field.length()> 0) {
                    fields[pos++] = field.toString();
                    field.setLength(0);
                }
            }
        }
        // pick up last field
        if (field.length() > 0) {
            fields[pos++] = field.toString();
        }
        String[] result = new String[pos];
        System.arraycopy(fields, 0, result, 0, pos);
        return result;
    }
    
    
    interface Splitter {
        boolean isSeparator(char ch); 
    }
    
    class CharSplitter implements Splitter {
        private char token;

        CharSplitter(char token) {
            this.token = token;
        }

        public boolean isSeparator(char ch) {
            if (ch == token)
                return true;
            return false;
        }
        
    }
    
    class WhitespaceSplitter implements Splitter {
        
        public boolean isSeparator(char ch) {
            if (Character.isWhitespace(ch))
                return true;
            return false;
        }
    }
}
