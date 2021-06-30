/**
 *
 *  Copyright (C) 2000-2007  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: StringUtils.java,v $
 *        Revision 1.2  2007-05-15 04:31:28  hans
 *        Added replaceAll which is used for jdk 1.1.8 compat.
 *
 *        Revision 1.1  2007/03/26 05:23:20  bruceb
 *        some handy string utils
 *
 *
 */

package com.enterprisedt.util;


/**
 *  Various useful string utilities
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
public class StringUtils {

    final private static int MAX_FIELDS = 100;
    
    private static StringUtils utils = new StringUtils();
    
    /**
     * Replaces all occurrences of 'from' in 'text' with 'to'.  Used when writing code
     * for JDK 1.1.
     * 
     * @param text String to replace substrings in.
     * @param from String to search for.
     * @param to String to replace with.
     * @return String with all occurrences of 'from' substituted with 'to'.
     */
    public static String replaceAll(String text, String from, String to)
    {
    	StringBuffer result = new StringBuffer();
    	int cursor = 0;  // start at the beginning
    	while (true) {
    		int fromPos = text.indexOf(from, cursor);				// find next 'from' string
    		if (fromPos>=0) {										// if we find one...
	    		result.append(text.substring(cursor, fromPos));		//   then copy stuff before it
	    		result.append(to);									//   and the 'to' string
	    		cursor = fromPos + from.length();					//   and skip the rest of the 'from' string
    		} else {												// otherwise...    			
    			if (cursor<text.length())							//   if we're not at the end
    				result.append(text.substring(cursor));			//      then copy the rest of the string 
   				break;												//      and finish
    		}
    	}
    	return result.toString();
    }
  
    /**
     * Splits string consisting of fields separated by
     * whitespace into an array of strings. Yes, we could
     * use String.split() but this would restrict us to 1.4+
     * 
     * @param str   string to split
     * @return array of fields
     */
    public static String[] split(String str) {
        return split(str, utils.new WhitespaceSplitter());
    }
    
    /**
     * Splits string consisting of fields separated by
     * whitespace into an array of strings. Yes, we could
     * use String.split() but this would restrict us to 1.4+
     * 
     * @param str   string to split
     * @return array of fields
     */
    public static String[] split(String str, char token) {
        return split(str, utils.new CharSplitter(token));
    }
    /**
     * Splits string consisting of fields separated by
     * whitespace into an array of strings. Yes, we could
     * use String.split() but this would restrict us to 1.4+
     * 
     * @param str   string to split
     * @return array of fields
     */
    private static String[] split(String str, Splitter splitter) {
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

    
    public interface Splitter {
        boolean isSeparator(char ch); 
    }
    
    public class CharSplitter implements Splitter {
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
    
    public class WhitespaceSplitter implements Splitter {
        
        public boolean isSeparator(char ch) {
            if (Character.isWhitespace(ch))
                return true;
            return false;
        }
    }
}
