package org.genepattern.util;

import java.io.File;

import org.genepattern.server.CommandLineParserTest;

/**
 * Utility methods for unit testing.
 * 
 * @author pcarr
 */
public class TestUtil {

    /**
     * Helper class which returns the parent File of this source file.
     * @return
     */
    public static File getSourceDir() {
        String cname = CommandLineParserTest.class.getCanonicalName();
        int idx = cname.lastIndexOf('.');
        String dname = cname.substring(0, idx);
        dname = dname.replace('.', '/');
        File sourceDir = new File("test/src/" + dname);
        return sourceDir;
    }

}
