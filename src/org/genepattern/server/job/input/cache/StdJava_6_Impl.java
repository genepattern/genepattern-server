package org.genepattern.server.job.input.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;

public final class StdJava_6_Impl extends CachedFtpFile {
    private static Logger log = Logger.getLogger(StdJava_6_Impl.class);
    // this is the same default as edtFTP
    final public static int DEFAULT_BUFFER_SIZE = 16384;

    public StdJava_6_Impl(final GpConfig gpConfig, final String urlString) {
        super(gpConfig, urlString);
    }

    public boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting, final int connectTimeout_ms, final int readTimeout_ms) 
            throws IOException, InterruptedException, DownloadException {
        if (toFile==null) {
            throw new DownloadException("Invalid arg: toFile==null");
        }
        if (toFile.exists()) {
            if (log.isDebugEnabled()) { log.debug("toFile exists: "+toFile.getAbsolutePath()); }
            if (deleteExisting) {
                if (log.isDebugEnabled()) { 
                    log.debug("deleting existing file: "+toFile.getAbsolutePath());
                }
                final boolean success=toFile.delete();
                log.debug("success="+success);
                if (!success) {
                    throw new DownloadException("failed to delete existing file: "+toFile.getAbsolutePath());
                }
            }
            else {
                throw new DownloadException("file already exists: "+toFile.getAbsolutePath());
            }
        }

        //if necessary, create parent download directory
        mkdirs(toFile);

        boolean interrupted=false;
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            if (log.isDebugEnabled()) { log.debug("starting download from "+fromUrl); }
            final int bufsize=DEFAULT_BUFFER_SIZE;

            URLConnection connection=fromUrl.openConnection();
            connection.setConnectTimeout(connectTimeout_ms);
            connection.setReadTimeout(readTimeout_ms);
            connection.connect();
            InputStream urlIn=connection.getInputStream();
            in = new BufferedInputStream(urlIn);
            fout = new FileOutputStream(toFile);

            final byte data[] = new byte[bufsize];
            int count;
            while (!interrupted && (count = in.read(data, 0, bufsize)) != -1) {
                fout.write(data, 0, count);
                if (Thread.interrupted()) {
                    interrupted=true;
                }
                else {
                    Thread.yield();
                }
            }
        }
        finally {
            if (in != null) {
                in.close();
            }
            if (fout != null) {
                fout.close();
            }
        }
        if (interrupted) {
            if (log.isDebugEnabled()) {
                log.debug("interrupted download from "+fromUrl);
            }
            throw new InterruptedException();
        }
        if (log.isDebugEnabled()) { log.debug("completed download from "+fromUrl); }
        return true;
    }
}