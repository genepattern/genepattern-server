/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.oro.io.GlobFilenameFilter;

/**
 * A FileFilter developed for use from the FindCommand.
 * 
 * @author pcarr
 */
public class FindFileFilter implements FileFilter { 
    private List<GlobFilenameFilter> globs = new ArrayList<GlobFilenameFilter>();
    private List<GlobFilenameFilter> ignoreGlobs = new ArrayList<GlobFilenameFilter>();
    
    public void clear() {
        globs.clear();
        ignoreGlobs.clear();
    }
    
    /** 
     * If the pattern is preceded by a '!' character, it means it's an exclusion pattern.
     */
    public void addGlob(String pattern) {
        boolean ignore = false;
        if (pattern.startsWith("!")) {
            pattern = pattern.substring(1);
            ignore = true;
        } 
        GlobFilenameFilter glob =  new GlobFilenameFilter(); 
        glob.setFilterExpression(pattern);
        if (ignore) {
            ignoreGlobs.add(glob);
        }
        else {
            globs.add(glob);
        }
    }

    public boolean accept(File arg0) {
        if (arg0 == null) {
            return false;
        }
        
        //case 0: if no patterns are set, return true
        if (ignoreGlobs.size() == 0 && globs.size() == 0) {
            return true;
        }

        //case 1: if 'ignoreGlobs' are set, but nothing else ...
        if (ignoreGlobs.size() > 0 && globs.size() == 0) {
            for(GlobFilenameFilter glob : ignoreGlobs) {
                if (glob.accept(arg0)) {
                    //return false if there's a match
                    return false;
                }
            }
            //otherwise true
            return true;
        }
        
        //case 2: if 'ignoreGlobs' are not set ...
        if (ignoreGlobs.size() == 0 && globs.size() > 0) {
            for(GlobFilenameFilter glob : globs) {
                if (glob.accept(arg0)) {
                    //if there's a match, return true
                    return true;
                }
            }
            //otherwise false
            return false;
        }
        
        //case 3: both ignoreGlobs and globs are set 
        for(GlobFilenameFilter ignoreGlob : ignoreGlobs) {
            if (ignoreGlob.accept(arg0)) {
                //if it matches an ignore, return false
                return false;
            }
        }
        for(GlobFilenameFilter glob : globs) {
            if (glob.accept(arg0)) {
                //if it matches a glob, return true
                return true;
            }
        }
        
        //otherwise, false
        return false;
    }
}
