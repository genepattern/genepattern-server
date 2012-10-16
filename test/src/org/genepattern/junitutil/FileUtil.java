package org.genepattern.junitutil;

import java.io.File;

public class FileUtil {
    /**
     * Helper class which returns the parent File of this source file.
     * @return
     */
    public static File getSourceDir(final Class<?> clazz) {
        String cname = clazz.getCanonicalName();
        int idx = cname.lastIndexOf('.');
        String dname = cname.substring(0, idx);
        dname = dname.replace('.', '/');
        File sourceDir = new File("test/src/" + dname);
        return sourceDir;
    }
    
    /**
     * Get a File object, from the name of a file which is in the same directory as this source file.
     * @param filename
     * @return
     */
    public static File getSourceFile(final Class<?> clazz, String filename) {
        File p = getSourceDir(clazz);
        return new File(p, filename);
    }

}
