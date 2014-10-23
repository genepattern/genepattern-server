package org.genepattern.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Utility class for GenePattern input files.
 * 
 * @author pcarr
 */
public class FileUtil {
    private static Logger log = Logger.getLogger(FileUtil.class);

    /**
     * Replace all File.separator characters with the forward slash ('/').
     * @param file
     * @return
     */
    public static final String getPathForwardSlashed(final File file) {
        return getPath(file, "/");
    }

    /**
     * Replace all File.separator characters with the given separatorChar.
     * 
     * @param file, e.g. "dir\\subdir\\file.txt"
     * @param separatorChar, e.g. "/"
     * @return
     */
    public static final String getPath(final File file, final String separatorChar) {
        String pathStr = file.getPath();
        return replaceSeparatorChars(pathStr, separatorChar);
    }
    
    /**
     * Replace all File.separator characters with the given separatorChar.
     * 
     * @param file, e.g. "dir\\subdir\\file.txt"
     * @param separatorChar, e.g. "/"
     * @return
     */
    public static final String getPath(final Path path, final String separatorChar) {
        String pathStr=path.toString();
        return replaceSeparatorChars(pathStr, separatorChar);
    }
    
    public static final String replaceSeparatorChars(final String pathStr, final String separatorChar) {
        String r = pathStr.replace( File.separator, separatorChar);
        return r;
    }

    /**
     * Helper method to compare two files, returning true if they are equal, or if their canonical paths are equal.
     */
    public static boolean fileEquals(File f1, File f2) {
        if (f1 == null) {
            //special case: null == null
            return f2 == null;
        }
        if (f1.equals(f2)) {
            return true;
        }
        
        //special case: check canonical paths
        try {
            String f1path = f1.getCanonicalPath();
            String f2path = f2.getCanonicalPath();
            return f1path != null && f1path.equals(f2path);
        }
        catch (IOException e) {
            log.error("Error getting canonical path: "+e.getLocalizedMessage(), e);
            return false;
        }
    }

    /**
     * Is the given input file a web upload file?
     */
    public static boolean isWebUpload(File inputFile) {
        if (inputFile == null) {
            log.error("Invalid input, null");
            return false;
        }
        File inputFileParent = inputFile.getParentFile();
        File inputFileGrandParent = inputFileParent == null ? null : inputFileParent.getParentFile();

        File webUploadDirectory = new GpConfig.Builder().build().getTempDir(null);
        return fileEquals(inputFileGrandParent, webUploadDirectory);
    }
    
    /**
     * Is the given input file a soap upload file?
     */
    public static boolean isSoapUpload(File inputFile) {
        if (inputFile == null) {
            log.error("Invalid input, null");
            return false;
        }
        File inputFileParent = inputFile.getParentFile();
        File inputFileGrandParent = inputFileParent == null ? null : inputFileParent.getParentFile();
        File soapAttachmentDir = new File(System.getProperty("soap.attachment.dir"));
        return fileEquals(inputFileGrandParent, soapAttachmentDir);
    }

    /**
     * If the given file is in the user dir for the given user, return the path to the file, relative
     * to the user dir. Otherwise, return null.
     * 
     * @param userContext
     * @param fileObj
     * @return
     */
    public static String getUserUploadPath(GpContext userContext, File fileObj) {
        File userDir = ServerConfigurationFactory.instance().getUserUploadDir(userContext);
        String relativePath = FileUtil.getRelativePath(userDir, fileObj);
        if (relativePath != null) {
            return relativePath;
        }
        return null;
    }
    
    /**
     * @param userContext
     * @param fileObj
     * @return true if the given file is in the user upload directory
     */
    public static boolean isInUserUploadDir(GpContext userContext, File fileObj) {
        File userUploadDir = ServerConfigurationFactory.instance().getUserUploadDir(userContext);
        return isDescendant(userUploadDir, fileObj);
    }

    /**
     * Is the given child file a descendant of the parent file,
     * based on comparing canonical path names.
     * 
     * Returns true if the files are 
     * 
     * @param parent
     * @param child
     * @return true if the files are equal or if the child is a descendant of the parent.
     * 
     * Note: swallows IOException, which can occur when calling getCanonicalPath, logs the error and returns false.
     * TODO: doesn't follow symbolic links
     * 
     * See: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5002170
     */
    public static boolean isDescendant(File parent, File child) {
        if (child.equals(parent)) {
            return true;
        } 

        try {
            //assume canonical paths are sufficient
            String canonicalParent = parent.getAbsoluteFile().getCanonicalPath();
            String canonicalChild = child.getAbsoluteFile().getCanonicalPath();
            if (canonicalChild.startsWith(canonicalParent)) {
                return true;
            }

        }
        catch (IOException e) {
            log.error("Error in isDescendant(parent="+ parent.getAbsolutePath()
                    +", child="+child.getAbsolutePath()
                    +"): "+e.getLocalizedMessage(), e);
        }
        return false;
    }

    /**
     * Get the relative path from the parent to the child, if, and only if, the child is a descendant of the parent.
     * Otherwise, return null.
     * 
     * @param parent
     * @param child
     * @return
     */
    public static String getRelativePath(File parent, File child) { 
        try {
            File relativePath = relativizePath(parent, child);
            if (relativePath != child) {
                return relativePath.getPath();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            log.error(e);
            return null;
        }
    }
    
    /**
     * Transform the given path into a path relative to the given parent directory.
     * 
     * @param childFile, The path.
     * @param parentDir, The dir.
     * @return If childFile is descendant of the parentDir, the path relative to the parentDir, Otherwise the childFile unmodified.
     * @throws IOException, in the event of system errors with {@link File#getCanonicalFile()}
     */
    public static File relativizePath(File parentDir, File childFile)
            throws IOException {
        File parent = parentDir.getCanonicalFile();
        File child = childFile.getCanonicalFile();
        if (!child.getPath().startsWith(parent.getPath())) {
            return childFile;
        }
        if (child.equals(parent)) {
            return childFile;
        }
        List<String> names = new ArrayList<String>();
        while (!child.equals(parent)) {
            names.add(child.getName());
            child = child.getParentFile();
        }
        File f = new File(names.get(names.size() - 1));
        for (int i = names.size() - 2; i >= 0; i--) {
            f = new File(f, names.get(i));
        }
        return f;
    }

}
