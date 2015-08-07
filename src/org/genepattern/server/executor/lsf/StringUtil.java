/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.lsf;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {
    /**
     * Split the given command line into tokens delimited by space char, or enclosed within quotes.
     * Make sure to handle escaped quote characters. don't include enclosing quote characters in the returned strings.
     * 
     * @param commandLine
     * @return
     */
    public static List<String> splitCommandLine(String commandLine) {
        List<String> rval = new ArrayList<String>();
        
        int idx = 0;
        while(true) {
            int startIdx = nextNonWsIdx(idx, commandLine);
            if (startIdx >= commandLine.length()) {
                //no more tokens
                break;
            }
            char delim = ' ';
            if (commandLine.charAt(startIdx) == '\"') {
                delim = '\"';
                ++startIdx;
            }
            //jump to the end, ignoring escape ('\') characters
            int endIdx = nextIdx(1+idx, delim, commandLine);
            String token = commandLine.substring(startIdx, endIdx);
            rval.add(token);
            idx = endIdx + 1;
        }
        
        return rval;
    }

    /** get the next non whitespace character from the string. */
    private static int nextNonWsIdx(int idx, String commandLine) {
        while(idx < commandLine.length()) {
            char c = commandLine.charAt(idx);
            if (!Character.isWhitespace(c)) {
                break;
            }
            ++idx;
        }
        return idx;
    }
    
    /** get the end index of the current token */
    private static int nextIdx(int idx, char delim, String commandLine) {
        while(idx < commandLine.length()) {
            char c = commandLine.charAt(idx);
            if (c == '\\') {
                //escape char
                ++idx;
            }
            else if (c == delim) {
                return idx;
            }
            ++idx;
        }
        return idx;
    }

}
