package org.genepattern.server.job.input.choice;

import java.io.File;
import java.io.FileFilter;

/**
 * A filter for local directory listings.
 * @author pcarr
 *
 */
public class LocalDirFilter extends DirFilter implements FileFilter {
    public LocalDirFilter(final String choiceDirFilter) {
        super(choiceDirFilter);
    }

    @Override
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
}