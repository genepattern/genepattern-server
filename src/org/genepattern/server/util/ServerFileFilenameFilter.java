package org.genepattern.server.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.oro.io.GlobFilenameFilter;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.executor.CommandProperties;

/**
 * Filter out files from the server file path based on a pattern specified using GLOB syntax.
 * 
 * Note: This is a copy of the JobResultsFilenameFilter.
 * 
 * <p>
 * Some examples, 
 * <pre>
   server.browse.file.system.filter: ".*"
   server.browse.file.system.filter: [".DS_Store", "Thumbs.db", ".*", "*~"]
 * </pre>
 * 
 * @author pcarr
 */
public class ServerFileFilenameFilter implements FilenameFilter {
    public static final String KEY = "server.browse.file.system.filter";
    
    private List<GlobFilenameFilter> globs = new ArrayList<GlobFilenameFilter>();
    
    //factory methods
    /**
     * Initialize a FilenameFilter for the given user. This filter is to be used when browser server file paths
     * from the GUI.
     * 
     * @param userContext
     * @return
     */
    public static FilenameFilter getServerFilenameFilter(Context userContext) {
        if (userContext==null) {
            //TODO: log an error?
            //TODO: use hard-coded default values?
            return new ServerFileFilenameFilter();
        }
        ServerFileFilenameFilter filter = new ServerFileFilenameFilter();
        CommandProperties.Value globPatterns = ServerConfiguration.instance().getValue(userContext, KEY);
        for(String glob : globPatterns.getValues()) {
            filter.addGlob(glob);
        }
        return filter;
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
        for(GlobFilenameFilter glob : globs) {
            //DO NOT accept the file if the pattern matches the glob
            if (glob.accept(dir, name)) {
                return false;
            }
        }
        return true;
    }
}
