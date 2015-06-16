/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.util;

import java.util.Comparator;

import org.apache.log4j.Logger;

public class LSIDVersionComparator implements Comparator<String> {
    Logger log = Logger.getLogger(LSIDVersionComparator.class);
    public static final LSIDVersionComparator INSTANCE = new LSIDVersionComparator();

	public int compare(String s0, String s1) {	
	    if (s0 == null) {
	        s0 = "";
	    }
	    if (s1 == null) {
	        s1 = "";
	    }
		String[] s0Tokens = s0.split("\\.");
		String[] s1Tokens = s1.split("\\.");
		int min = Math.min(s0Tokens.length, s1Tokens.length);
		
		for (int i = 0; i < min; i++) {
		    //check for ("")
		    String s0Str = s0Tokens[i];
		    String s1Str = s1Tokens[i];
		    if (s0Str == "") {
		        if (s1Str == "") {
		            
		        }
		    }
		    int s0Int = -1;
		    int s1Int = -1;
            boolean s0IsInt = false;
            boolean s1IsInt = false;
            try {
                s0Int = Integer.parseInt(s0Tokens[i]);
                s0IsInt = true;
            }
            catch (NumberFormatException e) {
                log.warn(e);
            }
            try {
                s1Int = Integer.parseInt(s1Tokens[i]);
                s1IsInt = true;
            }
            catch (NumberFormatException e) {
                log.warn(e);
            }
            //special case only if the flags don't match
            if (s0IsInt != s1IsInt) {
                //rule: an unspecified version (null or empty string) is less than zero, but greater than -1
                if (s0IsInt) {
                    if (s0Int >= 0) {
                        return 1;
                    }
                    else {
                        return -1;
                    }
                }
                else {
                    if (s1Int >= 0) {
                        return -1;
                    }
                    else {
                        return 1;
                    }
                }
            }
			if (s0Int < s1Int) {
				return -1;
			} 
			else if (s0Int > s1Int) {
				return 1;
			}
		}
		if (s0Tokens.length > s1Tokens.length) {
			return 1;
		} else if (s0Tokens.length < s1Tokens.length) {
			return -1;
		} else {
			return 0;
		}
	}

}
