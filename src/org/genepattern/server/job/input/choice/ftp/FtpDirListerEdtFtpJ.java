/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice.ftp;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.job.input.choice.DirFilter;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;

/**
 * Implement FTP directory listing with EdtFTPj client.
 * 
 * @author pcarr
 */
public class FtpDirListerEdtFtpJ implements FtpDirLister {
    private static final Logger log = Logger.getLogger(FtpDirListerEdtFtpJ.class);
    
    public static FtpDirListerEdtFtpJ createFromConfig(GpConfig gpConfig, final GpContext gpContext, final boolean passiveMode) {
        if (gpConfig==null) {
            gpConfig=ServerConfigurationFactory.instance();
        }
        String webmaster=gpConfig.getGPProperty(gpContext, "webmaster", "gp-help@broadinstitute.org");
        
        FtpDirListerEdtFtpJ dirLister = new FtpDirListerEdtFtpJ();
        dirLister.ftpUsername=gpConfig.getGPProperty(gpContext, PROP_FTP_USERNAME, "anonymous");
        dirLister.ftpPassword=gpConfig.getGPProperty(gpContext, PROP_FTP_PASSWORD, webmaster);
        dirLister.passive=passiveMode;
        dirLister.defaultTimeout_ms=gpConfig.getGPIntegerProperty(gpContext, PROP_FTP_SOCKET_TIMEOUT, 60000);
        
        return dirLister;
    }
    
    // anonymous login
    private String ftpUsername="anonymous";
    private String ftpPassword="gp-help@broadinstitute.org";
    private boolean passive=true;
    private int defaultTimeout_ms=60*1000; //60 seconds

    public FileTransferClient initFtpClient(final URL fromUrl) throws FTPException, IOException, ListFtpDirException {
        FileTransferClient ftpClient = null;
        boolean error=false;
        try {
            ftpClient = new FileTransferClient();
            final String host=fromUrl.getHost();
            ftpClient.setRemoteHost(host);
            ftpClient.setUserName(ftpUsername);
            ftpClient.setPassword(ftpPassword);
            ftpClient.setTimeout(defaultTimeout_ms);
            ftpClient.connect();
            ftpClient.setContentType(FTPTransferType.BINARY);
            if (passive) {
                ftpClient.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
            }
            else {
                ftpClient.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.ACTIVE);
            }
        }
        catch (FTPException e) {
            error=true;
            throw e;
        }
        catch (IOException e) {
            error=true;
            throw e;
        }
        catch (Throwable t) {
            error=true;
            final String msg="Error connecting to "+fromUrl+", "+t.getLocalizedMessage();
            log.debug(msg, t);
            throw new ListFtpDirException(msg, t);            
        }
        finally {
            if (error && ftpClient != null) {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    }
                    catch (Throwable t) {
                        log.error("Error disconnecting ftp client, fromUrl="+fromUrl, t);
                    }
                }
            }
        }
        return ftpClient;
    }

    protected URL initUrl(String str) throws ListFtpDirException {
        try {
            return new URL(str);
        }
        catch (Throwable t) {
            log.error("Invalid url="+str, t);
            throw new ListFtpDirException("Invalid url="+str);
        }
    }
    
    @Override
    public List<FtpEntry> listFiles(String ftpDirUrl, DirFilter filter) throws ListFtpDirException {
        URL url = initUrl(ftpDirUrl);
        FileTransferClient ftpClient=null;
        try {
            ftpClient=initFtpClient(url);
            FTPFile[] ftpFiles=ftpClient.directoryList(url.getPath());
            return asFilesToDownload(filter, ftpDirUrl, ftpFiles);
        }
        catch (ListFtpDirException e) {
            throw e;
        }
        catch (Throwable t) {
            final String msg="Error listing ftpDir="+ftpDirUrl+", "+t.getLocalizedMessage();
            log.error(msg, t);
            throw new ListFtpDirException(msg, t);
        }
        finally {
            if (ftpClient != null) {
                try {
                    ftpClient.disconnect();
                }
                catch (Throwable t) {
                    log.error("Error in ftpClient.disconnect(), ftpDir="+ftpDirUrl, t);
                }
            } 
        }
    }

    protected FtpEntry initFtpEntry(final String ftpParentDir, final FTPFile ftpFile) {
        final String name=ftpFile.getName();
        final String encodedName=UrlUtil.encodeURIcomponent(name);
        final String value;
        if (ftpParentDir.endsWith("/")) {
            value=ftpParentDir + encodedName;
        }
        else {
            value=ftpParentDir + "/" + encodedName;
        }
        FtpEntry ftpEntry = new FtpEntry(ftpFile.getName(), value, ftpFile.isDir());
        return ftpEntry;
    }
    
    protected List<FtpEntry> asFilesToDownload(final DirFilter filter, final String ftpDir, final FTPFile[] files) {
        final List<FtpEntry> filesToDownload=new ArrayList<FtpEntry>();
        // filter
        for(FTPFile ftpFile : files) {
            final FtpEntry ftpEntry=initFtpEntry(ftpDir, ftpFile);
            if (!filter.accept(ftpEntry)) {
                log.debug("Skipping '"+ftpFile.getName()+ "' from ftpDir="+ftpDir);
            }
            else {
                filesToDownload.add(ftpEntry);
            }
        }
        return filesToDownload;
    }

}

