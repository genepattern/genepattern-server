/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice.ftp;

import java.util.List;

import org.genepattern.server.job.input.choice.DirFilter;

/**
 * Interface for listing the contents of a remote directory.
 * @author pcarr
 *
 */
public interface FtpDirLister {
    public static final String PROP_FTP_SOCKET_TIMEOUT="ftpDownloader.ftp_socketTimeout";
    public static final String PROP_FTP_DATA_TIMEOUT="ftpDownloader.ftp_dataTimeout";
    public static final String PROP_FTP_USERNAME="ftpDownloader.ftp_username";
    public static final String PROP_FTP_PASSWORD="ftpDownloader.ftp_password";
    public static final String PROP_FTP_PASV="ftpDownloader.ftp_pasv";

    List<FtpEntry> listFiles(String ftpDirUrl, DirFilter filter) throws ListFtpDirException;

}
