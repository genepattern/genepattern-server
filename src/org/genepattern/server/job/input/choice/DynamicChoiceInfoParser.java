package org.genepattern.server.job.input.choice;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.job.input.choice.ChoiceInfo.Status.Flag;
import org.genepattern.server.util.FindFileFilter;
import org.genepattern.webservice.ParameterInfo;

/**
 * Initialize the list of choices for a given parameter from a remote ftp server.
 * 
 * Example manifest entry,
 *     p4_choiceDir=ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/referenceAnnotation/gtf
 * 
 * @author pcarr
 *
 */
public class DynamicChoiceInfoParser implements ChoiceInfoParser {
    final static private Logger log = Logger.getLogger(DynamicChoiceInfoParser.class);
    
    @Override
    public boolean hasChoiceInfo(ParameterInfo param) {
        final String choiceDir = (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR);
        if (choiceDir != null) {
            return true;
        }
        final String declaredChoicesStr= (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE);
        if (declaredChoicesStr != null) {
            return true;
        }
        Map<String,String> legacy=param.getChoices();
        if (legacy != null && legacy.size()>0) {
            return true;
        }
        return false;
    }

    /**
     * Helper method to set the choiceInfo.allowCustomValue flag based on the
     *     properties in the manifest file.
     * 
     * @param choiceInfo
     * @param param
     */
    private static void initAllowCustomValue(final ChoiceInfo choiceInfo, final ParameterInfo param) {
        if (choiceInfo==null) {
            throw new IllegalArgumentException("choiceInfo==null");
        }
        final String allowCustomValueStr;
        if (param==null) {
            allowCustomValueStr=null;
        }
        else {
            allowCustomValueStr= (String) param.getAttributes().get( ChoiceInfo.PROP_CHOICE_ALLOW_CUSTOM_VALUE );
        }
        if (ChoiceInfoHelper.isSet(allowCustomValueStr)) {
            // if it's 'on' then it's true
            if ("on".equalsIgnoreCase(allowCustomValueStr.trim())) {
                choiceInfo.setAllowCustomValue(true);
            }
            else {
                //otherwise, false
                choiceInfo.setAllowCustomValue(false);
            }
        }
        else if (param.isInputFile()) {
            //by default, a file choice parameter allows a custom value
            choiceInfo.setAllowCustomValue(true);
        }
        else {
            //by default, a text choice parameter does not allow a custom value
            choiceInfo.setAllowCustomValue(false);
        }
    }

