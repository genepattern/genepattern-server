/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice.ftp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.job.input.choice.DirFilter;

/**
 * Remote ftp directory listing implemented with the apache commons FTP client.
 * @author pcarr
 *
 */
public class FtpDirListerCommonsNet_3_3 implements FtpDirLister {
    private static Logger log = Logger.getLogger(FtpDirListerCommonsNet_3_3.class);
    
    /**
     * Initialize an ftp directory lister using the default ftp client settings.
     * The default values can be configured in the config_yaml file for the server.
     * <pre>
       # amount of time in milliseconds
       gp.server.choice.ftp_socketTimeout: 15000
       gp.server.choice.ftp_dataTimeout: 15000
       gp.server.choice.ftp_username: "anonymous"
       gp.server.choice.ftp_password: "gp-help@broadinstitute.org"
     * </pre>
     * 
     * @param gpConfig
     * @param gpContext
     * @param passiveModeFromModuleParam, this should be null in all cases with the exception of an override
     *     which is declared in the manifest file for the module, e.g. 'p0_choiceDirFtpPassiveMode='false'
     * @return
     */
    public static FtpDirListerCommonsNet_3_3 createFromConfig(GpConfig gpConfig, final GpContext gpContext, boolean passiveMode) {
        if (gpConfig==null) {
            gpConfig=ServerConfigurationFactory.instance();
        }
        int socketTimeout=gpConfig.getGPIntegerProperty(gpContext, PROP_FTP_SOCKET_TIMEOUT, 30000);
        int dataTimeout=gpConfig.getGPIntegerProperty(gpContext, PROP_FTP_DATA_TIMEOUT, 30000);
        
        String webmaster=gpConfig.getGPProperty(gpContext, "webmaster", "gp-help@broadinstitute.org");
        String username=gpConfig.getGPProperty(gpContext, PROP_FTP_USERNAME, "anonymous");
        String password=gpConfig.getGPProperty(gpContext, PROP_FTP_PASSWORD, webmaster);
        return new Builder()
            .socketTimeout(socketTimeout)
            .dataTimeout(dataTimeout)
            .username(username)
            .password(password)
            .passive(passiveMode)
            .build();
    }

    private final int defaultTimeout_ms; //=15*1000; //15 seconds
    private final int socketTimeout_ms; //=15*1000; //15 seconds
    // anonymous login
    private final String ftpUsername; //="anonymous";
    private final String ftpPassword; //="gp-help@broadinstitute.org";
    //toggle passive mode
    private final boolean passive;
    
    public FtpDirListerCommonsNet_3_3() {
        this.defaultTimeout_ms=15*1000; //15 seconds
        this.socketTimeout_ms=15*1000; //15 seconds
        // anonymous login
        this.ftpUsername="anonymous";
        this.ftpPassword="gp-help@broadinstitute.org";
        // by default, use passive mode FTP transfer
        this.passive=true;
    }
    private FtpDirListerCommonsNet_3_3(Builder in) {
        this.defaultTimeout_ms=in.dataTimeout_ms;
        this.socketTimeout_ms=in.socketTimeout_ms;
        this.ftpUsername=in.ftpUsername;
        this.ftpPassword=in.ftpPassword;
        this.passive=in.passive;
    }
    
    public static final class Builder {
        private int dataTimeout_ms=15*1000; //15 seconds
        private int socketTimeout_ms=15*1000; //15 seconds
        // anonymous login
        private String ftpUsername="anonymous";
        private String ftpPassword="gp-help@broadinstitute.org";
        private boolean passive=true;
        
        public Builder dataTimeout(int dataTimeout_ms) {
            this.dataTimeout_ms=dataTimeout_ms;
            return this;
        }
        public Builder socketTimeout(int socketTimeout_ms) {
            this.socketTimeout_ms=socketTimeout_ms;
            return this;
        }
        public Builder username(String username) {
            this.ftpUsername=username;
            return this;
        }
        public Builder password(String password) {
            this.ftpPassword=password;
            return this;
        }
        public Builder passive(final boolean b) {
            this.passive=b;
            return this;
        }
        
        public FtpDirListerCommonsNet_3_3 build() {
            return new FtpDirListerCommonsNet_3_3(this);
        }
    }

    @Override
    public List<FtpEntry> listFiles(String ftpDir, DirFilter filter) throws ListFtpDirException {
        final URL ftpUrl;
        try {
            ftpUrl=new URL(ftpDir);
            if (!"ftp".equalsIgnoreCase(ftpUrl.getProtocol())) {
                log.error("Invalid ftpDir="+ftpDir);
                throw new ListFtpDirException("Module error, Invalid ftpDir="+ftpDir);
            }
        }
        catch (MalformedURLException e) {
            log.error("Invalid ftpDir="+ftpDir, e);
            throw new ListFtpDirException("Module error, Invalid ftpDir="+ftpDir);
        }

        if (log.isDebugEnabled()) {
            log.debug("connecting to host="+ftpUrl.getHost());
        }

        FTPFile[] files;
        final FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.setDataTimeout(defaultTimeout_ms);
            if (passive) {
                ftpClient.enterLocalPassiveMode();
            }
            else {
                ftpClient.enterLocalActiveMode();
            }
            final int port;
            if (ftpUrl.getPort()>0) {
                port=ftpUrl.getPort();
            }
            else if (ftpUrl.getDefaultPort()>0) {
                port=ftpUrl.getDefaultPort();
            }
            else {
                port=-1;
            }
            if (port>0) {
                log.debug("port="+port);
                ftpClient.connect(ftpUrl.getHost(), port);
            }
            else {
                log.debug("port=<not set>");
                ftpClient.connect(ftpUrl.getHost());
            }
            
            ftpClient.setSoTimeout(socketTimeout_ms);
            // After connection attempt, you should check the reply code to verify success.
            final int reply = ftpClient.getReplyCode();
            if(!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                log.error("Connection refused, ftpDir="+ftpDir);
                throw new ListFtpDirException("Connection refused, ftpDir="+ftpDir);
            }
            // anonymous login
            boolean success=ftpClient.login(ftpUsername, ftpPassword);
            if (!success) {
                final String errorMessage="Login error, ftpDir="+ftpDir;
                log.error(errorMessage);
                throw new ListFtpDirException(errorMessage);
            }
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            //ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            //check for valid path
            success=ftpClient.changeWorkingDirectory(ftpUrl.getPath());
            if (!success) {
                final String errorMessage="Error CWD="+ftpUrl.getPath()+", ftpDir="+ftpDir;
                log.error(errorMessage);
                throw new ListFtpDirException(errorMessage);
            }

            log.debug("listing files from directory: "+ftpClient.printWorkingDirectory());
            files = ftpClient.listFiles();
            return asFilesToDownload(filter, ftpDir, files);
        }
        catch (IOException e) {
            String errorMessage="Error listing files from "+ftpDir+": "+e.getLocalizedMessage();
            log.error(errorMessage, e);
            throw new ListFtpDirException(errorMessage);
        }
        catch (Throwable t) {
            String errorMessage="Unexpected error listing files from "+ftpDir+", "+t.getClass().getName();
            log.error(errorMessage, t);
            throw new ListFtpDirException(errorMessage);
        }
        finally {
            if(ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } 
                catch(IOException ioe) {
                    // do nothing
                    log.warn("Error disconnecting from ftp client, ftpDir="+ftpDir, ioe);
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
        FtpEntry ftpEntry = new FtpEntry(ftpFile.getName(), value, ftpFile.isDirectory());
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
