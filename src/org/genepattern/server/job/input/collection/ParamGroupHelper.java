package org.genepattern.server.job.input.collection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.job.input.ParamListHelper.Record;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.job.input.collection.ParamGroupWriter.Column;

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
    
    private final Context jobContext;
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
        if (in.jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (in.param==null) {
            throw new IllegalArgumentException("param==null");
        }
        if (in.groupInfo==null) {
            throw new IllegalArgumentException("groupInfo==null");
        }
        this.jobContext=in.jobContext;
        this.param=in.param;
        this.groupInfo=in.groupInfo;
        this.filenameSuffix=in.filenameSuffix;
        this.downloadExternalFiles=in.downloadExternalFiles;
        try {
            this.gpFilePaths=initFilelist(jobContext, downloadExternalFiles);
        }
        catch (Exception e) {
            log.error(e);
            throw new IllegalArgumentException("Error initializing gpFilePaths for param");
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
    
    private GpFilePath initToFile() throws Exception {
        //now, create a new group file, add it into the user uploads directory for the given job
        JobInputFileUtil fileUtil = new JobInputFileUtil(jobContext);
        final int index=-1;
        final String pname=param.getParamId().getFqName();
        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, pname, filenameSuffix);
        return gpFilePath;
    }

    public GpFilePath createFilelist() throws Exception {
        writeGroupFile(toFile, gpFilePaths);
        return toFile;
    }
    
    public List<GpFilePath> getGpFilePaths() {
        return Collections.unmodifiableList(gpFilePaths);
    }
    
    /**
     * Step through each item in the list of input values, 
     * convert to an appropriate GpFilePath instance,
     * and if necessary and requested, automatically download external data files.
     * @param jobContext
     * @return
     */
    private List<GpFilePath> initFilelist(final Context jobContext, final boolean downloadExternalFiles) 
    throws Exception 
    {
        final List<GpFilePath> gpFilePaths=new ArrayList<GpFilePath>();
        
        for(final ParamValue value : param.getValues()) {
            final Record rec=ParamListHelper.initFromValue(jobContext, value);
            //if necessary, download data from external sites
            if (downloadExternalFiles) {
                if (rec.getType().equals(Record.Type.EXTERNAL_URL)) {
                    ParamListHelper.forFileListCopyExternalUrlToUserUploads(jobContext, rec.getGpFilePath(), rec.getUrl());
                }
            }
            gpFilePaths.add(rec.getGpFilePath()); 
        } 
        return gpFilePaths;
    }
    
    /**
     * Generate a file list file of all of the input files, including groupId and url as extra columns.
     * By default this writes a file with three columns, (VALUE, GROUP, URL).
     * @param toFile, the output file
     * @param gpFilePaths, the GpFilePath representation of each input file, it must be in the same order as
     *     the values appear in the 'param' instance.
     * @throws Exception
     */
    private void writeGroupFile(final GpFilePath toFile, final List<GpFilePath> gpFilePaths) throws Exception {
        writeGroupFile(toFile.getServerFile(), gpFilePaths);
    }
    private void writeGroupFile(final File toFile, final List<GpFilePath> gpFilePaths) throws Exception {
        final DefaultParamGroupWriter writer=new DefaultParamGroupWriter.Builder(toFile)
            .addColumn(Column.VALUE)
            .addColumn(Column.GROUP)
            .addColumn(Column.URL)
            .includeHeader(true)
            .tableWriter(new TsvWriter())
            .build();
        writer.writeParamGroup(groupInfo, param, gpFilePaths);
    }
    
    public static class Builder {
        private Context jobContext=null;
        private GroupInfo groupInfo=null;
        private final Param param;
        private String filenameSuffix=".group."+TsvWriter.EXT;
        private boolean downloadExternalFiles=true;
        private GpFilePath toFile=null;

        public Builder(final Param param) {
            this.param=param;
        }
        public Builder jobContext(final Context jobContext) {
            this.jobContext=jobContext;
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
