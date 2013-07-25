package org.genepattern.server.job.input.choice;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.job.input.choice.ChoiceInfo.Status.Flag;
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
                String userMessage="Server error initializing list of choices from "+ftpDir;
                String developerMessage="Error initializing choices for '"+param.getName()+"' from "+ftpDir+": "+t.getLocalizedMessage();
                log.error(developerMessage, t);
                final ChoiceInfo choiceInfo = new ChoiceInfo();
                choiceInfo.setFtpDir(ftpDir);
                choiceInfo.setStatus(Flag.ERROR, userMessage);
                log.error(t);
            }
        }

        //the new way (>= 3.7.0), check for 'choice' attribute in manifest
        final String declaredChoicesStr= (String) param.getAttributes().get("choice");
        if (declaredChoicesStr != null) {
            log.debug("parsing choice entry from manifest for parm="+param.getName());
            choices=ParameterInfo._initChoicesFromString(declaredChoicesStr);
            log.debug("Initialized choices from choice attribute");
        }
        else {
            //the old way (<= 3.6.1, based on 'values' attribute in manifest)
            choices=param.getChoices();
            log.debug("Initialized choices from value attribute");
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
        choiceInfo.setStatus(Flag.OK, "Initialized static list from manifest");
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
                choiceInfo.setStatus(Flag.ERROR, "Module error, Invalid ftpDir="+ftpDir);
                return choiceInfo;
            }
        }
        catch (MalformedURLException e) {
            log.error("Invalid ftpDir="+ftpDir, e);
            choiceInfo.setStatus(Flag.ERROR, "Module error, Invalid ftpDir="+ftpDir);
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
                log.error("Connection refused, ftpDir="+ftpDir);
                choiceInfo.setStatus(Flag.ERROR, "Connection refused, ftpDir="+ftpDir);
                return choiceInfo;
            }
            // anonymous login
            final String ftpUsername="anonymous";
            final String ftpPassword="gp-help@broadinstitute.org";
            boolean success=ftpClient.login(ftpUsername, ftpPassword);
            if (!success) {
                log.error("Login error, ftpDir="+ftpDir);
                choiceInfo.setStatus(Flag.ERROR, "Login error, ftpDir="+ftpDir);
                return choiceInfo;
            }
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            //ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            files = ftpClient.listFiles(ftpUrl.getPath());
        }
        catch (IOException e) {
            String errorMessage="Error listing files from "+ftpDir;
            log.error(errorMessage, e);
            choiceInfo.setStatus(Flag.ERROR, errorMessage);
            return choiceInfo;
        }
        catch (Throwable t) {
            String errorMessage="Unexpected error listing files from "+ftpDir+", "+t.getClass().getName();
            log.error(errorMessage, t);
            choiceInfo.setStatus(Flag.ERROR, errorMessage);
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
            final String errorMessage="Error listing files from "+ftpDir;
            log.error(errorMessage);
            choiceInfo.setStatus(Flag.ERROR, errorMessage);
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
        final String statusMessage="Initialized "+choiceInfo.getChoices().size()+" choices from "+ftpDir+" on "+new Date();
        log.debug(statusMessage);
        choiceInfo.setStatus(Flag.OK, statusMessage);
        return choiceInfo;
    }
}
