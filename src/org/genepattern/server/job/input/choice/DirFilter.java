package org.genepattern.server.job.input.choice;

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

    public static enum Type {
        file,
        dir,
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
    private boolean inited=false;
    private void _init() {
        if (inited) {
            log.error("Must call this method once and only once, from the constructor");
        }
        inited=true;
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
}