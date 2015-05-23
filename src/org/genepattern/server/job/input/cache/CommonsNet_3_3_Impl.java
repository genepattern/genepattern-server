package org.genepattern.server.job.input.cache;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.genepattern.server.config.GpConfig;

public final class CommonsNet_3_3_Impl extends CachedFtpFile {

    public CommonsNet_3_3_Impl(final GpConfig gpConfig, final String urlString) {
        super(gpConfig, urlString);
    }

    @Override
    public boolean downloadFile(URL fromUrl, File toFile, boolean deleteExisting, int connectTimeout_ms, int readTimeout_ms) throws IOException, InterruptedException, DownloadException { 
        if (deleteExisting==false) {
            throw new DownloadException("deleteExisting must be false");
        }
        FileUtils.copyURLToFile(fromUrl, toFile, connectTimeout_ms, readTimeout_ms);
        return true;
    }
}