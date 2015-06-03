package org.genepattern.server.job.input.cache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.job.input.choice.ftp.FtpDirLister;
import org.genepattern.server.job.input.choice.ftp.FtpDirListerCommonsNet_3_3;
import org.genepattern.server.job.input.choice.ftp.FtpDirListerEdtFtpJ;

public class CachedFtpFileFactory {
    private static final Logger log = Logger.getLogger(CachedFtpFileFactory.class);

    private CachedFtpFileFactory() {}

    private static class LazyHolder {
        private static final CachedFtpFileFactory INSTANCE = new CachedFtpFileFactory();
    }

    public static CachedFtpFileFactory instance() {
        return LazyHolder.INSTANCE;
    }
    
    private final ExecutorService interruptionService=Executors.newCachedThreadPool();
    public void shutdownNow() {
        interruptionService.shutdownNow();
    }
    
    public CachedFtpFile newCachedFtpFile(final GpConfig gpConfig, final GpContext gpContext, final String urlString) {
        String str=gpConfig.getGPProperty(gpContext, CachedFtpFileType.PROP_FTP_DOWNLOADER_TYPE, CachedFtpFileType.EDT_FTP_J.name());
        CachedFtpFileType type=null;
        try {
            type=CachedFtpFileType.valueOf(str);
        }
        catch (Throwable t) {
            log.error(t);
            type=CachedFtpFileType.EDT_FTP_J;
        }
        return newCachedFtpFile(type, gpConfig, urlString);
    }

    public CachedFtpFile newCachedFtpFile(final CachedFtpFileType type, final GpConfig gpConfig, final String urlString) {
        if (type==null) {
            log.error("type==null");
            return CachedFtpFileType.EDT_FTP_J.newCachedFtpFile(gpConfig, urlString);
        }
        return type.newCachedFtpFile(gpConfig, urlString);
    }

    /**
     * This FTP downloader is implemented with standard Java 6 libraries.
     * @param urlString
     * @return
     */
    public CachedFtpFile newStdJava6Impl(final GpConfig gpConfig, final String urlString) {
        return new StdJava_6_Impl(gpConfig, urlString);
    }
    
    /** @deprecated, should pass in a GpConfig */
    public CachedFtpFile newApacheCommonsImpl(final String urlString) {
        return newApacheCommonsImpl(ServerConfigurationFactory.instance(), urlString);
    }
    
    /**
     * This FTP downloader uses the apache commons FTPClient. 
     * @param urlString
     * @return
     */
    public CachedFtpFile newApacheCommonsImpl(final GpConfig gpConfig, final String urlString) {
        return new CommonsNet_3_3_Impl(gpConfig, urlString);
    }
    
    /**
     * This FTP downloader uses the edtFTPj library, with additional support
     * for handling interrupted exceptions.
     * 
     * @param urlString
     * @return
     */
    public CachedFtpFile newEdtFtpJImpl(final GpConfig gpConfig, final String urlString) {
        return new EdtFtpJImpl(gpConfig, urlString, interruptionService);
    }
    
    /**
     * strictly for debugging and testing, this implementation uses the basic static 
     * method available from the edtFTPj library. It is not wrapped in a separate
     * thread for handling interrupted exceptions.
     * 
     * @param urlString
     * @return
     */
    public CachedFtpFile newEdtFtpJImpl_simple(final GpConfig gpConfig, final String urlString) {
        return new EdtFtpJ_simple(gpConfig, urlString);
    }

    public static FtpDirLister initDirListerFromConfig(GpConfig gpConfig, GpContext gpContext, final boolean passiveMode) {
        if (gpConfig==null) {
            gpConfig=ServerConfigurationFactory.instance();
        }
        final String clientType=gpConfig.getGPProperty(gpContext, CachedFtpFileType.PROP_FTP_DOWNLOADER_TYPE, CachedFtpFileType.EDT_FTP_J.name());
        if (log.isDebugEnabled()) {
            log.debug("Initializing ftpDirLister from clientType="+clientType);
        }
        if (clientType.startsWith("EDT_FTP_J")) {
            if (log.isDebugEnabled()) {
                log.debug("initializing EDT_FTP_J directory lister");
            }
            // use EDT_FTP_J directory lister
            return FtpDirListerEdtFtpJ.createFromConfig(gpConfig, gpContext, passiveMode);
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug("initializing CommonsNet directory lister");
            }
            FtpDirLister dirLister = FtpDirListerCommonsNet_3_3.createFromConfig(gpConfig, gpContext, passiveMode);
            return dirLister;
        }
    }

    /**
     * Initialize an ftp directory lister. 
     * Customization options can be set in the config_yaml file.
     * 
     * <pre>
    # By default use EDT_FTP_J client, optionally revert to COMMONS_NET_3_3 client.
    ftpDownloader.type: EDT_FTP_J | COMMONS_NET_3_3
    gp.server.choice.ftp_username: "anonymous"
    gp.server.choice.ftp_password: "gp-help@broadinstitute.org"
    # amount of time in milliseconds
    gp.server.choice.ftp_socketTimeout: 15000
    # only for COMMONS_NET_3_3 client
    gp.server.choice.ftp_dataTimeout: 15000
     * </pre>
     * 
     * @param gpConfig
     * @param gpContext
     * @return
     */
    public static FtpDirLister initDirListerFromConfig(GpConfig gpConfig, GpContext gpContext) {
        if (gpConfig==null) {
            gpConfig=ServerConfigurationFactory.instance();
        }
        // special-case, initialize default passiveMode from gpConfig
        boolean passiveMode=gpConfig.getGPBooleanProperty(gpContext, FtpDirLister.PROP_FTP_PASV, true);
        return CachedFtpFileFactory.initDirListerFromConfig(gpConfig, gpContext, passiveMode);
    }

}