    @Override
    public ChoiceInfo initChoiceInfo(final ParameterInfo param) {
        final Map<String,String> choices;
        //the new way (>= 3.7.0), check for remote ftp directory
        final String choiceDir = (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR);
        if (choiceDir != null) {
            log.debug("Initializing drop-down from remote source for param="+param.getName()+", ChoiceInfo.PROP_CHOICE_DIR="+choiceDir);
            try {
                final ChoiceInfo choiceInfoFromFtp = initChoicesFromFtp(param, choiceDir);
                log.debug(choiceInfoFromFtp.getStatus());
                choiceInfoFromFtp.initDefaultValue(param);
                log.debug("initial selection: "+choiceInfoFromFtp.getSelected());
                DynamicChoiceInfoParser.initAllowCustomValue(choiceInfoFromFtp, param);
                log.debug("isAllowCustomValue: "+choiceInfoFromFtp.isAllowCustomValue());
                return choiceInfoFromFtp;
            }
            catch (Throwable t) {
                String userMessage="Server error initializing drop-down menu from "+choiceDir;
                String developerMessage="Error initializing drop-down menu for '"+param.getName()+"' from "+choiceDir+": "+t.getLocalizedMessage();
                log.error(developerMessage, t);
                final ChoiceInfo choiceInfo = new ChoiceInfo(param.getName());
                choiceInfo.setChoiceDir(choiceDir);
                choiceInfo.setStatus(Flag.ERROR, userMessage);
                return choiceInfo;
            }
        }

        //the new way (>= 3.7.0), check for 'choice' attribute in manifest
        final String declaredChoicesStr= (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE);
        if (declaredChoicesStr != null) {
            log.debug("Initializing "+ChoiceInfo.PROP_CHOICE+" entry from manifest for parm="+param.getName());
            choices=ParameterInfo._initChoicesFromString(declaredChoicesStr);
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
        
        final ChoiceInfo choiceInfo=new ChoiceInfo(param.getName());
        for(final Entry<String,String> choiceEntry : choices.entrySet()) {
            Choice choice = new Choice(choiceEntry.getKey(), choiceEntry.getValue());
            choiceInfo.add(choice);
        }
        choiceInfo.setStatus(Flag.OK, "Initialized static list from manifest");
        
        //initialize default value for choice, must do this after the list of choices is initialized
        choiceInfo.initDefaultValue(param);
        DynamicChoiceInfoParser.initAllowCustomValue(choiceInfo, param);
        
        log.debug("initial selection: "+choiceInfo.getSelected());
        return choiceInfo;
    }

    public static class ChoiceDirFilter implements FTPFileFilter {
        public static enum Type {
            file,
            dir,
            any
        }
        
        private Type type=Type.file;
        final private String choiceDirFilter;
        final private FindFileFilter globs;
        
        public ChoiceDirFilter(final ParameterInfo param) {
            if (param==null) {
                throw new IllegalArgumentException("param==null");
            }
            if (param.getAttributes()==null) {
                throw new IllegalArgumentException("param.attributes==null");
            }
            choiceDirFilter = (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR_FILTER);
            if (!ChoiceInfoHelper.isSet(choiceDirFilter)) {
                globs=null;
            }
            else {
                globs = new FindFileFilter();
            
                //parse this as a ';' separated list of patterns
                String[] patterns=choiceDirFilter.split(Pattern.quote(";"));
                for(final String pattern : patterns) {
                    if (!ChoiceInfoHelper.isSet(pattern)) {
                        //skip empty pattern
                    }
                    else if (pattern.startsWith("type="+Type.dir)) {
                        type=Type.dir;
                    }
                    else if (pattern.startsWith("type="+Type.file)) {
                        type=Type.file;
                    }
                    else if (pattern.startsWith("type="+Type.any)) {
                        type=Type.any;
                    }
                    else {
                        //it's a glob pattern
                        globs.addGlob(pattern);
                    }
                }
            }
        }

        @Override
        public boolean accept(final FTPFile ftpFile) {
            if (type==Type.file) {
                if (!ftpFile.isFile()) {
                    return false;
                }
            }
            else if (type==Type.dir) {
                if (!ftpFile.isDirectory()) {
                    return false;
                }
            }
            
            //check for glob patterns
            if (globs==null) {
                return true;
            }
            return globs.accept(new File(ftpFile.getName()));
        }

    }
    
    /**
     * Initialize the ChoiceInfo from an ftp directory.
     * 
     * @param ftpDir
     * @return
     */
    private ChoiceInfo initChoicesFromFtp(final ParameterInfo param, final String ftpDir) throws ChoiceInfoException {
        final ChoiceInfo choiceInfo=new ChoiceInfo(param.getName());
        choiceInfo.setChoiceDir(ftpDir);
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
                final String errorMessage="Login error, ftpDir="+ftpDir;
                log.error(errorMessage);
                choiceInfo.setStatus(Flag.ERROR, errorMessage);
                return choiceInfo;
            }
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            //ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            
            //check for valid path
            success=ftpClient.changeWorkingDirectory(ftpUrl.getPath());
            if (!success) {
                final String errorMessage="Error CWD="+ftpUrl.getPath()+", ftpDir="+ftpDir;
                log.error(errorMessage);
                choiceInfo.setStatus(Flag.ERROR, errorMessage);
                return choiceInfo;
            }
            
            log.debug("listing files from directory: "+ftpClient.printWorkingDirectory());
            files = ftpClient.listFiles();
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
        
        //optionally filter
        final FTPFileFilter choiceDirFilter = new ChoiceDirFilter(param);
        for(FTPFile ftpFile : files) {
            if (!choiceDirFilter.accept(ftpFile)) {
                log.debug("Skipping '"+ftpFile.getName()+ "' from ftpDir="+ftpDir);
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
                final Choice choice=new Choice(name, value, ftpFile.isDirectory());
                choiceInfo.add(choice);
            }
        }
        if (choiceInfo.getChoices().size()==0) {
            choiceInfo.setStatus(Flag.WARNING, "No matching files in "+ftpDir);
        }
        else {
            final String statusMessage="Initialized "+choiceInfo.getChoices().size()+" choices from "+ftpDir+" on "+new Date();
            choiceInfo.setStatus(Flag.OK, statusMessage);
        }
        return choiceInfo;
    }
}
