package org.genepattern.server.job.input.cache;

import org.genepattern.server.config.GpConfig;

/**
 * Enum of downloaders, used to specify which implementation to use.
 * @author pcarr
 */
public enum CachedFtpFileType {

    /** Hand coded implementation with Java 6 standard library methods */
    JAVA_6 {
        @Override
        public CachedFtpFile newCachedFtpFile(final GpConfig gpConfig, final String urlString) {
            return CachedFtpFileFactory.instance().newStdJava6Impl(gpConfig, urlString);
        }
    },
    /** Apache Commons Net 3.3 implementation */
    COMMONS_NET_3_3 {
        @Override
        public CachedFtpFile newCachedFtpFile(final GpConfig gpConfig, String urlString) {
            return CachedFtpFileFactory.instance().newApacheCommonsImpl(gpConfig, urlString);
        }
    },
    /** edtFTPj, Enterprise Distributed Technologies library, with additional handling for interrupted exceptions */
    EDT_FTP_J {
        @Override
        public CachedFtpFile newCachedFtpFile(final GpConfig gpConfig, String urlString) {
            return CachedFtpFileFactory.instance().newEdtFtpJImpl(gpConfig, urlString);
        }
    },
    /** edtFTPj, Enterprise Distributed Technologies library, single line static method call, with no support for handling interrupted exceptions */
    EDT_FTP_J_SIMPLE {
        @Override
        public CachedFtpFile newCachedFtpFile(final GpConfig gpConfig, String urlString) {
            return CachedFtpFileFactory.instance().newEdtFtpJImpl_simple(gpConfig, urlString);
        }
    };

    public static final String PROP_FTP_DOWNLOADER_TYPE="ftpDownloader.type";

    /**
     * Get or create a new instance of the downloader for the given url value.
     * @param urlString
     * @return
     */
    public abstract CachedFtpFile newCachedFtpFile(final GpConfig gpConfig, final String urlString);
}