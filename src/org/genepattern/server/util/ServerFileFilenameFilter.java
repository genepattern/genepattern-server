package org.genepattern.server.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.oro.io.GlobFilenameFilter;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.config.ServerConfigurationFactory;
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
    final static private Logger log = Logger.getLogger(ServerFileFilenameFilter.class);

    public static final String KEY = "server.browse.file.system.filter";
    public static final String[] GLOBS = {
        "Thumbs.db",
        ".DS_Store", //i know, this is redundant
        ".*",
        "*~"
    };
    
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
            log.error("userContext==null");
        }
        else if (userContext.getUserId()==null || userContext.getUserId().length()==0) {
            log.error("userContext.userId is not set");
        }
        CommandProperties.Value globPatterns = ServerConfigurationFactory.instance().getValue(userContext, KEY);
        ServerFileFilenameFilter filter=new ServerFileFilenameFilter();
        if (globPatterns == null) {
            //not set, use the default values 
            if (GLOBS != null) {
                for(String glob : GLOBS) {
                    filter.addGlob(glob);
                }
            }
            return filter;
        }
        else if (globPatterns.getNumValues()==1 && !globPatterns.isFromCollection()) {
            //special-case, the property was initialized from a String (instead of an array)
            // in this case, we check for comma-separated values
            filter.setGlob(globPatterns.getValue());
        }
        else {
            for(String glob : globPatterns.getValues()) {
                filter.addGlob(glob);
            }
        }
        return filter;
    }
    
    public static FilenameFilter getServerFilenameFilter(final String glob) {
        ServerFileFilenameFilter filter=new ServerFileFilenameFilter();
        filter.setGlob(glob);
        return filter;
    }
    
    /**
     * @param patterns, a comma-separated list of glob patterns, to apply in order. 
     *     A Null or empty input means don't use a glob pattern.
     *     E.g. setGlob(".nfs*,.lsf*");
     */
    private void setGlob(String patterns) {
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
    private void _setGlob(String pattern) {
        //null or empty string means ignore glob pattern
        if (pattern == null || pattern.trim().length() == 0) {
            globs = null;
            return;
        }
        //init if necessary
        globs.clear();
        addGlob(pattern);
    }
    
    private void addGlob(String pattern) {
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
