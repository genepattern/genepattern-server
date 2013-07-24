package org.genepattern.server.job.input.choice;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.webservice.ParameterInfo;

/**
 * Initialize the list of choices for a given parameter from a remote ftp server.
 * 
 * Example manifest entry,
 *     p4_choiceDirFtp=ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf
 * 
 * @author pcarr
 *
 */
public class DynamicChoiceInfoParser implements ChoiceInfoParser {
    final static private Logger log = Logger.getLogger(DynamicChoiceInfoParser.class);

    @Override
    public boolean hasChoiceInfo(ParameterInfo param) {
        final String choiceDirFtp = (String) param.getAttributes().get("choiceDirFtp");
        if (choiceDirFtp != null) {
            return true;
        }
        final String declaredChoicesStr= (String) param.getAttributes().get("choice");
        if (declaredChoicesStr != null) {
            return true;
        }
        Map<String,String> legacy=param.getChoices();
        if (legacy != null && legacy.size()>0) {
            return true;
        }
        return false;
    }

    @Override
    public ChoiceInfo initChoiceInfo(ParameterInfo param) {
        final Map<String,String> choices;
        //the new way (>= 3.7.0), check for remote ftp directory
        final String ftpDir = (String) param.getAttributes().get("choiceDirFtp");
        if (ftpDir != null) {
            log.debug("Initializing choice from remote source for param="+param.getName()+", choiceDirFtp="+ftpDir);
            try {
                final ChoiceInfo choiceInfoFromFtp = initChoicesFromFtp(ftpDir);
                return choiceInfoFromFtp;
            }
            catch (Throwable t) {
                String userMessage="Server error initializing list of choices from remote server: "+ftpDir;
                String developerMessage="Error initializing choices for '"+param.getName()+"' from "+ftpDir+": "+t.getLocalizedMessage();
                log.error(developerMessage, t);
                final ChoiceInfo choiceInfo = new ChoiceInfo();
                choiceInfo.setFtpDir(ftpDir);
                choiceInfo.setStatus(new ChoiceInfoException.Status(ChoiceInfoException.Status.Flag.ERROR, userMessage));
                log.error(t);
            }
        }

        //the new way (>= 3.7.0), check for choice= in manifest
        final String declaredChoicesStr= (String) param.getAttributes().get("choice");
        if (declaredChoicesStr != null) {
            log.debug("parsing choice entry from manifest for parm="+param.getName());
            choices=ParameterInfo._initChoicesFromString(declaredChoicesStr);
        }
        else {
            //the old way
            choices=param.getChoices();
        }

        if (choices==null) {
            log.debug("choices is null, param="+param.getName());
            return null;
        }
        if (choices.size()==0) {
            log.debug("choices.size()==0, param="+param.getName());
            return null;
        }
        
        final ChoiceInfo choiceInfo=new ChoiceInfo();
        for(final Entry<String,String> choiceEntry : choices.entrySet()) {
            Choice choice = new Choice(choiceEntry.getKey(), choiceEntry.getValue());
            choiceInfo.add(choice);
        }
        return choiceInfo;
    }
    
    /**
     * Initialize the ChoiceInfo from an ftp directory.
     * 
     * @param ftpDir
     * @return
     */
    private ChoiceInfo initChoicesFromFtp(final String ftpDir) throws ChoiceInfoException {
        final ChoiceInfo choiceInfo=new ChoiceInfo();
        choiceInfo.setFtpDir(ftpDir);
        final URL ftpUrl;
        try {
            ftpUrl=new URL(ftpDir);
            if (!"ftp".equalsIgnoreCase(ftpUrl.getProtocol())) {
                log.error("Invalid ftpDir="+ftpDir);
                final ChoiceInfoException.Status status=new ChoiceInfoException.Status(ChoiceInfoException.Status.Flag.ERROR,
                        "Module error, Invalid ftpDir="+ftpDir);
                choiceInfo.setStatus(status);
                return choiceInfo;
            }
        }
        catch (MalformedURLException e) {
            log.error("Invalid ftpDir="+ftpDir, e);
            final ChoiceInfoException.Status status=new ChoiceInfoException.Status(ChoiceInfoException.Status.Flag.ERROR,
                    "Module error, Invalid ftpDir="+ftpDir);
            choiceInfo.setStatus(status);
            return choiceInfo;
        }

        FTPFile[] files;
        final FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpUrl.getHost());
            // After connection attempt, you should check the reply code to verify success.
            final int reply = ftpClient.getReplyCode();
            if(!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                log.error("FTP server refused connection, ftpDir="+ftpDir);
                
                return choiceInfo;
            }
            // anonymous login
            boolean success=ftpClient.login("anonymous", "genepatt@broadinstitute.org");
            if (!success) {
                log.error("FTP server login error, ftpDir="+ftpDir);
                return choiceInfo;
            }
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            //ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            files = ftpClient.listFiles(ftpUrl.getPath());
        }
        catch (IOException e) {
            log.error("Error listing files from remote ftp dir="+ftpDir, e);
            //TODO: choiceInfo.addStatusMessage(ChoiceInfo.InitError.ConnectionError);
            return choiceInfo;
        }
        catch (Throwable t) {
            log.error("Unexpected exception listing files from remote ftp dir="+ftpDir, t);
            //TODO: choiceInfo.addStatusMessage(ChoiceInfo.InitError.ConnectionError);
            return choiceInfo;
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
        
        if (files==null) {
            log.error("Error listing files from ftpDir="+ftpDir);
            return choiceInfo;
        }
        for(FTPFile ftpFile : files) {
            if (!ftpFile.isFile()) {
                log.debug("Skipping '"+ftpFile.getName()+ "' from ftpDir="+ftpDir+". It's not a file");
            }
            else {
                final String name=ftpFile.getName();
                final String encodedName=UrlUtil.encodeURIcomponent(name);
                final String value;
                if (ftpDir.endsWith("/")) {
                    value=ftpDir + encodedName;
                }
                else {
                    value=ftpDir + "/" + encodedName;
                }
                final Choice choice=new Choice(name, value);
                choiceInfo.add(choice);
            }
        }
        return choiceInfo;
    }
}
