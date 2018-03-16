/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.util;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;

import com.google.common.collect.ImmutableList;

/**
 * A FileFilter which can have 'include' and 'exclude' patterns.
 * It uses the java.nio.file.PathMatcher class to allow glob patterns 
 * which span multiple directories.  It's implemented as a generic class 
 * to allow additional PathMatcher types to be added without rewriting
 * the accept method. The current implementation uses glob patterns.
 * 
 * <p>
 * The {@link #accept(File)} method follows these rules:
 * <pre>
 * When there are:
 *     (1) no patterns ... return true (accept all files, equivalent to "--include '*'")
 *     
 *     (2) only excludePatterns ...
 *         ... return false on any matching exclude pattern
 *         ... return true if there are no matching exclude patterns
 *     
 *     (3) only includePatterns ...
 *         ... return true on any matching include pattern
 *         ... return false if there are no matching include patterns
 *     
 *     (4) exclude and include patterns ... 
 *         (excludes are matched first)
 *         ... return false on any matching exclude pattern
 *         (include patterns are matched second)
 *         ... return true on any matching include pattern
 *         (only if there are no matching exclude patterns)
 *         ... return false if there are no matching exclude or include patterns
 * </pre>
 * 
 * <p>Note: create a new static factory initializer if you want to use a different
 * PathMatcher implementation.
 * 
 * <p>
 * This filter is designed to be initialized from the config_yaml file, 
 * from a list of zero or more glob patterns. Use the '!' prefix to 
 * denote an exclusion pattern.
 * <pre>
       # template 
       <key>: <list-of-glob-patterns>
       # example: exclude all md5 files
       file-filter: [ "!*.md5" ]
       #example 2: include all java files except for Tests
       file-filter: [ "*.java", "!Test*.java" ]
 * </pre>
 * 
 * @see "https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-"
 * @see "https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob"
 * 
 */
public class GenericFileFilter<K extends PathMatcher> implements FileFilter {

    /**
     * Initialize a glob filter from the GP configuration based on a 
     * @param gpConfig the server configuration
     * @param gpContext the current context
     * @param key the key in the yaml file, e.g. 'jobs.FilenameFilter'
     * @param defaultValue a default value (if desired) when there is no matching value in the config file.
     * 
     * @return a FileFilter
     */
    public static FileFilter initGlobFilter(final GpConfig gpConfig, final GpContext gpContext, final String key, final Value defaultValue, final boolean acceptAll) {
        final Value value=gpConfig.getValue(gpContext, key, defaultValue);
        final List<String> globPatterns;
        if (value != null) {
            globPatterns=value.getValues();
        }
        else if (defaultValue != null) {
            globPatterns=defaultValue.getValues();
        }
        else {
            globPatterns=null;
        }
        return initGlobFilter(acceptAll, globPatterns);
    }

    public static FileFilter initGlobFilter(final String globPattern) {
        final boolean acceptAll=true;
        return initGlobFilter(acceptAll, Arrays.asList(globPattern));
    }

    public static FileFilter initGlobFilter(final boolean acceptAll, final String globPattern) {
        return initGlobFilter(acceptAll, Arrays.asList(globPattern));
    }

    public static FileFilter initGlobFilter(final List<String> globPatterns) {
        final boolean acceptAll=true;
        return initGlobFilter(acceptAll, globPatterns);
    }

    public static FileFilter initGlobFilter(final boolean acceptAll, final List<String> globPatterns) {
        if (globPatterns==null || globPatterns.size()==0) {
            return new GenericFileFilter<PathMatcher>(acceptAll);
        }
        final List<PathMatcher> includeGlobs=new ArrayList<PathMatcher>();
        final List<PathMatcher> excludeGlobs=new ArrayList<PathMatcher>();
        for(String globPattern : globPatterns) {
            final boolean isExclude=globPattern.startsWith("!");
            if (isExclude) {
                // strip leading '!'
                globPattern=globPattern.substring(1);
            }
            final PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
            if (isExclude) {
                excludeGlobs.add(matcher);
            }
            else {
                includeGlobs.add(matcher);
            }
        } 
        return new GenericFileFilter<PathMatcher>(acceptAll, includeGlobs, excludeGlobs);
    }

    /** 
     * When no patterns are specified, set this to false to accept None, set it to true to accept All. 
     * 
     * When creating a filter with no patterns you must choose a default behavior.
     *     ... by default accept all files, or
     *     ... by default accept no files
     */
    private final boolean defaultToAcceptAll;
    private final ImmutableList<K> includePatterns;
    private final ImmutableList<K> excludePatterns;

    private GenericFileFilter() {
        this(true);
    }

    private GenericFileFilter(final boolean acceptAll) {
        this(acceptAll, new ArrayList<K>(), new ArrayList<K>());
    }

    private GenericFileFilter(final boolean acceptAll, final List<K> includePatterns,final List<K>  excludePatterns) {
        this.defaultToAcceptAll=acceptAll;
        this.includePatterns = ImmutableList.copyOf(includePatterns);
        this.excludePatterns = ImmutableList.copyOf(excludePatterns);
    }

    public ImmutableList<K> getIncludePatterns() {
        return includePatterns;
    }
    
    public ImmutableList<K> getExcludePatterns() {
        return excludePatterns;
    }

    public boolean accept(final File file) {
        if (file == null) {
            return false;
        }
        final Path filePath=file.toPath();
        
        //case 0: has no patterns, accept all (equivalent to "--include '*'")
        if (excludePatterns.size() == 0 && includePatterns.size() == 0) {
            return defaultToAcceptAll;
        }

        //case 1: only has excludePatterns ...
        if (excludePatterns.size() > 0 && includePatterns.size() == 0) {
            for(K glob : excludePatterns) {
                if (glob.matches(filePath)) {
                    //return false if there's a match
                    return false;
                }
            }
            //otherwise true
            return true;
        }
        
        //case 2: only has includePatterns 
        if (excludePatterns.size() == 0 && includePatterns.size() > 0) {
            for(final K includePattern : includePatterns) {
                if (includePattern.matches(filePath)) {
                    // ... return true on any match
                    return true;
                }
            }
            // ... return false if there are no matches
            return false;
        }
        
        //case 3: has include and exclude patterns ...
        for(final K excludePattern : excludePatterns) {
            if (excludePattern.matches(filePath)) {
                // ... return false on any matching exclude pattern
                return false;
            }
        }
        for(final K matcher : includePatterns) {
            if (matcher.matches(filePath)) {
                // ... return true on any matching include pattern, only if there
                //         are no matching exclude patterns
                return true;
            }
        }
        
        //otherwise, false
        return false;
    }
}
