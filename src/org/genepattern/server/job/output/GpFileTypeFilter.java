package org.genepattern.server.job.output;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * For filtering files from the job results directory.
 * When the FilenameFilter accepts the file it is added to the list of job result files, 
 * otherwise it is recorded as a hidden file in the database.
 * 
 * @author pcarr
 *
 */
public interface GpFileTypeFilter extends FilenameFilter {
    /**
     * Determine the GpFileType based on the type of the file.
     * 
     * @param jobDir
     * @param relativeFile
     * @param attrs
     * @return
     */
    GpFileType getGpFileType(File jobDir, File relativeFile, BasicFileAttributes attrs);
}
