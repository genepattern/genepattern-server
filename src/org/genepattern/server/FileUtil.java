package org.genepattern.server;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.DirectoryManager;

/**
 * Utility class for GenePattern input files.
 * 
 * @author pcarr
 */
public class FileUtil {
    private static Logger log = Logger.getLogger(FileUtil.class);

    /**
     * Helper method to compare two files, returning true if they are equal, or if their canonical paths are equal.
     */
    private static boolean fileEquals(File f1, File f2) {
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
        File webUploadDirectory = new File(System.getProperty("java.io.tmpdir"));
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
     * Is the given input file included in the modules libdir?
     * @param inputFile
     * @return
     */
    public static boolean isInLibdir(File inputFile) {
        if (inputFile == null) {
            log.error("Invalid input, null");
            return false;
        }
        File inputFileParent = inputFile.getParentFile();
        File inputFileGrandParent = inputFileParent == null ? null : inputFileParent.getParentFile();
        String libdirPath = DirectoryManager.getLibDir();
        File libdir = new File(libdirPath);
        return fileEquals(inputFileGrandParent, libdir);
    }

}
