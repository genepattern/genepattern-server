package org.genepattern.server.job.input.cache;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.genepattern.server.config.GpConfig;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferClient;

public final class EdtFtpJ_simple extends CachedFtpFile {
    public EdtFtpJ_simple(final GpConfig gpConfig, final String urlString) {
        super(gpConfig, urlString);
    }

    @Override
    public boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting, final int connectTimeout_ms, final int readTimeout_ms) throws IOException, InterruptedException, DownloadException {
        if (deleteExisting==false) {
            throw new DownloadException("deleteExisting must be false");
        }
        mkdirs(toFile);
        try {
            FileTransferClient.downloadURLFile(toFile.getAbsolutePath(), fromUrl.toExternalForm());
            return true;
        }
        catch (FTPException e) {
            throw new DownloadException("Error downloading file from "+fromUrl, e);
        }
    }
}