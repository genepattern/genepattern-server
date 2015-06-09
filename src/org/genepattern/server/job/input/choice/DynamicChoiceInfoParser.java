/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

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
                    choiceInfoFromFtp = initChoicesFromFtp(param, choiceDir);
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
    protected ChoiceInfo initChoicesFromFtp(final ParameterInfo param, final String ftpDir) throws ChoiceInfoException {
        final ChoiceInfo choiceInfo=new ChoiceInfo(param.getName());
        choiceInfo.setChoiceDir(ftpDir);
        final DirFilter dirFilter=new DirFilter(param);
        
        //special-case, local.choiceDir
        final LocalChoiceInfoObj localChoice = new LocalChoiceInfoObj(gpConfig, jobContext, ftpDir, dirFilter);
        if (localChoice.hasLocalChoiceDir()) {
            for(final Choice choice : localChoice.getLocalChoices()) {
                choiceInfo.add(choice);
            }
            if (choiceInfo.getChoices().size()==0) {
                choiceInfo.setStatus(Flag.WARNING, "No matching files in "+ftpDir);
            }
            else {
                final String statusMessage="Initialized "+choiceInfo.getChoices().size()+" choices from local dir="+localChoice.getLocalChoiceDir()+", mapped to remote dir="+ftpDir;
                choiceInfo.setStatus(Flag.OK, statusMessage);
            }
            return choiceInfo;
        }
        
        initChoiceInfoEntriesFromFtp(param, ftpDir, choiceInfo, dirFilter);

        // must set the status flag
        if (choiceInfo.getChoices().size()==0) {
            choiceInfo.setStatus(Flag.WARNING, "No matching files in "+ftpDir);
        }
        else {
            final String statusMessage="Initialized "+choiceInfo.getChoices().size()+" choices from "+ftpDir+" on "+new Date();
            choiceInfo.setStatus(Flag.OK, statusMessage);
        }
        
        return choiceInfo;
    }
    
    private ChoiceInfo initChoiceInfoEntriesFromFtp(final ParameterInfo param, final String ftpDir, final ChoiceInfo choiceInfo, final DirFilter dirFilter) {
        final FtpDirLister ftpDirLister=CachedFtpFileFactory.initDirListerFromConfig(gpConfig, jobContext);
        List<FtpEntry> ftpEntries=null;
        try {
            ftpEntries=ftpDirLister.listFiles(ftpDir, dirFilter);
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
        
        return choiceInfo;
    }
}
