/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.collection;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.GpFilePathException;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListException;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.job.input.collection.ParamGroupWriter.Column;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;

/**
 * Helper class for preparing input values and generating a parameter group file for a given module input parameter.
 * Create one of these for each group input parameter for each run of a job.
 * 
 * Workflow:
 *     1) initialize (via static create method)
 *     2) start ...
 *     3) when complete, get the GpFilePath object representing the parameter group file
 * 
 * It's up to the caller to modify the job input parameter, replacing the actual list of values with a single value param group file.
 *  
 * @author pcarr
 *
 */
public class ParamGroupHelper {
    private static final Logger log = Logger.getLogger(ParamGroupHelper.class);
    
    private final HibernateSessionManager mgr;
    private final GpConfig gpConfig;
    private final GpContext jobContext;
    private final JobInput jobInput;
    private final String baseGpHref;
    private final ParameterInfo formalParam;
    private final GroupInfo groupInfo;
    private final Param param;
    private final List<GpFilePath> gpFilePaths;
    private final GpFilePath toFile;
    
    /**
     * The automatically generated name for the file list file is the <param.name>+<filenameSuffix>,
     * For example, for a parameter named 'input.files', 
     *     input.files.group.tsv
     */
    private final String filenameSuffix;
    /**
     * when true, automatically download external data files before creating the group list file.
     */
    private final boolean downloadExternalFiles;

    private ParamGroupHelper(final Builder in) {
        if (in.mgr==null) {
            this.mgr=org.genepattern.server.database.HibernateUtil.instance();
        }
        else {
            this.mgr=in.mgr;
        }
        if (in.gpConfig==null) {
            this.gpConfig=ServerConfigurationFactory.instance();
        }
        else {
            this.gpConfig=in.gpConfig;
        }
        if (in.jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (in.jobInput==null) {
            throw new IllegalArgumentException("jobInput==null");
        }
        if (in.param==null) {
            throw new IllegalArgumentException("param==null");
        }
        if (in.groupInfo==null) {
            throw new IllegalArgumentException("groupInfo==null");
        }
        this.jobContext=in.jobContext;
        this.jobInput=in.jobInput;
        this.baseGpHref=ParamListHelper.initBaseGpHref(gpConfig, jobInput);
        if (in.parameterInfoRecord != null) {
            this.formalParam=in.parameterInfoRecord.getFormal();
        }
        else {
            log.warn("Missing parameterInfoRecord.formal");
            this.formalParam=null;
        }
        this.param=in.param;
        this.groupInfo=in.groupInfo;
        this.filenameSuffix=in.filenameSuffix;
        this.downloadExternalFiles=in.downloadExternalFiles;
        try {
            this.gpFilePaths=ParamListHelper.getListOfValues(mgr, gpConfig, jobContext, jobInput, formalParam, param, downloadExternalFiles);
        }
        catch (Throwable t) {
            String msg="Error initializing gpFilePaths for param='"+formalParam.getName()+"': "+t.getMessage();
            log.error(msg, t);
            throw new IllegalArgumentException(t);
        }
        if (in.toFile != null) {
            this.toFile=in.toFile;
        }
        else {
            try {
                this.toFile=initToFile();
            }
            catch (Throwable t) {
                log.error(t);
                throw new IllegalArgumentException("Error initialing toFile for param: "+t.getLocalizedMessage());
            }
        }
    }
    
    private GpFilePath initToFile() throws GpFilePathException { 
        //now, create a new group file, add it into the user uploads directory for the given job
        JobInputFileUtil fileUtil = new JobInputFileUtil(gpConfig, jobContext);
        final int index=-1;
        final String pname=param.getParamId().getFqName();
        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, pname, filenameSuffix);
        return gpFilePath;
    }

    public GpFilePath createFilelist() throws ParamListException {
        writeGroupFile(toFile, gpFilePaths);
        return toFile;
    }
    
    public List<GpFilePath> getGpFilePaths() {
        return Collections.unmodifiableList(gpFilePaths);
    }
    
    /**
     * Generate a file list file of all of the input files, including groupId and url as extra columns.
     * By default this writes a file with three columns, (VALUE, GROUP, URL).
     * @param toFile, the output file
     * @param gpFilePaths, the GpFilePath representation of each input file, it must be in the same order as
     *     the values appear in the 'param' instance.
     * @throws Exception
     */
    private void writeGroupFile(final GpFilePath toFile, final List<GpFilePath> gpFilePaths) throws ParamListException {
        writeGroupFile(toFile.getServerFile(), gpFilePaths);
    }
    private void writeGroupFile(final File toFile, final List<GpFilePath> gpFilePaths) throws ParamListException {
        final DefaultParamGroupWriter writer=initParamGroupWriter(baseGpHref, toFile);
        writer.writeParamGroup(groupInfo, param, gpFilePaths);
    }
    
    protected static DefaultParamGroupWriter initParamGroupWriter(final String baseGpHref, final File toFile) {
        final DefaultParamGroupWriter writer=new DefaultParamGroupWriter.Builder(toFile)
            .baseGpHref(baseGpHref)
            .addColumn(Column.VALUE)
            .addColumn(Column.GROUP)
            .addColumn(Column.URL)
            .includeHeader(true)
            .build();
        return writer;
    }
    
    public static class Builder {
        private HibernateSessionManager mgr=null;
        private GpConfig gpConfig=null;
        private GpContext jobContext=null;
        private JobInput jobInput=null;
        private ParameterInfoRecord parameterInfoRecord=null;

        private GroupInfo groupInfo=null;
        private final Param param;
        private String filenameSuffix=".group."+TsvWriter.EXT;
        private boolean downloadExternalFiles=true;
        private GpFilePath toFile=null;

        public Builder(final Param param) {
            this.param=param;
        }
        public Builder mgr(final HibernateSessionManager mgr) {
            this.mgr=mgr;
            return this;
        } 
        public Builder gpConfig(final GpConfig gpConfig) {
            this.gpConfig=gpConfig;
            return this;
        }
        public Builder jobContext(final GpContext jobContext) {
            this.jobContext=jobContext;
            return this;
        }
        public Builder jobInput(final JobInput jobInput) {
            this.jobInput=jobInput;
            return this;
        }
        public Builder parameterInfoRecord(ParameterInfoRecord parameterInfoRecord) {
            this.parameterInfoRecord=parameterInfoRecord;
            return this;
        }
        public Builder groupInfo(final GroupInfo groupInfo) {
            this.groupInfo=groupInfo;
            return this;
        }
        public Builder filenameSuffix(final String filenameSuffix) {
            this.filenameSuffix=filenameSuffix;
            return this;
        }
        public Builder downloadExternalFiles(boolean b) {
            this.downloadExternalFiles=b;
            return this;
        }
        public Builder toFile(final GpFilePath toFile) {
            this.toFile=toFile;
            return this;
        }
        public ParamGroupHelper build() {
            return new ParamGroupHelper(this);
        }
    }
}
