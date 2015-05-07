/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

/**
 * Utility class for copying a {@code java.io.InputStream} to a {@code java.io.OutputStream}.
 * 
 * @author jramirez, pcarr
 */
public class CopyStreamThread extends Thread {
    private static Logger log = Logger.getLogger(CopyStreamThread.class);

    private InputStream source;
    private OutputStream dest;
    private static final int buflen = 512;
    private volatile boolean interrupted = false;
    private boolean wroteBytes = false;

    /**
     * Creates a new instance.
     */
    public CopyStreamThread(InputStream source, OutputStream dest) {
        super(CopyStreamThread.class.getName());
        this.source = source;
        this.dest = dest;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        interrupted = true;
    }
    
    public boolean wroteBytes() {
        return wroteBytes;
    }

    /**
     * Copies output from an output stream into an input stream.
     */
    public void run() {
        byte[] buf = new byte[buflen];
        try {
            while (!hasBeenInterrupted()) {
                int n = source.read(buf, 0, buflen);
                if (n < 0) {
                    break;
                }
                wroteBytes = true;
                dest.write(buf, 0, n);
            }
        } 
        catch (IOException e) {
            if (!hasBeenInterrupted())
                log.error("Processing output stream", e);
        }
        try {
            dest.flush();
            dest.close();
            source.close();
        } 
        catch (IOException e) {
            log.error("Problem cleaning up", e);
        }
    }

    // utility method
    private boolean hasBeenInterrupted() {
        return interrupted;
    }
} 