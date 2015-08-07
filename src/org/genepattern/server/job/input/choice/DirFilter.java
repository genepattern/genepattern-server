/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import org.genepattern.server.job.input.choice.ftp.FtpEntry;
import org.genepattern.server.util.FindFileFilter;
import org.genepattern.webservice.ParameterInfo;

/**
 * Helper class for filtering directory listings for dynamic drop-down parameters.
 * This is similar to a Java FilenameFilter, except that in some cases we want to filter 
 * apache commons FTPFiles.
 * 
 * 
 * @author pcarr
 *
 */
public class DirFilter implements FileFilter {

    /**
     * Indicate the type of file to accept.
     * @author pcarr
     *
     */
    public static enum Type {
        /** the filter should accept entries of type 'file'. */
        file,
        /** the filter should accept entries of type 'dir'. */
        dir,
        /** the filter accepts 'file' and 'dir' types. */
        any
    }
    
    private Type type=Type.file;
    private final FindFileFilter globs=new FindFileFilter();
    
    /**
     * Default filter accepts 'Type.file' with the default filename filter. 
     * Files named '*.md5' and 'readme.*' are ignored.
     */
    public DirFilter() {
        this(Type.file, "!*.md5", "!readme.*"); //by default, only include files
    }
    
    public DirFilter(final Type type, String ... globPatterns) {
        this.type=type;
        for(String globPattern : globPatterns) {
            globs.addGlob(globPattern);
        }
    }

    public DirFilter(final ParameterInfo param) {
        this(ChoiceInfo.getChoiceDirFilter(param));
    }

    public DirFilter(String choiceDirFilter) {
        this.type=Type.file;
        _init(choiceDirFilter);
    }

    // should only call this once, from the constructor
    private void _init(String choiceDirFilter) {
        if (!ChoiceInfoHelper.isSet(choiceDirFilter)) {
            //by default, ignore '*.md5' and 'readme.*' files
            globs.addGlob("!*.md5");
            globs.addGlob("!readme.*");
        }
        else {
            //parse this as a ';' separated list of patterns
            final String[] patterns=choiceDirFilter.split(Pattern.quote(";"));
            for(final String patternIn : patterns) {
                //trim if necessary
                final String pattern=patternIn.trim();
                if (!ChoiceInfoHelper.isSet(pattern)) {
                    //skip empty pattern
                }
                else if (pattern.startsWith("type="+Type.dir)) {
                    type=Type.dir;
                }
                else if (pattern.startsWith("type="+Type.file)) {
                    type=Type.file;
                }
                else if (pattern.startsWith("type="+Type.any)) {
                    type=Type.any;
                }
                else {
                    //it's a glob pattern
                    globs.addGlob(pattern);
                }
            }
        }
    }
    
    public boolean accept(final File file) {
        if (type==Type.file) {
            if (!file.isFile()) {
                return false;
            }
        }
        else if (type==Type.dir) {
            if (!file.isDirectory()) {
                return false;
            }
        }
        
        //check for glob patterns
        if (globs==null) {
            return true;
        }
        return globs.accept(file);
    }

    public boolean accept(final FtpEntry ftpEntry) {
        if (!ftpEntry.isDir()) {
            if (!acceptsFile()) {
                return false;
            }
        }
        else {
            if (!acceptsDir()) {
                return false;
            }
        }
        
        return acceptName(ftpEntry.getName());
    }
    
    /**
     * Get the type of the filter, one of 'file', '', or ''.
     * @return
     */
    public Type getType() {
        return type;
    }
    
    /**
     * Get the FileFilter, for filtering by file name.
     * @return
     */
    public FileFilter getFileFilter() {
        return globs;
    }
    
    /**
     * @return true, if this filter accepts a file entry.
     */
    public boolean acceptsFile() {
        return type==Type.any || type==Type.file;
    }

    /**
     * @return true, if this filter accepts a directory entry.
     */
    public boolean acceptsDir() {
        return type==Type.any || type==Type.dir;
    }

    /**
     * @param name, the file name
     * @return true, if this filter accepts a file with the given name.
     */
    public boolean acceptName(final String name) {
        //check for glob patterns
        if (globs==null) {
            return true;
        }
        return globs.accept(new File(name));
    }
}