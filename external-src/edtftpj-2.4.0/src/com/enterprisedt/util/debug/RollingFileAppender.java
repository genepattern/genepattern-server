/**
 * 
 *  Copyright (C) 2010 Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  Bug fixes, suggestions and comments should be sent to support@enterprisedt.com
 *
 *  Change Log:
 *
 *    $Log: RollingFileAppender.java,v $
 *    Revision 1.4  2010-11-04 04:06:42  bruceb
 *    better logging in rollover()
 *
 *    Revision 1.3  2010-11-04 01:07:05  bruceb
 *    fix bug where size of file wasn't checked initially
 *
 *    Revision 1.2  2010-10-22 03:53:26  bruceb
 *    remove stderr printing
 *
 *    Revision 1.1  2010-10-15 06:10:29  bruceb
 *    rolling file appender changes
 *
 */
package com.enterprisedt.util.debug;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *  Rolling file appender that moves the log file to
 *  a backup file once it exceeds a certain size.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.4 $
 */
public class RollingFileAppender extends FileAppender {
    
    /**
     *  Revision control id
     */
    public final static String cvsId = "@(#)$Id: RollingFileAppender.java,v 1.4 2010-11-04 04:06:42 bruceb Exp $";

    private static long DEFAULT_MAXSIZE = 10 * 1024 * 1024;

    private static int CHECK_THRESHOLD_BYTES = 1024 * 5;
    
    private static String LINE_SEP = System.getProperty ("line.separator" );
    
    /*
     * Maximum size of file
     */
    private long maxFileSize = DEFAULT_MAXSIZE;

    /*
     * Record of bytes written in this check cycle
     */
    private int thresholdBytesWritten = 0;

    /*
     * Maximum number of backup files
     */
    private int maxSizeRollBackups = 1;
    
    /**
     * Constructor
     * 
     * @param file          file to log to
     * @param maxFileSize   maximum size of log file in bytes
     * @throws IOException
     */
    public RollingFileAppender(String file, long maxFileSize) throws IOException {
        super(file);
        this.maxFileSize = maxFileSize;
        checkSizeForRollover();
    }
    
    /**
     * Constructor
     * 
     * @param file      file to log to
     * @throws IOException
     */
    public RollingFileAppender(String file) throws IOException {
        super(file);
        checkSizeForRollover();
    }
    
    
    /**
     * Get the max size of a backup file
     * 
     * @return int
     */
    public int getMaxSizeRollBackups() {
        return maxSizeRollBackups;
    }

    /**
     * Set the maximum number of backup files
     * 
     * @param maxSizeRollBackups  number of files
     */
    public void setMaxSizeRollBackups(int maxSizeRollBackups) {
        this.maxSizeRollBackups = maxSizeRollBackups;
    }

    /**
     * Get the maximum number of backup files
     * 
     * @return int
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Log a message
     * 
     * @param msg  message to log
     */
    public synchronized void log(String msg) {
        if (!closed) {
            checkForRollover();
            log.println(msg);
            log.flush();
            thresholdBytesWritten += msg.length();
        }
    }
    
    /* (non-Javadoc)
     * @see com.enterprisedt.util.debug.Appender#log(java.lang.Throwable)
     */
    public synchronized void log(Throwable t) {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         t.printStackTrace(pw);
         String msg = sw.toString();
         if (!closed) {
             checkForRollover();
             log.println(msg);
             log.flush();
             thresholdBytesWritten += msg.length();
         }
    }
    
    
    private void checkForRollover() {
        // only check every so often to enhance performance
        if (thresholdBytesWritten < CHECK_THRESHOLD_BYTES)
            return;

        thresholdBytesWritten = 0;
        
        checkSizeForRollover();
    }
    
    
    private void checkSizeForRollover() {
        try {
            File f = new File(getFile());
            // check if bigger
            if (f.length() > maxFileSize) {
                rollover();
            }
        }
        catch (Exception ex) {
            String msg = "Failed to rollover log files: " + ex.getMessage();
            System.err.println(msg);
        }
    }
    

    private void rollover() throws IOException {
        close();
        StringBuffer msg = new StringBuffer();
        Exception ex = null;
        try {
            File master = new File(getFile());
            if (maxSizeRollBackups == 0) {
                if (!master.delete())
                    msg.append("Failed to delete file: " + master.getAbsolutePath() + LINE_SEP);
            }
            else // roll all files
            {
                // delete highest if exists
                File f = new File(getFile() + "." + maxSizeRollBackups);
                if (f.exists()) {
                    if (!f.delete())
                        msg.append("Failed to delete file: " + f.getAbsolutePath() + LINE_SEP);
                }
                for (int i = maxSizeRollBackups - 1; i > 0; i--) {
                    f = new File(getFile() + "." + i);
                    if (f.exists()) {
                        File renamed = new File(getFile() + "." + (i + 1));
                        if (!f.renameTo(renamed))
                            msg.append("Failed to rename file: " + f.getAbsolutePath() + " to " + renamed.getAbsolutePath() + LINE_SEP);
                    }
                }
                f = new File(getFile() + ".1");
                if (!master.renameTo(f))
                    msg.append("Failed to rename file: " + master.getAbsolutePath() + " to " + f.getAbsolutePath() + LINE_SEP);
            }
        }
        catch (Exception e) {
            ex = e;
        }
        finally {
            open();
            if (ex != null) {
                msg.append("Failed to rollover log files: " + ex.getMessage() + LINE_SEP);                
            }
            if (msg.length() > 0) {
                log.println(msg.toString());
                log.flush();
                thresholdBytesWritten += msg.length();
            }
        }
    }
    
 
}
