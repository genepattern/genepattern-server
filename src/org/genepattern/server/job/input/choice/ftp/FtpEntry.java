package org.genepattern.server.job.input.choice.ftp;

import java.util.Objects;

/**
 * The return value for the FtpDirLister#listFiles interface.
 * This is needed to pass in the isDir flag when building up a list of choices for
 * a drop-down menu on the job input form.
 * 
 * @author pcarr
 *
 */
public class FtpEntry {
    private final boolean isDir;
    /** the file name */
    private final String name;
    /** the full url */
    private final String value;
    
    public FtpEntry(final String fileName, final String ftpUrl) {
        this(fileName, ftpUrl, false);
    }

    public FtpEntry(final String fileName, final String ftpUrl, final boolean isDir) {
        this.name=fileName;
        this.value=ftpUrl;
        this.isDir=isDir;
    }
    
    public String getName() {
        return name;
    }
    public String getValue() {
        return value;
    }
    
    public boolean isDir() {
        return isDir;
    }
    
    public String toString() {
        return value;
    }
    
    public boolean equals(Object obj) {
        if (obj==null) {
            return false;
        }
        if (!(obj instanceof FtpEntry)) {
            return false;
        }
        FtpEntry arg= (FtpEntry) obj;
        return Objects.equals(name, arg.name)
            && Objects.equals(value, arg.value) 
            && Objects.equals(isDir, arg.isDir);
    }

    public int hashCode() {
        return Objects.hash(name, value, isDir);
    }
}
