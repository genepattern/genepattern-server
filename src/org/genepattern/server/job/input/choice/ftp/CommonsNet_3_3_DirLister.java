package org.genepattern.server.job.input.choice.ftp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.choice.RemoteDirLister;

/**
 * Remote ftp directory listing implemented with the apache commons FTP client.
 * @author pcarr
 *
 */
public class CommonsNet_3_3_DirLister implements RemoteDirLister<FTPFile,ListFtpDirException> {
    private static Logger log = Logger.getLogger(CommonsNet_3_3_DirLister.class);
    
    public static final String PROP_FTP_SOCKET_TIMEOUT="gp.server.choice.ftp_socketTimeout";
    public static final String PROP_FTP_DATA_TIMEOUT="gp.server.choice.ftp_dataTimeout";
    public static final String PROP_FTP_USERNAME="gp.server.choice.ftp_username";
    public static final String PROP_FTP_PASSWORD="gp.server.choice.ftp_password";
    
    public static RemoteDirLister<FTPFile,ListFtpDirException> createDefault() {
        return new CommonsNet_3_3_DirLister();
    }

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
     * @return
     */
    public static RemoteDirLister<FTPFile, ListFtpDirException> createFromConfig(final GpConfig gpConfig, final GpContext gpContext) {
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
            .build();
    }
    

    private final int defaultTimeout_ms; //=15*1000; //15 seconds
    private final int socketTimeout_ms; //=15*1000; //15 seconds
    // anonymous login
    private final String ftpUsername; //="anonymous";
    private final String ftpPassword; //="gp-help@broadinstitute.org";
    
    private CommonsNet_3_3_DirLister() {
        this.defaultTimeout_ms=15*1000; //15 seconds
        this.socketTimeout_ms=15*1000; //15 seconds
        // anonymous login
        this.ftpUsername="anonymous";
        this.ftpPassword="gp-help@broadinstitute.org";
    }
    private CommonsNet_3_3_DirLister(Builder in) {
        this.defaultTimeout_ms=in.dataTimeout_ms;
        this.socketTimeout_ms=in.socketTimeout_ms;
        this.ftpUsername=in.ftpUsername;
        this.ftpPassword=in.ftpPassword;
    }
    
    public static final class Builder {
        private int dataTimeout_ms=15*1000; //15 seconds
        private int socketTimeout_ms=15*1000; //15 seconds
        // anonymous login
        private String ftpUsername="anonymous";
        private String ftpPassword="gp-help@broadinstitute.org";
        
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
        
        public CommonsNet_3_3_DirLister build() {
            return new CommonsNet_3_3_DirLister(this);
        }

    }

    @Override
    public FTPFile[] listFiles(final String ftpDir) throws ListFtpDirException {
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

        FTPFile[] files;
        final FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.setDataTimeout(defaultTimeout_ms);
            ftpClient.connect(ftpUrl.getHost());
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
            ftpClient.enterLocalPassiveMode();

            //check for valid path
            success=ftpClient.changeWorkingDirectory(ftpUrl.getPath());
            if (!success) {
                final String errorMessage="Error CWD="+ftpUrl.getPath()+", ftpDir="+ftpDir;
                log.error(errorMessage);
                throw new ListFtpDirException(errorMessage);
            }

            log.debug("listing files from directory: "+ftpClient.printWorkingDirectory());
            files = ftpClient.listFiles();
            return files;
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

}
