package org.genepattern.server.job.input.choice;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
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
public class DirFilter {
    private static Logger log = Logger.getLogger(DirFilter.class);

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
    
    protected DirFilter.Type type=Type.file;
    final protected String choiceDirFilter;
    final protected FindFileFilter globs=new FindFileFilter();
    
    public DirFilter() {
        this.choiceDirFilter=null; //not set
        _init();
    }
    public DirFilter(final ParameterInfo param) {
        this.choiceDirFilter=ChoiceInfo.getChoiceDirFilter(param);
        _init();
    }
    public DirFilter(final String choiceDirFilter) {
        this.choiceDirFilter=choiceDirFilter;
        _init();
    }

    // should only call this once, from the constructor
    private void _init() {
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
    public boolean acceptsName(final String name) {
        //check for glob patterns
        if (globs==null) {
            return true;
        }
        return globs.accept(new File(name));
    }
}