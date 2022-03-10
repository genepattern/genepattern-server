/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;

import org.genepattern.server.job.input.cache.CachedFtpFileFactory;
import org.genepattern.server.job.input.choice.ChoiceInfo.Status.Flag;
import org.genepattern.server.job.input.choice.ftp.FtpDirLister;
import org.genepattern.server.job.input.choice.ftp.FtpEntry;
import org.genepattern.server.job.input.choice.ftp.ListFtpDirException;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONArray;
import org.json.JSONObject;

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
    private final static Logger log = Logger.getLogger(DynamicChoiceInfoParser.class);
    
    private final GpConfig gpConfig;
    private final GpContext jobContext;
    private final boolean initializeDynamicDropdown;
    
    public DynamicChoiceInfoParser(final GpConfig gpConfig, final GpContext jobContext) {
        //by default, do the remote call to list the directory contents
        this(gpConfig, jobContext, true);
    }
    public DynamicChoiceInfoParser(final GpConfig gpConfig, final GpContext jobContext, final boolean initializeDynamicDropdown) {
        this.gpConfig=gpConfig;
        this.jobContext=jobContext;
        this.initializeDynamicDropdown=initializeDynamicDropdown;
    }

    @Override
    public ChoiceInfo initChoiceInfo(final ParameterInfo param) {
        if (param==null) {
            log.error("param==null");
            return null;
        }
        final List<Choice> choiceList;
        //the new way (>= 3.7.0), check for remote ftp directory
        final String choiceDir = (String) param.getAttributes().get(ChoiceInfo.PROP_CHOICE_DIR);
        if (choiceDir != null) {
            log.debug("Initializing drop-down from remote source for param="+param.getName()+", ChoiceInfo.PROP_CHOICE_DIR="+choiceDir);
            try {
                final ChoiceInfo choiceInfoFromFtp;
                if (initializeDynamicDropdown) {
                    choiceInfoFromFtp = initChoicesFromRemote(param, choiceDir);
                }
                else {
                    choiceInfoFromFtp = initChoicesMeta(param, choiceDir);
                }
                log.debug(choiceInfoFromFtp.getStatus());
                choiceInfoFromFtp.initDefaultValue(param);
                log.debug("initial selection: "+choiceInfoFromFtp.getSelected());
                choiceInfoFromFtp.initAllowCustomValue(param);
                log.debug("isAllowCustomValue: "+choiceInfoFromFtp.isAllowCustomValue());
                
                if (ChoiceInfo.Status.Flag.ERROR==choiceInfoFromFtp.getStatus().getFlag()) {
                    //check for alternative static drop-down
                    final List<Choice> altChoices=ChoiceInfo.getDeclaredChoices(param);
                    for(final Choice choice : altChoices) {
                        choiceInfoFromFtp.add(choice);
                    }
                }
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

        choiceList=ChoiceInfo.getStaticChoices(param);
        if (choiceList==null) {
            log.debug("choiceList is null, param="+param.getName());
            return null;
        }
        if (choiceList.size()==0) {
            log.debug("choiceList.size()==0, param="+param.getName());
            return null;
        }
        
        final ChoiceInfo choiceInfo=new ChoiceInfo(param.getName());
        for(final Choice choice : choiceList) {
            choiceInfo.add(choice);
        }
        choiceInfo.setStatus(Flag.OK, "Initialized static list from manifest");
        
        //initialize default value for choice, must do this after the list of choices is initialized
        choiceInfo.initDefaultValue(param);
        choiceInfo.initAllowCustomValue(param);
        
        log.debug("initial selection: "+choiceInfo.getSelected());
        return choiceInfo;
    }

    /**
     * Initialize the meta-data for a dynamic drop-down, but don't do a directory listing.
     * A subsequent callback from a web client will list the files.
     * @param param
     * @param ftpDir
     * @return
     */
    private ChoiceInfo initChoicesMeta(final ParameterInfo param, final String ftpDir) {
        final ChoiceInfo choiceInfo=new ChoiceInfo(param.getName());
        choiceInfo.setChoiceDir(ftpDir);
        choiceInfo.setStatus(Flag.NOT_INITIALIZED, "Drop-down menu not initialized");
        return choiceInfo;
    }
    
    /**
     * Initialize the ChoiceInfo from an ftp directory.
     * This method does the remote directory listing, or if 
     * configured, it will do a local file system listing instead.
     * 
     * @param ftpDir
     * @return
     */
    protected ChoiceInfo initChoicesFromRemote(final ParameterInfo param, final String remoteDir) throws ChoiceInfoException {
        final ChoiceInfo choiceInfo=new ChoiceInfo(param.getName());
        choiceInfo.setChoiceDir(remoteDir);
        final DirFilter dirFilter=new DirFilter(param);
        
        //special-case, local.choiceDir
        final LocalChoiceInfoObj localChoice = new LocalChoiceInfoObj(gpConfig, jobContext, remoteDir, dirFilter);
        if (localChoice.hasLocalChoiceDir()) {
            for(final Choice choice : localChoice.getLocalChoices()) {
                choiceInfo.add(choice);
            }
            if (choiceInfo.getChoices().size()==0) {
                choiceInfo.setStatus(Flag.WARNING, "No matching files in "+remoteDir);
            }
            else {
                final String statusMessage="Initialized "+choiceInfo.getChoices().size()+" choices from local dir="+localChoice.getLocalChoiceDir()+", mapped to remote dir="+remoteDir;
                choiceInfo.setStatus(Flag.OK, statusMessage);
            }
            return choiceInfo;
        }
        
        if (remoteDir.startsWith("ftp")) {
            initChoiceInfoEntriesFromFtp(param, remoteDir, choiceInfo, dirFilter);
        } else if (remoteDir.startsWith("s3")) {
            initChoiceInfoEntriesFromS3(param, remoteDir, choiceInfo, dirFilter);
        } else if (remoteDir.startsWith("http")) {
            initChoiceInfoEntriesFromHTTPFile(param, remoteDir, choiceInfo, dirFilter);
        }
        
        return choiceInfo;
    }
    
    private ChoiceInfo initChoiceInfoEntriesFromFtp(final ParameterInfo param, final String ftpDir, final ChoiceInfo choiceInfo, final DirFilter dirFilter) {
        final FtpDirLister ftpDirLister=CachedFtpFileFactory.initDirListerFromConfig(gpConfig, jobContext);
        List<FtpEntry> ftpEntries=null;
        try {
            ftpEntries=ftpDirLister.listFiles(ftpDir, dirFilter);
            final String statusMessage="Initialized "+choiceInfo.getChoices().size()+" choices from "+ftpDir+" on "+new Date();
            choiceInfo.setStatus(Flag.OK, statusMessage);
        }
        catch (ListFtpDirException e) {
            log.debug("dynamic drop-down error, param="+param.getName()+", ftpDir="+ftpDir, e);
            choiceInfo.setStatus(Flag.ERROR, e.getLocalizedMessage());
            return choiceInfo;
        }
        catch (Throwable t) {
            log.error("Unexpected dynamic drop-down error, param="+param.getName()+", ftpDir="+ftpDir, t);
            choiceInfo.setStatus(Flag.ERROR, t.getLocalizedMessage());
            return choiceInfo;
        }
        if (ftpEntries==null) {
            final String errorMessage="Error listing files from "+ftpDir;
            log.error(errorMessage);
            choiceInfo.setStatus(Flag.ERROR, errorMessage);
            return choiceInfo;
        }

        // add entries to choiceInfo
        for(final FtpEntry ftpEntry : ftpEntries) {
            final Choice choice=new Choice(ftpEntry.getName(), ftpEntry.getValue(), ftpEntry.isDir());
            choiceInfo.add(choice);
        }
        if (choiceInfo.getChoices().size()==0) {
            choiceInfo.setStatus(Flag.WARNING, "No matching files in "+ftpDir);
        } 
        return choiceInfo;
    }
    
    private ChoiceInfo initChoiceInfoEntriesFromHTTPFile(final ParameterInfo param, final String fileUrl, final ChoiceInfo choiceInfo, final DirFilter dirFilter) {
        
        // 
        try {
            InputStream is = new URL(fileUrl).openStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
              sb.append((char) cp);
            }
            JSONArray json = new JSONArray(sb.toString());
            for (int i=0; i< json.length(); i++){
                JSONObject obj = json.getJSONObject(i);
                String label = obj.getString("label");
                String url = obj.getString("url");
                final Choice choice=new Choice(label, url, false);
                choiceInfo.add(choice);
             }
            
            
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
        }
        final String statusMessage="Initialized "+choiceInfo.getChoices().size()+" choices from "+fileUrl+" on "+new Date();
        choiceInfo.setStatus(Flag.OK, statusMessage);
        
        return choiceInfo;
    }
    
    
    private ChoiceInfo initChoiceInfoEntriesFromS3(final ParameterInfo param, final String s3DirURI, final ChoiceInfo choiceInfo, final DirFilter dirFilter) {
        
       
        try {
            String awsfilepath = gpConfig.getGPProperty(GpContext.getServerContext(),"aws-batch-script-dir");
            String awsfilename = gpConfig.getGPProperty(GpContext.getServerContext(), "aws-cli", "aws-cli.sh");
             
            String[] execArgs = new String[] {awsfilepath+awsfilename, "s3", "ls", s3DirURI};
            
            Process proc = Runtime.getRuntime().exec(execArgs);
            
            proc.waitFor();
            BufferedReader stdInput = new BufferedReader(new     InputStreamReader(proc.getInputStream()));
            
            // each line will look like  "yyyy-MM-dd HH:mm:ss"
            //         2020-12-01 13:19:01    4931101 tedslaptop/Users/liefeld/gp/users/739701.jpg
            
            String s = null;
            while (( s = stdInput.readLine()) != null) {
                String[] lineParts = s.split("\\s+");
                boolean isDir = false;
                String name = null;
                if (lineParts.length == 4){
                    name = lineParts[3];
                } else if (lineParts.length == 3) {
                    name = lineParts[2]; // directories
                    isDir = true;
                }
                
                
                if (isDir) {
                    // for dirs strip off the trailing space for the filter.  ftp does this automatically but in S3 the trailing
                    // slash is part of the key and the only thing indicating it is a directory
                    if (dirFilter.acceptName(name.substring(0,name.length()-1))){
                        final Choice choice=new Choice(name, s3DirURI+name, isDir);
                        choiceInfo.add(choice);
                    }
                } else {
                    if (dirFilter.acceptName(name)){
                        final Choice choice=new Choice(name, s3DirURI+name, isDir);
                        choiceInfo.add(choice);
                    }
                }
            }
            
            
            final String statusMessage="Initialized "+choiceInfo.getChoices().size()+" choices from "+s3DirURI+" on "+new Date();
            choiceInfo.setStatus(Flag.OK, statusMessage);
        } catch (Exception e) {
            final String errorMessage="Error listing files from "+s3DirURI;
            log.error(errorMessage);
            choiceInfo.setStatus(Flag.ERROR, errorMessage);
            return choiceInfo;
        }

        if (choiceInfo.getChoices().size()==0) {
            choiceInfo.setStatus(Flag.WARNING, "No matching files in "+s3DirURI);
        } 
        return choiceInfo;
    }
    
    
    
}
