/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.oro.io.GlobFilenameFilter;

/**
 * Filter out files from the jobResults directory based on 
 * a list of filenames to match exactly and/or 
 * a pattern specified using GLOB syntax.
 * 
 * <p>
 * Some examples, 
 * <pre>
   jobs.FilenameFilter=.*
   jobs.FilenameFilter=.nfs*
 * </pre>
 * 
 * @author pcarr
 */
public class JobResultsFilenameFilter implements FilenameFilter {
    public static final String KEY = "jobs.FilenameFilter";
    
    private Set<String> exactMatches = new TreeSet<String>();
    private List<GlobFilenameFilter> globs = new ArrayList<GlobFilenameFilter>();
    
    public void addExactMatch(String filename) {
        exactMatches.add(filename);
    }
    
    public void clearExactMatches() {
        exactMatches.clear();
    }
    
    /**
     * @param patterns, a comma-separated list of glob patterns, to apply in order. 
     *     A Null or empty input means don't use a glob pattern.
     *     E.g. setGlob(".nfs*,.lsf*");
     */
    public void setGlob(String patterns) {
        globs.clear();
        if (patterns == null || patterns.trim().length() == 0) {
            return;
        }
        String[] globs = patterns.split(",");
        for(String glob : globs) {
            addGlob(glob);
        }
    }

    /**
     * Null or empty input means don't use glob pattern.
     * @param pattern
     */
    public void _setGlob(String pattern) {
        //null or empty string means ignore glob pattern
        if (pattern == null || pattern.trim().length() == 0) {
            globs = null;
            return;
        }
        //init if necessary
        globs.clear();
        addGlob(pattern);
    }
    
    public void addGlob(String pattern) {
        GlobFilenameFilter glob =  new GlobFilenameFilter();            
        glob.setFilterExpression(pattern);
        globs.add(glob);
    }

    //implement FilenameFilter using GlobFilenameFilter
    public boolean accept(File dir, String name) {
        if (exactMatches.contains(name)) {
            return false;
        }
        for(GlobFilenameFilter glob : globs) {
            //DO NOT accept the file if the pattern matches the glob
            if (glob.accept(dir, name)) {
                return false;
            }
        }
        return true;
    }
}
