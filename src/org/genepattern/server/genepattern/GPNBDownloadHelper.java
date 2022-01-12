package org.genepattern.server.genepattern;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.util.FTPDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class GPNBDownloadHelper {
    final static String ENBLED_KEY = "notebook.enableDownloads";
    final static String BASE_URL_KEY = "notebook.url";
    final static String API_TOKEN_KEY = "notebook.token";

    static boolean isGPNBFile(GpContext context, URI uri) {
        GpConfig gpConfig = ServerConfigurationFactory.instance();
        String gpnbBaseURL = gpConfig.getGPProperty(context, GPNBDownloadHelper.BASE_URL_KEY, null);
        if (!uri.toString().startsWith(gpnbBaseURL + "/user/")) return false;     // Starts with the correct domain
        return uri.getPath().split("/").length >= 5;                        // Path structured correctly
    }

    static URI constructDownloadURL(GpContext context, URI uri) throws IOException {
        GpConfig gpConfig = ServerConfigurationFactory.instance();
        String gpnbAPIToken = gpConfig.getGPProperty(context, GPNBDownloadHelper.API_TOKEN_KEY, null);
        if (gpnbAPIToken == null) throw new IOException("No API token specified when constructing GPNB download URL");
        try {
            String downloadURL = uri.getScheme() + "://" + uri.getAuthority() + "/services/download/" + gpnbAPIToken + uri.getPath();
            return new URI(downloadURL);
        }
        catch (URISyntaxException e) {
            throw new IOException("Unable to construct GPNB download URL");
        }
    }

    static InputStream constructInputStream(URI uri) throws IOException {
        if (uri.getScheme().equalsIgnoreCase("ftp")) return constructFTPInputStream(uri);
        return uri.toURL().openConnection().getInputStream();
    }
    
    static InputStream constructFTPInputStream(URI uri) throws IOException {
        try {
            FTPDownloader ftpDownloader = new FTPDownloader(uri.getHost(), "anonymous", "genepattern@ucsd.edu");
            // replace the inputStream
            return ftpDownloader.downloadFileStream(uri.getPath());
        } catch (Exception e){
            throw new IOException(e);
        }
    }
    
    
}
